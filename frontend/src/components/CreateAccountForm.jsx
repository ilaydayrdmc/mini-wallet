import { useState } from 'react'
import { createAccount } from '../api'

function CreateAccountForm({ onCreated }) {
  const [ownerName, setOwnerName] = useState('')
  const [error, setError] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(e) {
    e.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await createAccount(ownerName.trim())
      setOwnerName('')
      onCreated()
    } catch (err) {
      setError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="card">
      <h2>Yeni hesap</h2>
      <div className="row">
        <input
          value={ownerName}
          onChange={(e) => setOwnerName(e.target.value)}
          placeholder="Hesap sahibinin adi"
          maxLength={100}
          required
        />
        <button type="submit" disabled={submitting || !ownerName.trim()}>
          {submitting ? 'Aciliyor...' : 'Hesap ac'}
        </button>
      </div>
      {error && <p className="error">{error}</p>}
    </form>
  )
}

export default CreateAccountForm
