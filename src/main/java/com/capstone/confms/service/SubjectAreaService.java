package com.capstone.confms.service;

import com.capstone.confms.dto.SubjectAreaDTO;
import com.capstone.confms.dto.response.SubjectAreaResponseDTO;
import com.capstone.confms.dto.response.PagedResponse;

public interface SubjectAreaService {
    SubjectAreaResponseDTO createSubjectArea(SubjectAreaDTO dto);

    SubjectAreaResponseDTO updateSubjectArea(Integer id, SubjectAreaDTO dto);

    SubjectAreaResponseDTO getSubjectAreaById(Integer id);

    PagedResponse<SubjectAreaResponseDTO> getAllSubjectAreas(int page, int size);

    PagedResponse<SubjectAreaResponseDTO> getSubjectAreasByTrackId(Integer trackId, int page, int size);

    void deleteSubjectArea(Integer id);
}
