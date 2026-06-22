import { reactive } from 'vue'

const alerts = reactive([])
let nextId = 0

export function useAlert() {
  function show(message, type = 'info', duration = 3000) {
    const id = nextId++
    alerts.push({ id, message, type })
    if (duration > 0) setTimeout(() => dismiss(id), duration)
  }

  function dismiss(id) {
    const idx = alerts.findIndex(a => a.id === id)
    if (idx !== -1) alerts.splice(idx, 1)
  }

  return {
    alerts,
    dismiss,
    error:   (msg) => show(msg, 'error'),
    info:    (msg) => show(msg, 'info'),
    success: (msg) => show(msg, 'success'),
    warning: (msg) => show(msg, 'warning'),
  }
}
