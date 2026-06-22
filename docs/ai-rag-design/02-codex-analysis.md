# 02. Codex GPT-5.5 (xhigh) 독립 분석

> 실행: `codex exec --skip-git-repo-check -s read-only` (model gpt-5.5, reasoning effort xhigh).
> 코드를 프롬프트에 인라인으로 제시한 read-only 리뷰. 아래는 codex의 원문(영문).

# Read-Only Chatbot + RAG Review

Assessment is based only on the inlined code/context.

## 1. Chat AI Design

**Strong parts**

- Server-side `userId` injection into tools is the right boundary. The model cannot choose another `userId`.
- Dedicated RAG Redis connection is good separation from cache Redis.
- Tool errors returning guidance strings is demo-friendly.
- Prompt-injection rule is directionally correct: retrieved docs are data, not instructions.

**Concrete risks / bugs**

- **State-changing tools rely too much on prompt obedience.** `createTravelPlan`, `addPlaceToPlan`, `removePlaceFromPlan`, and checklist creation should enforce server-side preconditions, not only system-prompt rules. A malicious retrieved doc can still influence the model’s next tool call.
- **`removePlaceFromPlan` is functionally weak.** It requires `placeId`, but neither user nor LLM reliably sees that ID. This will cause failed removals or wrong removals.
- **Conversation memory is under-specified.** If no `ChatMemoryRepository` is configured, Spring AI defaults to in-memory memory, so restart loses history. If JDBC is chosen, note that Spring AI JDBC chat memory does not persist tool-call/tool-response messages, so tool history can disappear unless you store action summaries separately.
- **Follow-up retrieval likely fails.** RAG search appears to use only the current `message`. Questions like “그 일정 첫날은?” may not retrieve the right plan unless the query is rewritten/compressed with conversation history.
- **Filter expression is built by string concatenation.** `userId == '...'` should not be hand-built. Use `FilterExpressionBuilder` and normalize/sanitize TAG values. This is both correctness and cross-user isolation risk.
- **Tool outputs are human strings, not structured contracts.** Good for the final answer, weak for UI refresh, audit, retries, and model follow-up.
- **Sync timeout design is only half the story.** Pool size 8 / queue 64 is good, but queue saturation, cancellation behavior, and `RejectedExecutionException` behavior need explicit handling.
- **Streaming timeout does not solve side-effect visibility.** The UI may receive a nice streamed answer while the actual plan state changed invisibly unless a mutation event/refetch mechanism exists.
- **Single `gpt-4o-mini` is acceptable for demo, but tool-routing needs stricter guards.** The issue is not the model alone; it is stateful tool execution without confirmation, structured arguments, or deterministic routing settings.

## 2. RAG Pipeline Assessment

**Main issues**

- **No similarity threshold is the largest retrieval-quality flaw.** `topK(4)` alone forces the “best 4” even when all are irrelevant. This increases hallucination and stale-context answers.
- **`topK=4` is probably too low after filtering by user.** For users with many plans/analyses, retrieve more candidates, then filter/rerank down.
- **Default `TokenTextSplitter` is acceptable for short plan summaries, weak for PDFs/chat transcripts.** It is token-based, not travel-domain-aware. Plans should be chunked by plan/day/place; PDFs by page/section; analyses by topic/time block.
- **Freshness/idempotency is unclear and likely broken.** `ingest()` creates new `Document`s without stable IDs. Re-indexing the same `docId` can duplicate chunks unless `deleteByDoc()` is always called first. Tool edits also need automatic re-index.
- **Citations are too weak.** The assistant needs explicit `docId/source/chunk` metadata in context and should cite the exact plan/analysis used.
- **No reranking/deduping.** With many chunks from the same plan, top results can be redundant. Use a post-processor or manual retrieval pipeline.
- **Hybrid retrieval matters for travel.** Exact place names, plan titles, addresses, and Korean proper nouns often work better with BM25/text search than pure embeddings.
- **Metadata model is too thin.** Add `sourceType`, `planId`, `analysisId`, `chunkNo`, `title`, `createdAt/updatedAt`, `planVersion`. Keep exact-filter fields as TAG-safe values; keep display fields separately.

## 3. Friction Verdicts

| # | Verdict | Assessment |
|---|---|---|
| 1 | **Agree** | Fixed `topK=4` with no threshold is a real hallucination/noise risk. Add threshold and tune with Korean travel queries. |
| 2 | **Agree** | No reranking is not fatal for a demo, but it becomes visible once users have many plans or uploaded docs. |
| 3 | **Agree, with nuance** | Default splitter is fine for tiny plan summaries, poor for PDFs/STT/KakaoTalk. Structure-aware chunking matters more than generic overlap. |
| 4 | **Agree** | Answers should cite plan/source/chunk or explicitly say no relevant saved data was found. |
| 5 | **Strongly agree** | This is probably worse than stated: duplicate chunks and stale chunks are both possible unless delete-before-index and edit-triggered reindex are guaranteed. |
| 6 | **Agree** | Unknown memory persistence is a production-demo risk. Also verify whether chosen repo persists tool interactions. |
| 7 | **Strongly agree** | `placeId`-only removal is a UX/tool-contract bug. |
| 8 | **Agree** | Chatbot mutations need frontend invalidation/refetch/event delivery. |
| 9 | **Partly disagree** | `gpt-4o-mini` is fine for demo. The bigger issue is no routing/confirmation/temperature/tool-guard policy. |
| 10 | **Agree** | Add Spring AI/Micrometer observations plus custom RAG metrics. |

