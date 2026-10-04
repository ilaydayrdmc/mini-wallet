// Backend ile konusan tek yer. Bilesenler fetch'i dogrudan cagirmaz.

async function request(path, options = {}) {
  const res = await fetch(path, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  })

  if (!res.ok) {
    // Backend hatalari ProblemDetail olarak doner: { detail, status, ... }
    const body = await res.json().catch(() => null)
    throw new Error(body?.detail ?? `Istek basarisiz (HTTP ${res.status})`)
  }
  return res.json()
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
