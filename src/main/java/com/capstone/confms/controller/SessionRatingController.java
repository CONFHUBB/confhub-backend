package com.capstone.confms.controller;

import com.capstone.confms.dto.request.SessionRatingRequest;
import com.capstone.confms.dto.response.SessionRatingResponse;
import com.capstone.confms.entity.SessionRating;
import com.capstone.confms.entity.User;
import com.capstone.confms.repository.SessionRatingRepository;
import com.capstone.confms.repository.UserRepository;
import com.capstone.confms.security.services.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/session-ratings")
@RequiredArgsConstructor
@Tag(name = "Session Ratings", description = "Rate and review programme sessions")
public class SessionRatingController {

    private final SessionRatingRepository sessionRatingRepository;
    private final UserRepository userRepository;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Submit or update a rating for a session (1–5 stars)")
    public ResponseEntity<SessionRatingResponse> rateSession(@Valid @RequestBody SessionRatingRequest req) {
        Integer userId = getCurrentUserId();
        User user = userRepository.findById(userId).orElseThrow();

        SessionRating rating = sessionRatingRepository
                .findBySessionIdAndUserId(req.getSessionId(), userId)
                .orElse(new SessionRating());

        rating.setSessionId(req.getSessionId());
        rating.setConferenceId(req.getConferenceId());
        rating.setUser(user);
        rating.setRating(req.getRating());
        rating.setComment(req.getComment());

        SessionRating saved = sessionRatingRepository.save(rating);
        return new ResponseEntity<>(toResponse(saved), HttpStatus.OK);
    }

    @GetMapping("/session/{sessionId}")
    @Operation(summary = "Get all ratings for a session")
    public ResponseEntity<List<SessionRatingResponse>> getRatingsForSession(@PathVariable String sessionId) {
        List<SessionRating> ratings = sessionRatingRepository.findBySessionId(sessionId);
        Double avg = sessionRatingRepository.findAverageRatingBySessionId(sessionId);
        Long count = sessionRatingRepository.countBySessionId(sessionId);

        List<SessionRatingResponse> responses = ratings.stream().map(r -> {
            SessionRatingResponse res = toResponse(r);
            res.setAverageRating(avg);
            res.setRatingCount(count);
            return res;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/session/{sessionId}/summary")
    @Operation(summary = "Get average rating and count for a session")
    public ResponseEntity<SessionRatingResponse> getSessionRatingSummary(@PathVariable String sessionId) {
        Double avg = sessionRatingRepository.findAverageRatingBySessionId(sessionId);
        Long count = sessionRatingRepository.countBySessionId(sessionId);

        return ResponseEntity.ok(SessionRatingResponse.builder()
                .sessionId(sessionId)
                .averageRating(avg != null ? Math.round(avg * 10.0) / 10.0 : null)
                .ratingCount(count != null ? count : 0L)
                .build());
    }

    @GetMapping("/conference/{conferenceId}/my-rating/{sessionId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get current user's rating for a specific session")
    public ResponseEntity<SessionRatingResponse> getMyRating(
            @PathVariable Integer conferenceId,
            @PathVariable String sessionId) {
        Integer userId = getCurrentUserId();
        return sessionRatingRepository.findBySessionIdAndUserId(sessionId, userId)
                .map(r -> ResponseEntity.ok(toResponse(r)))
                .orElse(ResponseEntity.notFound().build());
    }

    private SessionRatingResponse toResponse(SessionRating r) {
        return SessionRatingResponse.builder()
                .id(r.getId())
                .sessionId(r.getSessionId())
                .conferenceId(r.getConferenceId())
                .userId(r.getUser().getId())
                .userName(r.getUser().getFirstName() + " " + r.getUser().getLastName())
                .rating(r.getRating())
                .comment(r.getComment())
                .createdAt(r.getCreatedAt() != null ? r.getCreatedAt().toString() : null)
                .build();
    }

    private Integer getCurrentUserId() {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userDetails.getId();
    }
}
