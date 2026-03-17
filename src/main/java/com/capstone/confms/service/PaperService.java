package com.capstone.confms.service;

import com.capstone.confms.dto.*;
import com.capstone.confms.dto.response.*;

public interface PaperService {

    PaperResponseDTO createPaper(PaperDTO dto);

    PaperResponseDTO updatePaper(Integer id, PaperDTO dto);

    PaperResponseDTO updatePaperStatus(Integer id, PaperUpdateStatusDTO dto);

    PagedResponse<PaperResponseDTO> getAllPapers(int page, int size);

    PaperResponseDTO getPaperById(Integer id);

    void deletePaper(Integer id);

    // ==================== additional queries ====================
    /**
     * Retrieve all papers associated with a specific author (user) by their ID.
     */
    PagedResponse<PaperResponseDTO> getPapersByAuthor(Integer authorId, int page, int size);

    // BR-2.15: Withdraw + Restore
    PaperResponseDTO withdrawPaper(Integer id);
    PaperResponseDTO restorePaper(Integer id);

    /**
     * Get all papers in a conference (for Chair/PC paper management).
     */
    java.util.List<PaperResponseDTO> getPapersByConference(Integer conferenceId);

    // BR-3.28: Review Read-Only
    PaperResponseDTO toggleReviewReadOnly(Integer id, boolean readOnly);

    // BR-3.30: Discussion per paper
    PaperResponseDTO toggleDiscussion(Integer id, boolean enabled);

    // BR-3.43: Bulk status update
    java.util.List<PaperResponseDTO> bulkUpdatePaperStatus(java.util.List<PaperUpdateStatusDTO> dtos);

    // BR-3.30: Bulk discussion toggle
    java.util.List<PaperResponseDTO> bulkToggleDiscussion(java.util.List<Integer> paperIds, boolean enabled);
}