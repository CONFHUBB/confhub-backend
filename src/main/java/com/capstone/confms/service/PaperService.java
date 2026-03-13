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
}