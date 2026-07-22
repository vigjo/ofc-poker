import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { GameBoard } from "./GameBoard";
import type { RoomStateView } from "../api/types";
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

describe("GameBoard", () => {
  beforeEach(() => {
    vi.mocked(api.applyMove).mockReset();
  });

  it("shows the current player's dealt cards when it is their turn", () => {
    render(<GameBoard state={baseState()} seatToken="token" onStateUpdate={vi.fn()} />);
    expect(screen.getByRole("button", { name: "A of hearts" })).toBeInTheDocument();
    expect(screen.getByText(/select a card to place/i)).toBeInTheDocument();
  });

  it("shows a waiting message when it is not the player's turn", () => {
    const state = baseState();
    state.currentActor = "opponent";
    render(<GameBoard state={state} seatToken="token" onStateUpdate={vi.fn()} />);
    expect(screen.getByText(/waiting for other players/i)).toBeInTheDocument();
  });

  it("disables confirm until the required number of cards are placed", async () => {
    const user = userEvent.setup();
    render(<GameBoard state={baseState()} seatToken="token" onStateUpdate={vi.fn()} />);

    const confirmButton = screen.getByRole("button", { name: /confirm move/i });
    expect(confirmButton).toBeDisabled();

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

    expect(confirmButton).toBeEnabled();
  });

  it("submits the move and reports the updated state on confirm", async () => {
    const user = userEvent.setup();
    const onStateUpdate = vi.fn();
    const updated = baseState();
    vi.mocked(api.applyMove).mockResolvedValue(updated);

    render(<GameBoard state={baseState()} seatToken="seat-token" onStateUpdate={onStateUpdate} />);

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

    await user.click(screen.getByRole("button", { name: /confirm move/i }));

    expect(api.applyMove).toHaveBeenCalledWith("ROOM01", "seat-token", expect.any(Array), null);
    expect(onStateUpdate).toHaveBeenCalledWith(updated);
  });
});
