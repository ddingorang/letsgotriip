package com.trip.preprocessing.service;

import com.trip.global.error.GeneralException;
import com.trip.global.error.ResponseCode;
import com.trip.preprocessing.client.STTManager;
import com.trip.preprocessing.entity.UserAnalysisData;
import com.trip.preprocessing.entity.enums.AnalysisDataType;
import com.trip.preprocessing.repository.UserAnalysisDataRepository;
import com.trip.rag.UserDataIndexer;
import com.trip.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class PreprocessingService {

    private final UserAnalysisDataRepository userAnalysisDataRepository;
    private final STTManager sttManager;
    private final UserDataIndexer userDataIndexer;
    private final TravelPreferenceExtractor travelPreferenceExtractor;
    private final UserService userService;

    private final String uploadDir = "temp_uploads/";

    // PII 마스킹 정규식: 전화번호 / 주민등록번호 / 이메일
    // 전화/주민번호는 양화 구간이 리터럴('-'·'@')과 고정 길이로 분리되어 겹침이 없어 백트래킹 안전.
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("01[016789]-?\\d{3,4}-?\\d{4}");
    private static final Pattern RRN_PATTERN =
            Pattern.compile("\\d{6}-?\\d{7}");
    // ReDoS 수정: 기존 도메인부 "[A-Za-z0-9.-]+\\.[A-Za-z]{2,}" 는 '.' 가 char class 와
    // 뒤따르는 "\\.{2,}" 양쪽에서 매칭되어(겹치는 양화) 매칭 실패 시 파국적 백트래킹을 일으킴.
    // 도메인을 라벨 단위 "([A-Za-z0-9-]+\\.)+[A-Za-z]{2,63}" 로 바꿔 '.' 의 중복 매칭을 제거.
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("[A-Za-z0-9._%+-]+@([A-Za-z0-9-]+\\.)+[A-Za-z]{2,63}");

    // rollbackFor: processData가 던지는 checked IOException(파일 읽기 실패 등)에도
    // 트랜잭션이 롤백되도록 한다. 기본값은 unchecked만 롤백하므로, rawText가 채워지지
    // 않은(마스킹 전) 레코드가 커밋되어 잔존하는 것을 막는다.
    /** 키 → 한글 라벨(추천 도메인 6개 테마와 동일). */
    private static final java.util.Map<String, String> THEME_LABELS = java.util.Map.of(
            "sea", "바다", "mountain", "산·자연", "food", "맛집",
            "history", "역사·문화", "activity", "액티비티", "shopping", "쇼핑");
    private static final int PREVIEW_LIMIT = 1200;

    @Transactional(rollbackFor = Exception.class)
    public com.trip.preprocessing.dto.AnalysisResultResponse uploadAndProcess(
            Long userId, AnalysisDataType dataType, MultipartFile file) throws IOException {
        // 절대경로(시스템 temp)로 둔다. 상대경로면 MultipartFile.transferTo 가 서블릿 컨테이너
        // 임시 디렉터리 기준으로 해석돼 우리가 만든 디렉터리와 어긋나 FileNotFoundException 이 난다.
        Path directory = Paths.get(System.getProperty("java.io.tmpdir"), uploadDir).toAbsolutePath();
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
        }

        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path filePath = directory.resolve(fileName);
        file.transferTo(filePath.toFile());

        // PII 정합: STT/카톡 원본 파일에는 통화/대화 원문(전화번호·주민번호 등)이 들어 있다.
        // 처리(전사/마스킹) 후에는 DB에 마스킹된 rawText만 유지하고 원본 임시 파일은
        // 성공·실패 여부와 무관하게 항상 삭제해 디스크에 PII 원문이 잔존하지 않게 한다.
        try {
            UserAnalysisData analysisData = UserAnalysisData.builder()
                    .userId(userId)
                    .dataType(dataType)
                    .originalFileName(file.getOriginalFilename())
                    .storagePath(filePath.toString())
                    .build();

            userAnalysisDataRepository.save(analysisData);

            List<String> themeKeys = processData(analysisData, filePath.toFile());

            // 사용자에게 '실제로 분석됐음'을 보여주기 위해 마스킹된 전사 미리보기 + 추출된 취향을 반환.
            String masked = analysisData.getRawText() == null ? "" : analysisData.getRawText();
            int totalChars = masked.length();
            boolean truncated = totalChars > PREVIEW_LIMIT;
            String preview = truncated ? masked.substring(0, PREVIEW_LIMIT) : masked;
            List<String> labels = themeKeys.stream()
                    .map(k -> THEME_LABELS.getOrDefault(k, k)).toList();

            return new com.trip.preprocessing.dto.AnalysisResultResponse(
                    analysisData.getId(), preview, totalChars, truncated, themeKeys, labels);
        } finally {
            deleteTempFile(filePath);
        }
    }

    /** 처리 완료(성공·실패) 후 원본 임시 업로드 파일을 삭제한다(PII 원문 디스크 잔존 방지). */
    private void deleteTempFile(Path filePath) {
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("임시 업로드 파일 삭제 실패 — path={}, error={}", filePath, e.getMessage());
        }
    }

    private List<String> processData(UserAnalysisData analysisData, File file) throws IOException {
        if (analysisData.getDataType() == AnalysisDataType.KAKAO_TALK) {
            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            analysisData.updateRawText(maskPii(content));
        } else if (analysisData.getDataType() == AnalysisDataType.VOICE_CALL) {
            // NOTE(범위 밖): Whisper STT 호출이 @Transactional 안에서 동기 실행돼 DB 커넥션을
            // 외부 API 시간만큼 점유한다. 외부 호출을 트랜잭션 밖으로 빼는 리팩터는 별도 과제.
            String sttResult = sttManager.convertSpeechToText(file);
            // 전사 결과가 null/blank이면 빈 전사 → 성공 저장하지 않고 실패로 처리
            // (convertSpeechToText가 예외 대신 빈 값을 반환하는 구현을 대비한 방어)
            if (sttResult == null || sttResult.isBlank()) {
                log.error("STT 전사 결과가 비어 있습니다 — analysisId={}", analysisData.getId());
                throw new GeneralException(ResponseCode._INTERNAL_SERVER_ERROR, "음성 전사 결과가 비어 있습니다.");
            }
            // PII 마스킹 후 저장
            analysisData.updateRawText(maskPii(sttResult));
        }

        // 분석 결과(마스킹된 rawText)를 RAG 벡터스토어에 색인 → 챗봇이 분석데이터를 참조 가능.
        // 빈 텍스트(예: 빈 카톡 파일)는 색인하지 않는다. 색인 실패가 업로드/저장 트랜잭션을
        // 깨뜨리지 않도록 방어적으로 감싼다.
        String maskedText = analysisData.getRawText();
        List<String> themeKeys = List.of();
        if (maskedText != null && !maskedText.isBlank()) {
            try {
                userDataIndexer.indexAnalysis(analysisData.getUserId(), analysisData.getId(), maskedText);
            } catch (Exception e) {
                log.warn("분석데이터 RAG 색인 실패 — analysisId={}, error={}",
                        analysisData.getId(), e.getMessage());
            }

            // rawText에서 여행 취향 테마 키를 추출해 사용자 preferredInterests에 합집합 병합한다.
            // 추출/저장 실패가 업로드 트랜잭션을 깨지 않도록 전 과정을 방어적으로 감싼다.
            try {
                themeKeys = travelPreferenceExtractor.extractThemeKeys(maskedText);
                if (!themeKeys.isEmpty()) {
                    userService.mergePreferredInterests(analysisData.getUserId(), themeKeys);
                    log.info("분석데이터에서 취향 자동 반영 — analysisId={}, keys={}",
                            analysisData.getId(), themeKeys);
                }
            } catch (Exception e) {
                log.warn("분석데이터 취향 추출/반영 실패 — analysisId={}, error={}",
                        analysisData.getId(), e.getMessage());
            }
        }
        return themeKeys;
    }

    /**
     * 개인정보(PII) 마스킹: 전화번호 / 주민등록번호 / 이메일을 마스킹 토큰으로 치환한다.
     * 주민번호를 전화번호보다 먼저 치환해 패턴 충돌을 피한다.
     */
    String maskPii(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        String masked = RRN_PATTERN.matcher(text).replaceAll("[주민번호]");
        masked = PHONE_PATTERN.matcher(masked).replaceAll("[전화번호]");
        masked = EMAIL_PATTERN.matcher(masked).replaceAll("[이메일]");
        return masked;
    }
}
