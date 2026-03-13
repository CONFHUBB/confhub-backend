package com.capstone.confms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentPreviewDTO {
    private Integer conferenceId;
    private Integer totalPapers;
    private Integer totalReviewers;
    private Integer totalAssignments;
    private Integer unassignedPapers;  // papers chưa đủ reviewer
    private Integer overloadedReviewers; // reviewers vượt max capacity
    private List<AssignmentPreviewItemDTO> assignments;

    // Thống kê theo paper: paperId -> số reviewers
    private Map<Integer, Integer> reviewersPerPaper;

    // Thống kê theo reviewer: reviewerId -> số papers
    private Map<Integer, Integer> papersPerReviewer;
}
