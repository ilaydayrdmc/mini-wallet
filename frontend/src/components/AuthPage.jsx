import { useState } from 'react'
import { login, register } from '../api'

function AuthPage({ notice, onLoggedIn }) {
  const [mode, setMode] = useState('login') // 'login' | 'register'
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  const isRegister = mode === 'register'

  async function handleSubmit(e) {
    e.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      if (isRegister) {
        await register(username.trim(), password)
      }
      const { accessToken } = await login(username.trim(), password)
      onLoggedIn(accessToken)
    } catch (err) {
      setError(err.message)
      setSubmitting(false)
    }
  }

  function switchMode() {
    setMode(isRegister ? 'login' : 'register')
    setError(null)
  }

  return (
    <main className="container">
      <h1>Mini Wallet</h1>
      <form onSubmit={handleSubmit} className="card auth">
        <h2>{isRegister ? 'Kayit ol' : 'Giris yap'}</h2>
        {notice && <p className="notice">{notice}</p>}
        <label>
          Kullanici adi
          <input
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            autoComplete="username"
            minLength={3}
            maxLength={30}
            required
          />
        </label>
        <label>
          Sifre
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete={isRegister ? 'new-password' : 'current-password'}
            minLength={isRegister ? 8 : undefined}
            maxLength={72}
            required
          />
        </label>
        {isRegister && (
          <p className="muted small">
            Kullanici adi 3-30 karakter (harf, rakam, _ ve .), sifre en az 8 karakter olmali.
          </p>
        )}
        {error && <p className="error">{error}</p>}
        <button type="submit" disabled={submitting}>
          {submitting ? 'Bekleyin...' : isRegister ? 'Kayit ol' : 'Giris yap'}
        </button>
        <p className="muted small">
          {isRegister ? 'Zaten hesabin var mi? ' : 'Hesabin yok mu? '}
          <button type="button" className="link" onClick={switchMode}>
            {isRegister ? 'Giris yap' : 'Kayit ol'}
          </button>
        </p>
      </form>
    </main>
  )
}

export default AuthPage
