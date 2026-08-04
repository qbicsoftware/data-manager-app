import {firefox} from 'playwright';

const browser = await firefox.launch({ headless: true });
try {
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } });
  await page.goto('http://localhost:8080/dev/test-view', { waitUntil: 'networkidle', timeout: 30000 });
  await page.waitForTimeout(4000);

  // Scroll down to find Toast Notifications section
  await page.evaluate(() => new Promise(r => {
    const el = [...document.querySelectorAll('div')].find(d => d.textContent.includes('Success Toast'));
    if (el) el.scrollIntoView({ block: 'center' });
    setTimeout(r, 1000);
  }));
  await page.waitForTimeout(500);

  // Screenshot 1: buttons visible
  await page.screenshot({ path: 'toast-page.png', fullPage: false });

  // Click success toast
  const successBtn = await page.locator('button:has-text("Success Toast")').first();
  if (await successBtn.count()) { await successBtn.click(); await page.waitForTimeout(800); }

  // Click error toast
  const errorBtn = await page.locator('button:has-text("Error Toast")').first();
  if (await errorBtn.count()) { await errorBtn.click(); await page.waitForTimeout(800); }

  // Click info toast
  const infoBtn = await page.locator('button:has-text("Info Toast + Route")').first();
  if (await infoBtn.count()) { await infoBtn.click(); await page.waitForTimeout(800); }

  // Click error+retry
  const retryBtn = await page.locator('button:has-text("Error + Retry")').first();
  if (await retryBtn.count()) { await retryBtn.click(); await page.waitForTimeout(800); }

  // Click progress toast
  const progressBtn = await page.locator('button:has-text("Pending Task (Progress)")').first();
  if (await progressBtn.count()) { await progressBtn.click(); await page.waitForTimeout(800); }

  // Screenshot 2: toasts displayed
  await page.screenshot({ path: 'toast-toasts.png', fullPage: false });

  console.log('Saved: toast-page.png, toast-toasts.png');
} finally { await browser.close(); }
