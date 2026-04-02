package com.capstone.confhub.service.impl;

import com.capstone.confhub.entity.Conference;
import com.capstone.confhub.entity.ConferenceTrack;
import com.capstone.confhub.entity.Paper;
import com.capstone.confhub.entity.PaperFile;
import com.capstone.confhub.entity.Ticket;
import com.capstone.confhub.entity.TicketType;
import com.capstone.confhub.entity.User;
import com.capstone.confhub.exception.BadRequestException;
import com.capstone.confhub.exception.ForbiddenException;
import com.capstone.confhub.repository.PaperAuthorRepository;
import com.capstone.confhub.repository.PaperFileRepository;
import com.capstone.confhub.repository.PaperRepository;
import com.capstone.confhub.repository.TicketRepository;
import com.capstone.confhub.repository.UserRepository;
import com.capstone.confhub.utils.enums.PaperStatus;
import com.capstone.confhub.utils.enums.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentGenerationServiceImplTest {

    private static final int PAPER_ID = 10;
    private static final int USER_ID = 1;
    private static final int OTHER_USER_ID = 2;
    private static final int TICKET_ID = 100;
    private static final int CONFERENCE_ID = 200;

    @Mock
    private PaperRepository paperRepository;
    @Mock
    private PaperAuthorRepository paperAuthorRepository;
    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PaperFileRepository paperFileRepository;

    @InjectMocks
    private DocumentGenerationServiceImpl documentGenerationService;

    private Conference conference;
    private User user;
    private User otherUser;
    private Paper paper;
    private Ticket ticket;

    @BeforeEach
    void setUp() {
        conference = new Conference();
        conference.setId(CONFERENCE_ID);
        conference.setName("ConfHub 2026");
        conference.setLocation("HCM");
        conference.setWebsiteUrl("https://confhub.example");

        user = new User();
        user.setId(USER_ID);
        user.setFirstName("Jane");
        user.setLastName("Doe");
        user.setEmail("jane@example.com");

        otherUser = new User();
        otherUser.setId(OTHER_USER_ID);
        otherUser.setFirstName("John");
        otherUser.setLastName("Smith");
        otherUser.setEmail("john@example.com");

        ConferenceTrack track = new ConferenceTrack();
        track.setId(20);
        track.setConference(conference);

        paper = new Paper();
        paper.setId(PAPER_ID);
        paper.setTrack(track);
        paper.setTitle("Neural Inference");
        paper.setStatus(PaperStatus.ACCEPTED);

        TicketType ticketType = new TicketType();
        ticketType.setId(50);
        ticketType.setConference(conference);
        ticketType.setName("Author Pass");
        ticketType.setPrice(BigDecimal.valueOf(1000000));
        ticketType.setCurrency("VND");

        ticket = new Ticket();
        ticket.setId(TICKET_ID);
        ticket.setUser(user);
        ticket.setConference(conference);
        ticket.setTicketType(ticketType);
        ticket.setTicketTypeName("Author Pass");
        ticket.setPrice(BigDecimal.valueOf(1000000));
        ticket.setRegistrationNumber("REG-001");
        ticket.setPaymentStatus(PaymentStatus.COMPLETED);
    }

    private void assertPdfHeader(byte[] pdf) {
        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
        assertTrue(new String(pdf, 0, 4, StandardCharsets.ISO_8859_1).contains("%PDF"));
    }

    private int countZipEntries(byte[] zipBytes) throws Exception {
        int count = 0;
        try (ZipInputStream zis = new ZipInputStream(new java.io.ByteArrayInputStream(zipBytes))) {
            while (zis.getNextEntry() != null) {
                count++;
            }
        }
        return count;
    }

    @Test
    void generateAcceptanceLetterShouldReturnPdfForAcceptedPaperAuthor() {
        when(paperRepository.findById(PAPER_ID)).thenReturn(Optional.of(paper));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(paperAuthorRepository.existsByPaperIdAndUserId(PAPER_ID, USER_ID)).thenReturn(true);

        byte[] pdf = documentGenerationService.generateAcceptanceLetter(PAPER_ID, USER_ID);

        assertPdfHeader(pdf);
    }

    @Test
    void generateAcceptanceLetterShouldReturnPdfForPublishedPaperAuthor() {
        paper.setStatus(PaperStatus.PUBLISHED);
        when(paperRepository.findById(PAPER_ID)).thenReturn(Optional.of(paper));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(paperAuthorRepository.existsByPaperIdAndUserId(PAPER_ID, USER_ID)).thenReturn(true);

        byte[] pdf = documentGenerationService.generateAcceptanceLetter(PAPER_ID, USER_ID);

        assertPdfHeader(pdf);
    }

    @Test
    void generateAcceptanceLetterShouldThrowWhenPaperNotAccepted() {
        paper.setStatus(PaperStatus.UNDER_REVIEW);
        when(paperRepository.findById(PAPER_ID)).thenReturn(Optional.of(paper));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        assertThrows(RuntimeException.class,
                () -> documentGenerationService.generateAcceptanceLetter(PAPER_ID, USER_ID));
    }

    @Test
    void generateAcceptanceLetterShouldThrowWhenUserIsNotAuthor() {
        when(paperRepository.findById(PAPER_ID)).thenReturn(Optional.of(paper));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(paperAuthorRepository.existsByPaperIdAndUserId(PAPER_ID, USER_ID)).thenReturn(false);

        assertThrows(ForbiddenException.class,
                () -> documentGenerationService.generateAcceptanceLetter(PAPER_ID, USER_ID));
    }

    @Test
    void generateAcceptanceLetterShouldThrowWhenPaperNotFound() {
        when(paperRepository.findById(PAPER_ID)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> documentGenerationService.generateAcceptanceLetter(PAPER_ID, USER_ID));
    }

    @Test
    void generateAcceptanceLetterShouldThrowWhenUserNotFound() {
        when(paperRepository.findById(PAPER_ID)).thenReturn(Optional.of(paper));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> documentGenerationService.generateAcceptanceLetter(PAPER_ID, USER_ID));
    }

    @Test
    void generateInvoiceShouldReturnPdfForTicketOwner() {
        when(ticketRepository.findById(TICKET_ID)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        byte[] pdf = documentGenerationService.generateInvoice(TICKET_ID, USER_ID);

        assertPdfHeader(pdf);
    }

    @Test
    void generateInvoiceShouldThrowWhenRequesterIsNotOwner() {
        when(ticketRepository.findById(TICKET_ID)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(OTHER_USER_ID)).thenReturn(Optional.of(otherUser));

        assertThrows(ForbiddenException.class,
                () -> documentGenerationService.generateInvoice(TICKET_ID, OTHER_USER_ID));
    }

    @Test
    void generateInvoiceShouldThrowWhenTicketNotFound() {
        when(ticketRepository.findById(TICKET_ID)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> documentGenerationService.generateInvoice(TICKET_ID, USER_ID));
    }

    @Test
    void generateInvoiceShouldThrowWhenUserNotFound() {
        when(ticketRepository.findById(TICKET_ID)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> documentGenerationService.generateInvoice(TICKET_ID, USER_ID));
    }

    @Test
    void generateCertificateShouldReturnPdfForTicketOwner() {
        when(ticketRepository.findById(TICKET_ID)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        byte[] pdf = documentGenerationService.generateCertificate(TICKET_ID, USER_ID);

        assertPdfHeader(pdf);
    }

    @Test
    void generateCertificateShouldThrowWhenRequesterIsNotOwner() {
        when(ticketRepository.findById(TICKET_ID)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(OTHER_USER_ID)).thenReturn(Optional.of(otherUser));

        assertThrows(ForbiddenException.class,
                () -> documentGenerationService.generateCertificate(TICKET_ID, OTHER_USER_ID));
    }

    @Test
    void generateCertificateShouldThrowWhenTicketNotFound() {
        when(ticketRepository.findById(TICKET_ID)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> documentGenerationService.generateCertificate(TICKET_ID, USER_ID));
    }

    @Test
    void exportProceedingsShouldThrowWhenNoCameraReadyFiles() {
        when(paperFileRepository.findCameraReadyByConferenceId(CONFERENCE_ID)).thenReturn(List.of());

        assertThrows(BadRequestException.class,
                () -> documentGenerationService.exportProceedings(CONFERENCE_ID));
    }

    @Test
    void exportProceedingsShouldReturnZipEvenWhenAFileCannotBeDownloaded() {
        PaperFile file = new PaperFile();
        file.setPaper(paper);
        file.setUrl("bad://broken-url");
        when(paperFileRepository.findCameraReadyByConferenceId(CONFERENCE_ID)).thenReturn(List.of(file));

        byte[] zip = documentGenerationService.exportProceedings(CONFERENCE_ID);

        assertNotNull(zip);
        assertTrue(zip.length > 0);
    }

    @Test
    void exportProceedingsShouldContainEntryForDownloadableFile() throws Exception {
        Path tempPdf = Files.createTempFile("camera-ready-", ".pdf");
        Files.write(tempPdf, "%PDF-1.4 fake".getBytes(StandardCharsets.ISO_8859_1));

        PaperFile file = new PaperFile();
        file.setPaper(paper);
        file.setUrl(tempPdf.toUri().toString());
        when(paperFileRepository.findCameraReadyByConferenceId(CONFERENCE_ID)).thenReturn(List.of(file));

        byte[] zip = documentGenerationService.exportProceedings(CONFERENCE_ID);

        assertNotNull(zip);
        assertTrue(zip.length > 0);
        assertEquals(1, countZipEntries(zip));

        Files.deleteIfExists(tempPdf);
    }

    @Test
    void exportInvoicesShouldThrowWhenNoCompletedPaymentTickets() {
        ticket.setPaymentStatus(PaymentStatus.PENDING);
        when(ticketRepository.findByConferenceId(CONFERENCE_ID)).thenReturn(List.of(ticket));

        assertThrows(BadRequestException.class,
                () -> documentGenerationService.exportInvoices(CONFERENCE_ID));
    }

    @Test
    void exportInvoicesShouldReturnZipForCompletedPaymentTickets() {
        when(ticketRepository.findByConferenceId(CONFERENCE_ID)).thenReturn(List.of(ticket));

        byte[] zip = documentGenerationService.exportInvoices(CONFERENCE_ID);

        assertNotNull(zip);
        assertTrue(zip.length > 0);
    }

    @Test
    void exportInvoicesShouldIncludeOnlyCompletedTickets() throws Exception {
        Ticket pendingTicket = new Ticket();
        pendingTicket.setId(TICKET_ID + 1);
        pendingTicket.setUser(user);
        pendingTicket.setConference(conference);
        pendingTicket.setTicketType(ticket.getTicketType());
        pendingTicket.setTicketTypeName("Author Pass");
        pendingTicket.setPrice(BigDecimal.valueOf(800000));
        pendingTicket.setRegistrationNumber("REG-002");
        pendingTicket.setPaymentStatus(PaymentStatus.PENDING);

        when(ticketRepository.findByConferenceId(CONFERENCE_ID)).thenReturn(List.of(ticket, pendingTicket));

        byte[] zip = documentGenerationService.exportInvoices(CONFERENCE_ID);

        assertNotNull(zip);
        assertTrue(zip.length > 0);
        assertEquals(1, countZipEntries(zip));
    }
}

