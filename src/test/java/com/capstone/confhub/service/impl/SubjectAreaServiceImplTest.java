package com.capstone.confhub.service.impl;

import com.capstone.confhub.dto.SubjectAreaDTO;
import com.capstone.confhub.entity.ConferenceTrack;
import com.capstone.confhub.entity.SubjectArea;
import com.capstone.confhub.repository.ConferenceTrackRepository;
import com.capstone.confhub.repository.SubjectAreaRepository;
import jakarta.persistence.EntityNotFoundException;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubjectAreaServiceImplTest {

    private static final int TRACK_ID = 1;
    private static final int NEW_TRACK_ID = 2;
    private static final int SUBJECT_AREA_ID = 10;
    private static final int PARENT_AREA_ID = 99;

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
        track.setId(TRACK_ID);
        track.setName("AI Track");

        subjectArea = new SubjectArea();
        subjectArea.setId(SUBJECT_AREA_ID);
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
        dto.setTrackId(TRACK_ID);
        dto.setName("Machine Learning");
        dto.setDescription("ML topics");

        when(trackRepository.findById(TRACK_ID)).thenReturn(Optional.of(track));
        when(subjectAreaRepository.save(any(SubjectArea.class))).thenReturn(subjectArea);

        var result = subjectAreaService.createSubjectArea(dto);

        assertNotNull(result);
        assertEquals(SUBJECT_AREA_ID, result.getId());
        assertEquals(TRACK_ID, result.getTrackId());
        assertEquals("Machine Learning", result.getName());
    }

    @Test
    void createSubjectAreaShouldThrowWhenTrackNotFound() {
        SubjectAreaDTO dto = new SubjectAreaDTO();
        dto.setTrackId(TRACK_ID);
        dto.setName("Machine Learning");

        when(trackRepository.findById(TRACK_ID)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> subjectAreaService.createSubjectArea(dto));
    }

    @Test
    void createSubjectAreaShouldThrowWhenParentNotFound() {
        SubjectAreaDTO dto = new SubjectAreaDTO();
        dto.setTrackId(TRACK_ID);
        dto.setParentId(PARENT_AREA_ID);
        dto.setName("Machine Learning");

        when(trackRepository.findById(TRACK_ID)).thenReturn(Optional.of(track));
        when(subjectAreaRepository.findById(PARENT_AREA_ID)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> subjectAreaService.createSubjectArea(dto));
    }

    @Test
    void updateSubjectAreaShouldReturnResponse() {
        ConferenceTrack newTrack = new ConferenceTrack();
        newTrack.setId(NEW_TRACK_ID);

        SubjectArea parent = new SubjectArea();
        parent.setId(PARENT_AREA_ID);

        SubjectAreaDTO dto = new SubjectAreaDTO();
        dto.setTrackId(NEW_TRACK_ID);
        dto.setParentId(PARENT_AREA_ID);
        dto.setName("Deep Learning");
        dto.setDescription("DL topics");

        when(subjectAreaRepository.findById(SUBJECT_AREA_ID)).thenReturn(Optional.of(subjectArea));
        when(trackRepository.findById(NEW_TRACK_ID)).thenReturn(Optional.of(newTrack));
        when(subjectAreaRepository.findById(PARENT_AREA_ID)).thenReturn(Optional.of(parent));
        when(subjectAreaRepository.save(any(SubjectArea.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = subjectAreaService.updateSubjectArea(SUBJECT_AREA_ID, dto);

        assertNotNull(result);
        assertEquals(SUBJECT_AREA_ID, result.getId());
        assertEquals(NEW_TRACK_ID, result.getTrackId());
        assertEquals(PARENT_AREA_ID, result.getParentId());
        assertEquals("Deep Learning", result.getName());
    }

    @Test
    void updateSubjectAreaShouldThrowWhenSubjectAreaNotFound() {
        SubjectAreaDTO dto = new SubjectAreaDTO();
        dto.setName("Deep Learning");

        when(subjectAreaRepository.findById(SUBJECT_AREA_ID)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> subjectAreaService.updateSubjectArea(SUBJECT_AREA_ID, dto));
    }

    @Test
    void updateSubjectAreaShouldThrowWhenNewTrackNotFound() {
        SubjectAreaDTO dto = new SubjectAreaDTO();
        dto.setTrackId(NEW_TRACK_ID);
        dto.setName("Deep Learning");

        when(subjectAreaRepository.findById(SUBJECT_AREA_ID)).thenReturn(Optional.of(subjectArea));
        when(trackRepository.findById(NEW_TRACK_ID)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> subjectAreaService.updateSubjectArea(SUBJECT_AREA_ID, dto));
    }

    @Test
    void updateSubjectAreaShouldThrowWhenParentNotFound() {
        SubjectAreaDTO dto = new SubjectAreaDTO();
        dto.setParentId(PARENT_AREA_ID);
        dto.setName("Deep Learning");

        when(subjectAreaRepository.findById(SUBJECT_AREA_ID)).thenReturn(Optional.of(subjectArea));
        when(subjectAreaRepository.findById(PARENT_AREA_ID)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> subjectAreaService.updateSubjectArea(SUBJECT_AREA_ID, dto));
    }

    @Test
    void getSubjectAreaByIdShouldReturnResponse() {
        when(subjectAreaRepository.findById(SUBJECT_AREA_ID)).thenReturn(Optional.of(subjectArea));

        var result = subjectAreaService.getSubjectAreaById(SUBJECT_AREA_ID);

        assertNotNull(result);
        assertEquals(SUBJECT_AREA_ID, result.getId());
        assertEquals("Machine Learning", result.getName());
    }

    @Test
    void getSubjectAreaByIdShouldThrowWhenNotFound() {
        when(subjectAreaRepository.findById(SUBJECT_AREA_ID)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> subjectAreaService.getSubjectAreaById(SUBJECT_AREA_ID));
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
    void getAllSubjectAreasShouldReturnEmptyPage() {
        var page = new PageImpl<SubjectArea>(List.of(), PageRequest.of(0, 20), 0);
        when(subjectAreaRepository.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        var result = subjectAreaService.getAllSubjectAreas(0, 20);

        assertNotNull(result);
        assertEquals(0, result.getContent().size());
    }

    @Test
    void getSubjectAreasByTrackIdShouldReturnPagedResponse() {
        var page = new PageImpl<>(List.of(subjectArea), PageRequest.of(0, 20), 1);
        when(trackRepository.existsById(TRACK_ID)).thenReturn(true);
        when(subjectAreaRepository.findByTrackId(eq(TRACK_ID), any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        var result = subjectAreaService.getSubjectAreasByTrackId(TRACK_ID, 0, 20);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(0, result.getPage());
    }

    @Test
    void getSubjectAreasByTrackIdShouldThrowWhenTrackNotFound() {
        when(trackRepository.existsById(TRACK_ID)).thenReturn(false);

        assertThrows(EntityNotFoundException.class,
                () -> subjectAreaService.getSubjectAreasByTrackId(TRACK_ID, 0, 20));
    }

    @Test
    void deleteSubjectAreaShouldDelete() {
        when(subjectAreaRepository.existsById(SUBJECT_AREA_ID)).thenReturn(true);

        subjectAreaService.deleteSubjectArea(SUBJECT_AREA_ID);

        verify(subjectAreaRepository).deleteById(SUBJECT_AREA_ID);
    }

    @Test
    void deleteSubjectAreaShouldThrowWhenNotFound() {
        when(subjectAreaRepository.existsById(SUBJECT_AREA_ID)).thenReturn(false);

        assertThrows(EntityNotFoundException.class,
                () -> subjectAreaService.deleteSubjectArea(SUBJECT_AREA_ID));
    }
}
