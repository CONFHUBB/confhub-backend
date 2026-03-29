package com.capstone.confhub.service.impl;

import com.capstone.confhub.entity.Conference;
import com.capstone.confhub.entity.Paper;
import com.capstone.confhub.entity.PaperFile;
import com.capstone.confhub.entity.Ticket;
import com.capstone.confhub.entity.User;
import com.capstone.confhub.exception.BadRequestException;
import com.capstone.confhub.exception.ResourceNotFoundException;
import com.capstone.confhub.repository.PaperAuthorRepository;
import com.capstone.confhub.repository.PaperFileRepository;
import com.capstone.confhub.repository.PaperRepository;
import com.capstone.confhub.repository.TicketRepository;
import com.capstone.confhub.repository.UserRepository;
import com.capstone.confhub.service.DocumentGenerationService;
import com.capstone.confhub.utils.enums.PaymentStatus;
import com.capstone.confhub.utils.enums.PaperStatus;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class DocumentGenerationServiceImpl implements DocumentGenerationService {

    private final PaperRepository paperRepository;
    private final PaperAuthorRepository paperAuthorRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final PaperFileRepository paperFileRepository;

    @Override
    public byte[] generateAcceptanceLetter(Integer paperId, Integer userId) {
        Paper paper = paperRepository.findById(paperId).orElseThrow(() -> new RuntimeException("Paper not found"));
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        // Guard: paper must be ACCEPTED or PUBLISHED
        if (paper.getStatus() != PaperStatus.ACCEPTED && paper.getStatus() != PaperStatus.PUBLISHED) {
            throw new RuntimeException("Acceptance letter is only available for accepted papers");
        }
        // Guard: requesting user must be an author of this paper
        if (!paperAuthorRepository.existsByPaperIdAndUserId(paperId, userId)) {
            throw new com.capstone.confhub.exception.ForbiddenException("Access denied: you are not listed as an author of this paper");
        }

        Conference conference = paper.getTrack().getConference();

        return createPdfStream(document -> {
            addHeader(document, conference);
            addCurrentDate(document);
            addGreeting(document, user);

            Paragraph content = new Paragraph(
                    String.format("We are pleased to inform you that your paper titled \"%s\" has been ACCEPTED for presentation at %s.",
                            paper.getTitle(), conference.getName()),
                    FontFactory.getFont(FontFactory.HELVETICA, 12));
            content.setSpacingBefore(20f);
            document.add(content);

            Paragraph p2 = new Paragraph(
                    "Please proceed to complete your registration and submit your camera-ready version according to the conference deadlines.",
                    FontFactory.getFont(FontFactory.HELVETICA, 12));
            p2.setSpacingBefore(10f);
            document.add(p2);

            addSignature(document, conference);
        });
    }

    @Override
    public byte[] generateInvoice(Integer ticketId, Integer userId) {
        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow(() -> new RuntimeException("Ticket not found"));
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        if (!ticket.getUser().getId().equals(userId)) {
            throw new com.capstone.confhub.exception.ForbiddenException("Access denied: you are not the owner of this ticket");
        }

        Conference conference = ticket.getTicketType().getConference();

        return createPdfStream(document -> {
            addHeader(document, conference);
            addCurrentDate(document);
            
            Paragraph title = new Paragraph("INVOICE / RECEIPT", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18));
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingBefore(20f);
            title.setSpacingAfter(20f);
            document.add(title);

            document.add(new Paragraph("Billed To: " + user.getFirstName() + " " + user.getLastName()));
            document.add(new Paragraph("Email: " + user.getEmail()));
            
            document.add(new Paragraph("\nPayment Details:"));
            document.add(new Paragraph("Ticket Type: " + ticket.getTicketType().getName()));
            document.add(new Paragraph(String.format("Amount Paid: %,.0f %s", ticket.getTicketType().getPrice(), ticket.getTicketType().getCurrency())));
            document.add(new Paragraph("Registration Number: " + ticket.getRegistrationNumber()));
            
            String payDateStr = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            document.add(new Paragraph("Date of Payment: " + payDateStr));
            
            Paragraph p2 = new Paragraph("\nThank you for your payment.");
            document.add(p2);
        });
    }

    @Override
    public byte[] generateCertificate(Integer ticketId, Integer userId) {
        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow(() -> new RuntimeException("Ticket not found"));
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        if (!ticket.getUser().getId().equals(userId)) {
            throw new com.capstone.confhub.exception.ForbiddenException("Access denied: you are not the owner of this ticket");
        }

        Conference conference = ticket.getTicketType().getConference();

        return createPdfStream(document -> {
            Paragraph title = new Paragraph("CERTIFICATE OF ATTENDANCE", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24));
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingBefore(80f);
            title.setSpacingAfter(40f);
            document.add(title);

            Paragraph content = new Paragraph(
                    String.format("This is to certify that\n\n%s %s\n\nsuccessfully attended the\n\n%s\n\nheld at %s.",
                            user.getFirstName(), user.getLastName(), conference.getName(), conference.getLocation()),
                    FontFactory.getFont(FontFactory.HELVETICA, 16));
            content.setAlignment(Element.ALIGN_CENTER);
            document.add(content);

            addSignature(document, conference);
        });
    }

    // ─── Bulk Export: Proceedings ZIP ───────────────────────────────────────────

    @Override
    public byte[] exportProceedings(Integer conferenceId) {
        List<PaperFile> files = paperFileRepository.findCameraReadyByConferenceId(conferenceId);
        if (files.isEmpty()) {
            throw new BadRequestException("No camera-ready files found for this conference.");
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {

            int idx = 1;
            for (PaperFile pf : files) {
                try {
                    byte[] content = downloadUrl(pf.getUrl());
                    String entryName = String.format("%03d_paper_%d.pdf", idx++, pf.getPaper().getId());
                    zos.putNextEntry(new ZipEntry(entryName));
                    zos.write(content);
                    zos.closeEntry();
                } catch (IOException e) {
                    // Skip files that can't be downloaded; log and continue
                }
            }
            zos.finish();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate proceedings ZIP", e);
        }
    }

    // ─── Bulk Export: Invoices ZIP ───────────────────────────────────────────────

    @Override
    public byte[] exportInvoices(Integer conferenceId) {
        List<Ticket> tickets = ticketRepository.findByConferenceId(conferenceId).stream()
                .filter(t -> PaymentStatus.COMPLETED.equals(t.getPaymentStatus()))
                .toList();

        if (tickets.isEmpty()) {
            throw new BadRequestException("No completed payments found for this conference.");
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {

            for (Ticket ticket : tickets) {
                try {
                    byte[] pdf = generateInvoiceForExport(ticket);
                    String entryName = String.format("invoice_%s.pdf", ticket.getRegistrationNumber() != null
                            ? ticket.getRegistrationNumber() : "ticket_" + ticket.getId());
                    zos.putNextEntry(new ZipEntry(entryName));
                    zos.write(pdf);
                    zos.closeEntry();
                } catch (Exception e) {
                    // Skip failed PDFs and continue
                }
            }
            zos.finish();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate invoices ZIP", e);
        }
    }

    /** Internal invoice generator that doesn't require userId ownership check (admin export use). */
    private byte[] generateInvoiceForExport(Ticket ticket) {
        Conference conference = ticket.getTicketType() != null
                ? ticket.getTicketType().getConference()
                : ticket.getConference();
        User user = ticket.getUser();

        return createPdfStream(document -> {
            addHeader(document, conference);
            addCurrentDate(document);

            Paragraph title = new Paragraph("INVOICE / RECEIPT", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18));
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingBefore(20f);
            title.setSpacingAfter(20f);
            document.add(title);

            document.add(new Paragraph("Billed To: " + user.getFirstName() + " " + user.getLastName()));
            document.add(new Paragraph("Email: " + user.getEmail()));
            document.add(new Paragraph("\nTicket Type: " + ticket.getTicketTypeName()));
            document.add(new Paragraph(String.format("Amount Paid: %,.0f VND", ticket.getPrice())));
            document.add(new Paragraph("Registration Number: " + ticket.getRegistrationNumber()));
            document.add(new Paragraph("Date: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE)));
            document.add(new Paragraph("\nThank you for your payment."));

            addSignature(document, conference);
        });
    }

    /** Download bytes from a URL (public Firebase storage URL). */
    private byte[] downloadUrl(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        URLConnection connection = url.openConnection();
        connection.setConnectTimeout(10_000);
        connection.setReadTimeout(30_000);
        try (InputStream in = connection.getInputStream();
             ByteArrayOutputStream buf = new ByteArrayOutputStream()) {
            byte[] block = new byte[8192];
            int read;
            while ((read = in.read(block)) != -1) {
                buf.write(block, 0, read);
            }
            return buf.toByteArray();
        }
    }

    private byte[] createPdfStream(PdfContentGenerator generator) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(document, baos);
            document.open();
            generator.generate(document);
            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF document", e);
        }
    }

    private void addHeader(Document document, Conference conference) throws DocumentException {
        Paragraph header = new Paragraph(conference.getName(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16));
        header.setAlignment(Element.ALIGN_CENTER);
        document.add(header);

        if (conference.getWebsiteUrl() != null) {
            Paragraph website = new Paragraph(conference.getWebsiteUrl(), FontFactory.getFont(FontFactory.HELVETICA, 10));
            website.setAlignment(Element.ALIGN_CENTER);
            document.add(website);
        }
        document.add(new Paragraph("\n"));
    }

    private void addCurrentDate(Document document) throws DocumentException {
        Paragraph date = new Paragraph("Date: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                FontFactory.getFont(FontFactory.HELVETICA, 12));
        date.setAlignment(Element.ALIGN_RIGHT);
        document.add(date);
    }

    private void addGreeting(Document document, User user) throws DocumentException {
        Paragraph greeting = new Paragraph(String.format("Dear %s %s,", user.getFirstName(), user.getLastName()),
                FontFactory.getFont(FontFactory.HELVETICA, 12));
        greeting.setSpacingBefore(20f);
        document.add(greeting);
    }

    private void addSignature(Document document, Conference conference) throws DocumentException {
        Paragraph signature = new Paragraph("\n\nSincerely,\n\nOrganizing Committee\n" + conference.getName(),
                FontFactory.getFont(FontFactory.HELVETICA, 12));
        signature.setAlignment(Element.ALIGN_RIGHT);
        signature.setSpacingBefore(40f);
        document.add(signature);
    }

    @FunctionalInterface
    private interface PdfContentGenerator {
        void generate(Document document) throws Exception;
    }
}
