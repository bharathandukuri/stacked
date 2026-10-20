import { useState, useMemo, useCallback } from "react";
import {
  Play,
  Loader2,
  CheckCircle2,
  AlertCircle,
  Clock,
  Cpu,
  Terminal,
  Settings2,
  PlusCircle,
  Copy,
  Check,
  RotateCcw,
  FileCode2,
} from "lucide-react";
import { MonacoEditor } from "@/components/monaco-editor";
import { LineNumberedViewer } from "@/components/line-numbered-viewer";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
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
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import {
  ResizablePanelGroup,
  ResizablePanel,
  ResizableHandle,
} from "@/components/ui/resizable";
import { toast } from "@/components/ui/toast";
import { cn } from "@/lib/utils";
import { useCodingProblemStore } from "../../store";
import {
  getGenericLanguages,
  getLanguageById,
  resolveGeneratorScriptFileName,
} from "@/config/languages";
import {
  DEFAULT_TESTCASE_DELIMITER,
  parseGeneratedTestcases,
} from "@/lib/testcase-parser";

export interface GeneratorPanelProps {
  className?: string;
}

export function GeneratorPanel({ className }: GeneratorPanelProps) {
  const generatorLanguage = useCodingProblemStore(
    (state) => state.generatorLanguage
  );
  const generatorScript = useCodingProblemStore(
    (state) => state.generatorScript
  );
  const generatorStdin = useCodingProblemStore(
    (state) => state.generatorStdin
  );
  const generatorDelimiter = useCodingProblemStore(
    (state) => state.generatorDelimiter
  );
  const activeGeneratorTab = useCodingProblemStore(
    (state) => state.activeGeneratorTab
  );
  const isGenerating = useCodingProblemStore((state) => state.isGenerating);
  const generatorResult = useCodingProblemStore(
    (state) => state.generatorResult
  );
  const generatorAddedBatchSignature = useCodingProblemStore(
    (state) => state.generatorAddedBatchSignature
  );
  const actions = useCodingProblemStore((state) => state.actions);

  // Available languages: all generic programming languages (compiled & interpreted)
  const availableLanguages = useMemo(() => getGenericLanguages(), []);

  // Language info
  const langConfig = useMemo(
    () => getLanguageById(generatorLanguage),
    [generatorLanguage]
  );
  const scriptFileName = useMemo(
    () => resolveGeneratorScriptFileName(generatorLanguage),
    [generatorLanguage]
  );

  // Results pane tab: "preview" | "stdout" | "stderr"
  const [resultTab, setResultTab] = useState<"preview" | "stdout" | "stderr">(
    "preview"
  );
  // Active preview case index
  const [selectedPreviewIndex, setSelectedPreviewIndex] = useState(0);
  const [copiedPreview, setCopiedPreview] = useState(false);

  // Temporary delimiter input for the popover
  const [tempDelimiter, setTempDelimiter] = useState(generatorDelimiter);
  const [isDelimiterPopoverOpen, setIsDelimiterPopoverOpen] = useState(false);

  // Parse stdout on the fly when generatorResult changes
  const parseResult = useMemo(() => {
    if (
      !generatorResult ||
      generatorResult.status !== "SUCCESS" ||
      !generatorResult.stdout
    ) {
      return null;
    }
    return parseGeneratedTestcases(
      generatorResult.stdout,
      generatorDelimiter
    );
  }, [generatorResult, generatorDelimiter]);

  // Check if current batch was already added
  const currentBatchSignature = useMemo(() => {
    if (!parseResult || !parseResult.success) return null;
    return JSON.stringify(parseResult.testcases);
  }, [parseResult]);

  const isAlreadyAdded =
    Boolean(currentBatchSignature) &&
    generatorAddedBatchSignature === currentBatchSignature;

  // Handle delimiter save in popover
  const handleSaveDelimiter = useCallback(() => {
    const val = tempDelimiter.trim() || DEFAULT_TESTCASE_DELIMITER;
    actions.setGeneratorDelimiter(val);
    setTempDelimiter(val);
    setIsDelimiterPopoverOpen(false);
    toast.add({
      title: "Delimiter Updated",
      description: `Testcase delimiter set to "${val}"`,
      type: "success",
    });
  }, [tempDelimiter, actions]);

  // Handle reset delimiter
  const handleResetDelimiter = useCallback(() => {
    actions.setGeneratorDelimiter(DEFAULT_TESTCASE_DELIMITER);
    setTempDelimiter(DEFAULT_TESTCASE_DELIMITER);
    setIsDelimiterPopoverOpen(false);
  }, [actions]);

  // Handle adding parsed testcases to hidden testcases
  const handleAddGeneratedToHidden = useCallback(() => {
    const result = actions.addGeneratedTestcasesToHidden();
    if (result.added > 0) {
      toast.add({
        title: "Testcases Added",
        description: `Successfully added ${result.added} hidden testcase(s).`,
        type: "success",
      });
      if (result.error) {
        toast.add({
          title: "Limit Notice",
          description: result.error,
          type: "warning",
        });
      }
    } else if (result.error) {
      toast.add({
        title: "Could Not Add Testcases",
        description: result.error,
        type: "error",
      });
    }
  }, [actions]);

  // Copy current previewed testcase
  const handleCopyPreview = useCallback(() => {
    if (!parseResult || !parseResult.testcases[selectedPreviewIndex]) return;
    navigator.clipboard.writeText(
      parseResult.testcases[selectedPreviewIndex]
    );
    setCopiedPreview(true);
    setTimeout(() => setCopiedPreview(false), 1500);
  }, [parseResult, selectedPreviewIndex]);

  // Clean-slate initial code for generator
  const editorCode = useMemo(() => {
    if (generatorScript) return generatorScript;
    const isPy = generatorLanguage.startsWith("python");
    return isPy ? `# File: ${scriptFileName}\n` : `// File: ${scriptFileName}\n`;
  }, [generatorScript, generatorLanguage, scriptFileName]);

  return (
    <div
      className={cn(
        "flex h-full w-full flex-col overflow-hidden bg-background",
        className
      )}
    >
      {/* 1. Header Toolbar */}
      <div className="flex h-11 shrink-0 items-center justify-between border-b border-border/80 bg-muted/20 px-3">
        {/* Left: Language Select & Tabs & Delimiter */}
        <div className="flex items-center gap-2">
          {/* Language Selector */}
          <Select
            value={generatorLanguage}
            onValueChange={(val) => {
              if (val) actions.setGeneratorLanguage(val);
            }}
          >
            <SelectTrigger className="h-7 cursor-pointer gap-1.5 border-border/80 bg-background px-2.5 font-mono text-xs font-medium shadow-2xs">
              <SelectValue placeholder="Select Language" />
            </SelectTrigger>
            <SelectContent align="start" className="max-h-72">
              {availableLanguages.map((lang) => (
                <SelectItem
                  key={lang.id}
                  value={lang.id}
                  className="font-mono text-xs cursor-pointer"
                >
                  {lang.name}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>

          {/* Mode Switcher: Script vs Stdin */}
          <div className="flex items-center rounded-md border border-border/80 bg-muted/40 p-0.5 text-xs">
            <button
              type="button"
              onClick={() => actions.setActiveGeneratorTab("script")}
              className={cn(
                "flex h-6 cursor-pointer items-center gap-1.5 rounded-sm px-2.5 text-xs font-medium transition-colors",
                activeGeneratorTab === "script"
                  ? "bg-background text-foreground shadow-2xs font-semibold"
                  : "text-muted-foreground hover:text-foreground"
              )}
            >
              <FileCode2 className="size-3" />
              <span>Generator Script</span>
            </button>
            <button
              type="button"
              onClick={() => actions.setActiveGeneratorTab("stdin")}
              className={cn(
                "flex h-6 cursor-pointer items-center gap-1.5 rounded-sm px-2.5 text-xs font-medium transition-colors",
                activeGeneratorTab === "stdin"
                  ? "bg-background text-foreground shadow-2xs font-semibold"
                  : "text-muted-foreground hover:text-foreground"
              )}
            >
              <Terminal className="size-3" />
              <span>Script Stdin (Optional)</span>
              {generatorStdin.trim().length > 0 && (
                <span className="size-1.5 rounded-full bg-primary" />
              )}
            </button>
          </div>

          {/* Delimiter Configuration Popover */}
          <Popover
            open={isDelimiterPopoverOpen}
            onOpenChange={setIsDelimiterPopoverOpen}
          >
            <PopoverTrigger
              render={
                <button
                  type="button"
                  className="flex h-7 cursor-pointer items-center gap-1.5 rounded-md border border-border/70 bg-background px-2 font-mono text-[11px] text-muted-foreground transition-colors hover:border-foreground hover:text-foreground"
                  title="Configure Testcase Delimiter"
                >
                  <Settings2 className="size-3" />
                  <span className="hidden xl:inline">Delimiter:</span>
                  <span className="max-w-[120px] truncate text-foreground font-semibold">
                    {generatorDelimiter}
                  </span>
                </button>
              }
            />
            <PopoverContent align="start" className="w-84 space-y-3 p-3">
              <div>
                <p className="text-xs font-semibold text-foreground">
                  Testcase Delimiter
                </p>
                <p className="text-[11px] text-muted-foreground leading-relaxed">
                  The generator stdout is split by this delimiter into discrete
                  testcases. All whitespace and indentation are strictly
                  preserved.
                </p>
              </div>

              <div className="space-y-1.5">
                <label className="text-[10px] font-mono text-muted-foreground uppercase">
                  Delimiter String
                </label>
                <Input
                  value={tempDelimiter}
                  onChange={(e) => setTempDelimiter(e.target.value)}
                  placeholder="---STACKED_TESTCASE_DELIMITER---"
                  className="h-8 font-mono text-xs"
                />
              </div>

              <div className="flex items-center justify-between pt-1">
                <Button
                  type="button"
                  variant="ghost"
                  size="xs"
                  onClick={handleResetDelimiter}
                  className="cursor-pointer text-[11px] text-muted-foreground hover:text-foreground"
                >
                  <RotateCcw className="size-3 mr-1" />
                  Reset Default
                </Button>
                <Button
                  type="button"
                  size="xs"
                  onClick={handleSaveDelimiter}
                  className="cursor-pointer text-xs"
                >
                  Apply Delimiter
                </Button>
              </div>
            </PopoverContent>
          </Popover>
        </div>

        {/* Right: Run Generator Script Button */}
        <div className="flex items-center gap-2">
          <Button
            type="button"
            size="sm"
            disabled={isGenerating || !generatorScript.trim()}
            onClick={() => actions.runGenerator()}
            className="cursor-pointer gap-1.5 bg-primary px-3.5 text-xs font-semibold text-primary-foreground shadow-xs hover:bg-primary/90"
          >
            {isGenerating ? (
              <Loader2 className="size-3.5 animate-spin" />
            ) : (
              <Play className="size-3.5 fill-current" />
            )}
            <span>{isGenerating ? "Executing..." : "Run Generator"}</span>
          </Button>
        </div>
      </div>

      {/* 2. Main Resizable Area (Top: Script/Stdin Editor, Bottom: Execution Output & Preview) */}
      <div className="min-h-0 w-full flex-1 overflow-hidden">
        <ResizablePanelGroup orientation="vertical" className="h-full w-full">
          {/* Top Panel: Monaco Script Editor or Stdin Textarea */}
          <ResizablePanel
            defaultSize={55}
            minSize={30}
            className="flex flex-col overflow-hidden bg-background"
          >
            {activeGeneratorTab === "script" ? (
              <div className="relative h-full w-full">
                <MonacoEditor
                  language={langConfig?.monacoLanguage || "python"}
                  path={`generator/${generatorLanguage}/${scriptFileName}`}
                  value={editorCode}
                  onChange={(val) => actions.setGeneratorScript(val)}
                  height="100%"
                  width="100%"
                  bordered={false}
                  options={{
                    minimap: { enabled: false },
                    lineNumbers: "on",
                    scrollBeyondLastLine: false,
                    fontSize: 13,
                  }}
                />
              </div>
            ) : (
              <div className="flex h-full flex-col p-3">
                <div className="mb-2 flex items-center justify-between text-xs text-muted-foreground">
                  <span className="font-mono font-medium text-foreground">
                    stdin
                  </span>
                  <span>Input passed to generator script (optional)</span>
                </div>
                <div className="min-h-0 flex-1 overflow-hidden rounded-md border border-border/80 bg-card p-2">
                  <textarea
                    value={generatorStdin}
                    onChange={(e) => actions.setGeneratorStdin(e.target.value)}
                    placeholder="Enter optional standard input for generator script..."
                    className="h-full w-full resize-none border-0 bg-transparent font-mono text-xs text-foreground focus:outline-none leading-relaxed"
                  />
                </div>
              </div>
            )}
          </ResizablePanel>

          <ResizableHandle withHandle />

          {/* Bottom Panel: Output, Metrics, & Parsed Preview */}
          <ResizablePanel
            defaultSize={45}
            minSize={25}
            className="flex flex-col overflow-hidden border-t border-border/80 bg-background"
          >
            {/* Output Sub-Header Toolbar: Tabs on Left, Metrics & Add Action on Right */}
            <div className="flex h-9 shrink-0 items-center justify-between border-b border-border/80 bg-muted/20 px-3">
              {/* Left: View Switcher Tabs (Parsed Preview | stdout | stderr) */}
              <div className="flex items-center gap-1">
                <button
                  type="button"
                  onClick={() => setResultTab("preview")}
                  className={cn(
                    "flex h-6.5 cursor-pointer items-center gap-1.5 rounded-sm px-2 text-xs font-medium transition-colors",
                    resultTab === "preview"
                      ? "bg-background text-foreground shadow-2xs font-semibold"
                      : "text-muted-foreground hover:text-foreground"
                  )}
                >
                  <span>Parsed Testcases</span>
                  {parseResult?.success && (
                    <Badge
                      variant="secondary"
                      className="h-4 px-1 text-[9px] font-mono leading-none"
                    >
                      {parseResult.totalParsed}
                    </Badge>
                  )}
                </button>

                <button
                  type="button"
                  onClick={() => setResultTab("stdout")}
                  className={cn(
                    "flex h-6.5 cursor-pointer items-center gap-1.5 rounded-sm px-2 text-xs font-medium transition-colors",
                    resultTab === "stdout"
                      ? "bg-background text-foreground shadow-2xs font-semibold"
                      : "text-muted-foreground hover:text-foreground"
                  )}
                >
                  <span>Raw stdout</span>
                </button>

                {(generatorResult?.stderr || generatorResult?.error) && (
                  <button
                    type="button"
                    onClick={() => setResultTab("stderr")}
                    className={cn(
                      "flex h-6.5 cursor-pointer items-center gap-1.5 rounded-sm px-2 text-xs font-medium text-destructive transition-colors",
                      resultTab === "stderr"
                        ? "bg-destructive/10 text-destructive font-semibold shadow-2xs"
                        : "text-destructive/80 hover:text-destructive"
                    )}
                  >
                    <AlertCircle className="size-3" />
                    <span>Errors / stderr</span>
                  </button>
                )}
              </div>

              {/* Right: Status Badge & Execution Metrics & Add to Hidden Testcases Button */}
              <div className="flex items-center gap-2">
                {isGenerating && (
                  <Badge
                    variant="outline"
                    className="gap-1 border-primary/30 bg-primary/10 text-primary text-[11px]"
                  >
                    <Loader2 className="size-3 animate-spin" />
                    <span>Executing in sandbox...</span>
                  </Badge>
                )}

                {!isGenerating && generatorResult && (
                  <>
                    <Badge
                      variant="outline"
                      className={cn(
                        "gap-1 font-mono text-[10px] uppercase font-semibold",
                        generatorResult.status === "SUCCESS"
                          ? "border-emerald-500/30 bg-emerald-500/10 text-emerald-600 dark:text-emerald-400"
                          : "border-destructive/30 bg-destructive/10 text-destructive"
                      )}
                    >
                      {generatorResult.status === "SUCCESS" ? (
                        <CheckCircle2 className="size-3" />
                      ) : (
                        <AlertCircle className="size-3" />
                      )}
                      <span>{generatorResult.status}</span>
                    </Badge>

                    {generatorResult.executionTimeMs !== undefined && (
                      <span className="hidden sm:flex items-center gap-1 font-mono text-[11px] text-muted-foreground">
                        <Clock className="size-3" />
                        <span>{generatorResult.executionTimeMs}ms</span>
                      </span>
                    )}

                    {generatorResult.memoryUsageKb !== undefined && (
                      <span className="hidden sm:flex items-center gap-1 font-mono text-[11px] text-muted-foreground">
                        <Cpu className="size-3" />
                        <span>{(generatorResult.memoryUsageKb / 1024).toFixed(1)}MB</span>
                      </span>
                    )}
                  </>
                )}

                {parseResult && parseResult.success && (
                  <>
                    {isAlreadyAdded ? (
                      <Badge
                        variant="secondary"
                        className="gap-1 bg-muted font-normal text-muted-foreground"
                      >
                        <Check className="size-3 text-emerald-500" />
                        <span>Batch Added</span>
                      </Badge>
                    ) : (
                      <Button
                        type="button"
                        size="xs"
                        onClick={handleAddGeneratedToHidden}
                        className="cursor-pointer gap-1.5 bg-emerald-600 px-2.5 text-xs font-semibold text-white hover:bg-emerald-700 shadow-xs"
                      >
                        <PlusCircle className="size-3.5" />
                        <span>
                          Add to Hidden Testcases ({parseResult.totalParsed})
                        </span>
                      </Button>
                    )}
                  </>
                )}
              </div>
            </div>

            {/* Results Content Body */}
            <div className="min-h-0 w-full flex-1 overflow-hidden p-3 bg-card/40">
              {/* TAB 1: PARSED TESTCASES PREVIEW */}
              {resultTab === "preview" && (
                <div className="flex h-full flex-col overflow-hidden">
                  {parseResult?.success ? (
                    <div className="flex h-full flex-col gap-2">
                      {/* Horizontal Preview Selector Strip */}
                      <div className="flex shrink-0 items-center justify-between border-b border-border/60 pb-2">
                        <div className="no-scrollbar flex min-w-0 items-center gap-1.5 overflow-x-auto">
                          {parseResult.testcases.map((_, idx) => (
                            <button
                              key={idx}
                              type="button"
                              onClick={() => setSelectedPreviewIndex(idx)}
                              className={cn(
                                "flex h-6 cursor-pointer items-center rounded-md px-2.5 font-mono text-[11px] transition-colors",
                                idx === selectedPreviewIndex
                                  ? "bg-foreground text-background font-semibold"
                                  : "bg-muted text-muted-foreground hover:bg-muted/80 hover:text-foreground"
                              )}
                            >
                              Case {idx + 1}
                            </button>
                          ))}
                        </div>

                        <div className="flex shrink-0 items-center gap-2">
                          <Tooltip>
                            <TooltipTrigger
                              render={
                                <Button
                                  type="button"
                                  variant="ghost"
                                  size="icon-xs"
                                  onClick={handleCopyPreview}
                                  className="cursor-pointer text-muted-foreground hover:text-foreground"
                                >
                                  {copiedPreview ? (
                                    <Check className="size-3.5 text-emerald-500" />
                                  ) : (
                                    <Copy className="size-3.5" />
                                  )}
                                </Button>
                              }
                            />
                            <TooltipContent>Copy Testcase</TooltipContent>
                          </Tooltip>
                        </div>
                      </div>

                      {/* Active Preview Content */}
                      <div className="min-h-0 flex-1 overflow-hidden rounded-md border border-border/70 bg-background">
                        <LineNumberedViewer
                          content={parseResult.testcases[selectedPreviewIndex] || ""}
                          showLineNumbers={true}
                          className="border-0 rounded-none h-full"
                          maxHeight="100%"
                        />
                      </div>
                    </div>
                  ) : parseResult && !parseResult.success ? (
                    <div className="flex h-full flex-col items-center justify-center p-4 text-center">
                      <AlertCircle className="size-8 text-destructive mb-2" />
                      <p className="text-xs font-semibold text-destructive">
                        Testcase Parsing Failed
                      </p>
                      <p className="mt-1 max-w-md text-xs text-muted-foreground leading-relaxed">
                        {parseResult.error}
                      </p>
                    </div>
                  ) : generatorResult?.status === "COMPILATION_ERROR" ||
                    generatorResult?.status === "RUNTIME_ERROR" ? (
                    <div className="flex h-full flex-col items-center justify-center p-4 text-center">
                      <AlertCircle className="size-8 text-destructive mb-2" />
                      <p className="text-xs font-semibold text-destructive">
                        Generator Execution Failed ({generatorResult.status})
                      </p>
                      <p className="mt-1 max-w-md text-xs text-muted-foreground">
                        Switch to the Errors / stderr tab to inspect compilation
                        or runtime error details.
                      </p>
                    </div>
                  ) : (
                    <div className="flex h-full flex-col items-center justify-center text-xs text-muted-foreground">
                      <Terminal className="size-7 stroke-[1.5] text-muted-foreground/60 mb-2" />
                      <p>Run your script to generate and preview testcases.</p>
                      <p className="text-[11px] text-muted-foreground/80 mt-1">
                        Use the delimiter to separate multiple testcases.
                      </p>
                    </div>
                  )}
                </div>
              )}

              {/* TAB 2: RAW STDOUT */}
              {resultTab === "stdout" && (
                <div className="h-full w-full overflow-hidden rounded-md border border-border/70 bg-background">
                  <LineNumberedViewer
                    content={generatorResult?.stdout || ""}
                    emptyText="No standard output produced."
                    showLineNumbers={true}
                    className="border-0 rounded-none h-full"
                    maxHeight="100%"
                  />
                </div>
              )}

              {/* TAB 3: STDERR / ERRORS */}
              {resultTab === "stderr" && (
                <div className="h-full w-full overflow-hidden rounded-md border border-destructive/30 bg-destructive/5">
                  <LineNumberedViewer
                    content={generatorResult?.stderr || generatorResult?.error || ""}
                    variant="error"
                    emptyText="No error details reported."
                    showLineNumbers={true}
                    className="border-0 rounded-none h-full"
                    maxHeight="100%"
                  />
                </div>
              )}
            </div>
          </ResizablePanel>
        </ResizablePanelGroup>
      </div>
    </div>
  );
}

export default GeneratorPanel;
