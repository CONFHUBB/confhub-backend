package com.capstone.confhub.service.impl;

import com.capstone.confhub.entity.Paper;
import com.capstone.confhub.entity.PaperFile;
import com.capstone.confhub.exception.ResourceNotFoundException;
import com.capstone.confhub.integration.GeminiApiClient;
import com.capstone.confhub.repository.PaperFileRepository;
import com.capstone.confhub.repository.PaperRepository;
import com.capstone.confhub.utils.enums.PlagiarismStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlagiarismServiceTest {

    @Mock
    private PaperRepository paperRepository;
    @Mock
    private PaperFileRepository paperFileRepository;
    @Mock
    private GeminiApiClient geminiApiClient;

    private ObjectMapper objectMapper;

    @InjectMocks
    private PlagiarismService plagiarismService;

    private Paper paper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        plagiarismService = new PlagiarismService(paperRepository, paperFileRepository, geminiApiClient, objectMapper);

        paper = new Paper();
        paper.setId(10);
        paper.setTitle("A Study of Testing");
        paper.setAbstractField("Testing and quality assurance in software systems.");
    }

    @Test
    void getPlagiarismResultShouldReturnMappedFields() {
        paper.setPlagiarismScore(36.7);
        paper.setPlagiarismStatus(PlagiarismStatus.COMPLETED);
        paper.setPlagiarismDetailsJson("{\"k\":\"v\"}");
        when(paperRepository.findById(10)).thenReturn(Optional.of(paper));

        Map<String, Object> result = plagiarismService.getPlagiarismResult(10);

        assertEquals(10, result.get("paperId"));
        assertEquals(36.7, result.get("score"));
        assertEquals(PlagiarismStatus.COMPLETED, result.get("status"));
        assertEquals("{\"k\":\"v\"}", result.get("detailsJson"));
    }

    @Test
    void getPlagiarismResultShouldThrowWhenPaperMissing() {
        when(paperRepository.findById(10)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> plagiarismService.getPlagiarismResult(10));
    }

    @Test
    void resetPlagiarismShouldClearFieldsAndSave() {
        paper.setPlagiarismScore(10.0);
        paper.setPlagiarismStatus(PlagiarismStatus.FAILED);
        paper.setPlagiarismDetailsJson("error");
        when(paperRepository.findById(10)).thenReturn(Optional.of(paper));

        plagiarismService.resetPlagiarism(10);

        assertNull(paper.getPlagiarismScore());
        assertNull(paper.getPlagiarismStatus());
        assertNull(paper.getPlagiarismDetailsJson());
        verify(paperRepository).save(paper);
    }

    @Test
    void resetPlagiarismShouldThrowWhenPaperMissing() {
        when(paperRepository.findById(10)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> plagiarismService.resetPlagiarism(10));
    }

    @Test
    void recheckPlagiarismAsyncShouldSetCheckingAndInvokeAsync() {
        PlagiarismService spyService = spy(plagiarismService);
        when(paperRepository.findById(10)).thenReturn(Optional.of(paper));
        doNothing().when(spyService).checkPlagiarismAsync(10);

        spyService.recheckPlagiarismAsync(10);

        assertEquals(PlagiarismStatus.CHECKING, paper.getPlagiarismStatus());
        verify(paperRepository).save(paper);
        verify(spyService).checkPlagiarismAsync(10);
    }

    @Test
    void recheckPlagiarismAsyncShouldThrowWhenPaperMissing() {
        when(paperRepository.findById(10)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> plagiarismService.recheckPlagiarismAsync(10));
    }

    @Test
    void checkPlagiarismAsyncShouldMarkFailedOnException() {
        PlagiarismService spyService = spy(plagiarismService);
        doReturn(Optional.empty()).when(paperRepository).findById(10);

        spyService.checkPlagiarismAsync(10);

        verify(paperRepository, never()).save(any(Paper.class));
    }

    @Test
    void extractJsonFromReplyShouldHandleRawJson() throws Exception {
        String raw = "{\"matches\":[],\"overallScore\":12,\"summary\":\"ok\"}";

        String json = invokeExtractJsonFromReply(raw);

        assertEquals(raw, json);
    }

    @Test
    void extractJsonFromReplyShouldHandleMarkdownFence() throws Exception {
        String raw = "```json\n{\"matches\":[],\"overallScore\":20,\"summary\":\"ok\"}\n```";

        String json = invokeExtractJsonFromReply(raw);

        assertTrue(json.startsWith("{"));
        assertTrue(json.contains("overallScore"));
    }

    @Test
    void extractJsonFromReplyShouldReturnBestEffortWhenNoBraces() throws Exception {
        String raw = "not json";

        String json = invokeExtractJsonFromReply(raw);

        assertEquals("not json", json);
    }

    @Test
    void sanitizeJsonNewlinesShouldEscapeNewlinesInsideStrings() throws Exception {
        String raw = "{\"summary\":\"line1\nline2\"}";

        String sanitized = invokeSanitizeJsonNewlines(raw);

        assertTrue(sanitized.contains("line1\\nline2"));
    }

    @Test
    void sanitizeJsonNewlinesShouldPreserveEscapedQuotes() throws Exception {
        String raw = "{\"summary\":\"value with \\\"quoted\\\" text\"}";

        String sanitized = invokeSanitizeJsonNewlines(raw);

        assertTrue(sanitized.contains("\\\"quoted\\\""));
    }

    @Test
    void sanitizeJsonNewlinesShouldHandleCarriageReturn() throws Exception {
        String raw = "{\"summary\":\"a\rb\"}";

        String sanitized = invokeSanitizeJsonNewlines(raw);

        assertTrue(sanitized.contains("a\\nb"));
    }

    @Test
    void splitIntoChunksShouldReturnSingleChunkForShortText() throws Exception {
        List<String> chunks = invokeSplitIntoChunks("short text", 3, 1500);

        assertEquals(1, chunks.size());
        assertEquals("short text", chunks.get(0));
    }

    @Test
    void splitIntoChunksShouldSplitLargeTextIntoThreeChunks() throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6000; i++) {
            sb.append('a');
        }
        List<String> chunks = invokeSplitIntoChunks(sb.toString(), 3, 1500);

        assertTrue(chunks.size() >= 2);
        assertTrue(chunks.size() <= 3);
        assertTrue(chunks.stream().allMatch(c -> !c.isBlank()));
    }

    @Test
    void splitIntoChunksShouldPreferSentenceBoundary() throws Exception {
        String text = "Sentence one. Sentence two. Sentence three. Sentence four. Sentence five.";
        List<String> chunks = invokeSplitIntoChunks(text.repeat(120), 3, 500);

        assertTrue(chunks.size() >= 2);
        assertTrue(chunks.get(0).endsWith(".") || chunks.get(0).length() <= 500);
    }

    @Test
    void extractNgramsShouldReturnEmptyWhenInputTooShort() throws Exception {
        List<String> grams = invokeExtractNgrams("one two", 5);

        assertEquals(0, grams.size());
    }

    @Test
    void extractNgramsShouldFilterShortNgrams() throws Exception {
        List<String> grams = invokeExtractNgrams("a b c d e f g h i j", 5);

        assertEquals(0, grams.size());
    }

    @Test
    void extractNgramsShouldReturnNonEmptyForNormalInput() throws Exception {
        String text = "machine learning techniques improve text classification accuracy and performance significantly";

        List<String> grams = invokeExtractNgrams(text, 5);

        assertTrue(grams.size() > 0);
        assertTrue(grams.stream().allMatch(g -> g.length() >= 15));
    }

    @Test
    void extractNgramsShouldSampleWhenTooManyNgrams() throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 500; i++) {
            sb.append("token").append(i).append(" ");
        }

        List<String> grams = invokeExtractNgrams(sb.toString(), 5);

        assertTrue(grams.size() <= 20);
    }

    @Test
    void findMatchingSnippetShouldReturnNullWhenNoMatch() throws Exception {
        String snippet = invokeFindMatchingSnippet(List.of("alpha beta gamma delta epsilon"), "unrelated words here");

        assertNull(snippet);
    }

    @Test
    void findMatchingSnippetShouldReturnLongestMatchWhenMultiple() throws Exception {
        List<String> grams = List.of(
                "small overlap phrase",
                "very long overlap phrase with many words"
        );
        String otherText = "this contains very long overlap phrase with many words inside";

        String snippet = invokeFindMatchingSnippet(grams, otherText);

        assertNotNull(snippet);
        assertTrue(snippet.contains("very long overlap phrase with many words"));
    }

    @Test
    void buildTfIdfVectorShouldExcludeStopWordsAndNumbers() throws Exception {
        Map<String, Double> vector = invokeBuildTfIdfVector("the the model 2024 alpha beta beta");

        assertFalse(vector.containsKey("the"));
        assertFalse(vector.containsKey("2024"));
        assertTrue(vector.containsKey("alpha"));
        assertTrue(vector.containsKey("beta"));
    }

    @Test
    void buildTfIdfVectorShouldReturnRelativeFrequencies() throws Exception {
        Map<String, Double> vector = invokeBuildTfIdfVector("delta delta gamma");

        assertTrue(vector.get("delta") > vector.get("gamma"));
    }

    @Test
    void cosineSimilarityShouldReturnZeroWhenOneVectorEmpty() throws Exception {
        double sim = invokeCosineSimilarity(Map.of("a", 1.0), Map.of());
        assertEquals(0.0, sim);
    }

    @Test
    void cosineSimilarityShouldReturnOneForIdenticalVectors() throws Exception {
        double sim = invokeCosineSimilarity(Map.of("a", 0.5, "b", 0.5), Map.of("a", 0.5, "b", 0.5));
        assertEquals(1.0, sim, 1e-12);
    }

    @Test
    void cosineSimilarityShouldReturnExpectedFraction() throws Exception {
        double sim = invokeCosineSimilarity(Map.of("a", 1.0, "b", 0.0), Map.of("a", 0.5, "b", 0.5));
        assertTrue(sim > 0.70 && sim < 0.71);
    }

    @Test
    void parseWebSearchResponseShouldParseValidJson() throws Exception {
        String ai = "{\"matches\":[{\"url\":\"https://a.com\",\"title\":\"A\",\"snippet\":\"S\",\"similarity\":78}],\"overallScore\":65,\"summary\":\"done\"}";

        Object result = invokeParseWebSearchResponse(ai);

        double score = (double) result.getClass().getDeclaredMethod("score").invoke(result);
        Object matches = result.getClass().getDeclaredMethod("matches").invoke(result);
        String summary = (String) result.getClass().getDeclaredMethod("summary").invoke(result);

        assertTrue(score >= 78.0);
        assertEquals(1, ((List<?>) matches).size());
        assertEquals("done", summary);
    }

    @Test
    void parseWebSearchResponseShouldIgnoreLowSimilarityMatches() throws Exception {
        String ai = "{\"matches\":[{\"url\":\"https://a.com\",\"title\":\"A\",\"snippet\":\"S\",\"similarity\":8}],\"overallScore\":5,\"summary\":\"done\"}";

        Object result = invokeParseWebSearchResponse(ai);
        Object matches = result.getClass().getDeclaredMethod("matches").invoke(result);
        double score = (double) result.getClass().getDeclaredMethod("score").invoke(result);

        assertEquals(0, ((List<?>) matches).size());
        assertEquals(5.0, score);
    }

    @Test
    void parseWebSearchResponseShouldHandleMarkdownWrappedJson() throws Exception {
        String ai = "```json\n{\"matches\":[],\"overallScore\":0,\"summary\":\"none\"}\n```";

        Object result = invokeParseWebSearchResponse(ai);
        String summary = (String) result.getClass().getDeclaredMethod("summary").invoke(result);

        assertEquals("none", summary);
    }

    @Test
    void parseWebSearchResponseShouldHandleUnescapedNewlineStrings() throws Exception {
        String ai = "{\"matches\":[],\"overallScore\":0,\"summary\":\"line1\nline2\"}";

        Object result = invokeParseWebSearchResponse(ai);
        String summary = (String) result.getClass().getDeclaredMethod("summary").invoke(result);

        assertTrue(summary.contains("line1"));
    }

    @Test
    void parseWebSearchResponseShouldThrowOnNonJsonResponse() {
        assertThrows(RuntimeException.class, () -> invokeParseWebSearchResponse("Sorry, could not process"));
    }

    @Test
    void markFailedShouldSetFailedStatusAndErrorPayload() throws Exception {
        when(paperRepository.findById(10)).thenReturn(Optional.of(paper));

        invokeMarkFailed(10, "boom");

        assertEquals(PlagiarismStatus.FAILED, paper.getPlagiarismStatus());
        assertTrue(paper.getPlagiarismDetailsJson().contains("boom"));
        verify(paperRepository).save(paper);
    }

    @Test
    void markFailedShouldBeSafeWhenPaperMissing() throws Exception {
        when(paperRepository.findById(10)).thenReturn(Optional.empty());

        invokeMarkFailed(10, "boom");

        verify(paperRepository, never()).save(any(Paper.class));
    }

    @Test
    void extractPdfTextForPaperShouldReturnNullWhenNoFiles() throws Exception {
        when(paperFileRepository.findByPaper_Id(10)).thenReturn(List.of());

        String text = invokeExtractPdfTextForPaper(paper);

        assertNull(text);
    }

    @Test
    void extractPdfTextForPaperShouldReturnNullWhenNoActiveManuscript() throws Exception {
        PaperFile file = new PaperFile();
        file.setIsActive(false);
        file.setIsSupplementary(false);
        file.setIsCameraReady(false);
        when(paperFileRepository.findByPaper_Id(10)).thenReturn(List.of(file));

        String text = invokeExtractPdfTextForPaper(paper);

        assertNull(text);
    }

    @Test
    void extractPdfTextForPaperShouldReturnNullWhenUrlMissing() throws Exception {
        PaperFile file = new PaperFile();
        file.setIsActive(true);
        file.setIsSupplementary(false);
        file.setIsCameraReady(false);
        file.setUrl("");
        when(paperFileRepository.findByPaper_Id(10)).thenReturn(List.of(file));

        String text = invokeExtractPdfTextForPaper(paper);

        assertNull(text);
    }

    @Test
    void buildPaperTextShouldConcatenateTitleAndAbstract() throws Exception {
        String text = invokeBuildPaperText(paper);

        assertTrue(text.contains("A Study of Testing"));
        assertTrue(text.contains("Testing and quality assurance"));
    }

    @Test
    void buildPaperTextShouldHandleNullFields() throws Exception {
        Paper p = new Paper();
        p.setId(22);
        p.setTitle(null);
        p.setAbstractField(null);

        String text = invokeBuildPaperText(p);

        assertEquals("", text);
    }

    @Test
    void roundShouldRoundToOneDecimalPlace() throws Exception {
        assertEquals(12.3, invokeRound(12.34));
        assertEquals(12.4, invokeRound(12.35));
        assertEquals(0.0, invokeRound(0.04));
    }

    @Test
    void parseWebSearchResponseShouldKeepTopFiveMatchesOnly() throws Exception {
        String ai = "{\"matches\":[" +
                "{\"url\":\"https://1.com\",\"title\":\"1\",\"snippet\":\"1\",\"similarity\":91}," +
                "{\"url\":\"https://2.com\",\"title\":\"2\",\"snippet\":\"2\",\"similarity\":90}," +
                "{\"url\":\"https://3.com\",\"title\":\"3\",\"snippet\":\"3\",\"similarity\":89}," +
                "{\"url\":\"https://4.com\",\"title\":\"4\",\"snippet\":\"4\",\"similarity\":88}," +
                "{\"url\":\"https://5.com\",\"title\":\"5\",\"snippet\":\"5\",\"similarity\":87}," +
                "{\"url\":\"https://6.com\",\"title\":\"6\",\"snippet\":\"6\",\"similarity\":86}]," +
                "\"overallScore\":50,\"summary\":\"ok\"}";

        Object result = invokeParseWebSearchResponse(ai);
        Object matches = result.getClass().getDeclaredMethod("matches").invoke(result);

        assertEquals(5, ((List<?>) matches).size());
    }

    @Test
    void parseWebSearchResponseShouldTakeMaxBetweenOverallAndHighestMatch() throws Exception {
        String ai = "{\"matches\":[{\"url\":\"https://a.com\",\"title\":\"A\",\"snippet\":\"S\",\"similarity\":30}],\"overallScore\":75,\"summary\":\"ok\"}";

        Object result = invokeParseWebSearchResponse(ai);
        double score = (double) result.getClass().getDeclaredMethod("score").invoke(result);

        assertEquals(75.0, score);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Double> invokeBuildTfIdfVector(String text) throws Exception {
        Method method = PlagiarismService.class.getDeclaredMethod("buildTfIdfVector", String.class);
        method.setAccessible(true);
        return (Map<String, Double>) method.invoke(plagiarismService, text);
    }

    private double invokeCosineSimilarity(Map<String, Double> v1, Map<String, Double> v2) throws Exception {
        Method method = PlagiarismService.class.getDeclaredMethod("cosineSimilarity", Map.class, Map.class);
        method.setAccessible(true);
        return (double) method.invoke(plagiarismService, v1, v2);
    }

    @SuppressWarnings("unchecked")
    private List<String> invokeExtractNgrams(String text, int n) throws Exception {
        Method method = PlagiarismService.class.getDeclaredMethod("extractNgrams", String.class, int.class);
        method.setAccessible(true);
        return (List<String>) method.invoke(plagiarismService, text, n);
    }

    private String invokeFindMatchingSnippet(List<String> grams, String text) throws Exception {
        Method method = PlagiarismService.class.getDeclaredMethod("findMatchingSnippet", List.class, String.class);
        method.setAccessible(true);
        return (String) method.invoke(plagiarismService, grams, text);
    }

    @SuppressWarnings("unchecked")
    private List<String> invokeSplitIntoChunks(String text, int maxChunks, int maxChars) throws Exception {
        Method method = PlagiarismService.class.getDeclaredMethod("splitIntoChunks", String.class, int.class, int.class);
        method.setAccessible(true);
        return (List<String>) method.invoke(plagiarismService, text, maxChunks, maxChars);
    }

    private String invokeSanitizeJsonNewlines(String json) throws Exception {
        Method method = PlagiarismService.class.getDeclaredMethod("sanitizeJsonNewlines", String.class);
        method.setAccessible(true);
        return (String) method.invoke(plagiarismService, json);
    }

    private String invokeExtractJsonFromReply(String text) throws Exception {
        Method method = PlagiarismService.class.getDeclaredMethod("extractJsonFromReply", String.class);
        method.setAccessible(true);
        return (String) method.invoke(plagiarismService, text);
    }

    private Object invokeParseWebSearchResponse(String ai) throws Exception {
        Method method = PlagiarismService.class.getDeclaredMethod("parseWebSearchResponse", String.class);
        method.setAccessible(true);
        try {
            return method.invoke(plagiarismService, ai);
        } catch (InvocationTargetException ex) {
            if (ex.getCause() instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException(ex.getCause());
        }
    }

    private void invokeMarkFailed(Integer paperId, String reason) throws Exception {
        Method method = PlagiarismService.class.getDeclaredMethod("markFailed", Integer.class, String.class);
        method.setAccessible(true);
        method.invoke(plagiarismService, paperId, reason);
    }

    private String invokeExtractPdfTextForPaper(Paper p) throws Exception {
        Method method = PlagiarismService.class.getDeclaredMethod("extractPdfTextForPaper", Paper.class);
        method.setAccessible(true);
        return (String) method.invoke(plagiarismService, p);
    }

    private String invokeBuildPaperText(Paper p) throws Exception {
        Method method = PlagiarismService.class.getDeclaredMethod("buildPaperText", Paper.class);
        method.setAccessible(true);
        return (String) method.invoke(plagiarismService, p);
    }

    private double invokeRound(double value) throws Exception {
        Method method = PlagiarismService.class.getDeclaredMethod("round", double.class);
        method.setAccessible(true);
        return (double) method.invoke(plagiarismService, value);
    }
}


