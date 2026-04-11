package com.capstone.confhub.controller;

import com.capstone.confhub.entity.Paper;
import com.capstone.confhub.exception.ResourceNotFoundException;
import com.capstone.confhub.repository.PaperRepository;
import com.capstone.confhub.service.impl.PlagiarismService;
import com.capstone.confhub.utils.enums.PlagiarismStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/plagiarism")
@RequiredArgsConstructor
@Tag(name = "Plagiarism", description = "Plagiarism checking operations")
public class PlagiarismController {

    private final PlagiarismService plagiarismService;
    private final PaperRepository paperRepository;

    @GetMapping("/paper/{paperId}")
    @Operation(summary = "Get plagiarism check result for a paper")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getPlagiarismResult(@PathVariable Integer paperId) {
        return ResponseEntity.ok(plagiarismService.getPlagiarismResult(paperId));
    }

    @PostMapping("/paper/{paperId}/recheck")
    @Operation(summary = "Trigger a manual plagiarism re-check for a paper")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> recheckPlagiarism(@PathVariable Integer paperId) {
        // 1. Set status to CHECKING synchronously so immediate GET returns correct status
        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new ResourceNotFoundException("Paper not found: " + paperId));
        paper.setPlagiarismStatus(PlagiarismStatus.CHECKING);
        paperRepository.save(paper);

        // 2. Kick off async check — called through Spring proxy so @Async actually works
        plagiarismService.checkPlagiarismAsync(paperId);

        // 3. Return immediately with CHECKING status
        return ResponseEntity.ok(plagiarismService.getPlagiarismResult(paperId));
    }

    @DeleteMapping("/paper/{paperId}")
    @Operation(summary = "Reset plagiarism data for a paper (used when manuscript is deleted)")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> resetPlagiarism(@PathVariable Integer paperId) {
        plagiarismService.resetPlagiarism(paperId);
        return ResponseEntity.noContent().build();
    }
}
