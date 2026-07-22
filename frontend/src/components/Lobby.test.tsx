import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { Lobby } from "./Lobby";
import type { RoomStateView } from "../api/types";
import { api } from "../api/client";

vi.mock("../api/client", () => ({
  api: { startGame: vi.fn() },
  ApiError: class ApiError extends Error {
    status: number;
    constructor(message: string, status: number) {
      super(message);
      this.status = status;
    }
  },
}));

function waitingRoom(overrides: Partial<RoomStateView> = {}): RoomStateView {
  return {
    roomId: "ROOM01",
    inviteCode: "ROOM01",
    version: 1,
    status: "WAITING",
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
        pendingCount: 0,
        pendingCards: undefined,
        isYou: true,
      },
    ],
    lastHandResult: undefined,
    runningTotals: { me: 0 },
    ...overrides,
  };
}

describe("Lobby", () => {
  beforeEach(() => {
    vi.mocked(api.startGame).mockReset();
  });

  it("disables start for the host until there are at least two players", () => {
    render(<Lobby state={waitingRoom()} seatToken="token" onStateUpdate={vi.fn()} />);
    expect(screen.getByRole("button", { name: /start game/i })).toBeDisabled();
    expect(screen.getByText(/need 2-3 players/i)).toBeInTheDocument();
  });

  it("enables start for the host once a second player joins", async () => {
    const state = waitingRoom();
    state.players = [
      ...state.players!,
      {
        playerId: "bob",
        displayName: "Bob",
        seatIndex: 1,
        host: false,
        connected: true,
        fantasyland: false,
        top: [],
        middle: [],
        bottom: [],
        pendingCount: 0,
        pendingCards: undefined,
        isYou: false,
      },
    ];
    const onStateUpdate = vi.fn();
    const startedState = waitingRoom({ status: "IN_PROGRESS" });
    vi.mocked(api.startGame).mockResolvedValue(startedState);

    render(<Lobby state={state} seatToken="token" onStateUpdate={onStateUpdate} />);
    const startButton = screen.getByRole("button", { name: /start game/i });
    expect(startButton).toBeEnabled();

    await userEvent.click(startButton);
    expect(api.startGame).toHaveBeenCalledWith("ROOM01", "token");
    expect(onStateUpdate).toHaveBeenCalledWith(startedState);
  });

  it("shows a waiting message for non-host players", () => {
    const state = waitingRoom();
    state.players = [{ ...state.players![0]!, host: true, isYou: false }];
    render(<Lobby state={state} seatToken="token" onStateUpdate={vi.fn()} />);
    expect(screen.getByText(/waiting for the host to start/i)).toBeInTheDocument();
  });
});
