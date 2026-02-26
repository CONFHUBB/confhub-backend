package com.capstone.confms.service;

import com.capstone.confms.dto.*;
import com.capstone.confms.dto.response.*;

public interface PaperRebuttalService {
    PaperRebuttalResponseDTO createPaperRebuttal(PaperRebuttalDTO dto);

    PaperRebuttalResponseDTO updatePaperRebuttal(Integer id, PaperRebuttalDTO dto);

    PagedResponse<PaperRebuttalResponseDTO> getAllPaperRebuttals(int page, int size);

    PaperRebuttalResponseDTO getPaperRebuttalById(Integer id);

    void deletePaperRebuttal(Integer id);
}