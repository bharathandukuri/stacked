package com.bharath.stacked.modules.execution.mapper;

import com.bharath.stacked.modules.execution.enums.IsolateExecutionStatus;
import com.bharath.stacked.modules.execution.dto.response.IsolateExecutionResult;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class IsolateMetadataParser {
    public IsolateExecutionResult parseMetadata(
            String metadata,
            String stdout,
            String stderr
    ) {
        Map<String, String> values = new HashMap<>();

        for (String line : metadata.split("\\R")) {
            line = line.trim();

            if (line.isEmpty()) {
                continue;
            }

            int separator = line.indexOf(':');

            if (separator <= 0) {
                continue;
            }

            String key = line.substring(0, separator).trim();
            String value = line.substring(separator + 1).trim();

            values.put(key, value);
        }

        return new IsolateExecutionResult(
                parseStatus(values.get("status"), values.get("message"), values.get("exitcode"), stderr),
                stdout,
                stderr,
                parseDouble(values.get("time")),
                parseDouble(values.get("time-wall")),
                parseLong(values.get("max-rss")),
                parseLong(values.get("exitcode")),
                parseLong(values.get("exitsig")),
                parseBoolean(values.get("killed")),
                parseLong(values.get("csw-voluntary")),
                parseLong(values.get("csw-forced"))
        );
    }

    private IsolateExecutionStatus parseStatus(String status, String message, String exitcode, String stderr) {
        if ("TO".equalsIgnoreCase(status) || (message != null && message.toLowerCase().contains("time limit"))) {
            return IsolateExecutionStatus.TIME_LIMIT_EXCEEDED;
        }

        if (isMemoryLimitExceeded(status, message, stderr)) {
            return IsolateExecutionStatus.MEMORY_LIMIT_EXCEEDED;
        }

        if (status == null || status.isBlank()) {
            if (exitcode != null && !exitcode.isBlank() && !"0".equals(exitcode.trim())) {
                return IsolateExecutionStatus.RUNTIME_ERROR;
            }
            return IsolateExecutionStatus.SUCCESS;
        }

        return switch (status) {
            case "RE", "SG" -> IsolateExecutionStatus.RUNTIME_ERROR;
            case "TO" -> IsolateExecutionStatus.TIME_LIMIT_EXCEEDED;
            case "ML" -> IsolateExecutionStatus.MEMORY_LIMIT_EXCEEDED;
            default -> IsolateExecutionStatus.SYSTEM_ERROR;
        };
    }

    private boolean isMemoryLimitExceeded(String status, String message, String stderr) {
        if ("ML".equalsIgnoreCase(status)) {
            return true;
        }
        if (message != null && message.toLowerCase().contains("memory")) {
            return true;
        }
        if (stderr != null && !stderr.isBlank()) {
            String lowerStderr = stderr.toLowerCase();
            // Stack exhaustion must be classified as RUNTIME_ERROR, not MEMORY_LIMIT_EXCEEDED
            if (lowerStderr.contains("stackoverflowerror")
                    || lowerStderr.contains("recursionerror")
                    || lowerStderr.contains("maximum call stack size exceeded")) {
                return false;
            }

            if (lowerStderr.contains("std::bad_alloc")
                    || lowerStderr.contains("memoryerror")
                    || lowerStderr.contains("outofmemoryerror")
                    || lowerStderr.contains("heap out of memory")
                    || lowerStderr.contains("out of memory")
                    || (lowerStderr.contains("allocation failed") && lowerStderr.contains("heap"))) {
                return true;
            }
        }
        return false;
    }

    private Double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return Double.parseDouble(value);
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return Long.parseLong(value);
    }

    private Integer parseInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return Integer.parseInt(value);
    }

    private Boolean parseBoolean(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return Boolean.parseBoolean(value);
    }
}
