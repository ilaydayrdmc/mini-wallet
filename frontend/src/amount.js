// "12,50" ve "12.50" yazimlarini kabul eder; gecersizse null doner
export function parseAmount(text) {
  const value = Number(text.trim().replace(',', '.'))
  return text.trim() !== '' && Number.isFinite(value) && value >= 0.01 ? value : null
}
