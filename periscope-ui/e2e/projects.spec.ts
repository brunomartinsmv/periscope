import { expect, test } from '@playwright/test';
import { loginAsAdmin } from './helpers';

test.describe('projects', () => {
  test('cria projeto com título único, aparece na lista e exclui', async ({ page }) => {
    const title = `E2E Projeto ${Date.now()}`;

    await loginAsAdmin(page);

    page.once('dialog', (dialog) => dialog.accept());

    await page.getByTestId('project-create').click();
    await expect(page.getByTestId('project-form')).toBeVisible();
    await page.getByTestId('project-title').fill(title);
    await page.getByTestId('project-description').fill('criado pelo Playwright');
    await page.getByTestId('project-save').click();

    await expect(page.getByTestId('projects-table').getByRole('link', { name: title })).toBeVisible();

    const row = page.getByTestId('projects-table').locator('tr', { hasText: title });
    await row.getByRole('button', { name: 'Excluir' }).click();

    await expect(page.getByTestId('projects-table').getByRole('link', { name: title })).toHaveCount(0);
  });
});
