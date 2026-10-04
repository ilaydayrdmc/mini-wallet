import { expect, test } from '@playwright/test'

const PASSWORD = 'gizli-sifre-123'

// Her test kendine ozgu kullanici adi kullanir; testler birbirini etkilemez
let counter = 0
const uniqueName = (prefix) => `${prefix}.${Date.now().toString(36)}${counter++}`

// ---- yardimcilar ----

/** API uzerinden kullanici olusturur ve giris yapar; token dondurur. */
async function apiSignUp(request, username) {
  const credentials = { username, password: PASSWORD }
  const registered = await request.post('/api/auth/register', { data: credentials })
  expect(registered.status()).toBe(201)
  const login = await request.post('/api/auth/login', { data: credentials })
  return (await login.json()).accessToken
}

async function apiCreateAccount(request, token, ownerName) {
  const res = await request.post('/api/accounts', {
    data: { ownerName },
    headers: { Authorization: `Bearer ${token}` },
  })
  expect(res.status()).toBe(201)
  return (await res.json()).id
}

async function apiBalance(request, token, accountId) {
  const res = await request.get('/api/accounts', { headers: { Authorization: `Bearer ${token}` } })
  const account = (await res.json()).find((a) => a.id === accountId)
  return account.balance
}

/** Arayuzden kayit olur (kayittan sonra otomatik giris yapilir). */
async function uiSignUp(page, username) {
  await page.goto('/')
  await page.getByRole('button', { name: 'Kayit ol' }).click()
  await page.getByLabel('Kullanici adi').fill(username)
  await page.getByLabel('Sifre').fill(PASSWORD)
  await page.locator('button[type="submit"]').click()
  await expect(page.getByRole('button', { name: 'Cikis' })).toBeVisible()
}

async function uiCreateAccount(page, name) {
  await page.getByPlaceholder('Hesap sahibinin adi').fill(name)
  await page.getByRole('button', { name: 'Hesap ac' }).click()
  const row = page.getByRole('row', { name: new RegExp(name) })
  await expect(row).toBeVisible()
  return row
}

// ---- testler ----

test.describe('giris ve oturum', () => {
  test('token yokken giris ekrani acilir', async ({ page }) => {
    await page.goto('/')
    await expect(page.getByRole('heading', { name: 'Giris yap' })).toBeVisible()
  })

  test('yanlis sifrede backend hatasi gosterilir', async ({ page }) => {
    await page.goto('/')
    await page.getByLabel('Kullanici adi').fill('olmayan.kullanici')
    await page.getByLabel('Sifre').fill('yanlis-sifre-1')
    await page.locator('button[type="submit"]').click()
    await expect(page.getByText('Kullanici adi veya sifre hatali')).toBeVisible()
  })

  test('kayit, yenileme, cikis ve yeniden giris', async ({ page }) => {
    const username = uniqueName('oturum')
    await uiSignUp(page, username)
    await expect(page.getByText(username)).toBeVisible()
    await expect(page.getByText('Henuz hesap yok')).toBeVisible()

    await page.reload()
    await expect(page.getByText(username)).toBeVisible() // oturum surer

    await page.getByRole('button', { name: 'Cikis' }).click()
    await expect(page.getByRole('heading', { name: 'Giris yap' })).toBeVisible()
    expect(await page.evaluate(() => localStorage.getItem('wallet.token'))).toBeNull()

    await page.getByLabel('Kullanici adi').fill(username)
    await page.getByLabel('Sifre').fill(PASSWORD)
    await page.locator('button[type="submit"]').click()
    await expect(page.getByText(username)).toBeVisible()
  })

  test('ayni kullanici adiyla ikinci kayit reddedilir', async ({ page, request }) => {
    const username = uniqueName('tekrar')
    await apiSignUp(request, username)

    await page.goto('/')
    await page.getByRole('button', { name: 'Kayit ol' }).click()
    await page.getByLabel('Kullanici adi').fill(username.toUpperCase())
    await page.getByLabel('Sifre').fill(PASSWORD)
    await page.locator('button[type="submit"]').click()
    await expect(page.getByText('zaten alinmis')).toBeVisible()
  })

  test('gecersiz token: giris ekranina donulur ve aciklama gosterilir', async ({ page }) => {
    await page.goto('/')
    await page.evaluate(() => localStorage.setItem('wallet.token', 'aaa.bbb.ccc'))
    await page.reload()
    await expect(page.getByText('Oturum suresi doldu')).toBeVisible()
    await expect(page.getByRole('heading', { name: 'Giris yap' })).toBeVisible()
  })

  test('giris ekrani acikken saklanan bozuk token girisi engellemez', async ({ page, request }) => {
    const username = uniqueName('bozuk')
    await apiSignUp(request, username)

    // Giris ekrani zaten acik; bozuk token sonradan saklanir (ornegin baska sekmeden).
    // Giris istegine token eklenirse backend acik endpoint'te bile 401 verir.
    await page.goto('/')
    await expect(page.getByRole('heading', { name: 'Giris yap' })).toBeVisible()
    await page.evaluate(() => localStorage.setItem('wallet.token', 'aaa.bbb.ccc'))
    await page.getByLabel('Kullanici adi').fill(username)
    await page.getByLabel('Sifre').fill(PASSWORD)
    await page.locator('button[type="submit"]').click()
    await expect(page.getByRole('button', { name: 'Cikis' })).toBeVisible()
  })
})

