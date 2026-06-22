import { reactive } from 'vue'

const state = reactive({
  visible: false,
  label: '',
  placeholder: '',
  _resolve: null,
})

export function usePrompt() {
  function prompt(label, placeholder = '') {
    state.label = label
    state.placeholder = placeholder
    state.visible = true
    return new Promise((resolve) => {
      state._resolve = resolve
    })
  }

  function answer(value) {
    state.visible = false
    state._resolve?.(value)
    state._resolve = null
  }

  return { state, prompt, answer }
}
