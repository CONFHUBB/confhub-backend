package com.capstone.confhub.service.impl;

import com.capstone.confhub.dto.BiddingDTO;
import com.capstone.confhub.entity.Bidding;
import com.capstone.confhub.entity.Conference;
import com.capstone.confhub.entity.ConferenceActivity;
import com.capstone.confhub.entity.ConferenceTrack;
import com.capstone.confhub.entity.Paper;
import com.capstone.confhub.entity.ReviewerInterest;
import com.capstone.confhub.entity.SubjectArea;
import com.capstone.confhub.entity.TrackReviewSetting;
import com.capstone.confhub.entity.User;
import com.capstone.confhub.repository.BiddingRepository;
import com.capstone.confhub.repository.ConferenceActivityRepository;
import com.capstone.confhub.repository.PaperAuthorRepository;
import com.capstone.confhub.repository.PaperConflictRepository;
import com.capstone.confhub.repository.PaperRepository;
import com.capstone.confhub.repository.ReviewerInterestRepository;
import com.capstone.confhub.repository.TrackReviewSettingRepository;
import com.capstone.confhub.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.capstone.confhub.utils.enums.ActivityType;
import com.capstone.confhub.utils.enums.BidValue;

import com.capstone.confhub.utils.enums.PaperStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BiddingServiceImplTest {

    @Mock
    private BiddingRepository biddingRepository;
    @Mock
    private PaperRepository paperRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ConferenceActivityRepository conferenceActivityRepository;
    @Mock
    private PaperConflictRepository paperConflictRepository;
    @Mock
    private ReviewerInterestRepository reviewerInterestRepository;
    @Mock
    private TrackReviewSettingRepository trackReviewSettingRepository;
    @Mock
    private PaperAuthorRepository paperAuthorRepository;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private BiddingServiceImpl biddingService;

    private Paper paper;
    private User reviewer;
    private Bidding bid;
    private ConferenceActivity biddingActivity;

    @BeforeEach
    void setUp() {
        Conference conference = new Conference();
        conference.setId(1);

        ConferenceTrack track = new ConferenceTrack();
        track.setId(2);
        track.setConference(conference);

        SubjectArea primaryArea = new SubjectArea();
        primaryArea.setId(3);
        primaryArea.setName("AI");

        paper = new Paper();
        paper.setId(10);
        paper.setTrack(track);
        paper.setTitle("Paper Title");
        paper.setAbstractField("Abstract");
        paper.setStatus(PaperStatus.SUBMITTED);
        paper.setPrimarySubjectArea(primaryArea);
        paper.setSecondarySubjectAreas(List.of());

        reviewer = new User();
        reviewer.setId(20);
        reviewer.setFirstName("Jane");
        reviewer.setLastName("Doe");

        bid = new Bidding();
        bid.setId(30);
        bid.setPaper(paper);
        bid.setReviewer(reviewer);
        bid.setBidValue(BidValue.EAGER);
        bid.setCreatedAt(LocalDateTime.now());
        bid.setUpdatedAt(LocalDateTime.now());

        biddingActivity = new ConferenceActivity();
        biddingActivity.setConference(conference);
        biddingActivity.setActivityType(ActivityType.REVIEWER_BIDDING);
        biddingActivity.setIsEnabled(true);
        biddingActivity.setDeadline(LocalDateTime.now().plusDays(1));
    }

    @Test
    void shouldCreateService() {
        assertNotNull(biddingService);
    }

    @Test
    void getBidsByReviewerAndConferenceShouldReturnList() {
        when(biddingRepository.findByReviewer_IdAndPaper_Track_Conference_Id(20, 1)).thenReturn(List.of(bid));

        var result = biddingService.getBidsByReviewerAndConference(20, 1);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getBidsByPaperShouldReturnList() {
        when(biddingRepository.findByPaper_Id(10)).thenReturn(List.of(bid));

        var result = biddingService.getBidsByPaper(10);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getBidsSummaryShouldReturnSummary() {
        when(biddingRepository.countByReviewer_IdAndBidValueAndPaper_Track_Conference_Id(20, BidValue.EAGER, 1)).thenReturn(1L);
        when(biddingRepository.countByReviewer_IdAndBidValueAndPaper_Track_Conference_Id(20, BidValue.WILLING, 1)).thenReturn(0L);
        when(biddingRepository.countByReviewer_IdAndBidValueAndPaper_Track_Conference_Id(20, BidValue.IN_A_PINCH, 1)).thenReturn(0L);
        when(biddingRepository.countByReviewer_IdAndBidValueAndPaper_Track_Conference_Id(20, BidValue.NOT_WILLING, 1)).thenReturn(0L);
        when(paperRepository.findByTrack_Conference_Id(1)).thenReturn(List.of(paper));

        var result = biddingService.getBidsSummary(20, 1);

        assertNotNull(result);
        assertEquals(1L, result.getTotalBids());
        assertEquals(1L, result.getTotalPapers());
    }

    @Test
    void deleteBidShouldDelete() {
        when(biddingRepository.existsById(30)).thenReturn(true);

        biddingService.deleteBid(30);

        verify(biddingRepository).deleteById(30);
    }

    @Test
    void getPapersForBiddingShouldReturnList() {
        ReviewerInterest interest = new ReviewerInterest();
        interest.setReviewer(reviewer);
        interest.setSubjectArea(paper.getPrimarySubjectArea());
        interest.setIsPrimary(true);

        TrackReviewSetting setting = new TrackReviewSetting();
        setting.setIsDoubleBlind(false);

        when(paperRepository.findByTrack_Conference_Id(1)).thenReturn(List.of(paper));
        when(userRepository.findById(20)).thenReturn(Optional.of(reviewer));
        when(paperConflictRepository.findByUser_Id(20)).thenReturn(List.of());
        when(biddingRepository.findByReviewer_IdAndPaper_Track_Conference_Id(20, 1)).thenReturn(List.of(bid));
        when(reviewerInterestRepository.findByReviewer_Id(20)).thenReturn(List.of(interest));
        when(paperAuthorRepository.findByPaperId(10)).thenReturn(List.of());
        when(trackReviewSettingRepository.findByTrackId(2)).thenReturn(Optional.of(setting));

        var result = biddingService.getPapersForBidding(20, 1);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(10, result.get(0).getPaperId());
        assertEquals(BidValue.EAGER, result.get(0).getCurrentBid());
    }

    @Test
    void submitOrUpdateBid_PaperNotFound() {
        com.capstone.confhub.dto.BiddingDTO dto = new com.capstone.confhub.dto.BiddingDTO();
        dto.setPaperId(999);
        when(paperRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(
            com.capstone.confhub.exception.ResourceNotFoundException.class,
            () -> biddingService.submitOrUpdateBid(dto)
        );
    }
}
