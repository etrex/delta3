export const showSuccessMessage = (message: string) => {
  const event = new CustomEvent('show-success-message', {
    detail: { message }
  })
  window.dispatchEvent(event)
}

export const showErrorMessage = (message: string) => {
  const event = new CustomEvent('show-error-message', {
    detail: { message }
  })
  window.dispatchEvent(event)
}