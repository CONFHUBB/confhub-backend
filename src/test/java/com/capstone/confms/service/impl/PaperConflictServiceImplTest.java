package com.capstone.confms.service.impl;

import com.capstone.confms.dto.PaperConflictDTO;
import com.capstone.confms.entity.Paper;
import com.capstone.confms.entity.PaperConflict;
import com.capstone.confms.entity.User;
import com.capstone.confms.repository.PaperConflictRepository;
import com.capstone.confms.repository.PaperRepository;
import com.capstone.confms.repository.UserRepository;
import com.capstone.confms.utils.enums.ConflictType;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaperConflictServiceImplTest {

    @Mock
    private PaperConflictRepository paperConflictRepository;
    @Mock
    private PaperRepository paperRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PaperConflictServiceImpl paperConflictService;

    private Paper paper;
    private User user;
    private PaperConflict conflict;

    @BeforeEach
    void setUp() {
        paper = new Paper();
        paper.setId(1);

        user = new User();
        user.setId(2);

        conflict = new PaperConflict();
        conflict.setId(10);
        conflict.setPaper(paper);
        conflict.setUser(user);
        conflict.setConflictType(ConflictType.CO_AUTHOR);
    }

    @Test
    void shouldCreateService() {
        assertNotNull(paperConflictService);
    }

    @Test
    void getAllPaperConflictsShouldReturnPagedResponse() {
        var page = new PageImpl<>(List.of(conflict), PageRequest.of(0, 20), 1);
        when(paperConflictRepository.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        var result = paperConflictService.getAllPaperConflicts(0, 20);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void createPaperConflictShouldReturnResponse() {
        PaperConflictDTO dto = PaperConflictDTO.builder().paperId(1).userId(2).conflictType(ConflictType.CO_AUTHOR).build();
        when(paperRepository.findById(1)).thenReturn(Optional.of(paper));
        when(userRepository.findById(2)).thenReturn(Optional.of(user));
        when(paperConflictRepository.save(any(PaperConflict.class))).thenReturn(conflict);

        var result = paperConflictService.createPaperConflict(dto);

        assertNotNull(result);
        assertEquals(10, result.getId());
    }

    @Test
    void updatePaperConflictShouldReturnResponse() {
        PaperConflictDTO dto = PaperConflictDTO.builder().paperId(1).userId(2).conflictType(ConflictType.CO_AUTHOR).build();
        when(paperConflictRepository.findById(10)).thenReturn(Optional.of(conflict));
        when(paperRepository.findById(1)).thenReturn(Optional.of(paper));
        when(userRepository.findById(2)).thenReturn(Optional.of(user));
        when(paperConflictRepository.save(any(PaperConflict.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = paperConflictService.updatePaperConflict(10, dto);

        assertNotNull(result);
        assertEquals(10, result.getId());
    }

    @Test
    void getPaperConflictByIdShouldReturnResponse() {
        when(paperConflictRepository.findById(10)).thenReturn(Optional.of(conflict));

        var result = paperConflictService.getPaperConflictById(10);

        assertNotNull(result);
        assertEquals(10, result.getId());
    }

    @Test
    void deletePaperConflictShouldDelete() {
        when(paperConflictRepository.existsById(10)).thenReturn(true);

        paperConflictService.deletePaperConflict(10);

        verify(paperConflictRepository).deleteById(10);
    }
}



