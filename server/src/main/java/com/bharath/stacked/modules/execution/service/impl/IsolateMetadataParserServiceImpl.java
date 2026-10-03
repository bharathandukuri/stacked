package com.bharath.stacked.modules.execution.service.impl;

import com.bharath.stacked.modules.execution.enums.IsolateExecutionStatus;
import com.bharath.stacked.modules.execution.model.IsolateExecutionResult;
import com.bharath.stacked.modules.execution.service.IsolateMetadataParserService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class IsolateMetadataParserServiceImpl implements IsolateMetadataParserService {
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
                parseStatus(values.get("status"), values.get("message")),
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

    private IsolateExecutionStatus parseStatus(String status, String message) {
        if (message != null && message.toLowerCase().contains("memory")) {
            return IsolateExecutionStatus.MEMORY_LIMIT_EXCEEDED;
        }

        if (status == null || status.isBlank()) {
            return IsolateExecutionStatus.SUCCESS;
        }

        return switch (status) {
            case "RE", "SG" -> IsolateExecutionStatus.RUNTIME_ERROR;
            case "TO" -> IsolateExecutionStatus.TIME_LIMIT_EXCEEDED;
            case "ML" -> IsolateExecutionStatus.MEMORY_LIMIT_EXCEEDED;
            case "XX" -> IsolateExecutionStatus.SYSTEM_ERROR;
            default -> IsolateExecutionStatus.SYSTEM_ERROR;
        };
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
