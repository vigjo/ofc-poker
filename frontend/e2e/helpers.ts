import type { Page } from "@playwright/test";

/** Creates a room from the home page and waits for the lobby to render. */
export async function createRoom(page: Page, displayName: string): Promise<string> {
  await page.goto("/");
  await page.getByLabel("Display name").fill(displayName);
  await page.getByRole("button", { name: "Create room" }).click();
  await page.waitForURL(/\/room\/[A-Z0-9]+$/);
  await page.getByText("Invite code:").waitFor();
  const roomId = new URL(page.url()).pathname.split("/").pop();
  if (!roomId) {
    throw new Error("Room id missing from URL after create");
  }
  return roomId;
}

/** Joins an existing room by invite code and waits for the lobby to render. */
export async function joinRoom(page: Page, roomId: string, displayName: string): Promise<void> {
  await page.goto("/");
  await page.getByRole("tab", { name: "Join with a code" }).click();
  await page.getByLabel("Display name").fill(displayName);
  await page.getByLabel("Invite code").fill(roomId);
  await page.getByRole("button", { name: "Join room" }).click();
  await page.waitForURL(new RegExp(`/room/${roomId}$`));
}

/** Host-only: starts the game once every seat has joined. */
export async function startGame(hostPage: Page): Promise<void> {
  await hostPage.getByRole("button", { name: "Start game" }).click();
}

/**
 * Plays one full turn (place + confirm; leftover auto-discarded) on this page if
 * it is currently this player's turn. Returns whether a turn was played, so
 * the caller can poll both players' pages until the hand ends.
 */
export async function playTurnIfActive(page: Page): Promise<boolean> {
  const handSection = page.locator(".game-board__hand");
  if (!(await handSection.isVisible().catch(() => false))) {
    return false;
  }

  const cardButtons = page.locator(".game-board__hand-cards button");
  const pendingCount = await cardButtons.count();
  if (pendingCount === 0) {
    return false;
  }
  const placeCount = pendingCount === 5 || pendingCount === 13 ? pendingCount : pendingCount - 1;
  const rows = page.locator(".game-board__you .player-board__row--clickable");

  for (let i = 0; i < placeCount; i++) {
    await cardButtons.first().click();
    await rows.first().click();
  }

  await page.locator(".game-board__confirm").click();
  return true;
}

/** Waits for and returns whichever page currently has an active turn. */
export async function waitForActivePage(pages: Page[], timeoutMs = 10_000): Promise<Page> {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    for (const page of pages) {
      if (
        await page
          .locator(".game-board__hand")
          .isVisible()
          .catch(() => false)
      ) {
        return page;
      }
    }
    await pages[0]!.waitForTimeout(200);
  }
  throw new Error("No page became active within the timeout");
}

/** Drives a full hand to completion by alternately polling both players' pages. */
export async function playHandToCompletion(pages: Page[], maxRounds = 40): Promise<void> {
  for (let round = 0; round < maxRounds; round++) {
    for (const page of pages) {
      if (
        await page
          .getByRole("heading", { name: "Hand complete" })
          .isVisible()
          .catch(() => false)
      ) {
        return;
      }
      await playTurnIfActive(page);
    }
  }
  throw new Error(`Hand did not complete within ${maxRounds} rounds`);
}
