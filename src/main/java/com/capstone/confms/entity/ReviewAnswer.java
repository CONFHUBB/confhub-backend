package com.capstone.confms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "review_answers")
public class ReviewAnswer extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private ReviewQuestion question;

    /**
     * Giá trị trả lời — cho cả text và numeric.
     * Với câu hỏi multiple choice: lưu ID của choice đã chọn.
     * Với câu hỏi text: lưu nội dung text.
     * Với câu hỏi rating/scale: lưu giá trị số (dạng String).
     */
    @Column(name = "answer_value", columnDefinition = "TEXT")
    private String answerValue;

    /**
     * ID của choice đã chọn (nếu câu hỏi là multiple choice/radio).
     * Nullable nếu câu hỏi là text/rating.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_choice_id")
    private ReviewQuestionChoice selectedChoice;
}
