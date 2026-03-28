package com.capstone.confhub.controller;

import com.capstone.confhub.entity.Conference;
import com.capstone.confhub.entity.User;
import com.capstone.confhub.entity.UserBookmark;
import com.capstone.confhub.exception.ResourceNotFoundException;
import com.capstone.confhub.repository.ConferenceRepository;
import com.capstone.confhub.repository.UserBookmarkRepository;
import com.capstone.confhub.repository.UserRepository;
import com.capstone.confhub.security.services.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bookmarks")
@RequiredArgsConstructor
@Tag(name = "Bookmarks", description = "Server-side bookmark persistence per user")
public class UserBookmarkController {

    private final UserBookmarkRepository bookmarkRepository;
    private final UserRepository userRepository;
    private final ConferenceRepository conferenceRepository;

    private Integer getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetailsImpl u) return u.getId();
        throw new RuntimeException("Not authenticated");
    }

    @GetMapping
    @Operation(summary = "Get all bookmarked conference IDs for the current user")
    public ResponseEntity<List<Integer>> getBookmarks() {
        Integer userId = getCurrentUserId();
        List<Integer> ids = bookmarkRepository.findByUser_Id(userId)
                .stream()
                .map(b -> b.getConference().getId())
                .toList();
        return ResponseEntity.ok(ids);
    }

    @PostMapping("/{conferenceId}")
    @Operation(summary = "Bookmark a conference")
    @Transactional
    public ResponseEntity<Void> addBookmark(@PathVariable Integer conferenceId) {
        Integer userId = getCurrentUserId();
        if (!bookmarkRepository.existsByUser_IdAndConference_Id(userId, conferenceId)) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            Conference conference = conferenceRepository.findById(conferenceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Conference not found with id " + conferenceId));
            UserBookmark bookmark = new UserBookmark();
            bookmark.setUser(user);
            bookmark.setConference(conference);
            bookmarkRepository.save(bookmark);
        }
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{conferenceId}")
    @Operation(summary = "Remove bookmark for a conference")
    @Transactional
    public ResponseEntity<Void> removeBookmark(@PathVariable Integer conferenceId) {
        Integer userId = getCurrentUserId();
        bookmarkRepository.deleteByUser_IdAndConference_Id(userId, conferenceId);
        return ResponseEntity.noContent().build();
    }
}
