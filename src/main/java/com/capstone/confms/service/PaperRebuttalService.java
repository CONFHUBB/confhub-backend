package com.capstone.confms.service;

import com.capstone.confms.dto.*;
import com.capstone.confms.dto.response.*;

import java.util.List;

public interface PaperRebuttalService {
    PaperRebuttalResponseDTO createPaperRebuttal(PaperRebuttalDTO dto);

    PaperRebuttalResponseDTO updatePaperRebuttal(Integer id, PaperRebuttalDTO dto);

    List<PaperRebuttalResponseDTO> getAllPaperRebuttals();

    PaperRebuttalResponseDTO getPaperRebuttalById(Integer id);

    void deletePaperRebuttal(Integer id);
}