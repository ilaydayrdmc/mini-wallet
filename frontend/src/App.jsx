import { useEffect, useState } from 'react'
import { getAccounts } from './api'
import AccountList from './components/AccountList'
import AccountPanel from './components/AccountPanel'
import CreateAccountForm from './components/CreateAccountForm'

function App() {
  const [accounts, setAccounts] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  // Bu sayac artinca liste yeniden yuklenir (ornegin hesap acildiktan sonra)
  const [reloadKey, setReloadKey] = useState(0)
  const [selectedId, setSelectedId] = useState(null)
  const reload = () => setReloadKey((k) => k + 1)
  const selected = accounts.find((a) => a.id === selectedId)

  useEffect(() => {
    let cancelled = false
    getAccounts()
      .then((data) => {
        if (cancelled) return
        setAccounts(data)
        setError(null)
      })
      .catch((err) => {
        if (!cancelled) setError(`Hesaplar yuklenemedi: ${err.message}`)
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [reloadKey])

  return (
    <main className="container">
      <h1>Mini Wallet</h1>
      <CreateAccountForm onCreated={reload} />
      {selected && <AccountPanel key={selected.id} account={selected} onChanged={reload} />}
      <section className="card">
        <h2>Hesaplar</h2>
        {loading && <p className="muted">Yukleniyor...</p>}
        {error && <p className="error">{error}</p>}
        {!loading && !error && <AccountList accounts={accounts} selectedId={selectedId} onSelect={setSelectedId} />}
      </section>
    </main>
  )
}

export default App
