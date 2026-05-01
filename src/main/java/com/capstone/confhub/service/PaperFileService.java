package com.capstone.confhub.service;

import com.capstone.confhub.dto.*;
import com.capstone.confhub.dto.response.*;

public interface PaperFileService {
    PaperFileResponseDTO createPaperFile(PaperFileDTO dto);

    PaperFileResponseDTO updatePaperFile(Integer id, PaperFileDTO dto);

    PagedResponse<PaperFileResponseDTO> getAllPaperFiles(int page, int size);

    PaperFileResponseDTO getPaperFileById(Integer id);

    java.util.List<PaperFileResponseDTO> getFilesByPaperId(Integer paperId);

    void deletePaperFile(Integer id);

    PaperFileResponseDTO createCameraReadyFile(PaperFileDTO dto);

    PaperFileResponseDTO createCopyrightSubmission(PaperFileDTO dto);

    PaperFileResponseDTO createSupplementaryFile(PaperFileDTO dto);

    void approveCameraReady(Integer paperId);

    void rejectCameraReady(Integer paperId);

    PaperFileResponseDTO setActiveFile(Integer fileId);

    java.util.List<PaperFileResponseDTO> getCameraReadyFilesByConference(Integer conferenceId);

    /** @deprecated Use findDuplicateInfo instead — duplicate detection is now advisory. */
    void validateNoDuplicateHash(PaperFileDTO dto);

    /** Returns human-readable duplicate info if the file hash matches another paper, or null if no duplicate found. */
    String findDuplicateInfo(PaperFileDTO dto);
}