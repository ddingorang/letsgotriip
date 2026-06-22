import { reactive } from 'vue'

const state = reactive({
  visible: false,
  message: '',
  _resolve: null,
})

export function useConfirm() {
  function confirm(message) {
    state.message = message
    state.visible = true
    return new Promise((resolve) => {
      state._resolve = resolve
    })
  }

  function answer(result) {
    state.visible = false
    state._resolve?.(result)
    state._resolve = null
  }

  return { state, confirm, answer }
}
