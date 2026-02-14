package com.capstone.confms.service;

import com.capstone.confms.dto.*;
import com.capstone.confms.dto.response.*;

import java.util.List;

public interface PaperFileService {
    PaperFileResponseDTO createPaperFile(PaperFileDTO dto);

    PaperFileResponseDTO updatePaperFile(Integer id, PaperFileDTO dto);

    List<PaperFileResponseDTO> getAllPaperFiles();

    PaperFileResponseDTO getPaperFileById(Integer id);

    void deletePaperFile(Integer id);
}