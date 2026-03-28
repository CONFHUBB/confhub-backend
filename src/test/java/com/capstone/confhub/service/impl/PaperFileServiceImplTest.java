package com.capstone.confhub.service.impl;

import com.capstone.confhub.dto.PaperFileDTO;
import com.capstone.confhub.entity.Paper;
import com.capstone.confhub.entity.PaperFile;
import com.capstone.confhub.repository.PaperFileRepository;
import com.capstone.confhub.repository.PaperRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PaperFileServiceImplTest {

    @Mock
    private PaperFileRepository paperFileRepository;
    @Mock
    private PaperRepository paperRepository;

    @InjectMocks
    private PaperFileServiceImpl paperFileService;

    private Paper paper;
    private PaperFile paperFile;

    @BeforeEach
    void setUp() {
        paper = new Paper();
        paper.setId(1);

        paperFile = new PaperFile();
        paperFile.setId(10);
        paperFile.setPaper(paper);
        paperFile.setUrl("https://file");
        paperFile.setIsActive(true);
    }

    @Test
    void shouldCreateService() {
        assertNotNull(paperFileService);
    }

    @Test
    void getAllPaperFilesShouldReturnPagedResponse() {
        var pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());
        var page = new PageImpl<>(List.of(paperFile), pageable, 1);
        when(paperFileRepository.findAll(pageable)).thenReturn(page);

        var result = paperFileService.getAllPaperFiles(0, 20);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void createPaperFileShouldReturnResponse() {
        PaperFileDTO dto = PaperFileDTO.builder().paperId(1).url("https://file").isActive(true).build();
        when(paperRepository.findById(1)).thenReturn(Optional.of(paper));
        when(paperFileRepository.save(any(PaperFile.class))).thenReturn(paperFile);

        var result = paperFileService.createPaperFile(dto);

        assertNotNull(result);
        assertEquals(10, result.getId());
        assertEquals("https://file", result.getUrl());
    }

    @Test
    void updatePaperFileShouldReturnResponse() {
        PaperFileDTO dto = PaperFileDTO.builder().paperId(1).url("https://updated").isActive(false).build();
        when(paperFileRepository.findById(10)).thenReturn(Optional.of(paperFile));
        when(paperRepository.findById(1)).thenReturn(Optional.of(paper));
        when(paperFileRepository.save(any(PaperFile.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = paperFileService.updatePaperFile(10, dto);

        assertNotNull(result);
        assertEquals("https://updated", result.getUrl());
        assertEquals(false, result.getIsActive());
    }

    @Test
    void getPaperFileByIdShouldReturnResponse() {
        when(paperFileRepository.findById(10)).thenReturn(Optional.of(paperFile));

        var result = paperFileService.getPaperFileById(10);

        assertNotNull(result);
        assertEquals(10, result.getId());
    }

    @Test
    void deletePaperFileShouldDelete() {
        when(paperFileRepository.existsById(10)).thenReturn(true);

        paperFileService.deletePaperFile(10);

        verify(paperFileRepository).deleteById(10);
    }
}
