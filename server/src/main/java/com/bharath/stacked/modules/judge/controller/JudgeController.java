package com.bharath.stacked.modules.judge.controller;

import com.bharath.stacked.common.api.ApiResponse;
import com.bharath.stacked.modules.judge.dto.SampleRunRequest;
import com.bharath.stacked.modules.judge.dto.SampleRunResponse;
import com.bharath.stacked.modules.judge.languages.LanguageDetails;
import com.bharath.stacked.modules.judge.languages.LanguageRegistry;
import com.bharath.stacked.modules.judge.service.JudgeService;
import jakarta.validation.Valid;
import org.jspecify.annotations.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RestController
@RequestMapping("/api/judge")
public class JudgeController {

    private final JudgeService judgeService;
    private final LanguageRegistry languageRegistry;

    public JudgeController(JudgeService judgeService, LanguageRegistry languageRegistry) {
        this.judgeService = judgeService;
        this.languageRegistry = languageRegistry;
    }

    @GetMapping("/languages")
    @NonNull
    public ResponseEntity<ApiResponse<Collection<LanguageDetails>>> getAllLanguages() {
        return ApiResponse.success(languageRegistry.getAllLanguages(), "Supported languages retrieved successfully");
    }

    @GetMapping("/languages/{id}")
    @NonNull
    public ResponseEntity<ApiResponse<LanguageDetails>> getLanguage(@PathVariable @NonNull String id) {
        LanguageDetails language = languageRegistry.getLanguageOrThrow(id);
        return ApiResponse.success(language, "Language details retrieved successfully");
    }

    @PostMapping("/sample-run")
    @NonNull
    public ResponseEntity<ApiResponse<SampleRunResponse>> executeSampleRun(
            @Valid @RequestBody @NonNull SampleRunRequest request) {
        SampleRunResponse response = judgeService.executeSampleRun(request);
        return ApiResponse.success(response, "Sample run executed successfully");
    }
}
