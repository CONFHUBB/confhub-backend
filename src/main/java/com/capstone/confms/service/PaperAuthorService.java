package com.capstone.confms.service;

import com.capstone.confms.dto.*;
import com.capstone.confms.dto.response.PaperAuthorResponseDTO;
import com.capstone.confms.dto.response.PagedResponse;

public interface PaperAuthorService {
    PaperAuthorResponseDTO createPaperAuthor(PaperAuthorDTO dto);

    PaperAuthorResponseDTO updatePaperAuthor(Integer id, PaperAuthorDTO dto);

    PagedResponse<PaperAuthorResponseDTO> getAllPaperAuthors(int page, int size);

    PaperAuthorResponseDTO getPaperAuthorById(Integer id);

    void deletePaperAuthor(Integer id);

    PagedResponse<PaperAuthorResponseDTO> getAuthorsByPaper(Integer paperId, int page, int size);

}