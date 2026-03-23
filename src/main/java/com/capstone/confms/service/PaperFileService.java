package com.capstone.confms.service;

import com.capstone.confms.dto.*;
import com.capstone.confms.dto.response.*;

public interface PaperFileService {
    PaperFileResponseDTO createPaperFile(PaperFileDTO dto);

    PaperFileResponseDTO updatePaperFile(Integer id, PaperFileDTO dto);

    PagedResponse<PaperFileResponseDTO> getAllPaperFiles(int page, int size);

    PaperFileResponseDTO getPaperFileById(Integer id);

    java.util.List<PaperFileResponseDTO> getFilesByPaperId(Integer paperId);

    void deletePaperFile(Integer id);

    PaperFileResponseDTO createCameraReadyFile(PaperFileDTO dto);

    void approveCameraReady(Integer paperId);

    java.util.List<PaperFileResponseDTO> getCameraReadyFilesByConference(Integer conferenceId);
}