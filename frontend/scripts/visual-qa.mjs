import { chromium } from 'playwright-core';
import { mkdir } from 'node:fs/promises';
import { join } from 'node:path';
import { tmpdir } from 'node:os';

const baseUrl = 'http://127.0.0.1:4200';
const output = join(tmpdir(), 'document-shield-qa');
await mkdir(output, { recursive: true });

const browser = await chromium.launch({
  executablePath: 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',
  headless: true,
});
const errors = [];
const results = [];
const documents = await fetch('http://127.0.0.1:8080/api/v1/documentos?size=1').then((response) =>
  response.json(),
);
const explorerDocumentId = documents.content?.[0]?.id;

async function inspectViewport(name, width, height) {
  const page = await browser.newPage({ viewport: { width, height }, deviceScaleFactor: 1 });
  page.on('console', (message) => {
    if (message.type() === 'error') errors.push(`${name} console: ${message.text()}`);
  });
  page.on('pageerror', (error) => errors.push(`${name} page: ${error.message}`));

  await page.goto(`${baseUrl}/dashboard`, { waitUntil: 'networkidle' });
  const title = await page.title();
  const meaningful = await page.getByRole('heading', { name: 'Visão geral' }).isVisible();
  const documentWidth = await page.evaluate(() => document.documentElement.scrollWidth);
  const horizontalOverflow = documentWidth > width;
  await page.screenshot({ path: join(output, `dashboard-${name}.png`), fullPage: true });

  let explorerVisible = false;
  let explorerOverflow = false;
  if (explorerDocumentId) {
    await page.goto(`${baseUrl}/documentos/${explorerDocumentId}/explorer`, {
      waitUntil: 'networkidle',
    });
    explorerVisible = await page.getByRole('heading', { name: /\.pdf$/i }).isVisible();
    explorerOverflow = (await page.evaluate(() => document.documentElement.scrollWidth)) > width;
    await page.screenshot({ path: join(output, `explorer-${name}.png`), fullPage: true });
  }

  if (width <= 760) {
    await page.goto(`${baseUrl}/dashboard`, { waitUntil: 'networkidle' });
    await page.getByRole('button', { name: 'Abrir menu' }).click();
    const menuVisible = await page.getByRole('navigation').isVisible();
    await page.screenshot({ path: join(output, `navigation-${name}.png`), fullPage: true });
    await page.locator('.scrim').click({ position: { x: width - 10, y: 100 } });
    results.push({
      name,
      width,
      height,
      title,
      meaningful,
      horizontalOverflow,
      menuVisible,
      explorerVisible,
      explorerOverflow,
    });
  } else {
    await page.goto(`${baseUrl}/documentos`, { waitUntil: 'networkidle' });
    const documentsVisible = await page
      .getByRole('heading', { name: 'Documentos', exact: true })
      .isVisible();
    await page.screenshot({ path: join(output, `documents-${name}.png`), fullPage: true });
    await page.goto(`${baseUrl}/analises`, { waitUntil: 'networkidle' });
    const analysesVisible = await page
      .getByRole('heading', { name: 'Análises', exact: true })
      .isVisible();
    results.push({
      name,
      width,
      height,
      title,
      meaningful,
      horizontalOverflow,
      documentsVisible,
      analysesVisible,
      explorerVisible,
      explorerOverflow,
    });
  }
  await page.close();
}

await inspectViewport('1920x1080', 1920, 1080);
await inspectViewport('1366x768', 1366, 768);
await inspectViewport('390x844', 390, 844);

console.log(JSON.stringify({ output, errors, results }));
await browser.close();
