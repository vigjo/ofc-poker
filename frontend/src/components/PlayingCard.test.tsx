import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { PlayingCard } from "./PlayingCard";

describe("PlayingCard", () => {
  it("renders the rank and suit as an accessible label", () => {
    render(<PlayingCard card="Ah" />);
    expect(screen.getByRole("button", { name: "A of hearts" })).toBeInTheDocument();
  });

  it("renders ten as '10' instead of 'T'", () => {
    render(<PlayingCard card="Ts" />);
    expect(screen.getByRole("button", { name: "10 of spades" })).toBeInTheDocument();
  });

  it("renders a face-down card when no card is given", () => {
    render(<PlayingCard faceDown />);
    expect(screen.getByLabelText("Face-down card")).toBeInTheDocument();
  });

  it("calls onClick when clicked and reflects selection via aria-pressed", async () => {
    const onClick = vi.fn();
    render(<PlayingCard card="Kc" onClick={onClick} selected />);

    const button = screen.getByRole("button", { name: "K of clubs" });
    expect(button).toHaveAttribute("aria-pressed", "true");

    await userEvent.click(button);
    expect(onClick).toHaveBeenCalledTimes(1);
  });

  it("is not interactive (disabled) without an onClick handler", () => {
    render(<PlayingCard card="2d" />);
    expect(screen.getByRole("button", { name: "2 of diamonds" })).toBeDisabled();
  });
});
