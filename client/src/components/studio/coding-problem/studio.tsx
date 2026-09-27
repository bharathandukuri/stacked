import { useRef } from "react"
import { StudioNavbar } from "./components/studio-navbar"
import {
  CodingProblemForm,
  type CodingProblemFormHandle,
} from "./components/coding-problem-form"
import { StepSolutions, type StepSolutionsHandle } from "./steps/step-solutions"
import { useCodingProblemStore } from "./store"

function CodingProblemStudio() {
  const step1Ref = useRef<CodingProblemFormHandle>(null)
  const step2Ref = useRef<StepSolutionsHandle>(null)

  const currentStep = useCodingProblemStore((state) => state.currentStep)
  const title = useCodingProblemStore((state) => state.values.title)
  const isSaving = useCodingProblemStore((state) => state.isSaving)
  const actions = useCodingProblemStore((state) => state.actions)

  const handleSubmitNavbar = () => {
    if (currentStep === 1) {
      step1Ref.current?.submit()
    } else {
      step2Ref.current?.submit()
    }
  }

  return (
    <div className="flex h-screen flex-col overflow-hidden bg-background text-foreground">
      <StudioNavbar
        mode="create"
        problemTitle={title}
        currentStep={currentStep}
        onStepChange={(step) => actions.setCurrentStep(step)}
        isSubmitting={isSaving}
        onSubmit={handleSubmitNavbar}
      />
      <main className="min-h-0 w-full flex-1 overflow-hidden">
        {currentStep === 1 ? (
          <div className="h-full w-full overflow-y-auto">
            <CodingProblemForm ref={step1Ref} />
          </div>
        ) : (
          <div className="h-full w-full overflow-hidden">
            <StepSolutions
              ref={step2Ref}
              onBack={() => actions.setCurrentStep(1)}
            />
          </div>
        )}
      </main>
    </div>
  )
}

export default CodingProblemStudio
