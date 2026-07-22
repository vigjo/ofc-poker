import { expect, test } from "@playwright/test";
import { createRoom, joinRoom, playHandToCompletion, startGame } from "./helpers";

test("two players can create, join, and complete a full Pineapple OFC hand", async ({
  browser,
}) => {
  const hostContext = await browser.newContext();
  const guestContext = await browser.newContext();
  const hostPage = await hostContext.newPage();
  const guestPage = await guestContext.newPage();

  try {
    const roomId = await createRoom(hostPage, "Alice");
    await joinRoom(guestPage, roomId, "Bob");

    // Host's lobby should reflect the second seat joining over the socket
    // without a manual refresh.
    await expect(hostPage.getByText("Bob")).toBeVisible();

    await startGame(hostPage);
    await expect(hostPage.locator(".game-board")).toBeVisible();
    await expect(guestPage.locator(".game-board")).toBeVisible();

    await playHandToCompletion([hostPage, guestPage]);

    await expect(hostPage.getByRole("heading", { name: "Hand complete" })).toBeVisible();
    await expect(guestPage.getByRole("heading", { name: "Hand complete" })).toBeVisible();

    // The scoring table should list both players by name.
    await expect(hostPage.locator(".hand-result__table")).toContainText("Alice");
    await expect(hostPage.locator(".hand-result__table")).toContainText("Bob");
  } finally {
    await hostContext.close();
    await guestContext.close();
  }
});
