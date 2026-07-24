import { expect, test, type Page } from '@playwright/test';

export async function loginAsAdmin(page: Page) {
  await page.goto('./login');
  await page.getByTestId('login-username').fill('admin');
  await page.getByTestId('login-password').fill('123456');
  await page.getByTestId('login-submit').click();
  await expect(page).toHaveURL(/\/projects\/?$/);
  await expect(page.getByRole('heading', { name: 'Projetos' })).toBeVisible();
}

/** Opens the first project that already has patents (title contains "Revisao" preferred). */
export async function openProjectWithPatents(page: Page) {
  await loginAsAdmin(page);
  const table = page.getByTestId('projects-table');
  await expect(table).toBeVisible();

  const revisao = page.getByRole('link', { name: /Projeto Revisao/i });
  if (await revisao.count()) {
    await revisao.first().click();
  } else {
    // Fall back: open first project link in the table
    await table.locator('tbody a').first().click();
  }
  await expect(page).toHaveURL(/\/projects\/[^/]+\/patents/);
  await expect(page.getByTestId('patents-table')).toBeVisible({ timeout: 20_000 });
}
