import { expect, test } from '@playwright/test';
import { openProjectWithPatents } from './helpers';

test.describe('patents', () => {
  test('abre projeto com patentes, verifica tabela e detalhe', async ({ page }) => {
    await openProjectWithPatents(page);

    const table = page.getByTestId('patents-table');
    await expect(table.locator('tbody tr').first()).toBeVisible();

    const firstLink = table.locator('tbody a').first();
    const title = (await firstLink.textContent())?.trim() || '';
    await firstLink.click();

    await expect(page).toHaveURL(/\/patents\/[^/]+$/);
    await expect(page.getByTestId('patent-detail-title')).toBeVisible();
    if (title && title !== '(sem título)') {
      await expect(page.locator('#title')).toHaveValue(title);
    }
  });
});
