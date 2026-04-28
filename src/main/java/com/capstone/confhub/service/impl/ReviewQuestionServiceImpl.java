package com.capstone.confhub.service.impl;

import com.capstone.confhub.dto.ReviewQuestionChoiceDTO;
import com.capstone.confhub.dto.ReviewQuestionDTO;
import com.capstone.confhub.dto.response.ImportResultDTO;
import com.capstone.confhub.dto.response.ImportResultDTO.ImportError;
import com.capstone.confhub.entity.ConferenceTrack;
import com.capstone.confhub.entity.ReviewQuestion;
import com.capstone.confhub.entity.ReviewQuestionChoice;
import com.capstone.confhub.repository.ConferenceTrackRepository;
import com.capstone.confhub.repository.ReviewQuestionRepository;
import com.capstone.confhub.exception.BadRequestException;
import com.capstone.confhub.service.ReviewQuestionService;
import com.capstone.confhub.utils.enums.ReviewQuestionType;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewQuestionServiceImpl implements ReviewQuestionService {

    private static final String[] REVIEW_QUESTION_HEADERS = {
            "text",
            "note",
            "type",
            "orderIndex",
            "maxLength",
            "showAs",
            "isRequired",
            "lockedForEdit",
            "visibleToOtherReviewers",
            "visibleToAuthorsDuringFeedback",
            "visibleToAuthorsAfterNotification",
            "visibleToMetaReviewers",
            "visibleToSeniorMetaReviewers",
            "choices"
    };

    private final ReviewQuestionRepository questionRepository;
    private final ConferenceTrackRepository trackRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ReviewQuestionDTO> getQuestionsByTrackId(Integer trackId) {
        if (!trackRepository.existsById(trackId)) {
            throw new EntityNotFoundException("Track not found with ID: " + trackId);
        }
        List<ReviewQuestion> questions = questionRepository.findByTrackIdOrderByOrderIndexAsc(trackId);
        return questions.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ReviewQuestionDTO createQuestion(Integer trackId, ReviewQuestionDTO dto) {
        ConferenceTrack track = trackRepository.findById(trackId)
                .orElseThrow(() -> new EntityNotFoundException("Track not found with ID: " + trackId));

        Integer currentCount = questionRepository.countByTrackId(trackId);

        ReviewQuestion question = new ReviewQuestion();
        question.setTrack(track);
        mapDtoToEntity(dto, question);

        // Auto-set order index to end of list if not provided
        if (dto.getOrderIndex() == null) {
            question.setOrderIndex(currentCount + 1);
        }

        // Handle choices
        if (dto.getChoices() != null && !dto.getChoices().isEmpty()) {
            for (int i = 0; i < dto.getChoices().size(); i++) {
                ReviewQuestionChoiceDTO choiceDTO = dto.getChoices().get(i);
                ReviewQuestionChoice choice = new ReviewQuestionChoice();
                choice.setText(choiceDTO.getText());
                choice.setValue(choiceDTO.getValue());
                choice.setOrderIndex(choiceDTO.getOrderIndex() != null ? choiceDTO.getOrderIndex() : i + 1);
                choice.setQuestion(question);
                question.getChoices().add(choice);
            }
        }

        ReviewQuestion saved = questionRepository.save(question);
        return mapToDTO(saved);
    }

    @Override
    @Transactional
    public ReviewQuestionDTO updateQuestion(Integer questionId, ReviewQuestionDTO dto) {
        ReviewQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new EntityNotFoundException("Review question not found with ID: " + questionId));

        mapDtoToEntity(dto, question);

        // Handle choices: clear old and add new
        question.getChoices().clear();
        if (dto.getChoices() != null) {
            for (int i = 0; i < dto.getChoices().size(); i++) {
                ReviewQuestionChoiceDTO choiceDTO = dto.getChoices().get(i);
                ReviewQuestionChoice choice = new ReviewQuestionChoice();
                choice.setText(choiceDTO.getText());
                choice.setValue(choiceDTO.getValue());
                choice.setOrderIndex(choiceDTO.getOrderIndex() != null ? choiceDTO.getOrderIndex() : i + 1);
                choice.setQuestion(question);
                question.getChoices().add(choice);
            }
        }

        ReviewQuestion saved = questionRepository.save(question);
        return mapToDTO(saved);
    }

    @Override
    @Transactional
    public void deleteQuestion(Integer questionId) {
        if (!questionRepository.existsById(questionId)) {
            throw new EntityNotFoundException("Review question not found with ID: " + questionId);
        }
        questionRepository.deleteById(questionId);
    }

    @Override
    @Transactional
    public List<ReviewQuestionDTO> reorderQuestions(Integer trackId, List<Integer> questionIds) {
        if (!trackRepository.existsById(trackId)) {
            throw new EntityNotFoundException("Track not found with ID: " + trackId);
        }

        List<ReviewQuestion> questions = questionRepository.findByTrackIdOrderByOrderIndexAsc(trackId);

        for (int i = 0; i < questionIds.size(); i++) {
            Integer qId = questionIds.get(i);
            for (ReviewQuestion q : questions) {
                if (q.getId().equals(qId)) {
                    q.setOrderIndex(i + 1);
                    break;
                }
            }
        }

        questionRepository.saveAll(questions);
        return questions.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void copyQuestionsToTrack(Integer sourceTrackId, Integer targetTrackId) {
        if (sourceTrackId.equals(targetTrackId)) {
            throw new IllegalArgumentException("Source and target track IDs cannot be the same");
        }

        ConferenceTrack sourceTrack = trackRepository.findById(sourceTrackId)
                .orElseThrow(() -> new EntityNotFoundException("Source track not found with ID: " + sourceTrackId));
        ConferenceTrack targetTrack = trackRepository.findById(targetTrackId)
                .orElseThrow(() -> new EntityNotFoundException("Target track not found with ID: " + targetTrackId));

        if (!sourceTrack.getConference().getId().equals(targetTrack.getConference().getId())) {
            throw new IllegalArgumentException("Cannot copy questions between different conferences");
        }

        List<ReviewQuestion> sourceQuestions = questionRepository.findByTrackIdOrderByOrderIndexAsc(sourceTrackId);
        Integer existingCount = questionRepository.countByTrackId(targetTrackId);

        List<ReviewQuestion> newQuestions = new ArrayList<>();
        int offset = existingCount;

        for (ReviewQuestion src : sourceQuestions) {
            ReviewQuestion copy = new ReviewQuestion();
            copy.setTrack(targetTrack);
            copy.setText(src.getText());
            copy.setNote(src.getNote());
            copy.setType(src.getType());
            copy.setOrderIndex(++offset);
            copy.setMaxLength(src.getMaxLength());
            copy.setShowAs(src.getShowAs());
            copy.setIsRequired(src.getIsRequired());
            copy.setLockedForEdit(src.getLockedForEdit());
            copy.setVisibleToOtherReviewers(src.getVisibleToOtherReviewers());
            copy.setVisibleToAuthorsDuringFeedback(src.getVisibleToAuthorsDuringFeedback());
            copy.setVisibleToAuthorsAfterNotification(src.getVisibleToAuthorsAfterNotification());
            copy.setVisibleToMetaReviewers(src.getVisibleToMetaReviewers());
            copy.setVisibleToSeniorMetaReviewers(src.getVisibleToSeniorMetaReviewers());

            // Copy choices
            for (ReviewQuestionChoice srcChoice : src.getChoices()) {
                ReviewQuestionChoice copyChoice = new ReviewQuestionChoice();
                copyChoice.setText(srcChoice.getText());
                copyChoice.setValue(srcChoice.getValue());
                copyChoice.setOrderIndex(srcChoice.getOrderIndex());
                copyChoice.setQuestion(copy);
                copy.getChoices().add(copyChoice);
            }

            newQuestions.add(copy);
        }

        questionRepository.saveAll(newQuestions);
    }

    @Override
    @Transactional(readOnly = true)
    public ImportResultDTO previewReviewQuestionsFromExcel(Integer trackId, MultipartFile file) {
        validateFile(file);
        if (!trackRepository.existsById(trackId)) {
            throw new EntityNotFoundException("Track not found with ID: " + trackId);
        }

        List<ImportError> errors = new ArrayList<>();

        try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            if (sheet == null) return errorResult("No sheet found in file");

            List<Map<String, String>> rows = parseAllRows(sheet, REVIEW_QUESTION_HEADERS, "ReviewQuestions", errors);
            validateReviewQuestionData(rows, errors);

            return ImportResultDTO.builder()
                    .success(errors.isEmpty())
                    .reviewQuestionPreviews(rows)
                    .errors(errors)
                    .build();
        } catch (IOException e) {
            throw new BadRequestException("Failed to read Excel file: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public ImportResultDTO importReviewQuestionsFromExcel(Integer trackId, MultipartFile file) {
        ImportResultDTO preview = previewReviewQuestionsFromExcel(trackId, file);
        if (!preview.isSuccess()) return preview;

        ConferenceTrack track = trackRepository.findById(trackId)
                .orElseThrow(() -> new EntityNotFoundException("Track not found with ID: " + trackId));

        int count = 0;
        for (Map<String, String> data : preview.getReviewQuestionPreviews()) {
            createQuestionFromImport(track, data, count + 1);
            count++;
        }

        return ImportResultDTO.builder()
                .success(true)
                .reviewQuestionsCreated(count)
                .build();
    }

    @Override
    public byte[] generateReviewQuestionTemplate() {
        try (Workbook wb = new XSSFWorkbook()) {
            CellStyle headerStyle = createHeaderStyle(wb);
            Sheet sheet = wb.createSheet("ReviewQuestions");
            writeHeaders(sheet, REVIEW_QUESTION_HEADERS, headerStyle);

            String[][] samples = {
                    {
                            "Overall score for this paper",
                            "Use the scale below to rate the submission quality.",
                            "OPTIONS_WITH_VALUE",
                            "1",
                            "",
                            "RADIO",
                            "true",
                            "false",
                            "true",
                            "false",
                            "false",
                            "false",
                            "false",
                            "Poor=1 | Fair=2 | Good=3 | Very Good=4 | Excellent=5"
                    },
                    {
                            "Please provide additional comments",
                            "Comment-only question for reviewer feedback.",
                            "COMMENT",
                            "2",
                            "2000",
                            "",
                            "false",
                            "false",
                            "true",
                            "false",
                            "false",
                            "false",
                            "false",
                            ""
                    },
                    {
                            "Recommendation",
                            "Choose the most appropriate recommendation.",
                            "OPTIONS",
                            "3",
                            "",
                            "DROPDOWN",
                            "true",
                            "false",
                            "true",
                            "false",
                            "false",
                            "false",
                            "false",
                            "Accept | Minor Revision | Major Revision | Reject"
                    },
                    {
                            "I confirm this review is original and confidential",
                            "Reviewer must accept the declaration before submitting.",
                            "AGREEMENT",
                            "4",
                            "",
                            "",
                            "true",
                            "false",
                            "false",
                            "false",
                            "false",
                            "false",
                            "false",
                            ""
                    }
            };

            for (int r = 0; r < samples.length; r++) {
                Row row = sheet.createRow(r + 1);
                for (int c = 0; c < samples[r].length; c++) {
                    row.createCell(c).setCellValue(samples[r][c]);
                }
            }

            return toBytes(wb);
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate template", e);
        }
    }

    private void createQuestionFromImport(ConferenceTrack track, Map<String, String> data, int fallbackOrder) {
        ReviewQuestion question = new ReviewQuestion();
        question.setTrack(track);
        question.setText(data.get("text"));
        question.setNote(blankToNull(data.get("note")));
        question.setType(parseType(data.get("type")));

        Integer orderIndex = parseInteger(data.get("orderIndex"));
        question.setOrderIndex(orderIndex != null ? orderIndex : fallbackOrder);

        question.setMaxLength(parseInteger(data.get("maxLength")));
        question.setShowAs(normalizeShowAs(data.get("showAs"), question.getType()));
        question.setIsRequired(parseBoolean(data.get("isRequired"), true));
        question.setLockedForEdit(parseBoolean(data.get("lockedForEdit"), false));
        question.setVisibleToOtherReviewers(parseBoolean(data.get("visibleToOtherReviewers"), false));
        question.setVisibleToAuthorsDuringFeedback(parseBoolean(data.get("visibleToAuthorsDuringFeedback"), false));
        question.setVisibleToAuthorsAfterNotification(parseBoolean(data.get("visibleToAuthorsAfterNotification"), false));
        question.setVisibleToMetaReviewers(parseBoolean(data.get("visibleToMetaReviewers"), false));
        question.setVisibleToSeniorMetaReviewers(parseBoolean(data.get("visibleToSeniorMetaReviewers"), false));

        for (ReviewQuestionChoiceDTO choiceDTO : parseChoices(data.get("choices"), question.getType())) {
            ReviewQuestionChoice choice = new ReviewQuestionChoice();
            choice.setText(choiceDTO.getText());
            choice.setValue(choiceDTO.getValue());
            choice.setOrderIndex(choiceDTO.getOrderIndex());
            choice.setQuestion(question);
            question.getChoices().add(choice);
        }

        questionRepository.save(question);
    }

    private void validateReviewQuestionData(List<Map<String, String>> rows, List<ImportError> errors) {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < rows.size(); i++) {
            int rowNum = i + 2;
            Map<String, String> d = rows.get(i);

            requireField(d, "text", "ReviewQuestions", rowNum, errors);
            requireField(d, "type", "ReviewQuestions", rowNum, errors);

            ReviewQuestionType type = parseTypeSafe(d.get("type"), rowNum, errors);
            if (type == null) {
                continue;
            }

            String text = blankToNull(d.get("text"));
            if (text != null && !seen.add(text.toLowerCase(Locale.ROOT))) {
                errors.add(ImportError.builder().sheet("ReviewQuestions").row(rowNum).column("text")
                        .message("Duplicate question text: " + text).build());
            }

            if ((type == ReviewQuestionType.OPTIONS || type == ReviewQuestionType.OPTIONS_WITH_VALUE)
                    && !notBlank(d.get("choices"))) {
                errors.add(ImportError.builder().sheet("ReviewQuestions").row(rowNum).column("choices")
                        .message("Choices are required for option-based questions").build());
            }

            if (type == ReviewQuestionType.COMMENT) {
                Integer maxLength = parseInteger(d.get("maxLength"));
                if (notBlank(d.get("maxLength")) && maxLength == null) {
                    errors.add(ImportError.builder().sheet("ReviewQuestions").row(rowNum).column("maxLength")
                            .message("Invalid maxLength value").build());
                }
                if (maxLength != null && maxLength <= 0) {
                    errors.add(ImportError.builder().sheet("ReviewQuestions").row(rowNum).column("maxLength")
                            .message("maxLength must be greater than 0").build());
                }
            }

            if (notBlank(d.get("orderIndex")) && parseInteger(d.get("orderIndex")) == null) {
                errors.add(ImportError.builder().sheet("ReviewQuestions").row(rowNum).column("orderIndex")
                        .message("Invalid orderIndex value").build());
            }

            if (notBlank(d.get("showAs")) && normalizeShowAs(d.get("showAs"), type) == null) {
                errors.add(ImportError.builder().sheet("ReviewQuestions").row(rowNum).column("showAs")
                        .message("Invalid showAs value. Use RADIO, CHECKBOX, DROPDOWN, or LISTBOX").build());
            }
        }
    }

    private ReviewQuestionType parseTypeSafe(String value, int rowNum, List<ImportError> errors) {
        try {
            return parseType(value);
        } catch (IllegalArgumentException ex) {
            errors.add(ImportError.builder().sheet("ReviewQuestions").row(rowNum).column("type")
                    .message("Invalid type: " + value + ". Valid: COMMENT, AGREEMENT, OPTIONS, OPTIONS_WITH_VALUE, TEXT, OPTION")
                    .build());
            return null;
        }
    }

    private ReviewQuestionType parseType(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Type is required");
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        return switch (normalized) {
            case "TEXT", "TEXTAREA", "COMMENT" -> ReviewQuestionType.COMMENT;
            case "AGREEMENT" -> ReviewQuestionType.AGREEMENT;
            case "OPTION", "OPTIONS" -> ReviewQuestionType.OPTIONS;
            case "OPTIONS_WITH_VALUE" -> ReviewQuestionType.OPTIONS_WITH_VALUE;
            default -> throw new IllegalArgumentException("Invalid review question type: " + raw);
        };
    }

    private String normalizeShowAs(String raw, ReviewQuestionType type) {
        if (type != ReviewQuestionType.OPTIONS && type != ReviewQuestionType.OPTIONS_WITH_VALUE) {
            return null;
        }
        if (raw == null || raw.isBlank()) {
            return "RADIO";
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        return switch (normalized) {
            case "RADIO", "CHECKBOX", "DROPDOWN", "LISTBOX" -> normalized;
            default -> null;
        };
    }

    private List<ReviewQuestionChoiceDTO> parseChoices(String raw, ReviewQuestionType type) {
        List<ReviewQuestionChoiceDTO> choices = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return choices;
        }

        String[] parts = raw.split("[|;]");
        int index = 1;
        for (String part : parts) {
            String item = part.trim();
            if (item.isEmpty()) continue;

            ReviewQuestionChoiceDTO choice = new ReviewQuestionChoiceDTO();
            choice.setOrderIndex(index++);

            String text = item;
            Integer value = null;
            int separatorIndex = item.indexOf('=');
            if (separatorIndex < 0) {
                separatorIndex = item.indexOf(':');
            }
            if (separatorIndex > 0) {
                text = item.substring(0, separatorIndex).trim();
                value = parseInteger(item.substring(separatorIndex + 1).trim());
            }

            choice.setText(text);
            if (type == ReviewQuestionType.OPTIONS_WITH_VALUE) {
                choice.setValue(value);
            }
            choices.add(choice);
        }

        return choices;
    }

    private Boolean parseBoolean(String raw, boolean defaultValue) {
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        Boolean parsed = parseBooleanValue(raw);
        return parsed != null ? parsed : defaultValue;
    }

    private String blankToNull(String value) {
        return notBlank(value) ? value.trim() : null;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BadRequestException("File is empty");
        String name = file.getOriginalFilename();
        if (name == null || !name.endsWith(".xlsx")) throw new BadRequestException("Only .xlsx files are supported");
    }

    private ImportResultDTO errorResult(String message) {
        return ImportResultDTO.builder()
                .success(false)
                .errors(List.of(ImportError.builder().sheet("").row(0).column("").message(message).build()))
                .build();
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

    private Integer parseInteger(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Integer.valueOf(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Boolean parseBooleanValue(String s) {
        if (s == null || s.isBlank()) return null;
        String normalized = s.trim().toLowerCase(Locale.ROOT);
        if ("true".equals(normalized)) return true;
        if ("false".equals(normalized)) return false;
        return null;
    }

    private boolean notBlank(String s) { return s != null && !s.isBlank(); }

    private void requireField(Map<String, String> data, String field, String sheet, int row, List<ImportError> errors) {
        if (!notBlank(data.get(field))) {
            errors.add(ImportError.builder().sheet(sheet).row(row).column(field).message(field + " is required").build());
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
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
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

    // ========== Mapping Helpers ==========

    private void mapDtoToEntity(ReviewQuestionDTO dto, ReviewQuestion entity) {
        if (dto.getText() != null) entity.setText(dto.getText());
        if (dto.getNote() != null) entity.setNote(dto.getNote());
        if (dto.getType() != null) entity.setType(dto.getType());
        if (dto.getOrderIndex() != null) entity.setOrderIndex(dto.getOrderIndex());
        if (dto.getMaxLength() != null) entity.setMaxLength(dto.getMaxLength());
        if (dto.getShowAs() != null) entity.setShowAs(dto.getShowAs());
        if (dto.getIsRequired() != null) entity.setIsRequired(dto.getIsRequired());
        if (dto.getLockedForEdit() != null) entity.setLockedForEdit(dto.getLockedForEdit());
        if (dto.getVisibleToOtherReviewers() != null) entity.setVisibleToOtherReviewers(dto.getVisibleToOtherReviewers());
        if (dto.getVisibleToAuthorsDuringFeedback() != null) entity.setVisibleToAuthorsDuringFeedback(dto.getVisibleToAuthorsDuringFeedback());
        if (dto.getVisibleToAuthorsAfterNotification() != null) entity.setVisibleToAuthorsAfterNotification(dto.getVisibleToAuthorsAfterNotification());
        if (dto.getVisibleToMetaReviewers() != null) entity.setVisibleToMetaReviewers(dto.getVisibleToMetaReviewers());
        if (dto.getVisibleToSeniorMetaReviewers() != null) entity.setVisibleToSeniorMetaReviewers(dto.getVisibleToSeniorMetaReviewers());
    }

    private ReviewQuestionDTO mapToDTO(ReviewQuestion entity) {
        ReviewQuestionDTO dto = new ReviewQuestionDTO();
        dto.setId(entity.getId());
        dto.setTrackId(entity.getTrack().getId());
        dto.setText(entity.getText());
        dto.setNote(entity.getNote());
        dto.setType(entity.getType());
        dto.setOrderIndex(entity.getOrderIndex());
        dto.setMaxLength(entity.getMaxLength());
        dto.setShowAs(entity.getShowAs());
        dto.setIsRequired(entity.getIsRequired());
        dto.setLockedForEdit(entity.getLockedForEdit());
        dto.setVisibleToOtherReviewers(entity.getVisibleToOtherReviewers());
        dto.setVisibleToAuthorsDuringFeedback(entity.getVisibleToAuthorsDuringFeedback());
        dto.setVisibleToAuthorsAfterNotification(entity.getVisibleToAuthorsAfterNotification());
        dto.setVisibleToMetaReviewers(entity.getVisibleToMetaReviewers());
        dto.setVisibleToSeniorMetaReviewers(entity.getVisibleToSeniorMetaReviewers());

        if (entity.getChoices() != null && !entity.getChoices().isEmpty()) {
            List<ReviewQuestionChoiceDTO> choiceDTOs = entity.getChoices().stream().map(choice -> {
                ReviewQuestionChoiceDTO cDto = new ReviewQuestionChoiceDTO();
                cDto.setId(choice.getId());
                cDto.setText(choice.getText());
                cDto.setValue(choice.getValue());
                cDto.setOrderIndex(choice.getOrderIndex());
                return cDto;
            }).collect(Collectors.toList());
            dto.setChoices(choiceDTOs);
        }

        return dto;
    }
}
