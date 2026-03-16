package com.capstone.confms.service.impl;

import com.capstone.confms.dto.PaperAuthorDTO;
import com.capstone.confms.entity.Paper;
import com.capstone.confms.entity.PaperAuthor;
import com.capstone.confms.entity.User;
import com.capstone.confms.repository.PaperAuthorRepository;
import com.capstone.confms.repository.PaperRepository;
import com.capstone.confms.repository.UserRepository;
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
public class PaperAuthorServiceImplTest {

    @Mock
    private PaperAuthorRepository paperAuthorRepository;
    @Mock
    private PaperRepository paperRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PaperAuthorServiceImpl paperAuthorService;

    private Paper paper;
    private User user;
    private PaperAuthor paperAuthor;

    @BeforeEach
    void setUp() {
        paper = new Paper();
        paper.setId(1);

        user = new User();
        user.setId(2);

        paperAuthor = new PaperAuthor();
        paperAuthor.setId(10);
        paperAuthor.setPaper(paper);
        paperAuthor.setUser(user);
    }

    @Test
    void shouldCreateService() {
        assertNotNull(paperAuthorService);
    }

    @Test
    void getAllPaperAuthorsShouldReturnPagedResponse() {
        var pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());
        var page = new PageImpl<>(List.of(paperAuthor), pageable, 1);
        when(paperAuthorRepository.findAll(pageable)).thenReturn(page);

        var result = paperAuthorService.getAllPaperAuthors(0, 20);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void getAuthorsByPaperShouldReturnPagedResponse() {
        var pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());
        var page = new PageImpl<>(List.of(paperAuthor), pageable, 1);
        when(paperAuthorRepository.findByPaperId(1, pageable)).thenReturn(page);

        var result = paperAuthorService.getAuthorsByPaper(1, 0, 20);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void createPaperAuthorShouldReturnResponse() {
        PaperAuthorDTO dto = PaperAuthorDTO.builder().paperId(1).userId(2).build();
        when(paperRepository.findById(1)).thenReturn(Optional.of(paper));
        when(userRepository.findById(2)).thenReturn(Optional.of(user));
        when(paperAuthorRepository.save(any(PaperAuthor.class))).thenReturn(paperAuthor);

        var result = paperAuthorService.createPaperAuthor(dto);

        assertNotNull(result);
        assertEquals(10, result.getId());
    }

    @Test
    void updatePaperAuthorShouldReturnResponse() {
        PaperAuthorDTO dto = PaperAuthorDTO.builder().paperId(1).userId(2).build();
        when(paperAuthorRepository.findById(10)).thenReturn(Optional.of(paperAuthor));
        when(paperRepository.findById(1)).thenReturn(Optional.of(paper));
        when(userRepository.findById(2)).thenReturn(Optional.of(user));
        when(paperAuthorRepository.save(any(PaperAuthor.class))).thenReturn(paperAuthor);

        var result = paperAuthorService.updatePaperAuthor(10, dto);

        assertNotNull(result);
        assertEquals(10, result.getId());
    }

    @Test
    void getPaperAuthorByIdShouldReturnResponse() {
        when(paperAuthorRepository.findById(10)).thenReturn(Optional.of(paperAuthor));

        var result = paperAuthorService.getPaperAuthorById(10);

        assertNotNull(result);
        assertEquals(10, result.getId());
    }

    @Test
    void deletePaperAuthorShouldDelete() {
        when(paperAuthorRepository.existsById(10)).thenReturn(true);

        paperAuthorService.deletePaperAuthor(10);

        verify(paperAuthorRepository).deleteById(10);
    }
}
