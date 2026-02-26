package com.capstone.confms.service;

import com.capstone.confms.dto.*;
import com.capstone.confms.dto.response.*;

public interface PaperCheckLogService {
    PaperCheckLogResponseDTO createPaperCheckLog(PaperCheckLogDTO dto);

    PaperCheckLogResponseDTO updatePaperCheckLog(Integer id, PaperCheckLogDTO dto);

    PagedResponse<PaperCheckLogResponseDTO> getAllPaperCheckLogs(int page, int size);

    PaperCheckLogResponseDTO getPaperCheckLogById(Integer id);

    void deletePaperCheckLog(Integer id);
}