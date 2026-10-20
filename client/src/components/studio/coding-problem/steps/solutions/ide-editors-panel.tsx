import { useEffect, useMemo } from "react";
import {
  FileCode,
  Sparkles,
  ShieldCheck,
  ShieldAlert,
  Info,
} from "lucide-react";
import { MonacoEditor } from "@/components/monaco-editor";
import { Button } from "@/components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { cn } from "@/lib/utils";
import { useCodingProblemStore } from "../../store";
import {
  getLanguageById,
  resolveStarterCodeFileName,
  resolveReferenceSolutionFileName,
  resolveVerificationScriptFileName,
} from "@/config/languages";

export interface IdeEditorsPanelProps {
  className?: string;
}

export function IdeEditorsPanel({ className }: IdeEditorsPanelProps) {
  const values = useCodingProblemStore((state) => state.values);
  const activeEditorMode = useCodingProblemStore(
    (state) => state.activeEditorMode
  );
  const activeCodeLanguage = useCodingProblemStore(
    (state) => state.activeCodeLanguage
  );
  const starterCodes = useCodingProblemStore((state) => state.starterCodes);
  const referenceSolutionLanguage = useCodingProblemStore(
    (state) => state.referenceSolutionLanguage
  );
  const referenceSolution = useCodingProblemStore(
    (state) => state.referenceSolution
  );
  const requireVerificationScript = useCodingProblemStore(
    (state) => state.requireVerificationScript
  );
  const verificationScriptLanguage = useCodingProblemStore(
    (state) => state.verificationScriptLanguage
  );
  const verificationScript = useCodingProblemStore(
    (state) => state.verificationScript
  );
  const actions = useCodingProblemStore((state) => state.actions);

  const isDatabase = values.problemType === "DATABASE";
  const supportedLanguages = useMemo(() => {
    return values.supportedLanguageIds.length > 0
      ? values.supportedLanguageIds
      : values.languages.length > 0
        ? values.languages
        : ["javascript-node-20"];
  }, [values.supportedLanguageIds, values.languages]);

  // Determine candidate language for the active mode
  let currentLangId = activeCodeLanguage;
  if (activeEditorMode === "starter") {
    currentLangId = activeCodeLanguage;
  } else if (activeEditorMode === "reference") {
    currentLangId = referenceSolutionLanguage;
  } else if (activeEditorMode === "verification") {
    currentLangId = verificationScriptLanguage;
  }

  // Ensure currentLangId is strictly one of the supported languages
  if (!supportedLanguages.includes(currentLangId)) {
    currentLangId = supportedLanguages[0] || "javascript-node-20";
  }

  // Synchronize store if active mode's language is out of supportedLanguages range
  useEffect(() => {
    if (activeEditorMode === "starter" && !supportedLanguages.includes(activeCodeLanguage) && supportedLanguages[0]) {
      actions.setActiveCodeLanguage(supportedLanguages[0]);
    } else if (activeEditorMode === "reference" && !supportedLanguages.includes(referenceSolutionLanguage) && supportedLanguages[0]) {
      actions.setReferenceSolutionLanguage(supportedLanguages[0]);
    } else if (activeEditorMode === "verification" && !supportedLanguages.includes(verificationScriptLanguage) && supportedLanguages[0]) {
      actions.setVerificationScriptLanguage(supportedLanguages[0]);
    }
  }, [
    activeEditorMode,
    activeCodeLanguage,
    referenceSolutionLanguage,
    verificationScriptLanguage,
    supportedLanguages,
    actions,
  ]);

  let currentFileName = "Solution.java";
  let currentCode = "";
  let modeTitle = "Starter Code";
  let modeDescription =
    "Boilerplate skeleton code provided to learners when starting the problem.";
  let Icon = FileCode;

  if (activeEditorMode === "starter") {
    currentFileName = resolveStarterCodeFileName(currentLangId);
    currentCode = starterCodes[currentLangId] ?? "";
    modeTitle = `Starter Code`;
    modeDescription =
      "Initial code template presented to candidates in the code editor.";
    Icon = FileCode;
  } else if (activeEditorMode === "reference") {
    currentFileName = resolveReferenceSolutionFileName(currentLangId);
    currentCode = referenceSolution;
    modeTitle = `Reference Solution`;
    modeDescription =
      "Authoritative accepted implementation used by the judge to evaluate testcase correctness.";
    Icon = Sparkles;
  } else if (activeEditorMode === "verification") {
    currentFileName = resolveVerificationScriptFileName(currentLangId);
    currentCode = verificationScript;
    modeTitle = `Verification Script`;
    modeDescription =
      "Custom output validator script for asserting candidate answers against reference outputs.";
    Icon = ShieldCheck;
  }

  const langConfig = getLanguageById(currentLangId);
  const monacoLanguage =
    langConfig?.monacoLanguage || (isDatabase ? "sql" : "javascript");

  const handleCodeChange = (newCode: string) => {
    if (activeEditorMode === "starter") {
      actions.setStarterCode(currentLangId, newCode);
    } else if (activeEditorMode === "reference") {
      actions.setReferenceSolution(newCode);
    } else if (activeEditorMode === "verification") {
      actions.setVerificationScript(newCode);
    }
  };

  const handleLanguageChange = (newLangId: string | null) => {
    if (!newLangId) return;
    if (activeEditorMode === "starter") {
      actions.setActiveCodeLanguage(newLangId);
    } else if (activeEditorMode === "reference") {
      actions.setReferenceSolutionLanguage(newLangId);
    } else if (activeEditorMode === "verification") {
      actions.setVerificationScriptLanguage(newLangId);
    }
  };

  const selectItemsRecord = useMemo(
    () =>
      supportedLanguages.reduce<Record<string, string>>((acc, id) => {
        acc[id] = getLanguageById(id)?.name || id;
        return acc;
      }, {}),
    [supportedLanguages]
  );

  // If verification script mode is active but verification is disabled
  if (activeEditorMode === "verification" && !requireVerificationScript) {
    return (
      <div
        className={cn(
          "flex h-full w-full flex-col items-center justify-center gap-4 bg-muted/10 p-8 text-center select-none",
          className
        )}
      >
        <div className="flex size-14 items-center justify-center rounded-2xl border border-border/80 bg-background shadow-xs">
          <ShieldAlert className="size-7 text-muted-foreground/60" />
        </div>

        <div className="max-w-md space-y-1.5">
          <h3 className="text-sm font-semibold text-foreground">
            Verification Script Disabled
          </h3>
          <p className="text-xs text-muted-foreground leading-relaxed">
            When disabled, the judge performs direct output comparison between
            candidate execution and reference solution output. Enable the
            verification script if your problem requires custom assertions,
            floating-point tolerance, or unordered graph/set checks.
          </p>
        </div>

        <Button
          type="button"
          size="sm"
          onClick={() => actions.setRequireVerificationScript(true)}
          className="cursor-pointer gap-2 font-medium text-xs shadow-xs"
        >
          <ShieldCheck className="size-4" />
          <span>Enable Verification Script</span>
        </Button>
      </div>
    );
  }

  return (
    <div
      className={cn(
        "flex h-full w-full flex-col overflow-hidden bg-background",
        className
      )}
    >
      {/* Editor Subheader Bar */}
      <div className="flex h-9 shrink-0 items-center justify-between border-b border-border/70 bg-muted/20 px-3 select-none">
        <div className="flex min-w-0 items-center gap-2">
          {/* Shadcn Select on top of the code editor */}
          <Select
            value={currentLangId}
            items={selectItemsRecord}
            onValueChange={handleLanguageChange}
            disabled={isDatabase && supportedLanguages.length <= 1}
          >
            <SelectTrigger
              size="sm"
              className="h-6.5 text-[11px] font-medium px-2 py-0 min-w-32"
            >
              <SelectValue placeholder="Select language" />
            </SelectTrigger>
            <SelectContent>
              {supportedLanguages.map((langId) => (
                <SelectItem key={langId} value={langId}>
                  {getLanguageById(langId)?.name || langId}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        {/* Right actions: Improved Info button and Reset to template button */}
        <div className="flex shrink-0 items-center gap-1">
          <Popover>
            <PopoverTrigger
              render={
                <Button
                  type="button"
                  variant="ghost"
                  size="icon-xs"
                  className="h-6 w-6 cursor-pointer text-muted-foreground hover:text-foreground"
                  title="Editor mode information"
                >
                  <Info className="size-3.5" />
                </Button>
              }
            />
            <PopoverContent
              align="end"
              side="bottom"
              className="w-80 p-3.5 text-xs shadow-md"
            >
              <div className="flex items-center gap-2.5 border-b border-border/70 pb-2.5">
                <div
                  className={cn(
                    "flex size-7 shrink-0 items-center justify-center rounded-md border",
                    activeEditorMode === "starter"
                      ? "border-primary/30 bg-primary/10 text-primary"
                      : activeEditorMode === "reference"
                        ? "border-emerald-500/30 bg-emerald-500/10 text-emerald-500"
                        : "border-blue-500/30 bg-blue-500/10 text-blue-500"
                  )}
                >
                  <Icon className="size-4" />
                </div>
                <div className="min-w-0 flex-1">
                  <p className="font-semibold text-foreground">{modeTitle}</p>
                  <p className="font-mono text-[11px] text-muted-foreground">
                    {currentFileName}
                  </p>
                </div>
              </div>
              <div className="mt-2.5 space-y-2 text-xs">
                <p className="text-muted-foreground leading-relaxed">
                  {modeDescription}
                </p>
                <div className="rounded-md bg-muted/50 px-2.5 py-1.5 text-[11px] text-muted-foreground">
                  <span className="font-medium text-foreground">Language:</span>{" "}
                  {getLanguageById(currentLangId)?.name || currentLangId}
                </div>
              </div>
            </PopoverContent>
          </Popover>
        </div>
      </div>

      {/* Editor Workspace */}
      <div className="min-h-0 w-full flex-1 overflow-hidden">
        <MonacoEditor
          language={monacoLanguage}
          path={`${activeEditorMode}/${currentLangId}/${currentFileName}`}
          value={currentCode}
          onChange={handleCodeChange}
          height="100%"
          width="100%"
          bordered={false}
          options={{
            minimap: { enabled: true },
            lineNumbers: "on",
            scrollBeyondLastLine: false,
            fontSize: 13,
          }}
        />
      </div>
    </div>
  );
}
