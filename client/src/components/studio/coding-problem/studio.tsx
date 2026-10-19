import { StudioNavbar } from "./components/studio-navbar";
import StatementStep from "./steps/statement-step";
import { useCodingProblemStore } from "./store";

function CodingProblemStudio() {
  const currentStep = useCodingProblemStore((state) => state.currentStep);
  const setCurrentStep = useCodingProblemStore((state) => state.setCurrentStep);
  const title = useCodingProblemStore((state) => state.values.title);

  return (
    <div className="flex h-screen flex-col overflow-hidden bg-background text-foreground">
      <StudioNavbar
        mode="create"
        problemTitle={title}
        currentStep={currentStep}
        onStepChange={setCurrentStep}
      />
      <main className="min-h-0 w-full flex-1 overflow-hidden">
        {currentStep === 1 && (
          <div className="h-full w-full overflow-y-auto">
            <StatementStep />
          </div>
        )}
      </main>
    </div>
  );
}

export default CodingProblemStudio;
