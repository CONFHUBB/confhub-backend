package com.capstone.confhub.service;

import com.capstone.confhub.dto.*;
import com.capstone.confhub.dto.response.*;

public interface PaperConflictService {
    PaperConflictResponseDTO createPaperConflict(PaperConflictDTO dto);

    PaperConflictResponseDTO updatePaperConflict(Integer id, PaperConflictDTO dto);

    PagedResponse<PaperConflictResponseDTO> getAllPaperConflicts(int page, int size);

    PaperConflictResponseDTO getPaperConflictById(Integer id);

    void deletePaperConflict(Integer id);

    java.util.List<PaperConflictResponseDTO> getConflictsByPaperId(Integer paperId);

    java.util.List<PaperConflictResponseDTO> getConflictsByConferenceId(Integer conferenceId);
}