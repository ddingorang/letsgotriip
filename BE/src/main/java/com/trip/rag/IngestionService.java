package com.trip.rag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 텍스트를 청킹 → 임베딩 → VectorStore 저장하는 RAG 인입(ingestion) 서비스.
 *
 * <p>각 청크는 {userId, docId, source} 메타데이터를 갖는 Document로 저장된다.
 * source 메타데이터는 답변 시 출처 표기·필터링에 사용된다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IngestionService {

    private final VectorStore vectorStore;

    private final TokenTextSplitter splitter = new TokenTextSplitter();

    /**
     * 텍스트를 청킹해 벡터스토어에 적재한다.
     *
     * @param userId 소유자 — 검색 시 사용자 격리 필터로 사용
     * @param docId  문서 식별자(예: "plan:42") — 재인덱싱/삭제 단위
     * @param source 출처 라벨(예: "내 여행기록", 업로드 파일명)
     * @param text   원문 텍스트
     */
    public void ingest(Long userId, String docId, String source, String text) {
        if (text == null || text.isBlank()) {
            log.debug("ingest 스킵 — 빈 텍스트. docId={}", docId);
            return;
        }

        Map<String, Object> metadata = Map.of(
                "userId", String.valueOf(userId),
                "docId", tagSafe(docId),
                "source", source);

        // 원문 1개 Document → TokenTextSplitter로 청크 분할 (메타데이터는 청크에 복사됨)
        Document sourceDoc = new Document(text, metadata);
        List<Document> chunks = splitter.apply(List.of(sourceDoc));

        if (chunks.isEmpty()) {
            log.debug("ingest 스킵 — 청크 없음. docId={}", docId);
            return;
        }

        vectorStore.add(chunks);
        log.info("RAG 적재 완료 — userId={}, docId={}, source={}, chunks={}",
                userId, docId, source, chunks.size());
    }

    /**
     * 특정 문서의 모든 청크를 삭제한다(재인덱싱 전 정리용·문서 삭제 정합용).
     * 메타데이터 docId가 tag 필드로 인덱싱되어 있어 필터 삭제가 가능하다.
     *
     * <p>PII 정합 주의: 벡터 삭제 실패를 조용히 삼키면(과거 구현) 삭제된 문서가 RAG에
     * 잔존해 검색·답변으로 노출될 수 있다(PII 유출). 따라서 실패를 삼키지 않고 예외를
     * 그대로 전파해 호출자(예: DocumentService.delete)가 DB/파일 삭제를 진행하지 않고
     * 롤백/에러 응답하도록 한다.</p>
     */
    public void deleteByDoc(String docId) {
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        Filter.Expression expr = b.eq("docId", tagSafe(docId)).build();
        vectorStore.delete(expr);
        log.info("RAG 문서 삭제 — docId={}", docId);
    }

    /**
     * docId를 RediSearch TAG 값으로 안전하게 만든다.
     *
     * <p>Spring AI RedisVectorStore의 필터 표현식 변환기는 TAG 값의 특수문자(예: ':')를
     * 이스케이프하지 않는다. 그래서 {@code docId="doc:1"} 같은 값을 그대로 쓰면
     * 삭제 시 생성되는 쿼리 {@code @docId:{doc:1}} 가 RediSearch 구문 오류를 내고
     * 0건이 삭제된다(문서가 인덱스에 잔존). 영숫자/언더스코어 외 문자를 '_'로 치환해
     * 적재·삭제가 동일한 안전한 값을 쓰도록 한다. (예: "doc:1"→"doc_1", "plan:42"→"plan_42")</p>
     */
    private static String tagSafe(String docId) {
        return docId == null ? null : docId.replaceAll("[^A-Za-z0-9_]", "_");
    }
}
