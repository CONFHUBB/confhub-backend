package com.capstone.confms.service.impl;

import com.capstone.confms.entity.Conference;
import com.capstone.confms.entity.Paper;
import com.capstone.confms.entity.Ticket;
import com.capstone.confms.entity.User;
import com.capstone.confms.repository.PaperRepository;
import com.capstone.confms.repository.TicketRepository;
import com.capstone.confms.repository.UserRepository;
import com.capstone.confms.service.DocumentGenerationService;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class DocumentGenerationServiceImpl implements DocumentGenerationService {

    private final PaperRepository paperRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    @Override
    public byte[] generateAcceptanceLetter(Integer paperId, Integer userId) {
        Paper paper = paperRepository.findById(paperId).orElseThrow(() -> new RuntimeException("Paper not found"));
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
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
    public byte[] generateVisaLetter(Integer ticketId, Integer userId) {
        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow(() -> new RuntimeException("Ticket not found"));
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        Conference conference = ticket.getTicketType().getConference();

        return createPdfStream(document -> {
            addHeader(document, conference);
            addCurrentDate(document);
            addGreeting(document, user);

            Paragraph content = new Paragraph(
                    String.format("This letter is to confirm that %s %s is a registered attendee for %s, which will be held at %s.",
                            user.getFirstName(), user.getLastName(), conference.getName(), conference.getLocation()),
                    FontFactory.getFont(FontFactory.HELVETICA, 12));
            content.setSpacingBefore(20f);
            document.add(content);

            Paragraph p2 = new Paragraph(
                    "We kindly request that you grant the necessary visa to enable their attendance at the conference. Should you require any further information, please do not hesitate to contact us.",
                    FontFactory.getFont(FontFactory.HELVETICA, 12));
            p2.setSpacingBefore(10f);
            document.add(p2);

            addSignature(document, conference);
        });
    }

    @Override
    public byte[] generateCertificate(Integer ticketId, Integer userId) {
        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow(() -> new RuntimeException("Ticket not found"));
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
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
