package com.capstone.confms.service.impl;

import com.capstone.confms.dto.response.ImportResultDTO;
import com.capstone.confms.dto.response.ImportResultDTO.ImportError;
import com.capstone.confms.entity.*;
import com.capstone.confms.exception.BadRequestException;
import com.capstone.confms.repository.*;
import com.capstone.confms.security.services.UserDetailsImpl;
import com.capstone.confms.service.ConferenceActivityService;
import com.capstone.confms.service.ConferenceImportService;
import com.capstone.confms.utils.enums.ConferenceStatus;
import com.capstone.confms.utils.enums.ConferenceTrackRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConferenceImportServiceImpl implements ConferenceImportService {

    private final ConferenceRepository conferenceRepository;
    private final ConferenceTrackRepository trackRepository;
    private final TrackReviewSettingRepository trackReviewSettingRepository;
    private final SubjectAreaRepository subjectAreaRepository;
    private final ConferenceUserTrackRepository conferenceUserTrackRepository;
    private final UserRepository userRepository;
    private final ConferenceActivityService conferenceActivityService;

    private static final String[] CONFERENCE_HEADERS = {
            "name", "acronym", "description", "location", "startDate", "endDate",
            "websiteUrl", "country", "province", "area", "contactInformation", "chairEmails",
            "bannerImageUrl", "conferenceIdNumber", "societySponsor"
    };
    private static final String[] TRACK_HEADERS = {"name", "description", "maxSubmissions"};
    private static final String[] SA_HEADERS = {"name", "description", "parentName"};

    // ===================== CONFERENCE =====================

    @Override
    public ImportResultDTO previewConferenceFromExcel(MultipartFile file) {
        validateFile(file);
        List<ImportError> errors = new ArrayList<>();

        try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            if (sheet == null) {
                return errorResult("No sheet found in file");
            }
            Map<String, String> data = parseRowWithHeaders(sheet, 1, CONFERENCE_HEADERS, "Conference", errors);
            validateConferenceData(data, errors);

            return ImportResultDTO.builder()
                    .success(errors.isEmpty())
                    .conferencePreview(data)
                    .errors(errors)
                    .build();
        } catch (IOException e) {
            throw new BadRequestException("Failed to read Excel file: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public ImportResultDTO importConferenceFromExcel(MultipartFile file) {
        ImportResultDTO preview = previewConferenceFromExcel(file);
        if (!preview.isSuccess()) return preview;

        Map<String, String> data = preview.getConferencePreview();
        Conference conference = createConference(data);
        conferenceActivityService.initializeDefaultActivitiesForConference(conference.getId());

        log.info("Imported conference: {} (ID: {})", conference.getName(), conference.getId());
        return ImportResultDTO.builder()
                .success(true)
                .conferenceId(conference.getId())
                .conferenceName(conference.getName())
                .build();
    }

    @Override
    public byte[] generateConferenceTemplate() {
        try (Workbook wb = new XSSFWorkbook()) {
            CellStyle headerStyle = createHeaderStyle(wb);
            Sheet sheet = wb.createSheet("Conference");
            writeHeaders(sheet, CONFERENCE_HEADERS, headerStyle);
            // Sample
            Row sample = sheet.createRow(1);
            String[] vals = {"IEEE Conference on AI 2026", "ICAI2026", "Annual conference on AI",
                    "Ho Chi Minh City", "2026-06-01", "2026-06-03", "https://icai2026.org",
                    "Vietnam", "Ho Chi Minh", "Computer Science", "contact@icai2026.org",
                    "chair1@mail.com,chair2@mail.com", "https://icai2026.org/banner.png",
                    "IEEE-12345", "IEEE, ACM"};
            for (int i = 0; i < vals.length; i++) sample.createCell(i).setCellValue(vals[i]);
            return toBytes(wb);
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate template", e);
        }
    }

    // ===================== TRACKS =====================

    @Override
    public ImportResultDTO previewTracksFromExcel(MultipartFile file) {
        validateFile(file);
        List<ImportError> errors = new ArrayList<>();

        try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            if (sheet == null) return errorResult("No sheet found in file");

            List<Map<String, String>> rows = parseAllRows(sheet, TRACK_HEADERS, "Tracks", errors);
            validateTrackData(rows, errors);

            return ImportResultDTO.builder()
                    .success(errors.isEmpty())
                    .trackPreviews(rows)
                    .errors(errors)
                    .build();
        } catch (IOException e) {
            throw new BadRequestException("Failed to read Excel file: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public ImportResultDTO importTracksFromExcel(Integer conferenceId, MultipartFile file) {
        ImportResultDTO preview = previewTracksFromExcel(file);
        if (!preview.isSuccess()) return preview;

        Conference conference = conferenceRepository.findById(conferenceId)
                .orElseThrow(() -> new BadRequestException("Conference not found: " + conferenceId));

        int count = 0;
        for (Map<String, String> data : preview.getTrackPreviews()) {
            createTrack(data, conference);
            count++;
        }

        log.info("Imported {} tracks for conference {}", count, conferenceId);
        return ImportResultDTO.builder()
                .success(true)
                .conferenceId(conferenceId)
                .tracksCreated(count)
                .build();
    }

    @Override
    public byte[] generateTrackTemplate() {
        try (Workbook wb = new XSSFWorkbook()) {
            CellStyle headerStyle = createHeaderStyle(wb);
            Sheet sheet = wb.createSheet("Tracks");
            writeHeaders(sheet, TRACK_HEADERS, headerStyle);
            String[][] samples = {
                    {"Machine Learning", "Papers about ML algorithms", "100"},
                    {"NLP", "Papers about text processing", "80"},
                    {"Computer Vision", "Papers about image analysis", "60"}
            };
            for (int r = 0; r < samples.length; r++) {
                Row row = sheet.createRow(r + 1);
                for (int c = 0; c < samples[r].length; c++) row.createCell(c).setCellValue(samples[r][c]);
            }
            return toBytes(wb);
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate template", e);
        }
    }

    // ===================== SUBJECT AREAS =====================

    @Override
    public ImportResultDTO previewSubjectAreasFromExcel(MultipartFile file) {
        validateFile(file);
        List<ImportError> errors = new ArrayList<>();

        try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            if (sheet == null) return errorResult("No sheet found in file");

            List<Map<String, String>> rows = parseAllRows(sheet, SA_HEADERS, "SubjectAreas", errors);
            validateSubjectAreaData(rows, errors);

            return ImportResultDTO.builder()
                    .success(errors.isEmpty())
                    .subjectAreaPreviews(rows)
                    .errors(errors)
                    .build();
        } catch (IOException e) {
            throw new BadRequestException("Failed to read Excel file: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public ImportResultDTO importSubjectAreasFromExcel(Integer trackId, MultipartFile file) {
        ImportResultDTO preview = previewSubjectAreasFromExcel(file);
        if (!preview.isSuccess()) return preview;

        ConferenceTrack track = trackRepository.findById(trackId)
                .orElseThrow(() -> new BadRequestException("Track not found: " + trackId));

        // First pass: create all without parents
        Map<String, SubjectArea> created = new LinkedHashMap<>();
        for (Map<String, String> data : preview.getSubjectAreaPreviews()) {
            SubjectArea sa = new SubjectArea();
            sa.setName(data.get("name"));
            sa.setDescription(data.getOrDefault("description", ""));
            sa.setTrack(track);
            created.put(sa.getName(), subjectAreaRepository.save(sa));
        }

        // Second pass: link parents
        for (Map<String, String> data : preview.getSubjectAreaPreviews()) {
            String parentName = data.get("parentName");
            if (parentName != null && !parentName.isBlank()) {
                SubjectArea parent = created.get(parentName);
                if (parent != null) {
                    SubjectArea sa = created.get(data.get("name"));
                    sa.setParent(parent);
                    subjectAreaRepository.save(sa);
                }
            }
        }

        log.info("Imported {} subject areas for track {}", created.size(), trackId);
        return ImportResultDTO.builder()
                .success(true)
                .subjectAreasCreated(created.size())
                .build();
    }

    @Override
    public byte[] generateSubjectAreaTemplate() {
        try (Workbook wb = new XSSFWorkbook()) {
            CellStyle headerStyle = createHeaderStyle(wb);
            Sheet sheet = wb.createSheet("SubjectAreas");
            writeHeaders(sheet, SA_HEADERS, headerStyle);
            String[][] samples = {
                    {"Deep Learning", "Neural network architectures", ""},
                    {"Reinforcement Learning", "RL algorithms", ""},
                    {"Transfer Learning", "Domain adaptation", "Deep Learning"},
                    {"Text Classification", "Document categorization", ""},
                    {"Object Detection", "Detecting objects in images", ""}
            };
            for (int r = 0; r < samples.length; r++) {
                Row row = sheet.createRow(r + 1);
                for (int c = 0; c < samples[r].length; c++) row.createCell(c).setCellValue(samples[r][c]);
            }
            return toBytes(wb);
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate template", e);
        }
    }

    // ===================== HELPERS =====================

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) throw new BadRequestException("File is empty");
        String name = file.getOriginalFilename();
        if (name == null || !name.endsWith(".xlsx")) throw new BadRequestException("Only .xlsx files are supported");
    }

    private ImportResultDTO errorResult(String message) {
        return ImportResultDTO.builder()
                .success(false)
                .errors(List.of(ImportError.builder().sheet("").row(0).column("").message(message).build()))
                .build();
    }

    private Map<String, String> parseRowWithHeaders(Sheet sheet, int rowIdx, String[] headers, String sheetName, List<ImportError> errors) {
        Map<String, String> data = new LinkedHashMap<>();
        Row row = sheet.getRow(rowIdx);
        if (row == null) {
            errors.add(ImportError.builder().sheet(sheetName).row(rowIdx + 1).column("").message("No data row found").build());
            return data;
        }
        for (int i = 0; i < headers.length; i++) {
            data.put(headers[i], getCellValueAsString(row.getCell(i)));
        }
        return data;
    }

    private List<Map<String, String>> parseAllRows(Sheet sheet, String[] headers, String sheetName, List<ImportError> errors) {
        List<Map<String, String>> result = new ArrayList<>();
        for (int r = 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null || isRowEmpty(row)) continue;
            Map<String, String> data = new LinkedHashMap<>();
            for (int i = 0; i < headers.length; i++) {
                data.put(headers[i], getCellValueAsString(row.getCell(i)));
            }
            result.add(data);
        }
        if (result.isEmpty()) {
            errors.add(ImportError.builder().sheet(sheetName).row(2).column("").message("No data rows found").build());
        }
        return result;
    }

    private void validateConferenceData(Map<String, String> data, List<ImportError> errors) {
        requireField(data, "name", "Conference", 2, errors);
        requireField(data, "acronym", "Conference", 2, errors);
        requireField(data, "location", "Conference", 2, errors);
        requireField(data, "startDate", "Conference", 2, errors);
        requireField(data, "endDate", "Conference", 2, errors);
        requireField(data, "websiteUrl", "Conference", 2, errors);

        if (notBlank(data.get("startDate")) && parseDate(data.get("startDate")) == null) {
            errors.add(ImportError.builder().sheet("Conference").row(2).column("startDate").message("Invalid date. Use yyyy-MM-dd").build());
        }
        if (notBlank(data.get("endDate")) && parseDate(data.get("endDate")) == null) {
            errors.add(ImportError.builder().sheet("Conference").row(2).column("endDate").message("Invalid date. Use yyyy-MM-dd").build());
        }
    }

    private void validateTrackData(List<Map<String, String>> rows, List<ImportError> errors) {
        Set<String> names = new HashSet<>();
        for (int i = 0; i < rows.size(); i++) {
            int rowNum = i + 2;
            Map<String, String> d = rows.get(i);
            requireField(d, "name", "Tracks", rowNum, errors);
            requireField(d, "description", "Tracks", rowNum, errors);
            requireField(d, "maxSubmissions", "Tracks", rowNum, errors);

            String name = d.get("name");
            if (notBlank(name) && !names.add(name)) {
                errors.add(ImportError.builder().sheet("Tracks").row(rowNum).column("name").message("Duplicate: " + name).build());
            }
            String maxSub = d.get("maxSubmissions");
            if (notBlank(maxSub)) {
                try { Integer.parseInt(maxSub); } catch (NumberFormatException e) {
                    errors.add(ImportError.builder().sheet("Tracks").row(rowNum).column("maxSubmissions").message("Must be a number").build());
                }
            }
        }
    }

    private void validateSubjectAreaData(List<Map<String, String>> rows, List<ImportError> errors) {
        Set<String> names = new HashSet<>();
        for (int i = 0; i < rows.size(); i++) {
            int rowNum = i + 2;
            Map<String, String> d = rows.get(i);
            requireField(d, "name", "SubjectAreas", rowNum, errors);

            String name = d.get("name");
            if (notBlank(name) && !names.add(name)) {
                errors.add(ImportError.builder().sheet("SubjectAreas").row(rowNum).column("name").message("Duplicate: " + name).build());
            }
            String parentName = d.get("parentName");
            if (notBlank(parentName) && !names.contains(parentName)) {
                // Check if parent is defined in an earlier row
                boolean found = false;
                for (int j = 0; j < i; j++) {
                    if (parentName.equals(rows.get(j).get("name"))) { found = true; break; }
                }
                if (!found) {
                    errors.add(ImportError.builder().sheet("SubjectAreas").row(rowNum).column("parentName").message("Parent '" + parentName + "' not found in earlier rows").build());
                }
            }
        }
    }

    private Conference createConference(Map<String, String> data) {
        User currentUser = getCurrentAuthenticatedUser();
        Conference conf = new Conference();
        conf.setName(data.get("name"));
        conf.setAcronym(data.get("acronym"));
        conf.setDescription(data.getOrDefault("description", ""));
        conf.setLocation(data.get("location"));
        conf.setStartDate(parseDate(data.get("startDate")));
        conf.setEndDate(parseDate(data.get("endDate")));
        conf.setWebsiteUrl(data.get("websiteUrl"));
        conf.setCountry(data.get("country"));
        conf.setProvince(data.get("province"));
        conf.setArea(data.get("area"));
        conf.setContactInformation(data.get("contactInformation"));
        conf.setChairEmails(data.get("chairEmails"));
        conf.setBannerImageUrl(data.getOrDefault("bannerImageUrl", ""));
        conf.setConferenceIdNumber(data.getOrDefault("conferenceIdNumber", ""));
        conf.setSocietySponsor(data.getOrDefault("societySponsor", ""));
        conf.setStatus(ConferenceStatus.PENDING);
        Conference saved = conferenceRepository.save(conf);

        ConferenceUserTrack chair = new ConferenceUserTrack();
        chair.setUser(currentUser);
        chair.setConference(saved);
        chair.setAssignedRole(ConferenceTrackRole.CONFERENCE_CHAIR);
        chair.setInvitedAt(LocalDateTime.now());
        chair.setIsAccepted(true);
        chair.setIsRegistered(true);
        conferenceUserTrackRepository.save(chair);
        return saved;
    }

    private void createTrack(Map<String, String> data, Conference conference) {
        ConferenceTrack track = new ConferenceTrack();
        track.setName(data.get("name"));
        track.setDescription(data.get("description"));
        track.setConference(conference);
        track.setMaxSubmissions(Integer.parseInt(data.get("maxSubmissions")));
        ConferenceTrack saved = trackRepository.save(track);

        TrackReviewSetting setting = new TrackReviewSetting();
        setting.setTrack(saved);
        setting.setIsDoubleBlind(true); // default
        trackReviewSettingRepository.save(setting);
    }

    // ── Utility ──

    private void requireField(Map<String, String> data, String field, String sheet, int row, List<ImportError> errors) {
        if (!notBlank(data.get(field))) {
            errors.add(ImportError.builder().sheet(sheet).row(row).column(field).message(field + " is required").build());
        }
    }

    private boolean notBlank(String s) { return s != null && !s.isBlank(); }

    private LocalDateTime parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try { return LocalDate.parse(s.trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd")).atStartOfDay(); }
        catch (DateTimeParseException e) {
            try { return LocalDateTime.parse(s.trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")); }
            catch (DateTimeParseException e2) { return null; }
        }
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) yield cell.getLocalDateTimeCellValue().toLocalDate().toString();
                double v = cell.getNumericCellValue();
                yield v == Math.floor(v) ? String.valueOf((int) v) : String.valueOf(v);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case BLANK -> "";
            default -> cell.toString().trim();
        };
    }

    private boolean isRowEmpty(Row row) {
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK && !getCellValueAsString(cell).isEmpty()) return false;
        }
        return true;
    }

    private CellStyle createHeaderStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont(); f.setBold(true); s.setFont(f);
        s.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return s;
    }

    private void writeHeaders(Sheet sheet, String[] headers, CellStyle style) {
        Row row = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(style);
            sheet.setColumnWidth(i, 5000);
        }
    }

    private byte[] toBytes(Workbook wb) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        return out.toByteArray();
    }

    private User getCurrentAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserDetailsImpl))
            throw new BadRequestException("No authenticated user found");
        UserDetailsImpl ud = (UserDetailsImpl) auth.getPrincipal();
        return userRepository.findById(ud.getId()).orElseThrow(() -> new BadRequestException("User not found"));
    }
}
