import { expect, test } from '@playwright/test';

test.describe('login', () => {
  test('admin / 123456 chega em /projects', async ({ page }) => {
    await page.goto('./login');
    await page.getByTestId('login-username').fill('admin');
    await page.getByTestId('login-password').fill('123456');
    await page.getByTestId('login-submit').click();
    await expect(page).toHaveURL(/\/projects\/?$/);
    await expect(page.getByRole('heading', { name: 'Projetos' })).toBeVisible();
  });

  test('credencial inválida mostra erro e permanece no login', async ({ page }) => {
    await page.goto('./login');
    await page.getByTestId('login-username').fill('admin');
    await page.getByTestId('login-password').fill('senha-errada');
    await page.getByTestId('login-submit').click();
    await expect(page.getByTestId('error-alert')).toContainText(/inválid/i);
    await expect(page).toHaveURL(/\/login/);
    await expect(page.getByTestId('login-submit')).toBeVisible();
  });
});