test.describe('cuzdan', () => {
  test('hesap ac, para yatir, cek ve gecmisi gor', async ({ page }) => {
    await uiSignUp(page, uniqueName('cuzdan'))
    const row = await uiCreateAccount(page, 'Maas hesabi')
    await row.click()

    const amount = page.getByPlaceholder('Tutar (orn. 100,50)')
    await amount.fill('100,50')
    await page.getByRole('button', { name: 'Yatir', exact: true }).click()
    await expect(page.locator('.balance')).toContainText('100,50')

    await amount.fill('40')
    await page.getByRole('button', { name: 'Cek', exact: true }).click()
    await expect(page.locator('.balance')).toContainText('60,50')

    await expect(page.getByRole('cell', { name: 'Para yatirma' })).toBeVisible()
    await expect(page.getByRole('cell', { name: 'Para cekme' })).toBeVisible()
  })

  test('bakiyeden fazla cekmede backend hatasi gosterilir ve bakiye degismez', async ({ page }) => {
    await uiSignUp(page, uniqueName('yetersiz'))
    const row = await uiCreateAccount(page, 'Kucuk hesap')
    await row.click()

    await page.getByPlaceholder('Tutar (orn. 100,50)').fill('5')
    await page.getByRole('button', { name: 'Cek', exact: true }).click()
    await expect(page.getByText('Yetersiz bakiye')).toBeVisible()
    await expect(page.locator('.balance')).toContainText('0,00')
  })

  test('baska kullanicinin hesabina hesap numarasiyla transfer', async ({ page, request }) => {
    // Alici: API ile olusturulan baska bir kullanicinin hesabi
    const receiverName = uniqueName('alici')
    const receiverToken = await apiSignUp(request, receiverName)
    const receiverAccountId = await apiCreateAccount(request, receiverToken, 'Alici hesabi')

    await uiSignUp(page, uniqueName('gonderen'))
    const row = await uiCreateAccount(page, 'Gonderen hesabi')
    await row.click()
    await page.getByPlaceholder('Tutar (orn. 100,50)').fill('100')
    await page.getByRole('button', { name: 'Yatir', exact: true }).click()
    await expect(page.locator('.balance')).toContainText('100,00')

    await page.locator('form select').selectOption('other')
    await page.getByLabel('Alici hesap numarasi').fill(String(receiverAccountId))
    await page.getByPlaceholder('Tutar', { exact: true }).fill('25')
    await page.getByRole('button', { name: 'Gonder' }).click()

    await expect(page.locator('.balance')).toContainText('75,00')
    await expect(page.getByRole('cell', { name: 'Giden transfer' })).toBeVisible()
    expect(await apiBalance(request, receiverToken, receiverAccountId)).toBe(25)
  })

  test('olmayan hesaba transferde hata gosterilir', async ({ page }) => {
    await uiSignUp(page, uniqueName('olmayan'))
    const row = await uiCreateAccount(page, 'Hesabim')
    await row.click()
    await page.getByPlaceholder('Tutar (orn. 100,50)').fill('10')
    await page.getByRole('button', { name: 'Yatir', exact: true }).click()
    await expect(page.locator('.balance')).toContainText('10,00')

    await page.locator('form select').selectOption('other')
    await page.getByLabel('Alici hesap numarasi').fill('99999999')
    await page.getByPlaceholder('Tutar', { exact: true }).fill('1')
    await page.getByRole('button', { name: 'Gonder' }).click()
    await expect(page.getByText('Hesap bulunamadi')).toBeVisible()
  })

  test('kullanici baska kullanicinin hesabini listesinde gormez', async ({ page, request }) => {
    const otherToken = await apiSignUp(request, uniqueName('baska'))
    await apiCreateAccount(request, otherToken, 'Gizli hesap')

    await uiSignUp(page, uniqueName('kendi'))
    await expect(page.getByText('Henuz hesap yok')).toBeVisible()
    await expect(page.getByText('Gizli hesap')).toHaveCount(0)
  })
})

test.describe('gorunum', () => {
  for (const width of [900, 375]) {
    test(`${width}px genislikte yatay tasma yok`, async ({ page }) => {
      await page.setViewportSize({ width, height: 900 })
      await uiSignUp(page, uniqueName('gorunum'))
      const row = await uiCreateAccount(page, 'Birikim')
      await row.click()
      await page.locator('form select').selectOption('other') // en genis transfer satiri
      const overflows = await page.evaluate(
        () => document.documentElement.scrollWidth > document.documentElement.clientWidth,
      )
      expect(overflows).toBe(false)
    })
  }
})
