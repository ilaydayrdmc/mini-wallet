// Token'in tarayicida saklanmasi.
// Not: localStorage, sayfadaki herhangi bir JavaScript tarafindan okunabilir (XSS riski).
// Bu proje icin basit ve kabul edilebilir; daha siki bir yontem HttpOnly cookie'dir.

const KEY = 'wallet.token'

export function getToken() {
  try {
    return localStorage.getItem(KEY)
  } catch {
    return null
  }
}

export function setToken(token) {
  try {
    localStorage.setItem(KEY, token)
  } catch {
    // localStorage kapaliysa (ozel pencere vb.) oturum sadece bellekte yasar
  }
}

export function clearToken() {
  try {
    localStorage.removeItem(KEY)
  } catch {
    // yoksay
  }
}

// JWT'nin ortasindaki kisim (payload) Base64URL ile kodlanmis JSON'dur.
// Sadece ekranda kullanici adini gostermek icin okuruz; DOGRULAMA backend'de yapilir.
export function usernameFromToken(token) {
  try {
    const payload = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')
    return JSON.parse(atob(payload)).username ?? null
  } catch {
    return null
  }
}
