package com.capstone.confms.service.impl;

import com.capstone.confms.dto.ReviewCommentDTO;
import com.capstone.confms.entity.Review;
import com.capstone.confms.entity.ReviewComment;
import com.capstone.confms.repository.ReviewCommentRepository;
import com.capstone.confms.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReviewCommentServiceImplTest {

    @Mock
    private ReviewCommentRepository reviewCommentRepository;
    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private ReviewCommentServiceImpl reviewCommentService;

    private Review review;
    private ReviewComment comment;

    @BeforeEach
    void setUp() {
        review = new Review();
        review.setId(1);

        comment = new ReviewComment();
        comment.setId(10);
        comment.setReview(review);
        comment.setContent("Good paper");
        comment.setIsVisibleToAuthor(true);
    }

    @Test
    void shouldCreateService() {
        assertNotNull(reviewCommentService);
    }

    @Test
    void getAllReviewCommentsShouldReturnPagedResponse() {
        var page = new PageImpl<>(List.of(comment), PageRequest.of(0, 20), 1);
        when(reviewCommentRepository.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        var result = reviewCommentService.getAllReviewComments(0, 20);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void createReviewCommentShouldReturnResponse() {
        ReviewCommentDTO dto = ReviewCommentDTO.builder().reviewId(1).content("Good paper").isVisibleToAuthor(true).build();
        when(reviewRepository.findById(1)).thenReturn(Optional.of(review));
        when(reviewCommentRepository.save(any(ReviewComment.class))).thenReturn(comment);

        var result = reviewCommentService.createReviewComment(dto);

        assertNotNull(result);
        assertEquals(10, result.getId());
    }

    @Test
    void updateReviewCommentShouldReturnResponse() {
        ReviewCommentDTO dto = ReviewCommentDTO.builder().reviewId(1).content("Updated").isVisibleToAuthor(false).build();
        when(reviewCommentRepository.findById(10)).thenReturn(Optional.of(comment));
        when(reviewCommentRepository.save(any(ReviewComment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = reviewCommentService.updateReviewComment(10, dto);

        assertNotNull(result);
        assertEquals("Updated", result.getContent());
        assertEquals(false, result.getIsVisibleToAuthor());
    }

    @Test
    void getReviewCommentByIdShouldReturnResponse() {
        when(reviewCommentRepository.findById(10)).thenReturn(Optional.of(comment));

        var result = reviewCommentService.getReviewCommentById(10);

        assertNotNull(result);
        assertEquals(10, result.getId());
    }

    @Test
    void deleteReviewCommentShouldDelete() {
        when(reviewCommentRepository.existsById(10)).thenReturn(true);

        reviewCommentService.deleteReviewComment(10);

        verify(reviewCommentRepository).deleteById(10);
    }
}



