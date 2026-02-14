package com.capstone.confms.service;

import com.capstone.confms.dto.*;
import com.capstone.confms.dto.response.*;

import java.util.List;

public interface PaperAuthorService {
    PaperAuthorResponseDTO createPaperAuthor(PaperAuthorDTO dto);

    PaperAuthorResponseDTO updatePaperAuthor(Integer id, PaperAuthorDTO dto);

    List<PaperAuthorResponseDTO> getAllPaperAuthors();

    PaperAuthorResponseDTO getPaperAuthorById(Integer id);

    void deletePaperAuthor(Integer id);

}