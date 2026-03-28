package com.capstone.confhub.service;

import com.capstone.confhub.dto.*;
import com.capstone.confhub.dto.response.PaperAuthorResponseDTO;
import com.capstone.confhub.dto.response.PagedResponse;

public interface PaperAuthorService {
    PaperAuthorResponseDTO createPaperAuthor(PaperAuthorDTO dto);

    PaperAuthorResponseDTO updatePaperAuthor(Integer id, PaperAuthorDTO dto);

    PagedResponse<PaperAuthorResponseDTO> getAllPaperAuthors(int page, int size);

    PaperAuthorResponseDTO getPaperAuthorById(Integer id);

    void deletePaperAuthor(Integer id);

    PagedResponse<PaperAuthorResponseDTO> getAuthorsByPaper(Integer paperId, int page, int size);

}