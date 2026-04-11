package com.capstone.confhub.service;

import com.capstone.confhub.dto.response.ImportResultDTO;
import org.springframework.web.multipart.MultipartFile;

public interface ConferenceImportService {

    // Conference
    ImportResultDTO previewConferenceFromExcel(MultipartFile file);
    ImportResultDTO importConferenceFromExcel(MultipartFile file);
    byte[] generateConferenceTemplate();

    // Tracks
    ImportResultDTO previewTracksFromExcel(MultipartFile file);
    ImportResultDTO importTracksFromExcel(Integer conferenceId, MultipartFile file);
    byte[] generateTrackTemplate();

    // Subject Areas
    ImportResultDTO previewSubjectAreasFromExcel(MultipartFile file);
    ImportResultDTO importSubjectAreasFromExcel(Integer conferenceId, MultipartFile file);
    byte[] generateSubjectAreaTemplate();

    // Members
    ImportResultDTO previewMembersFromExcel(MultipartFile file);
    ImportResultDTO importMembersFromExcel(Integer conferenceId, MultipartFile file);
    byte[] generateMemberTemplate();

    // Ticket Types
    ImportResultDTO previewTicketTypesFromExcel(Integer conferenceId, MultipartFile file);
    ImportResultDTO importTicketTypesFromExcel(Integer conferenceId, MultipartFile file);
    byte[] generateTicketTypeTemplate();
}
