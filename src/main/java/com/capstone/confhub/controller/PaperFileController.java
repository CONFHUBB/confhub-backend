package com.capstone.confhub.controller;

import com.capstone.confhub.dto.PaperFileDTO;
import com.capstone.confhub.dto.response.PaperFileResponseDTO;
import com.capstone.confhub.dto.response.PagedResponse;
import com.capstone.confhub.entity.User;
import com.capstone.confhub.exception.BadRequestException;
import com.capstone.confhub.repository.ReviewRepository;
import com.capstone.confhub.repository.TicketRepository;
import com.capstone.confhub.repository.UserRepository;
import com.capstone.confhub.service.FirebaseStorageService;
import com.capstone.confhub.service.PaperFileService;
import com.capstone.confhub.service.impl.PlagiarismService;
import com.capstone.confhub.utils.enums.PaymentStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/paper-file")
@RequiredArgsConstructor
@Tag(name = "Paper File Management", description = "Operations related to Paper File setup and related entities")
public class PaperFileController {

    private final PaperFileService paperFileService;
    private final FirebaseStorageService firebaseStorageService;
    private final PlagiarismService plagiarismService;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a paper file to Firebase Storage under conferences/{conferenceId}/papers/{paperId}/")
    public ResponseEntity<PaperFileResponseDTO> uploadPaperFile(
            @RequestParam("conferenceId") Integer conferenceId,
            @RequestParam("paperId") Integer paperId,
            @RequestParam("file") MultipartFile file) throws IOException {
        String downloadUrl = firebaseStorageService.uploadFile(file, conferenceId, paperId);
        PaperFileDTO dto = PaperFileDTO.builder()
                .paperId(paperId)
                .url(downloadUrl)
                .isActive(true)
                .build();
        PaperFileResponseDTO response = paperFileService.createPaperFile(dto);
        // Trigger async plagiarism check
        plagiarismService.checkPlagiarismAsync(paperId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping(value = "/upload-supplementary", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a supplementary file for a paper")
    public ResponseEntity<PaperFileResponseDTO> uploadSupplementaryFile(
            @RequestParam("conferenceId") Integer conferenceId,
            @RequestParam("paperId") Integer paperId,
            @RequestParam("file") MultipartFile file) throws IOException {
        String downloadUrl = firebaseStorageService.uploadFile(file, conferenceId, paperId);
        PaperFileDTO dto = PaperFileDTO.builder()
                .paperId(paperId)
                .url(downloadUrl)
                .isActive(true)
                .isSupplementary(true)
                .build();
        PaperFileResponseDTO response = paperFileService.createSupplementaryFile(dto);
        // Trigger async plagiarism check for supplementary files too
        plagiarismService.checkPlagiarismAsync(paperId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping(value = "/upload-camera-ready", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a camera-ready file (only for ACCEPTED papers, requires paid conference registration)")
    public ResponseEntity<PaperFileResponseDTO> uploadCameraReadyFile(
            @RequestParam("conferenceId") Integer conferenceId,
            @RequestParam("paperId") Integer paperId,
            @RequestParam("userId") Integer userId,
            @RequestParam("file") MultipartFile file) throws IOException {
        // Sprint 4 Gate: author must have a PAID ticket to upload camera-ready
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found: " + userId));
        boolean hasPaidTicket = ticketRepository.existsByUserAndConferenceIdAndPaymentStatus(
                user, conferenceId, PaymentStatus.COMPLETED);
        if (!hasPaidTicket) {
            throw new BadRequestException(
                    "You must register and pay the conference fee before uploading camera-ready files.");
        }
        String downloadUrl = firebaseStorageService.uploadFile(file, conferenceId, paperId);
        PaperFileDTO dto = PaperFileDTO.builder()
                .paperId(paperId)
                .url(downloadUrl)
                .isActive(true)
                .isCameraReady(true)
                .build();
        return new ResponseEntity<>(paperFileService.createCameraReadyFile(dto), HttpStatus.CREATED);
    }

    @PostMapping(value = "/upload-copyright", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a copyright submission file (only for ACCEPTED papers, requires paid conference registration)")
    public ResponseEntity<PaperFileResponseDTO> uploadCopyrightFile(
            @RequestParam("conferenceId") Integer conferenceId,
            @RequestParam("paperId") Integer paperId,
            @RequestParam("userId") Integer userId,
            @RequestParam("file") MultipartFile file) throws IOException {
        // Gate: author must have a PAID ticket to upload copyright
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found: " + userId));
        boolean hasPaidTicket = ticketRepository.existsByUserAndConferenceIdAndPaymentStatus(
                user, conferenceId, PaymentStatus.COMPLETED);
        if (!hasPaidTicket) {
            throw new BadRequestException(
                    "You must register and pay the conference fee before uploading copyright submission files.");
        }
        String downloadUrl = firebaseStorageService.uploadFile(file, conferenceId, paperId);
        PaperFileDTO dto = PaperFileDTO.builder()
                .paperId(paperId)
                .url(downloadUrl)
                .isActive(true)
                .isCopyrightSubmission(true)
                .build();
        return new ResponseEntity<>(paperFileService.createCopyrightSubmission(dto), HttpStatus.CREATED);
    }

    @PostMapping("/approve-camera-ready/{paperId}")
    @Operation(summary = "Approve camera-ready submission — transitions paper status to PUBLISHED")
    public ResponseEntity<Void> approveCameraReady(@PathVariable Integer paperId) {
        paperFileService.approveCameraReady(paperId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reject-camera-ready/{paperId}")
    @Operation(summary = "Reject camera-ready submission — transitions paper status to CAMERA_READY_REJECTED")
    public ResponseEntity<Void> rejectCameraReady(@PathVariable Integer paperId) {
        paperFileService.rejectCameraReady(paperId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/camera-ready/conference/{conferenceId}")
    @Operation(summary = "Get all camera-ready files for a conference")
    public ResponseEntity<List<PaperFileResponseDTO>> getCameraReadyFilesByConference(
            @PathVariable Integer conferenceId) {
        return ResponseEntity.ok(paperFileService.getCameraReadyFilesByConference(conferenceId));
    }

    @PostMapping
    @Operation(summary = "Create a new Paper File (with existing URL)")
    public ResponseEntity<PaperFileResponseDTO> createPaperFile(@Valid @RequestBody PaperFileDTO dto) {
        return new ResponseEntity<>(paperFileService.createPaperFile(dto), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all Paper Files")
    public ResponseEntity<PagedResponse<PaperFileResponseDTO>> getAllPaperFiles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size <= 0 || size > 100) {
            throw new BadRequestException("Invalid pagination parameters");
        }
        return ResponseEntity.ok(paperFileService.getAllPaperFiles(page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Paper File by ID")
    public ResponseEntity<PaperFileResponseDTO> getPaperFileById(@PathVariable Integer id) {
        return ResponseEntity.ok(paperFileService.getPaperFileById(id));
    }

    /**
     * Reviewer secure download — only accessible to reviewers who are assigned to the paper.
     * Returns the list of active (non-camera-ready) files for the paper.
     * The caller's identity is derived from the JWT via SecurityContextHolder.
     */
    @GetMapping("/paper/{paperId}/reviewer-files")
    @Operation(summary = "Get paper files for an assigned reviewer (auth-gated)")
    public ResponseEntity<List<PaperFileResponseDTO>> getFilesForReviewer(
            @PathVariable Integer paperId) {
        // 1. Extract current user from security context
        com.capstone.confhub.security.services.UserDetailsImpl userDetails =
                (com.capstone.confhub.security.services.UserDetailsImpl)
                org.springframework.security.core.context.SecurityContextHolder
                        .getContext().getAuthentication().getPrincipal();
        Integer reviewerId = userDetails.getId();

        // 2. Verify the caller has a Review assignment for this paper
        boolean isAssigned = reviewRepository.existsByPaper_IdAndReviewer_Id(paperId, reviewerId);
        if (!isAssigned) {
            throw new BadRequestException(
                    "Access denied: you are not assigned to review this paper.");
        }

        // 3. Return only active, non-camera-ready files
        List<PaperFileResponseDTO> files = paperFileService.getFilesByPaperId(paperId)
                .stream()
                .filter(f -> Boolean.TRUE.equals(f.getIsActive()) && !Boolean.TRUE.equals(f.getIsCameraReady()))
                .toList();

        return ResponseEntity.ok(files);
    }

    @GetMapping("/paper/{paperId}")
    @Operation(summary = "Get all Paper Files for a specific paper")
    public ResponseEntity<List<PaperFileResponseDTO>> getFilesByPaperId(@PathVariable Integer paperId) {
        return ResponseEntity.ok(paperFileService.getFilesByPaperId(paperId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update Paper File details")
    public ResponseEntity<PaperFileResponseDTO> updatePaperFile(@Valid @PathVariable Integer id,
            @RequestBody PaperFileDTO dto) {
        return ResponseEntity.ok(paperFileService.updatePaperFile(id, dto));
    }

    @PutMapping("/{id}/set-active")
    @Operation(summary = "Set a manuscript file as the active version (deactivates all others)")
    public ResponseEntity<PaperFileResponseDTO> setActiveFile(@PathVariable Integer id) {
        return ResponseEntity.ok(paperFileService.setActiveFile(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a Paper File")
    public ResponseEntity<Void> deletePaperFile(@PathVariable Integer id) {
        paperFileService.deletePaperFile(id);
        return ResponseEntity.noContent().build();
    }
}