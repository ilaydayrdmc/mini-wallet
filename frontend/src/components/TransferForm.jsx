import { useState } from 'react'
import { transfer } from '../api'
import { parseAmount } from '../amount'

// Listede olmayan (baska kullanicinin) bir hesaba hesap numarasiyla gondermek icin
const OTHER = 'other'

function TransferForm({ account, accounts, onChanged }) {
  const ownTargets = accounts.filter((a) => a.id !== account.id)
  const [choice, setChoice] = useState('') // '' | kendi hesabinin id'si | OTHER
  const [otherId, setOtherId] = useState('')
  const [amount, setAmount] = useState('')
  const [error, setError] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  const parsed = parseAmount(amount)
  const toId = Number(choice === OTHER ? otherId : choice)
  const validRecipient =
    choice !== '' && Number.isInteger(toId) && toId > 0 && toId !== account.id

  async function handleSubmit(e) {
    e.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await transfer(account.id, toId, parsed)
      setAmount('')
      onChanged()
    } catch (err) {
      setError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form onSubmit={handleSubmit}>
      <h3>Transfer</h3>
      <div className="row">
        <select value={choice} onChange={(e) => setChoice(e.target.value)} required>
          <option value="">Alici hesap sec</option>
          {ownTargets.map((a) => (
            <option key={a.id} value={a.id}>
              #{a.id} {a.ownerName} (benim)
            </option>
          ))}
          <option value={OTHER}>Baska bir hesap numarasi...</option>
        </select>
        {choice === OTHER && (
          <input
            value={otherId}
            onChange={(e) => setOtherId(e.target.value)}
            placeholder="Hesap no"
            inputMode="numeric"
            aria-label="Alici hesap numarasi"
          />
        )}
        <input
          value={amount}
          onChange={(e) => setAmount(e.target.value)}
          placeholder="Tutar"
          inputMode="decimal"
        />
        <button type="submit" disabled={submitting || !validRecipient || parsed === null}>
          Gonder
        </button>
      </div>
      {error && <p className="error">{error}</p>}
    </form>
  )
}

export default TransferForm
