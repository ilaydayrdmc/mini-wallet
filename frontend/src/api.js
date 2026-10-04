// Backend ile konusan tek yer. Bilesenler fetch'i dogrudan cagirmaz.
import { getToken } from './auth'

// Token gecersiz/suresi dolmus oldugunda (401) uygulamanin haberdar olmasi icin
let onUnauthorized = () => {}

export function setUnauthorizedHandler(handler) {
  onUnauthorized = handler
}

// withAuth=false: giris/kayit istekleri token gondermez. Suresi dolmus bir token
// gonderilirse backend, acik endpoint'lerde bile 401 doner.
async function request(path, { withAuth = true, ...options } = {}) {
  const token = withAuth ? getToken() : null
  const res = await fetch(path, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  })

  if (!res.ok) {
    if (res.status === 401 && token) {
      onUnauthorized()
      throw new Error('Oturum suresi doldu, lutfen tekrar giris yapin')
    }
    // Backend hatalari ProblemDetail olarak doner: { detail, status, ... }
    const body = await res.json().catch(() => null)
    throw new Error(body?.detail ?? `Istek basarisiz (HTTP ${res.status})`)
  }
  return res.json()
}

export function login(username, password) {
  return request('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
    withAuth: false,
  })
}

export function register(username, password) {
  return request('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
    withAuth: false,
  })
}

export function getAccounts() {
  return request('/api/accounts')
}

export function createAccount(ownerName) {
  return request('/api/accounts', {
    method: 'POST',
    body: JSON.stringify({ ownerName }),
  })
}

export function deposit(accountId, amount) {
  return request(`/api/accounts/${accountId}/deposit`, {
    method: 'POST',
    body: JSON.stringify({ amount }),
  })
}

export function withdraw(accountId, amount) {
  return request(`/api/accounts/${accountId}/withdraw`, {
    method: 'POST',
    body: JSON.stringify({ amount }),
  })
}

export function transfer(fromAccountId, toAccountId, amount) {
  return request('/api/transfers', {
    method: 'POST',
    body: JSON.stringify({ fromAccountId, toAccountId, amount }),
  })
}

export function getTransactions(accountId, page = 0, size = 5) {
  return request(`/api/accounts/${accountId}/transactions?page=${page}&size=${size}`)
}
