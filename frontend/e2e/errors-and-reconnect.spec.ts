import { expect, test } from "@playwright/test";
import { createRoom, joinRoom, playTurnIfActive, startGame, waitForActivePage } from "./helpers";

test("joining with an unknown invite code shows a friendly error", async ({ page }) => {
  await page.goto("/");
  await page.getByRole("tab", { name: "Join with a code" }).click();
  await page.getByLabel("Display name").fill("Nobody");
  await page.getByLabel("Invite code").fill("ZZZZZZ");
  await page.getByRole("button", { name: "Join room" }).click();

  await expect(page.getByRole("alert")).toBeVisible();
});

test("the confirm button stays disabled until a full, legal move is drafted", async ({
  browser,
}) => {
  const hostContext = await browser.newContext();
  const guestContext = await browser.newContext();
  const hostPage = await hostContext.newPage();
  const guestPage = await guestContext.newPage();

  try {
    const roomId = await createRoom(hostPage, "Alice");
    await joinRoom(guestPage, roomId, "Bob");
    await startGame(hostPage);

    const activePage = await waitForActivePage([hostPage, guestPage]);
    const confirmButton = activePage.locator(".game-board__confirm");
    await expect(confirmButton).toBeDisabled();

    // Placing a single card of the required five is not enough to submit.
    await activePage.locator(".game-board__hand-cards button").first().click();
    await activePage.locator(".game-board__you .player-board__row--clickable").first().click();
    await expect(confirmButton).toBeDisabled();
  } finally {
    await hostContext.close();
    await guestContext.close();
  }
});

test("refreshing mid-game reconnects to the same seat and game state", async ({ browser }) => {
  const hostContext = await browser.newContext();
  const guestContext = await browser.newContext();
  const hostPage = await hostContext.newPage();
  const guestPage = await guestContext.newPage();

  try {
    const roomId = await createRoom(hostPage, "Alice");
    await joinRoom(guestPage, roomId, "Bob");
    await startGame(hostPage);

    // Whoever is up first plays a turn so there is in-progress state to
    // recover, then reloads before the *other* player has acted.
    const activePage = await waitForActivePage([hostPage, guestPage]);
    await playTurnIfActive(activePage);

    await hostPage.reload();
    await expect(hostPage.locator(".game-board, .lobby, .hand-result")).toBeVisible();
    // The host should land back in the same room without re-entering a
    // display name - the join form must not reappear.
    await expect(hostPage.getByLabel("Display name")).toHaveCount(0);
    await expect(hostPage).toHaveURL(new RegExp(`/room/${roomId}$`));
  } finally {
    await hostContext.close();
    await guestContext.close();
  }
});
