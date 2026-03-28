package com.capstone.confhub.controller;

import com.capstone.confhub.entity.Conference;
import com.capstone.confhub.entity.SessionBookmark;
import com.capstone.confhub.entity.User;
import com.capstone.confhub.exception.ResourceNotFoundException;
import com.capstone.confhub.repository.ConferenceRepository;
import com.capstone.confhub.repository.SessionBookmarkRepository;
import com.capstone.confhub.repository.UserRepository;
import com.capstone.confhub.security.services.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Task 4 (corrected): Server-side persistence for My Schedule bookmarks.
 *
 *  GET    /api/v1/session-bookmarks/{conferenceId}          → list of bookmarked sessionIds
 *  POST   /api/v1/session-bookmarks/{conferenceId}/{sessionId} → add bookmark
 *  DELETE /api/v1/session-bookmarks/{conferenceId}/{sessionId} → remove bookmark
 *  DELETE /api/v1/session-bookmarks/{conferenceId}           → clear all for conference
 */
@RestController
@RequestMapping("/api/v1/session-bookmarks")
@RequiredArgsConstructor
@Tag(name = "Session Bookmarks", description = "My Schedule — persist bookmarked sessions server-side")
public class SessionBookmarkController {

    private final SessionBookmarkRepository bookmarkRepository;
    private final UserRepository userRepository;
    private final ConferenceRepository conferenceRepository;

    private Integer getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetailsImpl u) return u.getId();
        throw new RuntimeException("Not authenticated");
    }

    @GetMapping("/{conferenceId}")
    @Operation(summary = "Get all bookmarked session IDs for current user + conference")
    public ResponseEntity<List<String>> getBookmarks(@PathVariable Integer conferenceId) {
        Integer userId = getCurrentUserId();
        List<String> ids = bookmarkRepository
                .findByUser_IdAndConference_Id(userId, conferenceId)
                .stream()
                .map(SessionBookmark::getSessionId)
                .toList();
        return ResponseEntity.ok(ids);
    }

    @PostMapping("/{conferenceId}/{sessionId}")
    @Operation(summary = "Add a session bookmark")
    public ResponseEntity<Void> addBookmark(
            @PathVariable Integer conferenceId,
            @PathVariable String sessionId) {
        Integer userId = getCurrentUserId();
        if (!bookmarkRepository.existsByUser_IdAndConference_IdAndSessionId(userId, conferenceId, sessionId)) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            Conference conference = conferenceRepository.findById(conferenceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Conference not found: " + conferenceId));
            SessionBookmark bm = new SessionBookmark();
            bm.setUser(user);
            bm.setConference(conference);
            bm.setSessionId(sessionId);
            bookmarkRepository.save(bm);
        }
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{conferenceId}/{sessionId}")
    @Operation(summary = "Remove a single session bookmark")
    public ResponseEntity<Void> removeBookmark(
            @PathVariable Integer conferenceId,
            @PathVariable String sessionId) {
        Integer userId = getCurrentUserId();
        bookmarkRepository.deleteByUser_IdAndConference_IdAndSessionId(userId, conferenceId, sessionId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{conferenceId}")
    @Operation(summary = "Clear all session bookmarks for current user + conference")
    public ResponseEntity<Void> clearBookmarks(@PathVariable Integer conferenceId) {
        Integer userId = getCurrentUserId();
        bookmarkRepository.deleteByUser_IdAndConference_Id(userId, conferenceId);
        return ResponseEntity.noContent().build();
    }
}
