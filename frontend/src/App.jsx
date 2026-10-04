import { useEffect, useState } from 'react'

function App() {
  const [message, setMessage] = useState('Yukleniyor...')

  useEffect(() => {
    fetch('/api/hello')
      .then((res) => {
        if (!res.ok) throw new Error(`HTTP ${res.status}`)
        return res.json()
      })
      .then((data) => setMessage(data.message))
      .catch((err) => setMessage(`Backend'e ulasilamadi: ${err.message}`))
  }, [])

  return (
    <main style={{ maxWidth: 600, margin: '4rem auto', padding: '0 1rem' }}>
      <h1>Mini Wallet</h1>
      <p>{message}</p>
    </main>
  )
}

export default App
