package com.capstone.confms.service.impl;

import com.capstone.confms.dto.ReviewerInterestDTO;
import com.capstone.confms.entity.ReviewerInterest;
import com.capstone.confms.entity.SubjectArea;
import com.capstone.confms.entity.User;
import com.capstone.confms.repository.ReviewerInterestRepository;
import com.capstone.confms.repository.SubjectAreaRepository;
import com.capstone.confms.repository.UserRepository;
import com.capstone.confms.utils.enums.Expertise;
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
public class ReviewerInterestServiceImplTest {

    @Mock
    private ReviewerInterestRepository reviewerInterestRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SubjectAreaRepository subjectAreaRepository;

    @InjectMocks
    private ReviewerInterestServiceImpl reviewerInterestService;

    private User reviewer;
    private SubjectArea subjectArea;
    private ReviewerInterest interest;

    @BeforeEach
    void setUp() {
        reviewer = new User();
        reviewer.setId(1);

        subjectArea = new SubjectArea();
        subjectArea.setId(2);

        interest = new ReviewerInterest();
        interest.setId(10);
        interest.setReviewer(reviewer);
        interest.setSubjectArea(subjectArea);
        interest.setExpertise(Expertise.EXPERT);
    }

    @Test
    void shouldCreateService() {
        assertNotNull(reviewerInterestService);
    }

    @Test
    void getAllReviewerInterestsShouldReturnPagedResponse() {
        var page = new PageImpl<>(List.of(interest), PageRequest.of(0, 20), 1);
        when(reviewerInterestRepository.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        var result = reviewerInterestService.getAllReviewerInterests(0, 20);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void createReviewerInterestShouldReturnResponse() {
        ReviewerInterestDTO dto = ReviewerInterestDTO.builder().reviewerId(1).subjectAreaId(2).expertise(Expertise.INTERESTED).build();
        when(userRepository.findById(1)).thenReturn(Optional.of(reviewer));
        when(subjectAreaRepository.findById(2)).thenReturn(Optional.of(subjectArea));
        when(reviewerInterestRepository.save(any(ReviewerInterest.class))).thenReturn(interest);

        var result = reviewerInterestService.createReviewerInterest(dto);

        assertNotNull(result);
        assertEquals(10, result.getId());
    }

    @Test
    void updateReviewerInterestShouldReturnResponse() {
        ReviewerInterestDTO dto = ReviewerInterestDTO.builder().reviewerId(1).subjectAreaId(2).expertise(Expertise.INTERESTED).build();
        when(reviewerInterestRepository.findById(10)).thenReturn(Optional.of(interest));
        when(userRepository.findById(1)).thenReturn(Optional.of(reviewer));
        when(subjectAreaRepository.findById(2)).thenReturn(Optional.of(subjectArea));
        when(reviewerInterestRepository.save(any(ReviewerInterest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = reviewerInterestService.updateReviewerInterest(10, dto);

        assertNotNull(result);
        assertEquals(Expertise.INTERESTED, result.getExpertise());
    }

    @Test
    void getReviewerInterestByIdShouldReturnResponse() {
        when(reviewerInterestRepository.findById(10)).thenReturn(Optional.of(interest));

        var result = reviewerInterestService.getReviewerInterestById(10);

        assertNotNull(result);
        assertEquals(10, result.getId());
    }

    @Test
    void deleteReviewerInterestShouldDelete() {
        when(reviewerInterestRepository.existsById(10)).thenReturn(true);

        reviewerInterestService.deleteReviewerInterest(10);

        verify(reviewerInterestRepository).deleteById(10);
    }

    @Test
    void getInterestsByReviewerIdShouldReturnList() {
        when(reviewerInterestRepository.findByReviewer_Id(1)).thenReturn(List.of(interest));

        var result = reviewerInterestService.getInterestsByReviewerId(1);

        assertNotNull(result);
        assertEquals(1, result.size());
    }
}




