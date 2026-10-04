import { useEffect, useState } from 'react'
import { getTransactions } from '../api'

const money = new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' })
const dateTime = new Intl.DateTimeFormat('tr-TR', { dateStyle: 'short', timeStyle: 'short' })

const TYPES = {
  DEPOSIT: { label: 'Para yatirma', sign: 1 },
  WITHDRAWAL: { label: 'Para cekme', sign: -1 },
  TRANSFER_IN: { label: 'Gelen transfer', sign: 1 },
  TRANSFER_OUT: { label: 'Giden transfer', sign: -1 },
}

function TransactionHistory({ accountId }) {
  const [page, setPage] = useState(0)
  // Sonuc hangi sayfa icin geldiyse onu da saklariz; boylece "yukleniyor"
  // durumu ayri bir state olmadan, karsilastirarak bulunur.
  const [result, setResult] = useState({ page: null, data: null, error: null })

  useEffect(() => {
    let cancelled = false
    getTransactions(accountId, page)
      .then((data) => {
        if (!cancelled) setResult({ page, data, error: null })
      })
      .catch((err) => {
        if (!cancelled) setResult({ page, data: null, error: err.message })
      })
    return () => {
      cancelled = true
    }
  }, [accountId, page])

  const loading = result.page !== page
  const transactions = result.data?.content ?? []
  const info = result.data?.page

  return (
    <div>
      <h3>Islem gecmisi</h3>
      {loading && <p className="muted">Yukleniyor...</p>}
      {!loading && result.error && <p className="error">{result.error}</p>}
      {!loading && !result.error && transactions.length === 0 && (
        <p className="muted">Henuz islem yok.</p>
      )}
      {!loading && transactions.length > 0 && (
        <>
          <table>
            <thead>
              <tr>
                <th>Tarih</th>
                <th>Islem</th>
                <th className="num">Tutar</th>
                <th className="num">Bakiye</th>
              </tr>
            </thead>
            <tbody>
              {transactions.map((t) => {
                const type = TYPES[t.type] ?? { label: t.type, sign: 1 }
                return (
                  <tr key={t.id} className="static">
                    <td>{dateTime.format(new Date(t.createdAt))}</td>
                    <td>{type.label}</td>
                    <td className={`num ${type.sign > 0 ? 'in' : 'out'}`}>
                      {type.sign > 0 ? '+' : '-'}
                      {money.format(t.amount)}
                    </td>
                    <td className="num">{money.format(t.balanceAfter)}</td>
                  </tr>
                )
              })}
            </tbody>
          </table>
          <div className="pager">
            <button className="secondary" disabled={page === 0} onClick={() => setPage(page - 1)}>
              Onceki
            </button>
            <span className="muted">
              Sayfa {info.number + 1} / {info.totalPages}
            </span>
            <button
              className="secondary"
              disabled={page + 1 >= info.totalPages}
              onClick={() => setPage(page + 1)}
            >
              Sonraki
            </button>
          </div>
        </>
      )}
    </div>
  )
}

export default TransactionHistory
