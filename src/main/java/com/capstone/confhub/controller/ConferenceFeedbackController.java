package com.capstone.confhub.controller;

import com.capstone.confhub.dto.request.ConferenceFeedbackRequest;
import com.capstone.confhub.dto.response.ConferenceFeedbackResponse;
import com.capstone.confhub.dto.response.ConferenceFeedbackSummary;
import com.capstone.confhub.entity.Conference;
import com.capstone.confhub.entity.ConferenceFeedback;
import com.capstone.confhub.entity.Ticket;
import com.capstone.confhub.entity.User;
import com.capstone.confhub.entity.UserProfile;
import com.capstone.confhub.exception.ForbiddenException;
import com.capstone.confhub.exception.ResourceNotFoundException;
import com.capstone.confhub.repository.ConferenceFeedbackRepository;
import com.capstone.confhub.repository.ConferenceRepository;
import com.capstone.confhub.repository.TicketRepository;
import com.capstone.confhub.repository.UserProfileRepository;
import com.capstone.confhub.repository.UserRepository;
import com.capstone.confhub.security.services.UserDetailsImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/conferences/{conferenceId}/feedback")
@RequiredArgsConstructor
public class ConferenceFeedbackController {

    private final ConferenceFeedbackRepository feedbackRepository;
    private final ConferenceRepository conferenceRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    /**
     * Tạo hoặc cập nhật feedback cho conference.
     * Chỉ attendee đã check-in mới được phép.
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ConferenceFeedbackResponse> createOrUpdateFeedback(
            @PathVariable Integer conferenceId,
            @Valid @RequestBody ConferenceFeedbackRequest request) {

        Integer userId = getCurrentUserId();
        Conference conference = conferenceRepository.findById(conferenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Conference not found"));

        // Guard: user must have a ticket AND must be checked in
        Ticket ticket = ticketRepository.findByUser_IdAndConference_Id(userId, conferenceId)
                .orElse(null);
        if (ticket == null) {
            throw new ForbiddenException("You must register for this conference before leaving feedback");
        }
        if (!Boolean.TRUE.equals(ticket.getIsCheckedIn())) {
            throw new ForbiddenException("You must check in at the conference before leaving feedback");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Upsert: find existing or create new
        ConferenceFeedback feedback = feedbackRepository
                .findByConference_IdAndUser_Id(conferenceId, userId)
                .orElse(new ConferenceFeedback());

        feedback.setUser(user);
        feedback.setConference(conference);
        feedback.setRating(request.getRating());
        feedback.setComment(request.getComment());

        ConferenceFeedback saved = feedbackRepository.save(feedback);
        return ResponseEntity.ok(toResponse(saved));
    }

    /**
     * Lấy tất cả feedback của conference (public).
     */
    @GetMapping
    public ResponseEntity<List<ConferenceFeedbackResponse>> getFeedback(
            @PathVariable Integer conferenceId) {
        List<ConferenceFeedback> feedbacks = feedbackRepository
                .findByConference_IdOrderByCreatedAtDesc(conferenceId);
        List<ConferenceFeedbackResponse> responses = feedbacks.stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    /**
     * Lấy summary (avg rating, count, breakdown theo sao).
     */
    @GetMapping("/summary")
    public ResponseEntity<ConferenceFeedbackSummary> getSummary(
            @PathVariable Integer conferenceId) {
        Double avg = feedbackRepository.findAverageRatingByConferenceId(conferenceId);
        long total = feedbackRepository.countByConference_Id(conferenceId);

        ConferenceFeedbackSummary summary = ConferenceFeedbackSummary.builder()
                .averageRating(avg != null ? Math.round(avg * 10.0) / 10.0 : null)
                .totalCount(total)
                .rating5(feedbackRepository.countByConferenceIdAndRating(conferenceId, 5))
                .rating4(feedbackRepository.countByConferenceIdAndRating(conferenceId, 4))
                .rating3(feedbackRepository.countByConferenceIdAndRating(conferenceId, 3))
                .rating2(feedbackRepository.countByConferenceIdAndRating(conferenceId, 2))
                .rating1(feedbackRepository.countByConferenceIdAndRating(conferenceId, 1))
                .build();
        return ResponseEntity.ok(summary);
    }

    /**
     * Lấy feedback của current user (nếu có).
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ConferenceFeedbackResponse> getMyFeedback(
            @PathVariable Integer conferenceId) {
        Integer userId = getCurrentUserId();
        return feedbackRepository.findByConference_IdAndUser_Id(conferenceId, userId)
                .map(f -> ResponseEntity.ok(toResponse(f)))
                .orElse(ResponseEntity.noContent().build());
    }

    /**
     * Xóa feedback của mình.
     */
    @DeleteMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteMyFeedback(@PathVariable Integer conferenceId) {
        Integer userId = getCurrentUserId();
        ConferenceFeedback feedback = feedbackRepository
                .findByConference_IdAndUser_Id(conferenceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback not found"));
        feedbackRepository.delete(feedback);
        return ResponseEntity.noContent().build();
    }

    // ── Helpers ──

    private Integer getCurrentUserId() {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder
                .getContext().getAuthentication().getPrincipal();
        return userDetails.getId();
    }

    private ConferenceFeedbackResponse toResponse(ConferenceFeedback f) {
        String avatarUrl = null;
        try {
            Optional<UserProfile> profile = userProfileRepository.findByUserId(f.getUser().getId());
            if (profile.isPresent()) {
                avatarUrl = profile.get().getAvatarUrl();
            }
        } catch (Exception ignored) {}

        return ConferenceFeedbackResponse.builder()
                .id(f.getId())
                .conferenceId(f.getConference().getId())
                .userId(f.getUser().getId())
                .userFirstName(f.getUser().getFirstName())
                .userLastName(f.getUser().getLastName())
                .userAvatarUrl(avatarUrl)
                .rating(f.getRating())
                .comment(f.getComment())
                .createdAt(f.getCreatedAt())
                .build();
    }
}
