import { chromium } from 'playwright';

const browser = await chromium.launch();
const page = await browser.newPage();
await page.goto('http://localhost:8080/dev/login?logout=true');
await page.waitForTimeout(3000);

// Check if drawer exists and its attributes
const drawerInfo = await page.evaluate(() => {
  const appLayout = document.querySelector('vaadin-app-layout');
  if (!appLayout) return { found: false };
  
  return {
    found: true,
    drawerOpened: appLayout.getAttribute('drawer-opened'),
    hasDrawerSlot: appLayout.querySelector('[slot="drawer"]') !== null,
    drawerSlotContent: appLayout.querySelector('[slot="drawer"]')?.innerHTML?.substring(0, 200),
    drawerPart: appLayout.shadowRoot?.querySelector('[part="drawer"]')?.outerHTML?.substring(0, 200),
    classes: appLayout.className,
    attributes: Array.from(appLayout.attributes).map(a => `${a.name}=${a.value}`)
  };
});

console.log(JSON.stringify(drawerInfo, null, 2));
await browser.close();
