# AI 챗봇 · RAG 설계 심의 (Decision Log)

Triip의 **대화형 여행 어시스턴트(채팅 AI)**와 **RAG(검색증강생성)** 구조를 개선하기 위한
의사결정 과정을 기록하는 폴더입니다. 결정만이 아니라 **고민 과정**을 남깁니다.

## 참여
- **Claude (Opus 4.8)** — 코드베이스 분석, 통합/구현
- **Codex (GPT-5.5, reasoning effort: xhigh)** — 독립 아키텍처 리뷰 (`codex exec`)

## 문서
| 파일 | 내용 |
|---|---|
| [01-current-state.md](01-current-state.md) | 현재 AI/RAG 구현 분석 — 구조·강점·문제점 (Claude) |
| [02-codex-analysis.md](02-codex-analysis.md) | Codex GPT-5.5(xhigh) 독립 분석·권고 |
| [03-decision.md](03-decision.md) | 두 분석 종합 → 채택 결정·우선순위·실행 항목 |

## 방법
1. Claude가 현재 구현(`com.trip.assistant`, `com.trip.rag`)을 분석해 `01`을 작성.
2. 동일 코드베이스를 Codex GPT-5.5(xhigh)에 read-only로 제시, 독립 분석을 `02`로 수집.
3. 교차 검토 후 합의/이견을 `03`에 종합하고 실행 우선순위를 확정.
