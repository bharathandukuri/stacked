import Statement from "./steps/statement"
import { StudioNavbar } from "./components/studio-navbar"

function CodingProblemStudio() {
  return (
    <div className="min-h-screen bg-background text-foreground">
      <StudioNavbar mode="create" activeTab="form" />
      <main className="w-full">
        <Statement />
      </main>
    </div>
  )
}

export default CodingProblemStudio
