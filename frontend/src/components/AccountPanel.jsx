import { useState } from 'react'
import { deposit, withdraw } from '../api'
import { parseAmount } from '../amount'
import TransactionHistory from './TransactionHistory'
import TransferForm from './TransferForm'

const money = new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' })

function AccountPanel({ account, accounts, reloadKey, onChanged }) {
  const [amount, setAmount] = useState('')
  const [error, setError] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  const parsed = parseAmount(amount)
  const valid = parsed !== null

  async function run(action) {
    setError(null)
    setSubmitting(true)
    try {
      await action(account.id, parsed)
      setAmount('')
      onChanged()
    } catch (err) {
      setError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section className="card">
      <h2>
        {account.ownerName} <span className="muted">(#{account.id})</span>
      </h2>
      <p className="balance">{money.format(account.balance)}</p>
      <div className="row">
        <input
          value={amount}
          onChange={(e) => setAmount(e.target.value)}
          placeholder="Tutar (orn. 100,50)"
          inputMode="decimal"
        />
        <button disabled={submitting || !valid} onClick={() => run(deposit)}>
          Yatir
        </button>
        <button
          className="secondary"
          disabled={submitting || !valid}
          onClick={() => run(withdraw)}
        >
          Cek
        </button>
      </div>
      {error && <p className="error">{error}</p>}
      <hr />
      <TransferForm key={account.id} account={account} accounts={accounts} onChanged={onChanged} />
      <hr />
      <TransactionHistory key={reloadKey} accountId={account.id} />
    </section>
  )
}

export default AccountPanel
