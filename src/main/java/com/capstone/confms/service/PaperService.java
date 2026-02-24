package com.capstone.confms.service;

import com.capstone.confms.dto.*;
import com.capstone.confms.dto.response.*;

import java.util.List;

public interface PaperService {

    PaperResponseDTO createPaper(PaperDTO dto);

    PaperResponseDTO updatePaper(Integer id, PaperDTO dto);

    PaperResponseDTO updatePaperStatus(Integer id, PaperUpdateStatusDTO dto);

    List<PaperResponseDTO> getAllPapers();

    PaperResponseDTO getPaperById(Integer id);

    void deletePaper(Integer id);

    // ==================== additional queries ====================
    /**
     * Retrieve all papers associated with a specific author (user) by their ID.
     */
    List<PaperResponseDTO> getPapersByAuthor(Integer authorId);

}