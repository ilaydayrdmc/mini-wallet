import { useState } from 'react'
import { transfer } from '../api'
import { parseAmount } from '../amount'

function TransferForm({ account, accounts, onChanged }) {
  const targets = accounts.filter((a) => a.id !== account.id)
  const [toId, setToId] = useState('')
  const [amount, setAmount] = useState('')
  const [error, setError] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  const parsed = parseAmount(amount)

  async function handleSubmit(e) {
    e.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await transfer(account.id, Number(toId), parsed)
      setAmount('')
      onChanged()
    } catch (err) {
      setError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  if (targets.length === 0) {
    return <p className="muted">Transfer icin baska bir hesap gerekli.</p>
  }

  return (
    <form onSubmit={handleSubmit}>
      <h3>Transfer</h3>
      <div className="row">
        <select value={toId} onChange={(e) => setToId(e.target.value)} required>
          <option value="">Alici hesap sec</option>
          {targets.map((a) => (
            <option key={a.id} value={a.id}>
              #{a.id} {a.ownerName}
            </option>
          ))}
        </select>
        <input
          value={amount}
          onChange={(e) => setAmount(e.target.value)}
          placeholder="Tutar"
          inputMode="decimal"
        />
        <button type="submit" disabled={submitting || !toId || parsed === null}>
          Gonder
        </button>
      </div>
      {error && <p className="error">{error}</p>}
    </form>
  )
}

export default TransferForm
