import { Plus, Trash2, Database, Table as TableIcon } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { cn } from "@/lib/utils"
import {
  SQL_COLUMN_TYPES,
  type DatabaseTableSchema,
  type DatabaseColumn,
} from "../types"

export interface SchemaBuilderProps {
  tables: DatabaseTableSchema[]
  onChange: (tables: DatabaseTableSchema[]) => void
  className?: string
  readOnly?: boolean
}

export function SchemaBuilder({
  tables,
  onChange,
  className,
  readOnly = false,
}: SchemaBuilderProps) {
  const handleAddTable = () => {
    const nextIdx = tables.length + 1
    const newTable: DatabaseTableSchema = {
      id: `table-${Date.now()}`,
      name: `Table${nextIdx}`,
      columns: [
        { name: "id", type: "int" },
        { name: "name", type: "varchar" },
      ],
    }
    onChange([...tables, newTable])
  }

  const handleRemoveTable = (tableId: string) => {
    onChange(tables.filter((t) => t.id !== tableId))
  }

  const handleUpdateTableName = (tableId: string, name: string) => {
    const cleaned = name.trim().replace(/\s+/g, "_")
    onChange(
      tables.map((t) => (t.id === tableId ? { ...t, name: cleaned } : t))
    )
  }

  const handleAddColumn = (tableId: string) => {
    onChange(
      tables.map((t) => {
        if (t.id !== tableId) return t
        const newCol: DatabaseColumn = {
          name: `col_${t.columns.length + 1}`,
          type: "varchar",
        }
        return { ...t, columns: [...t.columns, newCol] }
      })
    )
  }

  const handleRemoveColumn = (tableId: string, colIndex: number) => {
    onChange(
      tables.map((t) => {
        if (t.id !== tableId) return t
        return {
          ...t,
          columns: t.columns.filter((_, i) => i !== colIndex),
        }
      })
    )
  }

  const handleUpdateColumn = (
    tableId: string,
    colIndex: number,
    field: "name" | "type",
    val: string
  ) => {
    onChange(
      tables.map((t) => {
        if (t.id !== tableId) return t
        const cols = [...t.columns]
        cols[colIndex] = {
          ...cols[colIndex],
          [field]: field === "name" ? val.trim().replace(/\s+/g, "_") : val,
        }
        return { ...t, columns: cols }
      })
    )
  }

  return (
    <div className={cn("rounded-xl border border-border/80 bg-card p-5 shadow-xs space-y-4", className)}>
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <Database className="size-4 text-primary" />
          <h3 className="text-sm font-semibold text-foreground">
            Database Schema (Input Tables)
          </h3>
        </div>

        {!readOnly && (
          <Button
            type="button"
            variant="outline"
            size="xs"
            onClick={handleAddTable}
            className="cursor-pointer gap-1 text-xs"
          >
            <Plus className="size-3.5" />
            <span>Add Table</span>
          </Button>
        )}
      </div>

      <p className="text-xs text-muted-foreground">
        Define relational tables, column names and SQL data types. Both Examples and Sample Test Cases will populate data for these tables.
      </p>

      {/* Tables List */}
      {tables.length === 0 ? (
        <div className="flex flex-col items-center justify-center rounded-xl border border-dashed border-border/80 bg-muted/20 p-6 text-center text-xs text-muted-foreground">
          <TableIcon className="size-5 text-muted-foreground mb-1" />
          <p>No tables defined yet.</p>
          {!readOnly && (
            <Button
              type="button"
              variant="secondary"
              size="xs"
              onClick={handleAddTable}
              className="mt-2 cursor-pointer gap-1 text-xs"
            >
              <Plus className="size-3" />
              <span>Add First Table</span>
            </Button>
          )}
        </div>
      ) : (
        <div className="space-y-4">
          {tables.map((table, tIdx) => (
            <div
              key={table.id || tIdx}
              className="space-y-3 rounded-xl border border-border/70 bg-muted/20 p-3.5"
            >
              {/* Table Header */}
              <div className="flex items-center justify-between gap-2">
                <div className="flex items-center gap-2 flex-1 max-w-sm">
                  <Label className="text-xs font-semibold text-foreground shrink-0">
                    Table Name:
                  </Label>
                  <Input
                    disabled={readOnly}
                    value={table.name}
                    onChange={(e) => handleUpdateTableName(table.id, e.target.value)}
                    placeholder="e.g. Users"
                    className="h-8 font-mono text-xs bg-background"
                  />
                </div>

                {!readOnly && (
                  <div className="flex items-center gap-1.5">
                    <Button
                      type="button"
                      variant="outline"
                      size="xs"
                      onClick={() => handleAddColumn(table.id)}
                      className="h-7 cursor-pointer gap-1 text-[11px]"
                    >
                      <Plus className="size-3" />
                      <span>Add Column</span>
                    </Button>

                    <Button
                      type="button"
                      variant="ghost"
                      size="icon-xs"
                      onClick={() => handleRemoveTable(table.id)}
                      className="h-7 w-7 text-muted-foreground hover:text-destructive cursor-pointer"
                      title="Remove Table"
                    >
                      <Trash2 className="size-3.5" />
                    </Button>
                  </div>
                )}
              </div>

              {/* Columns Table */}
              <div className="space-y-1.5 pt-1">
                <Label className="text-[11px] font-semibold text-muted-foreground uppercase tracking-wider">
                  Columns ({table.columns.length}):
                </Label>

                <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-2">
                  {table.columns.map((col, cIdx) => (
                    <div
                      key={cIdx}
                      className="flex items-center gap-1.5 rounded-lg border border-border/60 bg-background p-2 text-xs shadow-2xs"
                    >
                      <Input
                        disabled={readOnly}
                        value={col.name}
                        onChange={(e) =>
                          handleUpdateColumn(table.id, cIdx, "name", e.target.value)
                        }
                        placeholder="col_name"
                        className="h-7 flex-1 font-mono text-[11px]"
                      />

                      <div className="w-24 shrink-0">
                        <Select
                          disabled={readOnly}
                          value={col.type}
                          onValueChange={(val) => {
                            if (val) handleUpdateColumn(table.id, cIdx, "type", val)
                          }}
                        >
                          <SelectTrigger className="h-7 w-full font-mono text-[11px]">
                            <SelectValue placeholder="type" />
                          </SelectTrigger>
                          <SelectContent className="max-h-48 font-mono text-xs">
                            {SQL_COLUMN_TYPES.map((type) => (
                              <SelectItem key={type} value={type}>
                                {type}
                              </SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                      </div>

                      {!readOnly && table.columns.length > 1 && (
                        <button
                          type="button"
                          onClick={() => handleRemoveColumn(table.id, cIdx)}
                          className="text-muted-foreground hover:text-destructive cursor-pointer p-0.5"
                          title="Remove Column"
                        >
                          <Trash2 className="size-3" />
                        </button>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

export default SchemaBuilder
