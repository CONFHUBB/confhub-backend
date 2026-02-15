package com.capstone.confms.service;

import com.capstone.confms.dto.*;
import com.capstone.confms.dto.response.*;

import java.util.List;

public interface PaperConflictService {
    PaperConflictResponseDTO createPaperConflict(PaperConflictDTO dto);

    PaperConflictResponseDTO updatePaperConflict(Integer id, PaperConflictDTO dto);

    List<PaperConflictResponseDTO> getAllPaperConflicts();

    PaperConflictResponseDTO getPaperConflictById(Integer id);

    void deletePaperConflict(Integer id);
}