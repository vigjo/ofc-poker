import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { GameBoard } from "./GameBoard";
import type { Card, RoomStateView } from "../api/types";
import { api } from "../api/client";

vi.mock("../api/client", () => ({
  api: { applyMove: vi.fn() },
  ApiError: class ApiError extends Error {
    status: number;
    constructor(message: string, status: number) {
      super(message);
      this.status = status;
    }
  },
}));

const FL_CARDS: Card[] = [
  "Ah",
  "Kh",
  "Qh",
  "Jh",
  "Th",
  "9h",
  "8h",
  "7h",
  "6h",
  "5h",
  "4h",
  "3h",
  "2h",
];

function baseState(): RoomStateView {
  return {
    roomId: "ROOM01",
    inviteCode: "ROOM01",
    version: 3,
    status: "IN_PROGRESS",
    currentActor: "me",
    street: "INITIAL",
    players: [
      {
        playerId: "me",
        displayName: "Alice",
        seatIndex: 0,
        host: true,
        connected: true,
        fantasyland: false,
        top: [],
        middle: [],
        bottom: [],
        pendingCount: 5,
        pendingCards: ["Ah", "Kd", "Qs", "Jc", "2h"],
        isYou: true,
      },
      {
        playerId: "opponent",
        displayName: "Bob",
        seatIndex: 1,
        host: false,
        connected: true,
        fantasyland: false,
        top: [],
        middle: [],
        bottom: [],
        pendingCount: 5,
        pendingCards: undefined,
        isYou: false,
      },
    ],
    lastHandResult: undefined,
    runningTotals: { me: 0, opponent: 0 },
  };
}

function streetState(): RoomStateView {
  const state = baseState();
  state.street = "STREET_1";
  state.players![0] = {
    ...state.players![0]!,
    top: ["Ah"],
    middle: ["Kd", "Qs"],
    bottom: ["Jc", "2h"],
    pendingCount: 3,
    pendingCards: ["9c", "8d", "7s"],
  };
  return state;
}

function fantasylandState(overrides?: Partial<RoomStateView>): RoomStateView {
  const state = baseState();
  state.currentActor = "opponent";
  state.street = "INITIAL";
  state.players![0] = {
    ...state.players![0]!,
    fantasyland: true,
    pendingCount: 13,
    pendingCards: FL_CARDS,
  };
  state.players![1] = {
    ...state.players![1]!,
    pendingCount: 5,
  };
  return { ...state, ...overrides };
}

async function placeAllInitialCards(user: ReturnType<typeof userEvent.setup>) {
  for (const rank of [
    "A of hearts",
    "K of diamonds",
    "Q of spades",
    "J of clubs",
    "2 of hearts",
  ]) {
    await user.click(screen.getByRole("button", { name: rank }));
    const rows = screen.getAllByRole("button", { name: /Place selected card in .* row/i });
    await user.click(rows[0]!);
  }
}

