import { test, expect } from "@playwright/test";
import { createRoom, joinRoom, playHandToCompletion, playTurnIfActive, waitForActivePage, startGame } from "./helpers";
test("three players complete a hand and play the next", async ({ browser, baseURL }) => {
  test.setTimeout(120000);
  const pages = await Promise.all([1440,390,390].map(width => browser.newPage({baseURL,viewport:{width,height:900}})));
  try {
    const [host,guest,third] = pages;
    const code=await createRoom(host!,"Host");
    await joinRoom(guest!,code,"Guest");
    await joinRoom(third!,code,"Third");
    await expect(host!.getByText("Third",{exact:true})).toBeVisible();
    await startGame(host!);
    await playTurnIfActive(await waitForActivePage(pages));
    await guest!.reload();
    await expect(guest!.locator(".game-board")).toBeVisible();
    await playHandToCompletion(pages,120);
    const scores=await host!.locator(".hand-result__table").innerText();
    for(const page of pages) await expect(page.locator(".hand-result__table")).toHaveText(scores,{useInnerText:true});
    await host!.getByRole("button",{name:"Start next hand"}).click();
    await playTurnIfActive(await waitForActivePage(pages));
  } finally { await Promise.all(pages.map(p=>p.close())); }
});
