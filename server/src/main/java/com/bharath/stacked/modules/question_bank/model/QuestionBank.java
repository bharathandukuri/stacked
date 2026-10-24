package com.bharath.stacked.modules.question_bank.model;

import com.bharath.stacked.modules.question_bank.enums.QuestionBankStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "question_banks")
@CompoundIndexes({
    @CompoundIndex(name = "status_category_updated_idx", def = "{'status': 1, 'category': 1, 'updatedAt': -1}"),
    @CompoundIndex(name = "status_updated_idx", def = "{'status': 1, 'updatedAt': -1}")
})
public class QuestionBank {

    @Id
    private String id;

    private String title;

    private String description;

    @Indexed
    private String category;

    @Indexed
    @Builder.Default
    private QuestionBankStatus status = QuestionBankStatus.DRAFT;

    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @Builder.Default
    private List<QuestionBankItem> items = new ArrayList<>();

    private int totalQuestions;

    private int mcqCount;

    private int codingCount;

    private double totalPoints;

    private String authorId;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}