describe("GameBoard", () => {
  beforeEach(() => {
    vi.mocked(api.applyMove).mockReset();
  });

  it("shows the current player's dealt cards when it is their turn", () => {
    render(<GameBoard state={baseState()} seatToken="token" onStateUpdate={vi.fn()} />);
    expect(screen.getByRole("button", { name: "A of hearts" })).toBeInTheDocument();
    expect(screen.getByText(/press a number to select a card/i)).toBeInTheDocument();
  });

  it("shows a waiting message when it is not the player's turn", () => {
    const state = baseState();
    state.currentActor = "opponent";
    render(<GameBoard state={state} seatToken="token" onStateUpdate={vi.fn()} />);
    expect(screen.getByText(/waiting for other players/i)).toBeInTheDocument();
  });

  it("shows fantasyland cards even when another player is currentActor", () => {
    render(<GameBoard state={fantasylandState()} seatToken="token" onStateUpdate={vi.fn()} />);
    expect(screen.getByRole("button", { name: "A of hearts" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "2 of hearts" })).toBeInTheDocument();
    expect(screen.queryByText(/waiting for other players/i)).not.toBeInTheDocument();
    expect(document.querySelectorAll(".game-board__hand-cards button").length).toBe(13);
  });

  it("shows fantasyland cards when currentActor is null (all-FL)", () => {
    render(
      <GameBoard
        state={fantasylandState({ currentActor: undefined })}
        seatToken="token"
        onStateUpdate={vi.fn()}
      />,
    );
    expect(screen.getByLabelText("Your dealt cards")).toBeInTheDocument();
    expect(screen.queryByText(/waiting for other players/i)).not.toBeInTheDocument();
  });

  it("disables confirm until the required number of cards are placed", async () => {
    const user = userEvent.setup();
    render(<GameBoard state={baseState()} seatToken="token" onStateUpdate={vi.fn()} />);

    const confirmButton = screen.getByRole("button", { name: /confirm move/i });
    expect(confirmButton).toBeDisabled();

    await placeAllInitialCards(user);

    expect(confirmButton).toBeEnabled();
  });

  it("submits the move and reports the updated state on confirm", async () => {
    const user = userEvent.setup();
    const onStateUpdate = vi.fn();
    const updated = baseState();
    vi.mocked(api.applyMove).mockResolvedValue(updated);

    render(<GameBoard state={baseState()} seatToken="seat-token" onStateUpdate={onStateUpdate} />);

    await placeAllInitialCards(user);
    await user.click(screen.getByRole("button", { name: /confirm move/i }));

    expect(api.applyMove).toHaveBeenCalledWith("ROOM01", "seat-token", expect.any(Array), null);
    expect(onStateUpdate).toHaveBeenCalledWith(updated);
  });

  it("enables confirm on a street after placing two cards without an explicit discard", async () => {
    const user = userEvent.setup();
    render(<GameBoard state={streetState()} seatToken="token" onStateUpdate={vi.fn()} />);

    const confirmButton = screen.getByRole("button", { name: /confirm move/i });
    expect(confirmButton).toBeDisabled();

    await user.click(screen.getByRole("button", { name: "9 of clubs" }));
    await user.click(screen.getAllByRole("button", { name: /Place selected card in .* row/i })[0]!);
    await user.click(screen.getByRole("button", { name: "8 of diamonds" }));
    await user.click(screen.getAllByRole("button", { name: /Place selected card in .* row/i })[0]!);

    expect(screen.getByText(/will discard/i)).toBeInTheDocument();
    expect(confirmButton).toBeEnabled();
  });

  it("auto-sends the leftover card as discard on street confirm", async () => {
    const user = userEvent.setup();
    const onStateUpdate = vi.fn();
    vi.mocked(api.applyMove).mockResolvedValue(streetState());

    render(
      <GameBoard state={streetState()} seatToken="seat-token" onStateUpdate={onStateUpdate} />,
    );

    await user.click(screen.getByRole("button", { name: "9 of clubs" }));
    await user.click(screen.getAllByRole("button", { name: /Place selected card in .* row/i })[0]!);
    await user.click(screen.getByRole("button", { name: "8 of diamonds" }));
    await user.click(screen.getAllByRole("button", { name: /Place selected card in .* row/i })[0]!);
    await user.click(screen.getByRole("button", { name: /confirm move/i }));

    expect(api.applyMove).toHaveBeenCalledWith(
      "ROOM01",
      "seat-token",
      [
        { card: "9c", row: "TOP" },
        { card: "8d", row: "TOP" },
      ],
      "7s",
    );
    expect(onStateUpdate).toHaveBeenCalled();
  });

  it("selects and places cards with digit hotkeys", async () => {
    const user = userEvent.setup();
    render(<GameBoard state={streetState()} seatToken="token" onStateUpdate={vi.fn()} />);

    await user.keyboard("2");
    const hand = () => screen.getByLabelText("Your dealt cards");
    expect(
      within(hand()).getByRole("button", { name: "8 of diamonds" }),
    ).toHaveAttribute("aria-pressed", "true");

    await user.keyboard("1");
    expect(screen.getByText(/8d → top/i)).toBeInTheDocument();
    expect(
      within(hand()).queryByRole("button", { name: "8 of diamonds" }),
    ).not.toBeInTheDocument();
  });

  it("undoes the last draft placement with Backspace", async () => {
    const user = userEvent.setup();
    render(<GameBoard state={streetState()} seatToken="token" onStateUpdate={vi.fn()} />);

    await user.keyboard("1");
    await user.keyboard("1");
    expect(screen.getByText(/9c → top/i)).toBeInTheDocument();

    await user.keyboard("{Backspace}");
    expect(screen.queryByText(/9c → top/i)).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: "9 of clubs" })).toBeInTheDocument();
  });

  it("confirms with Enter when the move is ready, including auto-discard", async () => {
    const user = userEvent.setup();
    vi.mocked(api.applyMove).mockResolvedValue(streetState());

    render(<GameBoard state={streetState()} seatToken="seat-token" onStateUpdate={vi.fn()} />);

    // After each placement, remaining hand cards re-index from 1.
    await user.keyboard("1");
    await user.keyboard("1");
    await user.keyboard("1");
    await user.keyboard("1");
    await user.keyboard("{Enter}");

    expect(api.applyMove).toHaveBeenCalledWith(
      "ROOM01",
      "seat-token",
      [
        { card: "9c", row: "TOP" },
        { card: "8d", row: "TOP" },
      ],
      "7s",
    );
  });

  it("shows hotkey badges on hand cards", () => {
    render(<GameBoard state={streetState()} seatToken="token" onStateUpdate={vi.fn()} />);
    const handCards = document.querySelector(".game-board__hand-cards")!;
    expect(within(handCards as HTMLElement).getByText("1")).toBeInTheDocument();
    expect(within(handCards as HTMLElement).getByText("2")).toBeInTheDocument();
    expect(within(handCards as HTMLElement).getByText("3")).toBeInTheDocument();
  });
});
