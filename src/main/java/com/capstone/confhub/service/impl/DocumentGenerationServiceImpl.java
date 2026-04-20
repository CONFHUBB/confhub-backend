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
import com.lowagie.text.Phrase;
import com.lowagie.text.PageSize;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPageEventHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.awt.Color;
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

    // ── Brand Colors ──
    private static final Color PRIMARY = new Color(67, 56, 202);       // Indigo-600
    private static final Color PRIMARY_LIGHT = new Color(238, 242, 255); // Indigo-50
    private static final Color ACCENT = new Color(16, 185, 129);        // Emerald-500
    private static final Color TEXT_DARK = new Color(31, 41, 55);       // Gray-800
    private static final Color TEXT_MUTED = new Color(107, 114, 128);   // Gray-500
    private static final Color BORDER = new Color(229, 231, 235);       // Gray-200
    private static final Color BG_LIGHT = new Color(249, 250, 251);     // Gray-50
    private static final Color GOLD = new Color(180, 140, 36);          // Gold for certificate

    // ── Fonts ──
    private static final Font FONT_BRAND = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, PRIMARY);
    private static final Font FONT_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, TEXT_DARK);
    private static final Font FONT_SUBTITLE = FontFactory.getFont(FontFactory.HELVETICA, 11, TEXT_MUTED);
    private static final Font FONT_HEADING = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, PRIMARY);
    private static final Font FONT_BODY = FontFactory.getFont(FontFactory.HELVETICA, 11, TEXT_DARK);
    private static final Font FONT_BODY_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, TEXT_DARK);
    private static final Font FONT_SMALL = FontFactory.getFont(FontFactory.HELVETICA, 9, TEXT_MUTED);
    private static final Font FONT_LABEL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, TEXT_MUTED);
    private static final Font FONT_VALUE = FontFactory.getFont(FontFactory.HELVETICA, 11, TEXT_DARK);
    private static final Font FONT_BIG_VALUE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, PRIMARY);

    // ════════════════════════════════════════════════════════════════════════════
    // ACCEPTANCE LETTER
    // ════════════════════════════════════════════════════════════════════════════

    @Override
    public byte[] generateAcceptanceLetter(Integer paperId, Integer userId) {
        Paper paper = paperRepository.findById(paperId).orElseThrow(() -> new RuntimeException("Paper not found"));
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        if (paper.getStatus() != PaperStatus.ACCEPTED && paper.getStatus() != PaperStatus.AWAITING_REGISTRATION && paper.getStatus() != PaperStatus.REGISTERED && paper.getStatus() != PaperStatus.AWAITING_CAMERA_READY && paper.getStatus() != PaperStatus.CAMERA_READY_SUBMITTED && paper.getStatus() != PaperStatus.PUBLISHED) {
            throw new RuntimeException("Acceptance letter is only available for accepted papers");
        }
        if (!paperAuthorRepository.existsByPaperIdAndUserId(paperId, userId)) {
            throw new com.capstone.confhub.exception.ForbiddenException("Access denied: you are not listed as an author of this paper");
        }

        Conference conference = paper.getTrack().getConference();
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"));

        return createPdfStream(document -> {
            // ── Letterhead: colored top bar ──
            addColoredTopBar(document);

            // ── Conference Name (brand) ──
            Paragraph brand = new Paragraph(conference.getName(), FONT_BRAND);
            brand.setAlignment(Element.ALIGN_CENTER);
            brand.setSpacingBefore(25f);
            document.add(brand);

            if (conference.getWebsiteUrl() != null) {
                Paragraph web = new Paragraph(conference.getWebsiteUrl(), FONT_SMALL);
                web.setAlignment(Element.ALIGN_CENTER);
                document.add(web);
            }

            if (conference.getLocation() != null) {
                Paragraph loc = new Paragraph(conference.getLocation(), FONT_SMALL);
                loc.setAlignment(Element.ALIGN_CENTER);
                document.add(loc);
            }

            // ── Date (right-aligned) ──
            Paragraph date = new Paragraph(dateStr, FONT_BODY);
            date.setAlignment(Element.ALIGN_RIGHT);
            date.setSpacingBefore(30f);
            document.add(date);

            // ── Title banner ──
            addSectionBanner(document, "LETTER OF ACCEPTANCE", 20f);

            // ── Greeting ──
            Paragraph greeting = new Paragraph(
                    String.format("Dear %s %s,", user.getFirstName(), user.getLastName()), FONT_BODY);
            greeting.setSpacingBefore(25f);
            document.add(greeting);

            // ── Body text ──
            Paragraph p1 = new Paragraph(
                    String.format(
                            "We are pleased to inform you that your paper has been ACCEPTED for presentation at %s.",
                            conference.getName()),
                    FONT_BODY);
            p1.setSpacingBefore(15f);
            p1.setLeading(18f);
            document.add(p1);

            // ── Paper Details Box ──
            addInfoBox(document, new String[][]{
                    {"Paper ID", "#" + paper.getId()},
                    {"Title", paper.getTitle()},
                    {"Track", paper.getTrack() != null ? paper.getTrack().getName() : "—"},
            }, 15f);

            // ── Next steps ──
            Paragraph p2 = new Paragraph(
                    "Please proceed to complete your registration and submit your camera-ready version " +
                            "according to the conference deadlines. For detailed instructions, please visit the conference website.",
                    FONT_BODY);
            p2.setSpacingBefore(15f);
            p2.setLeading(18f);
            document.add(p2);

            Paragraph p3 = new Paragraph(
                    "We look forward to your participation and contribution to the conference.",
                    FONT_BODY);
            p3.setSpacingBefore(10f);
            p3.setLeading(18f);
            document.add(p3);

            // ── Signature ──
            addProfessionalSignature(document, conference);

            // ── Footer ──
            addFooterNote(document, "This is an official document generated by ConfHub. Ref: ACC-" + paper.getId());
        });
    }

    // ════════════════════════════════════════════════════════════════════════════
    // INVOICE / RECEIPT
    // ════════════════════════════════════════════════════════════════════════════

    @Override
    public byte[] generateInvoice(Integer ticketId, Integer userId) {
        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow(() -> new RuntimeException("Ticket not found"));
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        if (!ticket.getUser().getId().equals(userId)) {
            throw new com.capstone.confhub.exception.ForbiddenException("Access denied: you are not the owner of this ticket");
        }

        Conference conference = ticket.getTicketType().getConference();
        return buildInvoicePdf(ticket, user, conference);
    }

    private byte[] buildInvoicePdf(Ticket ticket, User user, Conference conference) {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"));
        String invoiceNo = "INV-" + (ticket.getRegistrationNumber() != null
                ? ticket.getRegistrationNumber() : String.format("%06d", ticket.getId()));

        return createPdfStream(document -> {
            // ── Colored top bar ──
            addColoredTopBar(document);

            // ── Header: 2-column layout (Conference info | Invoice meta) ──
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{60, 40});
            headerTable.setSpacingBefore(20f);

            // Left: Conference info
            PdfPCell leftCell = new PdfPCell();
            leftCell.setBorder(Rectangle.NO_BORDER);
            leftCell.setPaddingBottom(10f);
            leftCell.addElement(new Paragraph(conference.getName(), FONT_BRAND));
            if (conference.getLocation() != null)
                leftCell.addElement(new Paragraph(conference.getLocation(), FONT_SMALL));
            if (conference.getWebsiteUrl() != null)
                leftCell.addElement(new Paragraph(conference.getWebsiteUrl(), FONT_SMALL));
            headerTable.addCell(leftCell);

            // Right: Invoice number and date
            PdfPCell rightCell = new PdfPCell();
            rightCell.setBorder(Rectangle.NO_BORDER);
            rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            rightCell.setPaddingBottom(10f);

            Paragraph invTitle = new Paragraph("INVOICE", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 28, PRIMARY));
            invTitle.setAlignment(Element.ALIGN_RIGHT);
            rightCell.addElement(invTitle);

            Paragraph invNo = new Paragraph(invoiceNo, FONT_SMALL);
            invNo.setAlignment(Element.ALIGN_RIGHT);
            rightCell.addElement(invNo);

            Paragraph invDate = new Paragraph("Date: " + dateStr, FONT_SMALL);
            invDate.setAlignment(Element.ALIGN_RIGHT);
            rightCell.addElement(invDate);

            headerTable.addCell(rightCell);
            document.add(headerTable);

            // ── Divider ──
            addDivider(document);

            // ── Bill To section ──
            Paragraph billToLabel = new Paragraph("BILL TO", FONT_LABEL);
            billToLabel.setSpacingBefore(10f);
            document.add(billToLabel);

            Paragraph billToName = new Paragraph(user.getFirstName() + " " + user.getLastName(), FONT_BODY_BOLD);
            billToName.setSpacingBefore(5f);
            document.add(billToName);

            Paragraph billToEmail = new Paragraph(user.getEmail(), FONT_BODY);
            document.add(billToEmail);

            // ── Items Table ──
            PdfPTable itemsTable = new PdfPTable(3);
            itemsTable.setWidthPercentage(100);
            itemsTable.setWidths(new float[]{55, 20, 25});
            itemsTable.setSpacingBefore(25f);

            // Table header
            addTableHeaderCell(itemsTable, "Description");
            addTableHeaderCell(itemsTable, "Qty");
            addTableHeaderCell(itemsTable, "Amount");

            // Table row
            addTableBodyCell(itemsTable, ticket.getTicketTypeName() + "\nConference Registration", false);
            addTableBodyCell(itemsTable, "1", false);
            addTableBodyCell(itemsTable, formatCurrency(ticket.getPrice().doubleValue()), false);

            document.add(itemsTable);

            // ── Totals ──
            PdfPTable totalsTable = new PdfPTable(2);
            totalsTable.setWidthPercentage(45);
            totalsTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalsTable.setSpacingBefore(5f);

            addTotalsRow(totalsTable, "Subtotal", formatCurrency(ticket.getPrice().doubleValue()), false);
            addTotalsRow(totalsTable, "Tax", "—", false);
            addTotalsRow(totalsTable, "TOTAL", formatCurrency(ticket.getPrice().doubleValue()), true);

            document.add(totalsTable);

            // ── Payment Status ──
            addPaymentStatusBadge(document, ticket);

            // ── Registration Info ──
            addInfoBox(document, new String[][]{
                    {"Registration No.", ticket.getRegistrationNumber() != null ? ticket.getRegistrationNumber() : "—"},
                    {"Payment Status", ticket.getPaymentStatus() != null ? ticket.getPaymentStatus().name() : "—"},
            }, 20f);

            // ── Footer ──
            addFooterNote(document, "This is a computer-generated invoice. No signature required. Ref: " + invoiceNo);
        });
    }

    // ════════════════════════════════════════════════════════════════════════════
    // CERTIFICATE OF ATTENDANCE
    // ════════════════════════════════════════════════════════════════════════════

    @Override
    public byte[] generateCertificate(Integer ticketId, Integer userId) {
        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow(() -> new RuntimeException("Ticket not found"));
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        if (!ticket.getUser().getId().equals(userId)) {
            throw new com.capstone.confhub.exception.ForbiddenException("Access denied: you are not the owner of this ticket");
        }

        Conference conference = ticket.getTicketType().getConference();
        String dateRange = formatDateRange(conference.getStartDate(), conference.getEndDate());

        return createPdfStream(document -> {
            // ── Decorative border ──
            addCertificateBorder(document);

            // ── Title ──
            Paragraph certLabel = new Paragraph("CERTIFICATE", FontFactory.getFont(FontFactory.HELVETICA, 14, Font.NORMAL, TEXT_MUTED));
            certLabel.setAlignment(Element.ALIGN_CENTER);
            certLabel.setSpacingBefore(60f);
            document.add(certLabel);

            Paragraph title = new Paragraph("OF ATTENDANCE", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 28, GOLD));
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingBefore(5f);
            title.setSpacingAfter(30f);
            document.add(title);

            // ── Decorative line ──
            PdfPTable line = new PdfPTable(1);
            line.setWidthPercentage(30);
            PdfPCell lineCell = new PdfPCell();
            lineCell.setBorderWidth(0);
            lineCell.setBorderWidthBottom(2f);
            lineCell.setBorderColorBottom(GOLD);
            lineCell.setFixedHeight(1f);
            line.addCell(lineCell);
            document.add(line);

            // ── "This is to certify that" ──
            Paragraph preamble = new Paragraph("This is to certify that", FONT_BODY);
            preamble.setAlignment(Element.ALIGN_CENTER);
            preamble.setSpacingBefore(30f);
            document.add(preamble);

            // ── Attendee Name (large) ──
            Paragraph name = new Paragraph(
                    user.getFirstName() + " " + user.getLastName(),
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 26, PRIMARY));
            name.setAlignment(Element.ALIGN_CENTER);
            name.setSpacingBefore(15f);
            name.setSpacingAfter(15f);
            document.add(name);

            // ── "has successfully attended" ──
            Paragraph attended = new Paragraph("has successfully attended the", FONT_BODY);
            attended.setAlignment(Element.ALIGN_CENTER);
            document.add(attended);

            // ── Conference Name ──
            Paragraph confName = new Paragraph(conference.getName(),
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, TEXT_DARK));
            confName.setAlignment(Element.ALIGN_CENTER);
            confName.setSpacingBefore(10f);
            document.add(confName);

            // ── Location & Dates ──
            if (conference.getLocation() != null) {
                Paragraph loc = new Paragraph("held at " + conference.getLocation(), FONT_SUBTITLE);
                loc.setAlignment(Element.ALIGN_CENTER);
                loc.setSpacingBefore(8f);
                document.add(loc);
            }

            if (dateRange != null) {
                Paragraph dates = new Paragraph(dateRange, FONT_SUBTITLE);
                dates.setAlignment(Element.ALIGN_CENTER);
                dates.setSpacingBefore(5f);
                document.add(dates);
            }

            // ── Signature area ──
            addProfessionalSignature(document, conference);

            // ── Footer ──
            addFooterNote(document, "Certificate ID: CERT-" + ticket.getId() + " | Generated by ConfHub");
        });
    }

    // ════════════════════════════════════════════════════════════════════════════
    // BULK EXPORTS
    // ════════════════════════════════════════════════════════════════════════════

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
                    // Skip files that can't be downloaded
                }
            }
            zos.finish();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate proceedings ZIP", e);
        }
    }

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
                    Conference conference = ticket.getTicketType() != null
                            ? ticket.getTicketType().getConference()
                            : ticket.getConference();
                    byte[] pdf = buildInvoicePdf(ticket, ticket.getUser(), conference);
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

    // ════════════════════════════════════════════════════════════════════════════
    // PDF BUILDING BLOCKS
    // ════════════════════════════════════════════════════════════════════════════

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

    /** Adds a bold colored bar at the very top of the page. */
    private void addColoredTopBar(Document document) throws DocumentException {
        PdfPTable bar = new PdfPTable(1);
        bar.setWidthPercentage(110);
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(PRIMARY);
        cell.setFixedHeight(6f);
        cell.setBorder(Rectangle.NO_BORDER);
        bar.addCell(cell);
        document.add(bar);
    }

    /** Adds a colored section banner (e.g., "LETTER OF ACCEPTANCE"). */
    private void addSectionBanner(Document document, String text, float spacingBefore) throws DocumentException {
        PdfPTable banner = new PdfPTable(1);
        banner.setWidthPercentage(100);
        banner.setSpacingBefore(spacingBefore);

        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.WHITE)));
        cell.setBackgroundColor(PRIMARY);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(12f);
        cell.setBorder(Rectangle.NO_BORDER);
        banner.addCell(cell);
        document.add(banner);
    }

    /** Adds a styled info box with key-value pairs. */
    private void addInfoBox(Document document, String[][] rows, float spacingBefore) throws DocumentException {
        PdfPTable box = new PdfPTable(2);
        box.setWidthPercentage(100);
        box.setWidths(new float[]{30, 70});
        box.setSpacingBefore(spacingBefore);

        for (String[] row : rows) {
            // Label
            PdfPCell labelCell = new PdfPCell(new Phrase(row[0], FONT_LABEL));
            labelCell.setBackgroundColor(BG_LIGHT);
            labelCell.setBorderColor(BORDER);
            labelCell.setBorderWidth(0.5f);
            labelCell.setPadding(10f);
            labelCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            box.addCell(labelCell);

            // Value
            PdfPCell valueCell = new PdfPCell(new Phrase(row[1], FONT_VALUE));
            valueCell.setBorderColor(BORDER);
            valueCell.setBorderWidth(0.5f);
            valueCell.setPadding(10f);
            valueCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            box.addCell(valueCell);
        }

        document.add(box);
    }

    /** Adds a horizontal divider line. */
    private void addDivider(Document document) throws DocumentException {
        PdfPTable divider = new PdfPTable(1);
        divider.setWidthPercentage(100);
        divider.setSpacingBefore(10f);
        divider.setSpacingAfter(5f);

        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setBorderWidthBottom(1f);
        cell.setBorderColorBottom(BORDER);
        cell.setFixedHeight(1f);
        divider.addCell(cell);
        document.add(divider);
    }

    /** Adds a table header cell with styling. */
    private void addTableHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE)));
        cell.setBackgroundColor(PRIMARY);
        cell.setPadding(10f);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }

    /** Adds a table body cell with alternating background. */
    private void addTableBodyCell(PdfPTable table, String text, boolean isAlt) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_BODY));
        cell.setBackgroundColor(isAlt ? BG_LIGHT : Color.WHITE);
        cell.setPadding(10f);
        cell.setBorderColor(BORDER);
        cell.setBorderWidth(0.5f);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }

    /** Adds a totals row to the summary table. */
    private void addTotalsRow(PdfPTable table, String label, String value, boolean isTotal) {
        Font labelFont = isTotal ? FONT_BODY_BOLD : FONT_BODY;
        Font valueFont = isTotal ? FONT_BIG_VALUE : FONT_BODY;

        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setBorderWidthTop(isTotal ? 1f : 0);
        labelCell.setBorderColorTop(BORDER);
        labelCell.setPadding(8f);
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setBorderWidthTop(isTotal ? 1f : 0);
        valueCell.setBorderColorTop(BORDER);
        valueCell.setPadding(8f);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(valueCell);
    }

    /** Adds a payment status badge. */
    private void addPaymentStatusBadge(Document document, Ticket ticket) throws DocumentException {
        String statusText = ticket.getPaymentStatus() != null ? ticket.getPaymentStatus().name() : "UNKNOWN";
        boolean isPaid = PaymentStatus.COMPLETED.equals(ticket.getPaymentStatus());
        Color badgeColor = isPaid ? ACCENT : new Color(239, 68, 68); // emerald or red

        PdfPTable badgeTable = new PdfPTable(1);
        badgeTable.setWidthPercentage(25);
        badgeTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
        badgeTable.setSpacingBefore(10f);

        PdfPCell cell = new PdfPCell(new Phrase(
                isPaid ? "✓ PAID" : statusText,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.WHITE)));
        cell.setBackgroundColor(badgeColor);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(8f);
        cell.setBorder(Rectangle.NO_BORDER);
        badgeTable.addCell(cell);
        document.add(badgeTable);
    }

    /** Adds a professional signature block. */
    private void addProfessionalSignature(Document document, Conference conference) throws DocumentException {
        Paragraph spacer = new Paragraph(" ");
        spacer.setSpacingBefore(40f);
        document.add(spacer);

        // Signature line
        PdfPTable sigTable = new PdfPTable(1);
        sigTable.setWidthPercentage(35);
        sigTable.setHorizontalAlignment(Element.ALIGN_RIGHT);

        PdfPCell lineCell = new PdfPCell();
        lineCell.setBorder(Rectangle.NO_BORDER);
        lineCell.setBorderWidthBottom(1f);
        lineCell.setBorderColorBottom(TEXT_DARK);
        lineCell.setFixedHeight(1f);
        sigTable.addCell(lineCell);
        document.add(sigTable);

        Paragraph sigName = new Paragraph("Organizing Committee", FONT_BODY_BOLD);
        sigName.setAlignment(Element.ALIGN_RIGHT);
        sigName.setSpacingBefore(8f);
        document.add(sigName);

        Paragraph sigConf = new Paragraph(conference.getName(), FONT_SUBTITLE);
        sigConf.setAlignment(Element.ALIGN_RIGHT);
        document.add(sigConf);
    }

    /** Adds a footer note at the bottom. */
    private void addFooterNote(Document document, String text) throws DocumentException {
        PdfPTable footer = new PdfPTable(1);
        footer.setWidthPercentage(100);
        footer.setSpacingBefore(40f);

        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA, 8, TEXT_MUTED)));
        cell.setBackgroundColor(BG_LIGHT);
        cell.setBorderColor(BORDER);
        cell.setBorderWidth(0.5f);
        cell.setPadding(8f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        footer.addCell(cell);
        document.add(footer);
    }

    /** Adds a decorative double-border for certificates. */
    private void addCertificateBorder(Document document) throws DocumentException {
        PdfPTable border = new PdfPTable(1);
        border.setWidthPercentage(100);

        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(GOLD);
        cell.setBorderWidth(3f);
        cell.setFixedHeight(0.1f);
        cell.setBackgroundColor(Color.WHITE);
        border.addCell(cell);
        document.add(border);
    }

    // ── Utility Methods ──

    private String formatCurrency(double amount) {
        return String.format("%,.0f VND", amount);
    }

    private String formatDateRange(LocalDateTime start, LocalDateTime end) {
        if (start == null) return null;
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
        if (end == null || start.toLocalDate().equals(end.toLocalDate())) {
            return start.format(fmt);
        }
        return start.format(fmt) + " — " + end.format(fmt);
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

    @FunctionalInterface
    private interface PdfContentGenerator {
        void generate(Document document) throws Exception;
    }
}
