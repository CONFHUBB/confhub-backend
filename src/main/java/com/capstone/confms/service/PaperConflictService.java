package com.capstone.confms.service;

import com.capstone.confms.dto.*;
import com.capstone.confms.dto.response.*;

public interface PaperConflictService {
    PaperConflictResponseDTO createPaperConflict(PaperConflictDTO dto);

    PaperConflictResponseDTO updatePaperConflict(Integer id, PaperConflictDTO dto);

    PagedResponse<PaperConflictResponseDTO> getAllPaperConflicts(int page, int size);

    PaperConflictResponseDTO getPaperConflictById(Integer id);

    void deletePaperConflict(Integer id);
}