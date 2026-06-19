검토 결과: REQUEST CHANGES. 지정 범위 중심, 수정 없음.

**[high] SSE 오류 이벤트를 성공처럼 무시**
- 위치: `frontend/src/api/index.js:360-373`, `frontend/src/stores/assistant.js:100-103`, `BE/src/main/java/com/trip/assistant/AssistantController.java:83-89`
- 왜: BE는 스트리밍 중 오류를 `event:error`로 내려주는데 FE 파서는 `conversationId/token`만 처리하고 `error`를 무시합니다. 결과적으로 빈 답변/부분 답변이 정상 종료처럼 보입니다.
- 수정: `event:error` 수신 시 throw 또는 명시적 error result 반환, store에서 에러 배너/실패 말풍선 처리.

**[high] 문서 색인 실패를 “문서 추가됨”으로 위장**
- 위치: `frontend/src/views/AssistantView.vue:367-370`, `frontend/src/stores/documents.js:49-53`, `BE/src/main/java/com/trip/document/dto/DocumentResponse.java:9-14`, `BE/src/main/java/com/trip/document/service/DocumentService.java:83`
- 왜: BE 응답에는 `status=FAILED`가 올 수 있는데 AssistantView는 반환 상태를 보지 않고 “이 자료를 바탕으로 질문할 수 있어요”라고 표시합니다.
- 수정: 업로드 응답의 `status`가 `INGESTED`일 때만 성공 안내. `FAILED/PENDING`은 별도 안내와 재시도 제공.

**[high] 마이페이지 계획/앨범 조회 실패가 빈상태로 둔갑**
- 위치: `frontend/src/views/MyPageView.vue:459-467`, `frontend/src/views/MyPageView.vue:129-138`, `frontend/src/views/MyPageView.vue:485-498`, `frontend/src/views/MyPageView.vue:148-157`
- 왜: `/api/plans`나 `/users/me/albums` 실패 시 배열을 비워서 “아직 여행 계획이 없어요”, “앨범은 곧 만나요”로 표시합니다. 서버 장애/인가 실패와 실제 빈 데이터를 구분 못 합니다.
- 수정: `plansError/albumsError` 상태를 두고 빈상태와 에러상태를 분리.

**[high] 뱃지/퀘스트 API 실패를 레벨1·0개로 위장**
- 위치: `frontend/src/stores/gamification.js:17-23`, `frontend/src/views/BadgesView.vue:134-141`, `frontend/src/views/BadgesView.vue:153-165`, `frontend/src/views/MyPageView.vue:44-73`
- 왜: gamification 실패를 store가 삼키고, 화면은 기본값 레벨 1/0개/빈 퀘스트를 실데이터처럼 렌더합니다.
- 수정: store에 `error`를 보존하고 실패 시 기존/기본 데이터를 성공처럼 표시하지 않기. 화면에 재시도 UI 추가.

**[med] `+ 앨범 만들기` 버튼이 죽어 있음**
- 위치: `frontend/src/views/MyPageView.vue:162-165`, `BE/src/main/java/com/trip/user/controller/AlbumController.java:51-57`
- 왜: BE에는 앨범 생성 API가 있는데 FE 버튼에 `@click`이 없습니다.
- 수정: 생성 시트/폼 연결 후 `POST /users/me/albums` 호출, 아니면 버튼 제거/비활성 표시.

**[med] 음성 첨부 UI와 BE 계약 불일치**
- 위치: `frontend/src/views/AssistantView.vue:175-181`, `frontend/src/views/AssistantView.vue:391-399`, `BE/src/main/java/com/trip/preprocessing/controller/PreprocessingController.java:33-39`
- 왜: UI는 “녹음 파일을 텍스트로 변환”이라고 하지만 BE는 `Long dataId`만 반환합니다. FE의 `extractTranscript()`는 실제 계약상 채워질 수 없습니다.
- 수정: BE가 transcript를 반환하게 하거나, UI를 “음성 분석 데이터 업로드”로 바꾸고 변환 텍스트 입력 기능을 제거.

**[med] 마이페이지 계획 목록이 50개에서 잘림**
- 위치: `frontend/src/views/MyPageView.vue:461`, `BE/src/main/java/com/trip/plan/controller/PlanController.java:39-43`
- 왜: BE는 페이지 API인데 FE는 `size:50` 한 번만 호출하고 더보기/페이지네이션이 없습니다.
- 수정: page metadata를 보존하고 더보기/무한스크롤 추가.

**[med] 뱃지 데이터 캐시가 오래된 값을 계속 보여줄 수 있음**
- 위치: `frontend/src/stores/gamification.js:14-20`, `frontend/src/views/MyPageView.vue:570-573`
- 왜: `loaded`가 true면 `load()`가 재조회하지 않습니다. 다른 화면에서 계획 저장/뱃지 획득 후 마이페이지 복귀 시 stale summary가 남습니다.
- 수정: 마이페이지 진입은 `refresh()` 사용, 또는 계획/보상 이벤트 후 캐시 무효화.

**[low] 프로필 기본 이미지 처리 주석/동작 불일치**
- 위치: `frontend/src/views/ProfileEditView.vue:97-100`, `frontend/src/views/MyPageView.vue:26`
- 왜: ProfileEdit은 `default-profile` URL을 숨기지만 MyPage는 그대로 렌더합니다. `frontend/public/images/default-profile.png`도 존재해서 주석의 “실제 파일이 없어 깨짐”과 다릅니다.
- 수정: 기본 이미지 정책을 한 곳으로 통일하고 오래된 필터/주석 제거.