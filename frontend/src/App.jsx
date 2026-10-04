import { useCallback, useEffect, useState } from 'react'
import { setUnauthorizedHandler } from './api'
import { clearToken, getToken, setToken, usernameFromToken } from './auth'
import AuthPage from './components/AuthPage'
import Wallet from './Wallet'

function App() {
  const [token, setTokenState] = useState(() => getToken())
  const [notice, setNotice] = useState(null)

  const logout = useCallback((message = null) => {
    clearToken()
    setTokenState(null)
    setNotice(message)
  }, [])

  // api.js bir istek 401 aldiginda (token suresi doldu/gecersiz) cikis yaptirir
  useEffect(() => {
    setUnauthorizedHandler(() => logout('Oturum suresi doldu, lutfen tekrar giris yapin'))
  }, [logout])

  function handleLoggedIn(newToken) {
    setToken(newToken)
    setTokenState(newToken)
    setNotice(null)
  }

  if (!token) {
    return <AuthPage notice={notice} onLoggedIn={handleLoggedIn} />
  }
  return <Wallet username={usernameFromToken(token)} onLogout={() => logout()} />
}

export default App
