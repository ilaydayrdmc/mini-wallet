const money = new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' })

function AccountList({ accounts, selectedId, onSelect }) {
  if (accounts.length === 0) {
    return <p className="muted">Henuz hesap yok. Yukaridan bir tane ac.</p>
  }

  return (
    <table>
      <thead>
        <tr>
          <th>No</th>
          <th>Hesap sahibi</th>
          <th className="num">Bakiye</th>
        </tr>
      </thead>
      <tbody>
        {accounts.map((a) => (
          <tr
            key={a.id}
            className={a.id === selectedId ? 'selected' : ''}
            onClick={() => onSelect(a.id)}
          >
            <td>{a.id}</td>
            <td>{a.ownerName}</td>
            <td className="num">{money.format(a.balance)}</td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}

export default AccountList
