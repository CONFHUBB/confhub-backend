package com.capstone.confhub.controller;

import com.capstone.confhub.dto.request.AutoAssignConfigDTO;
import com.capstone.confhub.dto.response.AssignmentPreviewDTO;
import com.capstone.confhub.dto.response.AssignmentPreviewItemDTO;
import com.capstone.confhub.service.ReviewerAssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/conferences/{conferenceId}/assignments")
@RequiredArgsConstructor
@Tag(name = "Reviewer Assignment", description = "APIs cho Program Chair quản lý reviewer assignments")
public class ReviewerAssignmentController {

    private final ReviewerAssignmentService assignmentService;

    @PostMapping("/auto-assign")
    @Operation(summary = "Chạy auto-assign reviewer (Program Chair)",
            description = "Chạy thuật toán gán reviewer dựa trên weighted scoring (bid + relevance). Trả về preview, chưa lưu DB.")
    public ResponseEntity<AssignmentPreviewDTO> runAutoAssign(
            @PathVariable Integer conferenceId,
            @Valid @RequestBody AutoAssignConfigDTO config) {
        config.setConferenceId(conferenceId);
        return ResponseEntity.ok(assignmentService.runAutoAssign(config));
    }

    @GetMapping
    @Operation(summary = "Xem danh sách assignments hiện tại trong conference")
    public ResponseEntity<AssignmentPreviewDTO> getCurrentAssignments(
            @PathVariable Integer conferenceId) {
        return ResponseEntity.ok(assignmentService.getCurrentAssignments(conferenceId));
    }

    @PostMapping("/confirm")
    @Operation(summary = "Chốt assignments (lưu vào DB)",
            description = "Nhận danh sách assignments từ auto-assign preview và lưu vào DB.")
    public ResponseEntity<List<AssignmentPreviewItemDTO>> confirmAssignments(
            @PathVariable Integer conferenceId,
            @RequestBody List<AssignmentPreviewItemDTO> assignments) {
        return new ResponseEntity<>(assignmentService.confirmAssignments(conferenceId, assignments), HttpStatus.CREATED);
    }

    @PostMapping("/manual")
    @Operation(summary = "Manual assign: thêm 1 reviewer vào 1 paper")
    public ResponseEntity<AssignmentPreviewItemDTO> manualAssign(
            @PathVariable Integer conferenceId,
            @RequestParam Integer paperId,
            @RequestParam Integer reviewerId) {
        return new ResponseEntity<>(assignmentService.manualAssign(paperId, reviewerId), HttpStatus.CREATED);
    }

    @DeleteMapping("/{reviewId}")
    @Operation(summary = "Xóa assignment (unassign reviewer from paper)")
    public ResponseEntity<Void> removeAssignment(
            @PathVariable Integer conferenceId,
            @PathVariable Integer reviewId) {
        assignmentService.removeAssignment(reviewId);
        return ResponseEntity.noContent().build();
    }
}
