package com.capstone.confhub.service.impl;

import com.capstone.confhub.dto.response.ImportResultDTO;
import com.capstone.confhub.dto.response.ImportResultDTO.ImportError;
import com.capstone.confhub.entity.*;
import com.capstone.confhub.exception.BadRequestException;
import com.capstone.confhub.repository.*;
import com.capstone.confhub.security.services.UserDetailsImpl;
import com.capstone.confhub.service.ConferenceActivityService;
import com.capstone.confhub.service.ConferenceImportService;
import com.capstone.confhub.service.EmailService;
import com.capstone.confhub.utils.enums.ConferenceStatus;
import com.capstone.confhub.utils.enums.ConferenceTrackRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
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
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserProfileRepository userProfileRepository;
    private final NotificationRepository notificationRepository;
    private final EmailService emailService;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    private static final int OTP_LENGTH = 6;

    private static final String[] CONFERENCE_HEADERS = {
            "name", "acronym", "description", "location", "startDate", "endDate",
            "websiteUrl", "country", "province", "area", "contactInformation", "chairEmails",
            "bannerImageUrl", "societySponsor"
    };
    private static final String[] TRACK_HEADERS = {"name", "description"};
    private static final String[] SA_HEADERS = {"trackName", "name", "description", "parentName"};

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
                    "IEEE, ACM"};
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
                    {"Machine Learning", "Papers about ML algorithms"},
                    {"NLP", "Papers about text processing"},
                    {"Computer Vision", "Papers about image analysis"}
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
    public ImportResultDTO importSubjectAreasFromExcel(Integer conferenceId, MultipartFile file) {
        ImportResultDTO preview = previewSubjectAreasFromExcel(file);
        if (!preview.isSuccess()) return preview;

        Conference conference = conferenceRepository.findById(conferenceId)
                .orElseThrow(() -> new BadRequestException("Conference not found: " + conferenceId));

        // Group rows by trackName
        Map<String, List<Map<String, String>>> byTrack = new LinkedHashMap<>();
        for (Map<String, String> data : preview.getSubjectAreaPreviews()) {
            String trackName = data.get("trackName");
            byTrack.computeIfAbsent(trackName, k -> new ArrayList<>()).add(data);
        }

        int totalCreated = 0;
        List<ImportError> errors = new ArrayList<>();

        for (Map.Entry<String, List<Map<String, String>>> entry : byTrack.entrySet()) {
            String trackName = entry.getKey();
            List<Map<String, String>> rows = entry.getValue();

            ConferenceTrack track = trackRepository.findByConferenceAndName(conference, trackName)
                    .orElse(null);
            if (track == null) {
                errors.add(ImportError.builder().sheet("SubjectAreas").row(0)
                        .column("trackName").message("Track '" + trackName + "' not found in this conference").build());
                continue;
            }

            // First pass: create all without parents
            Map<String, SubjectArea> created = new LinkedHashMap<>();
            for (Map<String, String> data : rows) {
                SubjectArea sa = new SubjectArea();
                sa.setName(data.get("name"));
                sa.setDescription(data.getOrDefault("description", ""));
                sa.setTrack(track);
                created.put(sa.getName(), subjectAreaRepository.save(sa));
            }

            // Second pass: link parents
            for (Map<String, String> data : rows) {
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
            totalCreated += created.size();
        }

        if (!errors.isEmpty()) {
            return ImportResultDTO.builder().success(false).errors(errors).build();
        }

        log.info("Imported {} subject areas for conference {}", totalCreated, conferenceId);
        return ImportResultDTO.builder()
                .success(true)
                .subjectAreasCreated(totalCreated)
                .build();
    }

    @Override
    public byte[] generateSubjectAreaTemplate() {
        try (Workbook wb = new XSSFWorkbook()) {
            CellStyle headerStyle = createHeaderStyle(wb);
            Sheet sheet = wb.createSheet("SubjectAreas");
            writeHeaders(sheet, SA_HEADERS, headerStyle);
            String[][] samples = {
                    {"AI Track", "Deep Learning", "Neural network architectures", ""},
                    {"AI Track", "Reinforcement Learning", "RL algorithms", ""},
                    {"AI Track", "Transfer Learning", "Domain adaptation", "Deep Learning"},
                    {"NLP Track", "Text Classification", "Document categorization", ""},
                    {"NLP Track", "Sentiment Analysis", "Opinion mining", "Text Classification"}
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

    // ===================== MEMBERS =====================

    private static final String[] MEMBER_HEADERS = {"email", "role", "trackName"};

    @Override
    public ImportResultDTO previewMembersFromExcel(MultipartFile file) {
        validateFile(file);
        List<ImportError> errors = new ArrayList<>();

        try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            if (sheet == null) return errorResult("No sheet found in file");

            List<Map<String, String>> rows = parseAllRows(sheet, MEMBER_HEADERS, "Members", errors);
            validateMemberData(rows, errors);

            // Add status column: EXISTING or NEW
            for (Map<String, String> row : rows) {
                String email = row.get("email");
                if (notBlank(email)) {
                    boolean exists = userRepository.findByEmail(email.trim()).isPresent();
                    row.put("status", exists ? "EXISTING" : "NEW");
                }
            }

            return ImportResultDTO.builder()
                    .success(errors.isEmpty())
                    .memberPreviews(rows)
                    .errors(errors)
                    .build();
        } catch (IOException e) {
            throw new BadRequestException("Failed to read Excel file: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public ImportResultDTO importMembersFromExcel(Integer conferenceId, MultipartFile file) {
        ImportResultDTO preview = previewMembersFromExcel(file);
        if (!preview.isSuccess()) return preview;

        Conference conference = conferenceRepository.findById(conferenceId)
                .orElseThrow(() -> new BadRequestException("Conference not found: " + conferenceId));

        List<ImportError> errors = new ArrayList<>();
        int count = 0;

        for (int i = 0; i < preview.getMemberPreviews().size(); i++) {
            Map<String, String> data = preview.getMemberPreviews().get(i);
            int rowNum = i + 2;
            String email = data.get("email").trim();
            String roleStr = data.get("role").trim().toUpperCase().replace(" ", "_");
            String trackName = data.get("trackName");

            // Find or create user by email
            User user = userRepository.findByEmail(email).orElse(null);
            boolean isNewUser = false;
            if (user == null) {
                // Auto-create placeholder account
                user = createPlaceholderUser(email);
                isNewUser = true;
                log.info("Created placeholder account for external user: {}", email);
            }

            // Parse role
            ConferenceTrackRole role;
            try {
                role = ConferenceTrackRole.valueOf(roleStr);
            } catch (IllegalArgumentException e) {
                errors.add(ImportError.builder().sheet("Members").row(rowNum).column("role")
                        .message("Invalid role: " + roleStr).build());
                continue;
            }

            // Find track if needed
            ConferenceTrack track = null;
            if (role == ConferenceTrackRole.PROGRAM_CHAIR || role == ConferenceTrackRole.REVIEWER) {
                if (!notBlank(trackName)) {
                    errors.add(ImportError.builder().sheet("Members").row(rowNum).column("trackName")
                            .message("Track is required for " + role).build());
                    continue;
                }
                track = trackRepository.findByConferenceAndName(conference, trackName.trim()).orElse(null);
                if (track == null) {
                    errors.add(ImportError.builder().sheet("Members").row(rowNum).column("trackName")
                            .message("Track not found: " + trackName).build());
                    continue;
                }
            }

            // Check duplicate – handle null track (CONFERENCE_CHAIR) explicitly
            final ConferenceTrack finalTrack = track;
            boolean exists;
            if (finalTrack == null) {
                exists = conferenceUserTrackRepository
                        .findAllByUser_IdAndConference_Id(user.getId(), conference.getId())
                        .stream()
                        .anyMatch(cut -> cut.getAssignedRole() == role && cut.getConferenceTrack() == null);
            } else {
                exists = conferenceUserTrackRepository
                        .findAllByUser_IdAndConference_Id(user.getId(), conference.getId())
                        .stream()
                        .anyMatch(cut -> cut.getAssignedRole() == role
                                && cut.getConferenceTrack() != null
                                && cut.getConferenceTrack().getId().equals(finalTrack.getId()));
            }
            if (exists) {
                errors.add(ImportError.builder().sheet("Members").row(rowNum).column("email")
                        .message("Duplicate: " + email + " already has role " + role).build());
                continue;
            }

            // Create assignment — ALL users start as pending (must accept/decline)
            ConferenceUserTrack cut = new ConferenceUserTrack();
            cut.setUser(user);
            cut.setConference(conference);
            cut.setAssignedRole(role);
            cut.setConferenceTrack(track);
            cut.setInvitedAt(LocalDateTime.now());
            cut.setIsAccepted(null);  // null = pending
            cut.setIsRegistered(false);
            cut.setInvitationToken(UUID.randomUUID().toString());
            cut.setTokenExpiresAt(LocalDateTime.now().plusDays(7));
            conferenceUserTrackRepository.save(cut);
            count++;

            // Set OTP for new users (needed for activation after accept)
            if (isNewUser) {
                String otp = generateOtp();
                user.setOtpCode(otp);
                user.setOtpExpiration(LocalDateTime.now().plusDays(7));
                userRepository.save(user);
            }

            // Send HTML invitation email to ALL users (existing + new)
            sendImportInvitationEmail(user, conference, cut, role, track);

            // Create notification for existing users
            if (!isNewUser) {
                String roleName = formatRoleName(role);
                boolean alreadyNotified = notificationRepository
                        .existsByUser_IdAndConference_IdAndType(user.getId(), conference.getId(), "INVITATION");
                if (!alreadyNotified) {
                    Notification notification = Notification.builder()
                            .user(user)
                            .conference(conference)
                            .title("You have been invited as " + roleName)
                            .message("You have been invited to join \"" + conference.getName() + "\" as " + roleName + ".")
                            .type("INVITATION")
                            .link("/my-profile/invitations")
                            .isRead(false)
                            .build();
                    notificationRepository.save(notification);
                }
            }
        }

        if (!errors.isEmpty()) {
            return ImportResultDTO.builder()
                    .success(false)
                    .errors(errors)
                    .membersCreated(count)
                    .build();
        }

        log.info("Imported {} members for conference {}", count, conferenceId);
        return ImportResultDTO.builder()
                .success(true)
                .conferenceId(conferenceId)
                .membersCreated(count)
                .build();
    }

    @Override
    public byte[] generateMemberTemplate() {
        try (Workbook wb = new XSSFWorkbook()) {
            CellStyle headerStyle = createHeaderStyle(wb);
            Sheet sheet = wb.createSheet("Members");
            writeHeaders(sheet, MEMBER_HEADERS, headerStyle);
            String[][] samples = {
                    {"reviewer1@example.com", "REVIEWER", "Machine Learning"},
                    {"chair1@example.com", "PROGRAM_CHAIR", "NLP"},
                    {"organizer@example.com", "CONFERENCE_CHAIR", ""},
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

    private void validateMemberData(List<Map<String, String>> rows, List<ImportError> errors) {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < rows.size(); i++) {
            int rowNum = i + 2;
            Map<String, String> d = rows.get(i);
            requireField(d, "email", "Members", rowNum, errors);
            requireField(d, "role", "Members", rowNum, errors);

            String email = d.get("email");
            String role = d.get("role");
            String trackName = d.get("trackName");
            if (notBlank(email) && notBlank(role)) {
                // Include trackName in the key so same user with same role on different tracks is valid
                String trackPart = notBlank(trackName) ? trackName.trim().toLowerCase() : "";
                String key = email.trim().toLowerCase() + "|" + role.trim().toUpperCase() + "|" + trackPart;
                if (!seen.add(key)) {
                    errors.add(ImportError.builder().sheet("Members").row(rowNum).column("email")
                            .message("Duplicate row: " + email + " with role " + role).build());
                }
            }

            // Validate role value
            if (notBlank(role)) {
                String normalized = role.trim().toUpperCase().replace(" ", "_");
                try {
                    ConferenceTrackRole.valueOf(normalized);
                } catch (IllegalArgumentException e) {
                    errors.add(ImportError.builder().sheet("Members").row(rowNum).column("role")
                            .message("Invalid role: " + role + ". Valid: CONFERENCE_CHAIR, PROGRAM_CHAIR, REVIEWER").build());
                }
            }
        }
    }
    // ===================== MEMBER CREATION HELPERS =====================

    /**
     * Creates a placeholder (inactive) user account for an external email.
     */
    private User createPlaceholderUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setFirstName(email.split("@")[0]);  // Use email prefix as temporary name
        user.setLastName("");
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));  // Random password
        user.setIsActive(false);
        userRepository.save(user);

        // Assign AUTHOR role
        Role authorRole = roleRepository.findByName("AUTHOR")
                .orElseThrow(() -> new BadRequestException("Role AUTHOR is not found"));
        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(authorRole);
        userRoleRepository.save(userRole);
        
        // Create empty UserProfile
        UserProfile profile = new UserProfile();
        profile.setUser(user);
        userProfileRepository.save(profile);

        return user;
    }

    /**
     * Sends an HTML invitation email with Accept/Decline buttons (same style as manual invite).
     */
    private void sendImportInvitationEmail(User user, Conference conference, ConferenceUserTrack cut,
                                            ConferenceTrackRole role, ConferenceTrack track) {
        try {
            String roleName = formatRoleName(role);
            String trackName = track != null ? track.getName() : null;
            String trackLabel = trackName != null ? " — " + trackName : "";
            String acceptLink = baseUrl + "/api/v1/email/accept/" + cut.getInvitationToken();
            String declineLink = baseUrl + "/api/v1/email/decline/" + cut.getInvitationToken();
            String fullName = (user.getFirstName() != null ? user.getFirstName() : "") + " "
                    + (user.getLastName() != null ? user.getLastName() : "");

            emailService.sendInvitationEmail(
                    user.getEmail(),
                    fullName.trim(),
                    "Invitation to " + conference.getName() + " as " + roleName + trackLabel,
                    conference.getName(),
                    roleName,
                    trackName,
                    acceptLink,
                    declineLink,
                    null,
                    null);
        } catch (Exception e) {
            log.error("Failed to send invitation email to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    private String formatRoleName(ConferenceTrackRole role) {
        return switch (role) {
            case CONFERENCE_CHAIR -> "Conference Chair";
            case PROGRAM_CHAIR -> "Program Chair";
            case REVIEWER -> "Reviewer";
            case AUTHOR -> "Author";
            case ATTENDEE -> "Attendee";
        };
    }

    private String generateOtp() {
        SecureRandom secureRandom = new SecureRandom();
        int bound = (int) Math.pow(10, OTP_LENGTH);
        int min = (int) Math.pow(10, OTP_LENGTH - 1);
        int otpValue = secureRandom.nextInt(bound - min) + min;
        return String.valueOf(otpValue);
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

            String name = d.get("name");
            if (notBlank(name) && !names.add(name)) {
                errors.add(ImportError.builder().sheet("Tracks").row(rowNum).column("name").message("Duplicate: " + name).build());
            }
        }
    }

    private void validateSubjectAreaData(List<Map<String, String>> rows, List<ImportError> errors) {
        Set<String> names = new HashSet<>();
        for (int i = 0; i < rows.size(); i++) {
            int rowNum = i + 2;
            Map<String, String> d = rows.get(i);
            requireField(d, "trackName", "SubjectAreas", rowNum, errors);
            requireField(d, "name", "SubjectAreas", rowNum, errors);

            String trackName = d.getOrDefault("trackName", "");
            String name = d.get("name");
            String uniqueKey = trackName + "::" + name;
            if (notBlank(name) && !names.add(uniqueKey)) {
                errors.add(ImportError.builder().sheet("SubjectAreas").row(rowNum).column("name").message("Duplicate: " + name + " in track " + trackName).build());
            }
            String parentName = d.get("parentName");
            if (notBlank(parentName)) {
                // Check if parent is defined in an earlier row within the same track
                boolean found = false;
                for (int j = 0; j < i; j++) {
                    if (parentName.equals(rows.get(j).get("name")) && trackName.equals(rows.get(j).getOrDefault("trackName", ""))) {
                        found = true; break;
                    }
                }
                if (!found) {
                    errors.add(ImportError.builder().sheet("SubjectAreas").row(rowNum).column("parentName").message("Parent '" + parentName + "' not found in earlier rows for track '" + trackName + "'").build());
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
