package com.bharath.stacked.modules.execution.service;

import com.bharath.stacked.modules.execution.model.IsolateExecutionResult;

public interface IsolateMetadataParserService {
    IsolateExecutionResult parseMetadata(String metadata, String stdout, String stderr);
}
