import { expect, test } from '@playwright/test';
import { openProjectWithPatents } from './helpers';

test.describe('harmonization', () => {
  test('busca sugestão ACME GMBH e verifica que aparece', async ({ page }) => {
    await openProjectWithPatents(page);

    const url = page.url();
    const match = url.match(/\/projects\/([^/]+)\/patents/);
    expect(match).toBeTruthy();
    await page.goto(`./projects/${match![1]}/harmonization`);

    await expect(page.getByRole('heading', { name: 'Harmonização' })).toBeVisible();

    await page.getByTestId('harmonization-query').fill('ACME GMBH');
    await expect(page.getByTestId('suggestions-list')).toBeVisible();
    await expect(page.getByTestId('suggestions-list')).toContainText('ACME GMBH');
  });
});
