import { createFormHook, createFormHookContexts } from "@tanstack/react-form"

// 1. Generate the required React Contexts for fields and forms
const { fieldContext, formContext, useFieldContext, useFormContext } =
  createFormHookContexts()

// 2. Create your application's specialized form hook
export const { useAppForm } = createFormHook({
  fieldContext,
  formContext,
  fieldComponents: {}, // Add custom bound UI components here later
  formComponents: {},
})

// Export context hooks for use inside your custom input components
export { fieldContext, formContext, useFieldContext, useFormContext }
