package com.capstone.confms.service;

import com.capstone.confms.dto.*;
import com.capstone.confms.dto.response.*;

import java.util.List;

public interface PaperCheckLogService {
    PaperCheckLogResponseDTO createPaperCheckLog(PaperCheckLogDTO dto);

    PaperCheckLogResponseDTO updatePaperCheckLog(Integer id, PaperCheckLogDTO dto);

    List<PaperCheckLogResponseDTO> getAllPaperCheckLogs();

    PaperCheckLogResponseDTO getPaperCheckLogById(Integer id);

    void deletePaperCheckLog(Integer id);
}