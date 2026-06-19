**Findings**

1. [high] 실제 키가 작업트리 `.env`에 노출됨 / `.env:3-7`에 OpenAI, TourAPI, Google secret, Kakao key가 실제 값으로 들어 있고, Kakao key는 `docker-compose.yml:131`, `frontend/Dockerfile:14-15`를 거쳐 `HotplaceRegisterView.vue:159`, `HotplaceDetailView.vue:135`의 브라우저 SDK URL에 실립니다. 공유 작업트리·로그·리뷰 산출물로 유출될 수 있습니다 / 노출 키 회전, Kakao 도메인 제한, 서버 키는 secret manager 또는 배포 환경 변수로만 주입.

2. [high] 관리자 pending 조회가 공개 GET 와일드카드에 걸림 / `SecurityConfig.java:61`이 `GET /community/hotplaces/*`를 permitAll로 열고, `HotPlaceController.java:71-76`의 `/pending`도 이 패턴에 매칭됩니다. 익명 호출은 `principal.userId()`에서 NPE가 나며 401/403 대신 500 경로가 됩니다 / `/pending`을 공개 매처보다 먼저 admin/authenticated로 제한하거나 `/admin/hotplaces/pending`으로 분리하고 principal null 방어 추가.

3. [high] 등록 즉시 승인되어 승인 플로우가 우회됨 / `HotPlaceService.java:51-61`이 등록 시 `APPROVED`로 저장하지만, `HotPlaceStatus.java:5-7`와 `HotPlaceController.java:71-92`에는 pending/approve/reject 플로우가 존재합니다. 악성·스팸 등록이 즉시 목록/지도에 노출됩니다 / 등록은 `PENDING`, 공개 목록은 `APPROVED`만, 관리자 승인 시 `APPROVED` 전환으로 계약을 맞추기.

4. [high] 공개 상세가 승인 상태를 검사하지 않음 / 목록은 `HotPlaceService.java:35-37`에서 `APPROVED`만 조회하지만, 상세는 `HotPlaceController.java:35-37` → `HotPlaceService.java:40-42` → `findHotPlace()` `HotPlaceService.java:147-149`로 id만 조회합니다. `SecurityConfig.java:61` 때문에 비회원도 pending/rejected id를 알면 상세를 볼 수 있습니다 / 공개 상세는 `findByIdAndStatus(APPROVED)`로 제한하고, 관리자/작성자용 조회는 별도 API로 분리.

5. [high] 등록 API 실패를 FE가 성공으로 둔갑시킴 / `HotplaceRegisterView.vue:284-302`에서 `hotplaceStore.create()` 실패 시에도 mock 데이터를 push하고 `registrationSuccess = true`, `router.back()`을 실행합니다. 401, 400, 500 모두 실제 저장 없이 성공처럼 보입니다 / catch에서는 성공 처리 금지, 에러 메시지 표시, mock fallback은 명시적 dev 모드에서만 허용.

6. [high] 위치 미선택·잘못된 좌표 등록 가능 / FE 기본 좌표가 `HotplaceRegisterView.vue:131-132`에 고정되어 있고, 유효성은 이름·카테고리만 봅니다(`HotplaceRegisterView.vue:138`). 제출 시 주소는 빈 문자열 가능(`HotplaceRegisterView.vue:275`), 좌표는 기본값으로 전송됩니다(`HotplaceRegisterView.vue:279-280`). BE DTO도 카테고리만 `@NotNull`입니다(`HotPlaceCreateRequest.java:10-16`) / FE는 선택 주소·마커 필수, BE는 `@NotBlank`, 길이, 위경도 범위 검증 추가.

7. [med] 목록/대기 목록 썸네일 N+1 / `HotPlaceService.java:35-37`과 `HotPlaceService.java:113-116`이 각 row마다 `getThumbnail()`을 호출하고, `getThumbnail()`은 매번 사진 쿼리를 실행합니다(`HotPlaceService.java:142-144`) / 대표 사진을 projection/fetch join/서브쿼리로 한 번에 조회하거나 thumbnail 컬럼을 비정규화.

8. [med] 상세·목록 mock fallback이 장애와 404를 숨김 / `getList()`는 실패 시 기존 mock을 반환합니다(`frontend/src/stores/hotplace.js:95-104`). `getDetail()`도 실패 시 `getById()` mock을 반환합니다(`frontend/src/stores/hotplace.js:111-118`), 상세 화면은 이를 정상 detail로 사용합니다(`HotplaceDetailView.vue:166-168`) / 운영에서는 fallback 제거, 404/장애 상태를 화면에 명시.

9. [med] 이미지 URL 무제한·무검증 저장 / `HotPlaceCreateRequest.java:16`의 `imageUrls`에 크기·URL 검증이 없고, `HotPlaceService.java:121-130`은 리스트 전체를 DB에 저장합니다. 상세는 첫 URL을 inline background로 렌더링합니다(`HotplaceDetailView.vue:6`) / 최대 개수·길이·프로토콜·도메인 검증, 가능하면 서버 업로드 URL만 허용.

10. [med] Kakao SDK 실패 후 검색 입력이 null deref 가능 / 키 누락 시 `loadKakao()`가 reject합니다(`HotplaceRegisterView.vue:157`), catch는 에러만 표시합니다(`HotplaceRegisterView.vue:260-262`). 하지만 검색 UI는 계속 보이고(`HotplaceRegisterView.vue:20-31`), `runSearch()`는 `geocoder.addressSearch`와 `ps.keywordSearch`를 무방어 호출합니다(`HotplaceRegisterView.vue:200`, `HotplaceRegisterView.vue:212`) / SDK ready 상태 전까지 검색 비활성화, `geocoder/ps` null guard 추가.

11. [low] 사진 UI와 상세 카운터가 죽은 기능/하드코딩 / 등록 화면의 사진 버튼은 handler/input이 없고(`HotplaceRegisterView.vue:100-109`), 제출 payload는 항상 `imageUrls: []`입니다(`HotplaceRegisterView.vue:278`). 상세는 실제 이미지 수와 무관하게 `사진 1/24`를 표시합니다(`HotplaceDetailView.vue:21`) / 업로드 기능을 연결하거나 UI 제거, 카운터는 `imageUrls.length` 기반으로 렌더.

12. [low] 상세 CTA가 동작하지 않음 / `길찾기` 버튼은 click handler가 없습니다(`HotplaceDetailView.vue:54-57`). 북마크 버튼도 handler가 없고 `bookmarked`는 초기값 이후 변경되지 않습니다(`HotplaceDetailView.vue:87-91`, `HotplaceDetailView.vue:114`) / 구현 전이면 disabled/숨김 처리, 구현 시 Kakao 길찾기 URL·저장 API 연결.

검토 중 Hotplace BE 경로에서 TourAPI/OpenAI/Redis Vector를 직접 호출하는 근거는 찾지 못했습니다. 해당 장애·타임아웃 리스크는 이 기능 경로보다는 전역 설정/다른 도메인 이슈입니다. 코드 수정은 하지 않았습니다.