package com.capstone.confhub.controller;

import com.capstone.confhub.dto.response.ImportResultDTO;
import com.capstone.confhub.service.ConferenceImportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConferenceImportControllerTest {

    @Mock
    private ConferenceImportService conferenceImportService;

    @InjectMocks
    private ConferenceImportController conferenceImportController;

    private MultipartFile mockFile;

    @BeforeEach
    void setUp() {
        mockFile = new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1, 2, 3});
    }

    @Test
    void shouldCreateController() {
        assertNotNull(conferenceImportController);
    }

    // ── Conference Template Tests ──

    @Test
    void conferenceTemplateShouldReturnOkWithXlsxContentType() {
        byte[] templateData = new byte[]{1, 2, 3, 4, 5};
        when(conferenceImportService.generateConferenceTemplate()).thenReturn(templateData);

        var result = conferenceImportController.conferenceTemplate();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(templateData, result.getBody());
        assertNotNull(result.getHeaders().getContentDisposition());
        assertTrue(result.getHeaders().getContentDisposition().getFilename().contains("conference_template.xlsx"));
    }

    @Test
    void conferenceTemplateShouldSetCorrectContentType() {
        byte[] templateData = new byte[]{1, 2, 3};
        when(conferenceImportService.generateConferenceTemplate()).thenReturn(templateData);

        var result = conferenceImportController.conferenceTemplate();

        assertNotNull(result.getHeaders().getContentType());
        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", 
            result.getHeaders().getContentType().toString());
    }

    @Test
    void conferenceTemplateShouldSetAttachmentHeader() {
        byte[] templateData = new byte[]{1, 2, 3};
        when(conferenceImportService.generateConferenceTemplate()).thenReturn(templateData);

        var result = conferenceImportController.conferenceTemplate();

        assertNotNull(result.getHeaders().getContentDisposition());
        assertTrue(result.getHeaders().getContentDisposition().isAttachment());
    }

    // ── Conference Preview Tests ──

    @Test
    void previewConferenceShouldReturnOkWithImportResult() {
        ImportResultDTO expectedResult = ImportResultDTO.builder()
                .success(true)
                .conferencePreview(java.util.Map.of("name", "Preview successful"))
                .build();

        when(conferenceImportService.previewConferenceFromExcel(any(MultipartFile.class)))
                .thenReturn(expectedResult);

        var result = conferenceImportController.previewConference(mockFile);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(expectedResult, result.getBody());
        assertTrue(result.getBody().isSuccess());
    }

    @Test
    void previewConferenceShouldReturnOkEvenWhenPreviewFails() {
        ImportResultDTO expectedResult = ImportResultDTO.builder()
                .success(false)
                .errors(java.util.List.of(ImportResultDTO.ImportError.builder()
                        .sheet("Conference")
                        .row(2)
                        .column("Name")
                        .message("Invalid data format")
                        .build()))
                .build();

        when(conferenceImportService.previewConferenceFromExcel(any(MultipartFile.class)))
                .thenReturn(expectedResult);

        var result = conferenceImportController.previewConference(mockFile);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertFalse(result.getBody().isSuccess());
    }

    @Test
    void previewConferenceShouldCallServiceWithCorrectFile() {
        ImportResultDTO expectedResult = ImportResultDTO.builder().success(true).build();
        when(conferenceImportService.previewConferenceFromExcel(any(MultipartFile.class)))
                .thenReturn(expectedResult);

        conferenceImportController.previewConference(mockFile);

        verify(conferenceImportService, times(1)).previewConferenceFromExcel(mockFile);
    }

    // ── Conference Import Tests ──

    @Test
    void importConferenceShouldReturnCreatedOnSuccess() {
        ImportResultDTO successResult = ImportResultDTO.builder()
                .success(true)
                .conferenceName("Import successful")
                .build();

        when(conferenceImportService.importConferenceFromExcel(any(MultipartFile.class)))
                .thenReturn(successResult);

        var result = conferenceImportController.importConference(mockFile);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertTrue(result.getBody().isSuccess());
    }

    @Test
    void importConferenceShouldReturnBadRequestOnFailure() {
        ImportResultDTO failResult = ImportResultDTO.builder()
                .success(false)
                .errors(java.util.List.of(ImportResultDTO.ImportError.builder()
                        .sheet("Conference")
                        .row(1)
                        .column("General")
                        .message("Import failed: invalid data")
                        .build()))
                .build();

        when(conferenceImportService.importConferenceFromExcel(any(MultipartFile.class)))
                .thenReturn(failResult);

        var result = conferenceImportController.importConference(mockFile);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertFalse(result.getBody().isSuccess());
    }

    @Test
    void importConferenceShouldReturnResultWithErrorMessages() {
        ImportResultDTO failResult = ImportResultDTO.builder()
                .success(false)
                .errors(java.util.List.of(ImportResultDTO.ImportError.builder()
                        .sheet("Conference")
                        .row(2)
                        .column("Name")
                        .message("Invalid conference name at row 2")
                        .build()))
                .build();

        when(conferenceImportService.importConferenceFromExcel(any(MultipartFile.class)))
                .thenReturn(failResult);

        var result = conferenceImportController.importConference(mockFile);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertFalse(result.getBody().getErrors().isEmpty());
        assertTrue(result.getBody().getErrors().get(0).getMessage().contains("Invalid conference name"));
    }

    // ── Track Template Tests ──

    @Test
    void trackTemplateShouldReturnOkWithXlsxContentType() {
        byte[] templateData = new byte[]{1, 2, 3, 4, 5};
        when(conferenceImportService.generateTrackTemplate()).thenReturn(templateData);

        var result = conferenceImportController.trackTemplate(1);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(templateData, result.getBody());
        assertTrue(result.getHeaders().getContentDisposition().getFilename().contains("track_template.xlsx"));
    }

    @Test
    void trackTemplateWithDifferentConferenceIdShouldWork() {
        byte[] templateData = new byte[]{1, 2, 3};
        when(conferenceImportService.generateTrackTemplate()).thenReturn(templateData);

        var result1 = conferenceImportController.trackTemplate(1);
        var result2 = conferenceImportController.trackTemplate(999);

        assertEquals(HttpStatus.OK, result1.getStatusCode());
        assertEquals(HttpStatus.OK, result2.getStatusCode());
    }

    // ── Track Preview Tests ──

    @Test
    void previewTracksShouldReturnOkWithResult() {
        ImportResultDTO expectedResult = ImportResultDTO.builder()
                .success(true)
                .trackPreviews(java.util.List.of(
                        java.util.Map.of("name", "Track A"),
                        java.util.Map.of("name", "Track B")))
                .build();

        when(conferenceImportService.previewTracksFromExcel(any(MultipartFile.class)))
                .thenReturn(expectedResult);

        var result = conferenceImportController.previewTracks(1, mockFile);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(2, result.getBody().getTrackPreviews().size());
    }

    @Test
    void previewTracksShouldCallServiceWithFile() {
        ImportResultDTO result = ImportResultDTO.builder().success(true).build();
        when(conferenceImportService.previewTracksFromExcel(any(MultipartFile.class)))
                .thenReturn(result);

        conferenceImportController.previewTracks(1, mockFile);

        verify(conferenceImportService, times(1)).previewTracksFromExcel(mockFile);
    }

    // ── Track Import Tests ──

    @Test
    void importTracksShouldReturnCreatedOnSuccess() {
        ImportResultDTO successResult = ImportResultDTO.builder()
                .success(true)
                .tracksCreated(3)
                .build();

        when(conferenceImportService.importTracksFromExcel(eq(1), any(MultipartFile.class)))
                .thenReturn(successResult);

        var result = conferenceImportController.importTracks(1, mockFile);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(3, result.getBody().getTracksCreated());
    }

    @Test
    void importTracksShouldReturnBadRequestOnFailure() {
        ImportResultDTO failResult = ImportResultDTO.builder()
                .success(false)
                .errors(java.util.List.of(ImportResultDTO.ImportError.builder()
                        .sheet("Tracks")
                        .row(3)
                        .column("Name")
                        .message("Duplicate track name")
                        .build()))
                .build();

        when(conferenceImportService.importTracksFromExcel(eq(1), any(MultipartFile.class)))
                .thenReturn(failResult);

        var result = conferenceImportController.importTracks(1, mockFile);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    @Test
    void importTracksWithConferenceIdShouldPassToService() {
        ImportResultDTO result = ImportResultDTO.builder().success(true).build();
        when(conferenceImportService.importTracksFromExcel(eq(42), any(MultipartFile.class)))
                .thenReturn(result);

        conferenceImportController.importTracks(42, mockFile);

        verify(conferenceImportService, times(1)).importTracksFromExcel(42, mockFile);
    }

    // ── Subject Area Template Tests ──

    @Test
    void subjectAreaTemplateShouldReturnOkWithXlsxContentType() {
        byte[] templateData = new byte[]{1, 2, 3, 4, 5};
        when(conferenceImportService.generateSubjectAreaTemplate()).thenReturn(templateData);

        var result = conferenceImportController.subjectAreaTemplate(1);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(templateData, result.getBody());
        assertTrue(result.getHeaders().getContentDisposition().getFilename().contains("subject_area_template.xlsx"));
    }

    // ── Subject Area Preview Tests ──

    @Test
    void previewSubjectAreasShouldReturnOkWithResult() {
        ImportResultDTO expectedResult = ImportResultDTO.builder()
                .success(true)
                .subjectAreaPreviews(java.util.List.of(
                        java.util.Map.of("name", "SA-1"),
                        java.util.Map.of("name", "SA-2"),
                        java.util.Map.of("name", "SA-3"),
                        java.util.Map.of("name", "SA-4"),
                        java.util.Map.of("name", "SA-5")))
                .build();

        when(conferenceImportService.previewSubjectAreasFromExcel(any(MultipartFile.class)))
                .thenReturn(expectedResult);

        var result = conferenceImportController.previewSubjectAreas(1, mockFile);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(5, result.getBody().getSubjectAreaPreviews().size());
    }

    // ── Subject Area Import Tests ──

    @Test
    void importSubjectAreasShouldReturnCreatedOnSuccess() {
        ImportResultDTO successResult = ImportResultDTO.builder()
                .success(true)
                .subjectAreasCreated(5)
                .build();

        when(conferenceImportService.importSubjectAreasFromExcel(eq(1), any(MultipartFile.class)))
                .thenReturn(successResult);

        var result = conferenceImportController.importSubjectAreas(1, mockFile);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(5, result.getBody().getSubjectAreasCreated());
    }

    @Test
    void importSubjectAreasShouldReturnBadRequestOnFailure() {
        ImportResultDTO failResult = ImportResultDTO.builder()
                .success(false)
                .errors(java.util.List.of(ImportResultDTO.ImportError.builder()
                        .sheet("SubjectAreas")
                        .row(3)
                        .column("Code")
                        .message("Duplicate subject area code")
                        .build()))
                .build();

        when(conferenceImportService.importSubjectAreasFromExcel(eq(1), any(MultipartFile.class)))
                .thenReturn(failResult);

        var result = conferenceImportController.importSubjectAreas(1, mockFile);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    // ── Member Template Tests ──

    @Test
    void memberTemplateShouldReturnOkWithXlsxContentType() {
        byte[] templateData = new byte[]{1, 2, 3, 4, 5};
        when(conferenceImportService.generateMemberTemplate()).thenReturn(templateData);

        var result = conferenceImportController.memberTemplate(1);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(templateData, result.getBody());
        assertTrue(result.getHeaders().getContentDisposition().getFilename().contains("member_template.xlsx"));
    }

    // ── Member Preview Tests ──

    @Test
    void previewMembersShouldReturnOkWithResult() {
        ImportResultDTO expectedResult = ImportResultDTO.builder()
                .success(true)
                .memberPreviews(java.util.List.of(
                        java.util.Map.of("email", "u1@test.com"),
                        java.util.Map.of("email", "u2@test.com"),
                        java.util.Map.of("email", "u3@test.com"),
                        java.util.Map.of("email", "u4@test.com"),
                        java.util.Map.of("email", "u5@test.com"),
                        java.util.Map.of("email", "u6@test.com"),
                        java.util.Map.of("email", "u7@test.com"),
                        java.util.Map.of("email", "u8@test.com"),
                        java.util.Map.of("email", "u9@test.com"),
                        java.util.Map.of("email", "u10@test.com")))
                .build();

        when(conferenceImportService.previewMembersFromExcel(any(MultipartFile.class)))
                .thenReturn(expectedResult);

        var result = conferenceImportController.previewMembers(1, mockFile);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(10, result.getBody().getMemberPreviews().size());
    }

    // ── Member Import Tests ──

    @Test
    void importMembersShouldReturnCreatedOnSuccess() {
        ImportResultDTO successResult = ImportResultDTO.builder()
                .success(true)
                .membersCreated(10)
                .build();

        when(conferenceImportService.importMembersFromExcel(eq(1), any(MultipartFile.class)))
                .thenReturn(successResult);

        var result = conferenceImportController.importMembers(1, mockFile);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(10, result.getBody().getMembersCreated());
    }

    @Test
    void importMembersShouldReturnBadRequestOnFailure() {
        ImportResultDTO failResult = ImportResultDTO.builder()
                .success(false)
                .errors(java.util.List.of(ImportResultDTO.ImportError.builder()
                        .sheet("Members")
                        .row(3)
                        .column("Email")
                        .message("Invalid email format at row 3")
                        .build()))
                .build();

        when(conferenceImportService.importMembersFromExcel(eq(1), any(MultipartFile.class)))
                .thenReturn(failResult);

        var result = conferenceImportController.importMembers(1, mockFile);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertFalse(result.getBody().getErrors().isEmpty());
        assertTrue(result.getBody().getErrors().get(0).getMessage().contains("Invalid email"));
    }

    @Test
    void importMembersWithConferenceIdShouldPassToService() {
        ImportResultDTO result = ImportResultDTO.builder().success(true).build();
        when(conferenceImportService.importMembersFromExcel(eq(50), any(MultipartFile.class)))
                .thenReturn(result);

        conferenceImportController.importMembers(50, mockFile);

        verify(conferenceImportService, times(1)).importMembersFromExcel(50, mockFile);
    }

    // ── Additional Edge Case Tests ──

    @Test
    void allTemplateEndpointsShouldReturnByteArray() {
        byte[] data = new byte[]{1, 2, 3};
        when(conferenceImportService.generateConferenceTemplate()).thenReturn(data);
        when(conferenceImportService.generateTrackTemplate()).thenReturn(data);
        when(conferenceImportService.generateSubjectAreaTemplate()).thenReturn(data);
        when(conferenceImportService.generateMemberTemplate()).thenReturn(data);

        assertNotNull(conferenceImportController.conferenceTemplate().getBody());
        assertNotNull(conferenceImportController.trackTemplate(1).getBody());
        assertNotNull(conferenceImportController.subjectAreaTemplate(1).getBody());
        assertNotNull(conferenceImportController.memberTemplate(1).getBody());
    }

    @Test
    void importResultShouldContainDetailsWhenAvailable() {
        ImportResultDTO result = ImportResultDTO.builder()
                .success(true)
                .conferenceName("Successfully imported 15 records")
                .build();

        when(conferenceImportService.importConferenceFromExcel(any(MultipartFile.class)))
                .thenReturn(result);

        var response = conferenceImportController.importConference(mockFile);

        assertNotNull(response.getBody().getConferenceName());
        assertTrue(response.getBody().getConferenceName().contains("15"));
    }

    @Test
    void multipleImportsWithDifferentFilesShouldWorkIndependently() {
        MultipartFile file1 = new MockMultipartFile("file", "test1.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1});
        MultipartFile file2 = new MockMultipartFile("file", "test2.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{2});

        ImportResultDTO result1 = ImportResultDTO.builder().success(true).conferenceId(5).build();
        ImportResultDTO result2 = ImportResultDTO.builder().success(true).conferenceId(10).build();

        when(conferenceImportService.previewConferenceFromExcel(file1)).thenReturn(result1);
        when(conferenceImportService.previewConferenceFromExcel(file2)).thenReturn(result2);

        var preview1 = conferenceImportController.previewConference(file1);
        var preview2 = conferenceImportController.previewConference(file2);

        assertEquals(5, preview1.getBody().getConferenceId());
        assertEquals(10, preview2.getBody().getConferenceId());
    }
}
