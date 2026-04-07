package com.capstone.confhub.service.impl;

import com.capstone.confhub.dto.request.AutoAssignConfigDTO;
import com.capstone.confhub.dto.response.AssignmentPreviewDTO;
import com.capstone.confhub.dto.response.AssignmentPreviewItemDTO;
import com.capstone.confhub.entity.Bidding;
import com.capstone.confhub.entity.Conference;
import com.capstone.confhub.entity.ConferenceTrack;
import com.capstone.confhub.entity.ConferenceUserTrack;
import com.capstone.confhub.entity.Notification;
import com.capstone.confhub.entity.Paper;
import com.capstone.confhub.entity.PaperAuthor;
import com.capstone.confhub.entity.Review;
import com.capstone.confhub.entity.ReviewerInterest;
import com.capstone.confhub.entity.SubjectArea;
import com.capstone.confhub.entity.TrackReviewSetting;
import com.capstone.confhub.entity.User;
import com.capstone.confhub.exception.BadRequestException;
import com.capstone.confhub.repository.BiddingRepository;
import com.capstone.confhub.repository.ConferenceUserTrackRepository;
import com.capstone.confhub.repository.NotificationRepository;
import com.capstone.confhub.repository.PaperAuthorRepository;
import com.capstone.confhub.repository.PaperConflictRepository;
import com.capstone.confhub.repository.PaperRepository;
import com.capstone.confhub.repository.ReviewRepository;
import com.capstone.confhub.repository.ReviewerInterestRepository;
import com.capstone.confhub.repository.TrackReviewSettingRepository;
import com.capstone.confhub.repository.UserRepository;
import com.capstone.confhub.utils.enums.BidValue;
import com.capstone.confhub.utils.enums.ConferenceTrackRole;
import com.capstone.confhub.utils.enums.ReviewStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    private PaperAuthorRepository paperAuthorRepository;
    @Mock
    private ReviewerInterestRepository reviewerInterestRepository;
    @Mock
    private ConferenceUserTrackRepository conferenceUserTrackRepository;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private TrackReviewSettingRepository trackReviewSettingRepository;

    @InjectMocks
    private ReviewerAssignmentServiceImpl reviewerAssignmentService;

    private Conference conference;
    private ConferenceTrack track;
    private Paper paper1;
    private Paper paper2;
    private User reviewer1;
    private User reviewer2;

    @BeforeEach
    void setUp() {
        conference = new Conference();
        conference.setId(1);
        conference.setName("ConfHub");

        track = new ConferenceTrack();
        track.setId(10);
        track.setConference(conference);

        paper1 = new Paper();
        paper1.setId(100);
        paper1.setTitle("Paper One");
        paper1.setTrack(track);

        paper2 = new Paper();
        paper2.setId(101);
        paper2.setTitle("Paper Two");
        paper2.setTrack(track);

        reviewer1 = new User();
        reviewer1.setId(201);
        reviewer1.setFirstName("Alice");
        reviewer1.setLastName("R1");
        reviewer1.setEmail("alice@uni.edu");

        reviewer2 = new User();
        reviewer2.setId(202);
        reviewer2.setFirstName("Bob");
        reviewer2.setLastName("R2");
        reviewer2.setEmail("bob@uni.edu");
    }

    @Test
    void shouldCreateService() {
        assertNotNull(reviewerAssignmentService);
    }

    @Test
    void runAutoAssignShouldThrowWhenNoPapers() {
        when(paperRepository.findByTrack_Conference_Id(1)).thenReturn(List.of());

        assertThrows(BadRequestException.class, () -> reviewerAssignmentService.runAutoAssign(config(1, 2, 3)));
    }

    @Test
    void runAutoAssignShouldThrowWhenNoReviewers() {
        when(paperRepository.findByTrack_Conference_Id(1)).thenReturn(List.of(paper1));
        when(conferenceUserTrackRepository.findByConference_IdAndAssignedRole(1, ConferenceTrackRole.REVIEWER)).thenReturn(List.of());

        assertThrows(BadRequestException.class, () -> reviewerAssignmentService.runAutoAssign(config(1, 1, 2)));
    }

    @Test
    void runAutoAssignShouldCreateAssignmentsWhenCandidatesAvailable() {
        when(paperRepository.findByTrack_Conference_Id(1)).thenReturn(List.of(paper1, paper2));
        when(conferenceUserTrackRepository.findByConference_IdAndAssignedRole(1, ConferenceTrackRole.REVIEWER))
                .thenReturn(List.of(reviewerTrack(reviewer1, 3), reviewerTrack(reviewer2, 3)));

        when(trackReviewSettingRepository.findByTrackId(10)).thenReturn(Optional.of(trackSetting(true)));

        when(paperAuthorRepository.findByPaperId(100)).thenReturn(List.of(authorOf(paper1, 300, "author1@x.edu")));
        when(paperAuthorRepository.findByPaperId(101)).thenReturn(List.of(authorOf(paper2, 301, "author2@x.edu")));

        when(biddingRepository.findByPaper_Id(100)).thenReturn(List.of(bid(paper1, reviewer1, BidValue.EAGER), bid(paper1, reviewer2, BidValue.WILLING)));
        when(biddingRepository.findByPaper_Id(101)).thenReturn(List.of(bid(paper2, reviewer1, BidValue.WILLING), bid(paper2, reviewer2, BidValue.EAGER)));

        when(reviewerInterestRepository.findByReviewer_Id(any())).thenReturn(List.of());
        when(paperConflictRepository.existsByPaper_IdAndUser_Id(any(), any())).thenReturn(false);
        when(reviewRepository.existsByPaper_IdAndReviewer_Id(any(), any())).thenReturn(false);
        when(reviewRepository.findByPaper_Track_Conference_Id(1)).thenReturn(List.of());

        AssignmentPreviewDTO result = reviewerAssignmentService.runAutoAssign(config(1, 1, 2));

        assertEquals(2, result.getTotalAssignments());
        assertEquals(0, result.getUnassignedPapers());
    }

    @Test
    void runAutoAssignShouldSkipDomainConflictWhenEnabled() {
        reviewer1.setEmail("alice@same.edu");

        when(paperRepository.findByTrack_Conference_Id(1)).thenReturn(List.of(paper1));
        when(conferenceUserTrackRepository.findByConference_IdAndAssignedRole(1, ConferenceTrackRole.REVIEWER))
                .thenReturn(List.of(reviewerTrack(reviewer1, 3)));

        when(trackReviewSettingRepository.findByTrackId(10)).thenReturn(Optional.of(trackSetting(true)));
        when(paperAuthorRepository.findByPaperId(100)).thenReturn(List.of(authorOf(paper1, 300, "author@same.edu")));
        when(biddingRepository.findByPaper_Id(100)).thenReturn(List.of(bid(paper1, reviewer1, BidValue.EAGER)));
        when(reviewerInterestRepository.findByReviewer_Id(201)).thenReturn(List.of());
        when(paperConflictRepository.existsByPaper_IdAndUser_Id(100, 201)).thenReturn(false);
        when(reviewRepository.findByPaper_Track_Conference_Id(1)).thenReturn(List.of());

        AssignmentPreviewDTO result = reviewerAssignmentService.runAutoAssign(config(1, 1, 2));

        assertEquals(0, result.getAssignments().size());
    }

    @Test
    void confirmAssignmentsShouldCreateReviewAndNotification() {
        AssignmentPreviewItemDTO item = AssignmentPreviewItemDTO.builder().paperId(100).reviewerId(201).build();

        when(reviewRepository.existsByPaper_IdAndReviewer_Id(100, 201)).thenReturn(false);
        when(paperRepository.findById(100)).thenReturn(Optional.of(paper1));
        when(userRepository.findById(201)).thenReturn(Optional.of(reviewer1));

        List<AssignmentPreviewItemDTO> result = reviewerAssignmentService.confirmAssignments(1, List.of(item));

        assertEquals(1, result.size());
        verify(reviewRepository).save(any(Review.class));
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void manualAssignShouldThrowWhenConflictExists() {
        when(paperConflictRepository.existsByPaper_IdAndUser_Id(100, 201)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> reviewerAssignmentService.manualAssign(100, 201));
    }

    @Test
    void manualAssignShouldCreateReviewWhenValid() {
        when(paperConflictRepository.existsByPaper_IdAndUser_Id(100, 201)).thenReturn(false);
        when(reviewRepository.existsByPaper_IdAndReviewer_Id(100, 201)).thenReturn(false);
        when(paperRepository.findById(100)).thenReturn(Optional.of(paper1));
        when(userRepository.findById(201)).thenReturn(Optional.of(reviewer1));

        AssignmentPreviewItemDTO result = reviewerAssignmentService.manualAssign(100, 201);

        assertEquals(100, result.getPaperId());
        assertEquals(201, result.getReviewerId());
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void removeAssignmentShouldThrowWhenCompleted() {
        Review review = new Review();
        review.setId(1);
        review.setStatus(ReviewStatus.COMPLETED);
        when(reviewRepository.findById(1)).thenReturn(Optional.of(review));

        assertThrows(BadRequestException.class, () -> reviewerAssignmentService.removeAssignment(1));
    }

    @Test
    void removeAssignmentShouldDeleteWhenAssigned() {
        Review review = new Review();
        review.setId(1);
        review.setStatus(ReviewStatus.ASSIGNED);
        when(reviewRepository.findById(1)).thenReturn(Optional.of(review));

        reviewerAssignmentService.removeAssignment(1);

        verify(reviewRepository).deleteById(1);
    }

    @Test
    void getCurrentAssignmentsShouldReturnStatistics() {
        Review review = new Review();
        review.setId(9);
        review.setPaper(paper1);
        review.setReviewer(reviewer1);
        review.setStatus(ReviewStatus.ASSIGNED);
        review.setTotalScore(BigDecimal.ZERO);

        when(reviewRepository.findByPaper_Track_Conference_Id(1)).thenReturn(List.of(review));
        when(paperRepository.findByTrack_Conference_Id(1)).thenReturn(List.of(paper1));
        when(biddingRepository.findByPaper_Id(100)).thenReturn(List.of(bid(paper1, reviewer1, BidValue.EAGER)));
        when(reviewerInterestRepository.findByReviewer_Id(201)).thenReturn(List.of());
        when(conferenceUserTrackRepository.findByConference_IdAndAssignedRole(1, ConferenceTrackRole.REVIEWER))
                .thenReturn(List.of(reviewerTrack(reviewer1, 3), reviewerTrack(reviewer2, 3)));

        AssignmentPreviewDTO result = reviewerAssignmentService.getCurrentAssignments(1);

        assertEquals(1, result.getTotalAssignments());
        assertEquals(1, result.getReviewersPerPaper().get(100));
        assertEquals(1, result.getPapersPerReviewer().get(201));
    }

    @Test
    void getBidScoreShouldSupportAllBidValues() throws Exception {
        assertEquals(0.25, invokeGetBidScore(null));
        assertEquals(1.0, invokeGetBidScore(BidValue.EAGER));
        assertEquals(0.75, invokeGetBidScore(BidValue.WILLING));
        assertEquals(0.25, invokeGetBidScore(BidValue.IN_A_PINCH));
        assertEquals(0.0, invokeGetBidScore(BidValue.NOT_WILLING));
    }

    @Test
    void calculateRelevanceScoreShouldHandlePrimaryAndSecondarySignals() throws Exception {
        SubjectArea parent = subject(50, null);
        SubjectArea primary = subject(1, parent);
        SubjectArea secondary = subject(2, parent);

        Paper paper = new Paper();
        paper.setId(88);
        paper.setTrack(track);
        paper.setPrimarySubjectArea(primary);
        paper.setSecondarySubjectAreas(List.of(secondary));

        List<ReviewerInterest> interests = List.of(
                interest(reviewer1, primary, true),
                interest(reviewer1, parent, true),
                interest(reviewer1, secondary, false)
        );

        double score = invokeRelevance(paper, interests);

        assertTrue(score > 0.4);
        assertTrue(score <= 1.0);
    }

    @Test
    void calculateRelevanceScoreShouldReturnZeroForEmptyInterests() throws Exception {
        Paper paper = new Paper();
        paper.setId(89);
        paper.setTrack(track);

        double score = invokeRelevance(paper, List.of());

        assertEquals(0.0, score);
    }

    private AutoAssignConfigDTO config(int conferenceId, int minReviewersPerPaper, int maxPapersPerReviewer) {
        AutoAssignConfigDTO cfg = new AutoAssignConfigDTO();
        cfg.setConferenceId(conferenceId);
        cfg.setMinReviewersPerPaper(minReviewersPerPaper);
        cfg.setMaxPapersPerReviewer(maxPapersPerReviewer);
        cfg.setBidWeight(0.6);
        cfg.setRelevanceWeight(0.4);
        cfg.setLoadBalancing(false);
        return cfg;
    }

    private ConferenceUserTrack reviewerTrack(User user, Integer quota) {
        ConferenceUserTrack cut = new ConferenceUserTrack();
        cut.setUser(user);
        cut.setConference(conference);
        cut.setAssignedRole(ConferenceTrackRole.REVIEWER);
        cut.setReviewerQuota(quota);
        return cut;
    }

    private TrackReviewSetting trackSetting(boolean enableDomainConflict) {
        TrackReviewSetting setting = new TrackReviewSetting();
        setting.setEnableDomainConflict(enableDomainConflict);
        return setting;
    }

    private PaperAuthor authorOf(Paper paper, Integer userId, String email) {
        User user = new User();
        user.setId(userId);
        user.setEmail(email);
        PaperAuthor author = new PaperAuthor();
        author.setPaper(paper);
        author.setUser(user);
        return author;
    }

    private Bidding bid(Paper paper, User reviewer, BidValue value) {
        Bidding bidding = new Bidding();
        bidding.setPaper(paper);
        bidding.setReviewer(reviewer);
        bidding.setBidValue(value);
        return bidding;
    }

    private SubjectArea subject(Integer id, SubjectArea parent) {
        SubjectArea sa = new SubjectArea();
        sa.setId(id);
        sa.setParent(parent);
        return sa;
    }

    private ReviewerInterest interest(User reviewer, SubjectArea subjectArea, boolean primary) {
        ReviewerInterest ri = new ReviewerInterest();
        ri.setReviewer(reviewer);
        ri.setSubjectArea(subjectArea);
        ri.setIsPrimary(primary);
        return ri;
    }

    private double invokeGetBidScore(BidValue value) throws Exception {
        Method method = ReviewerAssignmentServiceImpl.class.getDeclaredMethod("getBidScore", BidValue.class);
        method.setAccessible(true);
        return (double) method.invoke(reviewerAssignmentService, value);
    }

    private double invokeRelevance(Paper paper, List<ReviewerInterest> interests) throws Exception {
        Method method = ReviewerAssignmentServiceImpl.class.getDeclaredMethod("calculateRelevanceScore", Paper.class, List.class);
        method.setAccessible(true);
        return (double) method.invoke(reviewerAssignmentService, paper, interests);
    }
}