**Missed frictions**

- Cross-user leakage risk from hand-built filter expressions and TAG escaping.
- Follow-up question retrieval lacks query compression/rewrite.
- State-changing tools need server-side confirmation or preview/save split.
- Ingestion should be idempotent with stable chunk identity or delete-before-add.
- Redis schema/index health should validate dimension `1536`, metadata fields, and index existence.
- Tool calls should emit structured domain events, not only human strings.

## 4. Prioritized Recommendations

| Priority | Effort | Recommendation | Specific mechanism |
|---|---:|---|---|
| P0 | S | Add relevance gate | `SearchRequest.builder().similarityThreshold(0.65-0.80).topK(8-12).filterExpression(...)`; tune threshold empirically. |
| P0 | S | Stop string-building filters | Use `FilterExpressionBuilder().eq("userId", safeUserId)` and sanitize all Redis TAG fields consistently. |
| P0 | M | Make indexing idempotent and fresh | Before every plan/analysis reindex: `deleteByDoc(docId)` then `vectorStore.add(chunks)`. Trigger via `@TransactionalEventListener(phase = AFTER_COMMIT)` on plan create/update/add/remove. |
| P0 | S/M | Fix place removal | Add `listPlanPlaces(planId)` returning `dayNo/placeId/contentId/title`; or let `removePlaceFromPlan` accept title/contentId and resolve uniquely. |
| P0 | S/M | Persist memory intentionally | Use `MessageWindowChatMemory` with `RedisChatMemoryRepository` since Redis Stack is already present, or JDBC plus separate action summaries if tool history matters. |
| P1 | M | Add citations | Custom `QuestionAnswerAdvisor.promptTemplate()` or `RetrievalAugmentationAdvisor` context that includes `[source, docId, chunkNo]`; require cited answers. |
| P1 | M | Add follow-up query rewriting | `RetrievalAugmentationAdvisor` with `CompressionQueryTransformer` or `RewriteQueryTransformer` before vector retrieval. |
| P1 | M | Tune chunking by source type | `TokenTextSplitter.builder().withChunkSize(500-800).withMinChunkSizeChars(250-400).withKeepSeparator(true)` plus domain chunking by plan/day/PDF page. |
| P1 | M | Add rerank/dedupe | `DocumentPostProcessor` to dedupe by `docId`, prefer recent/current plans, and rerank top 20 down to 5-6. |
| P1 | M | Frontend refresh after tools | Publish `PlanChangedEvent`; expose WebSocket/SSE event or return structured mutation metadata so frontend refetches affected plan. |
| P1 | S/M | Add observability | Spring AI Micrometer observations for chat/model/vector/tool calls; custom counters for RAG hit rate, empty-context rate, tool-call success/failure, timeout, queue rejection. |
| P2 | M/L | Add hybrid retrieval | Use Redis text search/BM25 via `RedisVectorStore.searchByText(...)` or native RediSearch, union with vector results, normalize/rerank. |
| P2 | S | Split preview/save tools | `previewTravelPlan` returns candidate itinerary; `saveTravelPlan` requires explicit user confirmation. |
| P2 | S/M | Add model routing | Simple Q&A without tools/RAG; deterministic low-temperature tool path; optionally stronger model only for ambiguous planning/tool decisions. |
| P2 | M | Add golden-set eval | 20-50 Korean queries covering saved plans, stale plans, cross-user isolation, no-answer cases, and tool mutation flows. |

## 5. Single Biggest Risk

The biggest ship risk is **the assistant treating stale or irrelevant retrieved context as authoritative, then using that context in user-visible answers or plan mutations**. No similarity threshold, weak citations, uncertain re-indexing after edits, and immediate state-changing tools combine into one failure mode: the chatbot can confidently answer from the wrong itinerary or modify the wrong plan path while appearing successful.

## References

- Spring AI RAG / `QuestionAnswerAdvisor`, `RetrievalAugmentationAdvisor`, `VectorStoreDocumentRetriever`: https://docs.spring.io/spring-ai/reference/api/retrieval-augmented-generation.html
- Spring AI Redis Vector Store / metadata fields, HNSW, text search: https://docs.spring.io/spring-ai/reference/api/vectordbs/redis.html
- Spring AI Chat Memory repositories: https://docs.spring.io/spring-ai/reference/api/chat-memory.html
- Spring AI ETL / `TokenTextSplitter`: https://docs.spring.io/spring-ai/reference/api/etl-pipeline.html
- Spring AI Observability: https://docs.spring.io/spring-ai/reference/observability/index.html
