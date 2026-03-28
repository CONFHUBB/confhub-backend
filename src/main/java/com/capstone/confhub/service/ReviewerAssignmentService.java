package com.capstone.confhub.service;

import com.capstone.confhub.dto.request.AutoAssignConfigDTO;
import com.capstone.confhub.dto.response.AssignmentPreviewDTO;
import com.capstone.confhub.dto.response.AssignmentPreviewItemDTO;

import java.util.List;

public interface ReviewerAssignmentService {

    /**
     * Chạy thuật toán auto-assign và trả về preview (chưa lưu DB).
     */
    AssignmentPreviewDTO runAutoAssign(AutoAssignConfigDTO config);

    /**
     * Lưu assignments vào DB (chốt).
     */
    List<AssignmentPreviewItemDTO> confirmAssignments(Integer conferenceId,
                                                       List<AssignmentPreviewItemDTO> assignments);

    /**
     * Manual assign: thêm 1 reviewer vào 1 paper.
     */
    AssignmentPreviewItemDTO manualAssign(Integer paperId, Integer reviewerId);

    /**
     * Xóa assignment (review record).
     */
    void removeAssignment(Integer reviewId);

    /**
     * Lấy danh sách assignments hiện tại cho 1 conference.
     */
    AssignmentPreviewDTO getCurrentAssignments(Integer conferenceId);
}
