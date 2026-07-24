import { expect, test } from '@playwright/test';
import { openProjectWithPatents } from './helpers';

test.describe('reports', () => {
  test('abre relatórios, renderiza gráfico/tabela e troca de relatório', async ({ page }) => {
    await openProjectWithPatents(page);

    await page.getByRole('main').getByRole('link', { name: 'Relatórios' }).click();
    await expect(page).toHaveURL(/\/reports\/?$/);
    await expect(page.getByRole('heading', { name: 'Relatórios' })).toBeVisible();

    await expect(page.getByTestId('report-chart')).toBeVisible();
    await expect(page.getByTestId('report-table')).toBeVisible();
    await expect(page.getByTestId('report-table').locator('tbody tr').first()).toBeVisible();

    await page.getByTestId('report-select').selectOption('main-inventor');
    await expect(page.getByTestId('report-chart')).toBeVisible();
    await expect(page.getByTestId('report-table').locator('tbody tr').first()).toBeVisible();
  });
});
