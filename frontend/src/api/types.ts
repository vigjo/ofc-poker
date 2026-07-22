import type { components } from "./schema";

export type Card = components["schemas"]["Card"];
export type Row = components["schemas"]["Row"];
export type Street = components["schemas"]["Street"];
export type RoomStatus = components["schemas"]["RoomStatus"];
export type PlayerView = components["schemas"]["PlayerView"];
export type RoomStateView = components["schemas"]["RoomStateView"];
export type SeatCredentials = components["schemas"]["SeatCredentials"];
export type PlacementDto = components["schemas"]["PlacementDto"];
export type MoveRequest = components["schemas"]["MoveRequest"];
export type PlayerResult = components["schemas"]["PlayerResult"];
export type HandResult = components["schemas"]["HandResult"];

export const ROWS: Row[] = ["TOP", "MIDDLE", "BOTTOM"];

export const ROW_CAPACITY: Record<Row, number> = {
  TOP: 3,
  MIDDLE: 5,
  BOTTOM: 5,
};

export function cardRank(card: Card): string {
  return card.slice(0, -1);
}

export function cardSuit(card: Card): string {
  return card.slice(-1);
}

export function cardsEqual(a: Card, b: Card): boolean {
  return a === b;
}
