package com.capstone.confhub.service.impl;

import com.capstone.confhub.repository.BiddingRepository;
import com.capstone.confhub.repository.ConferenceUserTrackRepository;
import com.capstone.confhub.repository.NotificationRepository;
import com.capstone.confhub.repository.PaperConflictRepository;
import com.capstone.confhub.repository.PaperRepository;
import com.capstone.confhub.repository.ReviewRepository;
import com.capstone.confhub.repository.ReviewerInterestRepository;
import com.capstone.confhub.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class ReviewerAssignmentServiceImplTest {

    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private PaperRepository paperRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private BiddingRepository biddingRepository;
    @Mock
    private PaperConflictRepository paperConflictRepository;
    @Mock
    private ReviewerInterestRepository reviewerInterestRepository;
    @Mock
    private ConferenceUserTrackRepository conferenceUserTrackRepository;
    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private ReviewerAssignmentServiceImpl reviewerAssignmentService;

    @Test
    void shouldCreateService() {
        assertNotNull(reviewerAssignmentService);
    }
}
