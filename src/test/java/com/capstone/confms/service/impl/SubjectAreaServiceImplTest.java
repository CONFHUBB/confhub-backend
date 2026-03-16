package com.capstone.confms.service.impl;

import com.capstone.confms.dto.SubjectAreaDTO;
import com.capstone.confms.entity.ConferenceTrack;
import com.capstone.confms.entity.SubjectArea;
import com.capstone.confms.repository.ConferenceTrackRepository;
import com.capstone.confms.repository.SubjectAreaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubjectAreaServiceImplTest {

    @Mock
    private SubjectAreaRepository subjectAreaRepository;
    @Mock
    private ConferenceTrackRepository trackRepository;

    @InjectMocks
    private SubjectAreaServiceImpl subjectAreaService;

    private ConferenceTrack track;
    private SubjectArea subjectArea;

    @BeforeEach
    void setUp() {
        track = new ConferenceTrack();
        track.setId(1);
        track.setName("AI Track");

        subjectArea = new SubjectArea();
        subjectArea.setId(10);
        subjectArea.setTrack(track);
        subjectArea.setName("Machine Learning");
        subjectArea.setDescription("ML topics");
    }

    @Test
    void shouldCreateService() {
        assertNotNull(subjectAreaService);
    }

    @Test
    void createSubjectAreaShouldReturnResponse() {
        SubjectAreaDTO dto = new SubjectAreaDTO();
        dto.setTrackId(1);
        dto.setName("Machine Learning");
        dto.setDescription("ML topics");

        when(trackRepository.findById(1)).thenReturn(Optional.of(track));
        when(subjectAreaRepository.save(any(SubjectArea.class))).thenReturn(subjectArea);

        var result = subjectAreaService.createSubjectArea(dto);

        assertNotNull(result);
        assertEquals(10, result.getId());
        assertEquals(1, result.getTrackId());
        assertEquals("Machine Learning", result.getName());
    }

    @Test
    void updateSubjectAreaShouldReturnResponse() {
        ConferenceTrack newTrack = new ConferenceTrack();
        newTrack.setId(2);

        SubjectArea parent = new SubjectArea();
        parent.setId(99);

        SubjectAreaDTO dto = new SubjectAreaDTO();
        dto.setTrackId(2);
        dto.setParentId(99);
        dto.setName("Deep Learning");
        dto.setDescription("DL topics");

        when(subjectAreaRepository.findById(10)).thenReturn(Optional.of(subjectArea));
        when(trackRepository.findById(2)).thenReturn(Optional.of(newTrack));
        when(subjectAreaRepository.findById(99)).thenReturn(Optional.of(parent));
        when(subjectAreaRepository.save(any(SubjectArea.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = subjectAreaService.updateSubjectArea(10, dto);

        assertNotNull(result);
        assertEquals(10, result.getId());
        assertEquals(2, result.getTrackId());
        assertEquals(99, result.getParentId());
        assertEquals("Deep Learning", result.getName());
    }

    @Test
    void getSubjectAreaByIdShouldReturnResponse() {
        when(subjectAreaRepository.findById(10)).thenReturn(Optional.of(subjectArea));

        var result = subjectAreaService.getSubjectAreaById(10);

        assertNotNull(result);
        assertEquals(10, result.getId());
        assertEquals("Machine Learning", result.getName());
    }

    @Test
    void getAllSubjectAreasShouldReturnPagedResponse() {
        var page = new PageImpl<>(List.of(subjectArea), PageRequest.of(0, 20), 1);
        when(subjectAreaRepository.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        var result = subjectAreaService.getAllSubjectAreas(0, 20);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(0, result.getPage());
    }

    @Test
    void getSubjectAreasByTrackIdShouldReturnPagedResponse() {
        var page = new PageImpl<>(List.of(subjectArea), PageRequest.of(0, 20), 1);
        when(trackRepository.existsById(1)).thenReturn(true);
        when(subjectAreaRepository.findByTrackId(eq(1), any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        var result = subjectAreaService.getSubjectAreasByTrackId(1, 0, 20);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(0, result.getPage());
    }

    @Test
    void deleteSubjectAreaShouldDelete() {
        when(subjectAreaRepository.existsById(10)).thenReturn(true);

        subjectAreaService.deleteSubjectArea(10);

        verify(subjectAreaRepository).deleteById(10);
    }
}
