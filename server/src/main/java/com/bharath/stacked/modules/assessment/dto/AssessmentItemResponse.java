package com.bharath.stacked.modules.assessment.dto;

import com.bharath.stacked.modules.assessment.model.AssessmentItem;
import com.bharath.stacked.modules.assessment.model.OptionSnapshot;
import com.bharath.stacked.modules.assessment.model.QuestionSnapshot;
import com.bharath.stacked.modules.coding_problem.enums.CodingProblemType;
import com.bharath.stacked.modules.coding_problem.model.TestCase;
import com.bharath.stacked.modules.execution.dto.CodeExecutionConstraints;
import com.bharath.stacked.modules.mcq.enums.McqType;
import com.bharath.stacked.modules.question_bank.enums.BankQuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentItemResponse {

    private String questionId;
    private BankQuestionType questionType;
    private int order;
    private double points;
    private String title;

    // MCQ
    private String mcqContent;
    private McqType mcqType;
    @Builder.Default
    private List<OptionSnapshot> options = new ArrayList<>();
    private List<String> correctOptionIds; // Only exposed if isAdmin == true

    // Coding
    private String codingStatement;
    private CodingProblemType codingProblemType;
    @Builder.Default
    private List<String> supportedLanguageIds = new ArrayList<>();
    private Map<String, String> starterCode;
    private CodeExecutionConstraints executionConstraints;
    @Builder.Default
    private List<TestCase> sampleTestCases = new ArrayList<>();
    private List<TestCase> hiddenTestCases; // Only exposed if isAdmin == true

    public static AssessmentItemResponse fromModel(AssessmentItem item, boolean isAdmin) {
        if (item == null) return null;
        QuestionSnapshot snap = item.getSnapshot();

        AssessmentItemResponseBuilder builder = AssessmentItemResponse.builder()
                .questionId(item.getQuestionId())
                .questionType(item.getQuestionType())
                .order(item.getOrder())
                .points(item.getPoints())
                .title(snap != null ? snap.getTitle() : "");

        if (snap != null) {
            if (item.getQuestionType() == BankQuestionType.MCQ) {
                builder.mcqContent(snap.getMcqContent())
                        .mcqType(snap.getMcqType())
                        .options(snap.getOptions() != null ? snap.getOptions() : List.of());
                if (isAdmin) {
                    builder.correctOptionIds(snap.getCorrectOptionIds());
                }
            } else if (item.getQuestionType() == BankQuestionType.CODING) {
                builder.codingStatement(snap.getCodingStatement())
                        .codingProblemType(snap.getCodingProblemType())
                        .supportedLanguageIds(snap.getSupportedLanguageIds() != null ? snap.getSupportedLanguageIds() : List.of())
                        .starterCode(snap.getStarterCode())
                        .executionConstraints(snap.getExecutionConstraints())
                        .sampleTestCases(snap.getSampleTestCases() != null ? snap.getSampleTestCases() : List.of());
                if (isAdmin) {
                    builder.hiddenTestCases(snap.getHiddenTestCases());
                }
            }
        }

        return builder.build();
    }
}

