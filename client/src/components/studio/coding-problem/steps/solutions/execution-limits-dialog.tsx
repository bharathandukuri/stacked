import { useState, useMemo } from "react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
  DialogClose,
} from "@/components/ui/dialog";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { RotateCcw, Clock, Cpu } from "lucide-react";
import { useCodingProblemStore } from "../../store";
import {
  getLanguageById,
  getDefaultExecutionConstraints,
} from "@/config/languages";

export interface ExecutionLimitsDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function ExecutionLimitsDialog({
  open,
  onOpenChange,
}: ExecutionLimitsDialogProps) {
  const values = useCodingProblemStore((state) => state.values);
  const executionConstraints = useCodingProblemStore(
    (state) => state.executionConstraints
  );
  const actions = useCodingProblemStore((state) => state.actions);

  const supportedLanguages = useMemo(() => {
    return values.supportedLanguageIds.length > 0
      ? values.supportedLanguageIds
      : values.languages.length > 0
        ? values.languages
        : ["javascript-node-20"];
  }, [values.supportedLanguageIds, values.languages]);

  const activeCodeLanguage = useCodingProblemStore(
    (state) => state.activeCodeLanguage
  );

  const [selectedLang, setSelectedLang] = useState<string | null>(null);

  const currentLangId =
    selectedLang && supportedLanguages.includes(selectedLang)
      ? selectedLang
      : supportedLanguages.includes(activeCodeLanguage)
        ? activeCodeLanguage
        : supportedLanguages[0];

  const handleOpenChange = (nextOpen: boolean) => {
    if (!nextOpen) {
      setSelectedLang(null);
    }
    onOpenChange(nextOpen);
  };

  const currentLimits = executionConstraints[currentLangId] || {};
  const defaultLimits = getDefaultExecutionConstraints(currentLangId);

  const currentTimeLimit = currentLimits.timeLimitMs ?? defaultLimits.timeLimitMs;
  const currentMemoryLimit =
    currentLimits.memoryLimitKb ?? defaultLimits.memoryLimitKb;

  const handleTimeChange = (val: string) => {
    const num = val === "" ? undefined : Math.max(1, Math.min(60000, Number(val)));
    actions.setExecutionConstraint(currentLangId, {
      ...currentLimits,
      timeLimitMs: num,
    });
  };

  const handleMemoryChange = (val: string) => {
    const num = val === "" ? undefined : Math.max(1, Math.min(1048576, Number(val)));
    actions.setExecutionConstraint(currentLangId, {
      ...currentLimits,
      memoryLimitKb: num,
    });
  };

  const handleResetCurrent = () => {
    actions.resetExecutionConstraints(currentLangId);
  };

  const langItemsRecord = useMemo(
    () =>
      supportedLanguages.reduce<Record<string, string>>((acc, id) => {
        acc[id] = getLanguageById(id)?.name || id;
        return acc;
      }, {}),
    [supportedLanguages]
  );

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <div className="flex items-center gap-2">
            <Clock className="size-4 text-primary" />
            <DialogTitle>Execution Limits</DialogTitle>
          </div>
          <DialogDescription>
            Configure time and memory limits per language.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4 py-2">
          {/* Language Selector Dropdown */}
          <div className="space-y-1.5">
            <Label className="text-xs font-medium text-muted-foreground">
              Language
            </Label>
            <Select
              value={currentLangId}
              items={langItemsRecord}
              onValueChange={(val) => {
                if (val) setSelectedLang(val);
              }}
            >
              <SelectTrigger size="sm" className="w-full justify-between h-9 text-xs">
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

          {/* Time & Memory Inputs */}
          <div className="grid grid-cols-2 gap-3 pt-1">
            <div className="space-y-1.5">
              <Label
                htmlFor="dialog-time-limit"
                className="flex items-center gap-1 text-xs font-medium"
              >
                <Clock className="size-3 text-primary" />
                <span>Time Limit (ms)</span>
              </Label>
              <Input
                id="dialog-time-limit"
                type="number"
                min={1}
                max={60000}
                step={100}
                value={currentTimeLimit}
                onChange={(e) => handleTimeChange(e.target.value)}
                placeholder={String(defaultLimits.timeLimitMs)}
                className="font-mono text-xs h-9"
              />
              <span className="text-[10px] text-muted-foreground">
                Default: {defaultLimits.timeLimitMs} ms
              </span>
            </div>

            <div className="space-y-1.5">
              <Label
                htmlFor="dialog-memory-limit"
                className="flex items-center gap-1 text-xs font-medium"
              >
                <Cpu className="size-3 text-primary" />
                <span>Memory Limit (KB)</span>
              </Label>
              <Input
                id="dialog-memory-limit"
                type="number"
                min={1}
                max={1048576}
                step={1024}
                value={currentMemoryLimit}
                onChange={(e) => handleMemoryChange(e.target.value)}
                placeholder={String(defaultLimits.memoryLimitKb)}
                className="font-mono text-xs h-9"
              />
              <span className="text-[10px] text-muted-foreground">
                Default: {Math.round(defaultLimits.memoryLimitKb / 1024)} MB
              </span>
            </div>
          </div>
        </div>

        <DialogFooter className="flex items-center justify-between sm:justify-between pt-2">
          <Button
            type="button"
            variant="ghost"
            size="xs"
            onClick={handleResetCurrent}
            className="cursor-pointer gap-1 text-xs text-muted-foreground hover:text-foreground"
          >
            <RotateCcw className="size-3" />
            <span>Reset to Default</span>
          </Button>

          <DialogClose render={<Button type="button" size="sm" className="cursor-pointer text-xs">Done</Button>} />
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
