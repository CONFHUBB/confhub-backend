package com.capstone.confhub.controller;

import com.capstone.confhub.dto.response.EmailHistoryResponseDTO;
import com.capstone.confhub.entity.EmailHistory;
import com.capstone.confhub.exception.BadRequestException;
import com.capstone.confhub.repository.EmailHistoryRepository;
import com.capstone.confhub.utils.enums.EmailType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/email-history")
@RequiredArgsConstructor
@Tag(name = "Email History", description = "View email sending history")
public class EmailHistoryController {

    private final EmailHistoryRepository emailHistoryRepository;

    @GetMapping("/conference/{conferenceId}")
    @Operation(summary = "Get email history for a conference", description = "Returns paginated list of all emails sent for a specific conference")
    public ResponseEntity<Page<EmailHistoryResponseDTO>> getEmailHistoryByConference(
            @PathVariable Integer conferenceId,
            @RequestParam(required = false) String emailType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (page < 0 || size <= 0 || size > 100) {
            throw new BadRequestException("Invalid pagination parameters");
        }

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "sentAt"));

        Page<EmailHistory> result;
        if (emailType != null && !emailType.isEmpty()) {
            result = emailHistoryRepository.findByConference_IdAndEmailType(
                    conferenceId, EmailType.valueOf(emailType), pageRequest);
        } else {
            result = emailHistoryRepository.findByConference_Id(conferenceId, pageRequest);
        }

        Page<EmailHistoryResponseDTO> dtoPage = result.map(this::mapToDTO);
        return ResponseEntity.ok(dtoPage);
    }

    private EmailHistoryResponseDTO mapToDTO(EmailHistory entity) {
        return EmailHistoryResponseDTO.builder()
                .id(entity.getId())
                .fromEmail(entity.getFromEmail())
                .toEmail(entity.getToEmail())
                .ccEmails(entity.getCcEmails())
                .subject(entity.getSubject())
                .body(entity.getBody())
                .emailType(entity.getEmailType())
                .status(entity.getStatus())
                .errorMessage(entity.getErrorMessage())
                .conferenceId(entity.getConference() != null ? entity.getConference().getId() : null)
                .conferenceName(entity.getConference() != null ? entity.getConference().getName() : null)
                .sentAt(entity.getSentAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
