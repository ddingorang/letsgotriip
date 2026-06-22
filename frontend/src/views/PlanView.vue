<template>
  <div class="page">
    <header class="plan-header">
      <h1 class="header-title">내 여행 계획</h1>
      <div class="header-actions">
        <button
          v-if="plans.length >= 2"
          class="compare-toggle"
          :class="{ active: compareMode }"
          @click="toggleCompareMode"
          title="두 계획 비교"
        >
          <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="18" y1="20" x2="18" y2="10" />
            <line x1="12" y1="20" x2="12" y2="4" />
            <line x1="6" y1="20" x2="6" y2="14" />
          </svg>
        </button>
        <button class="add-btn" @click="goNewTrip" title="새 여행 만들기">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round">
            <line x1="12" y1="5" x2="12" y2="19" />
            <line x1="5" y1="12" x2="19" y2="12" />
          </svg>
        </button>
      </div>
    </header>

    <!-- 비교 모드 안내 배너 -->
    <div v-if="compareMode" class="compare-banner">
      <span class="compare-banner-text">
        비교할 계획을 선택하세요 ({{ compareSelection.length }}/{{ COMPARE_MAX }})
      </span>
      <div class="compare-banner-actions">
        <button
          class="compare-banner-run"
          :disabled="compareSelection.length < 2 || compareLoading"
          @click="runCompare"
        >{{ compareLoading ? '비교 중…' : `비교하기 (${compareSelection.length})` }}</button>
        <button class="compare-banner-cancel" @click="toggleCompareMode">취소</button>
      </div>
    </div>

    <div class="scroll-content">
      <!-- ── Loading state ────────────────────────────────────────────── -->
      <div v-if="listLoading && plans.length === 0" class="state-block">
        <div class="skeleton-card" />
        <div class="skeleton-card" />
        <div class="skeleton-card" />
      </div>

      <!-- ── Error state (목록 로드 실패) ─────────────────────────────── -->
      <div v-else-if="listError" class="state-block error-state">
        <div class="empty-icon">
          <svg width="52" height="52" viewBox="0 0 24 24" fill="none" stroke="var(--color-line)" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10" />
            <line x1="12" y1="8" x2="12" y2="12" />
            <line x1="12" y1="16" x2="12.01" y2="16" />
          </svg>
        </div>
        <p class="empty-title">계획을 불러오지 못했어요</p>
        <p class="empty-sub">{{ listError }}</p>
        <button class="create-ai-btn" :disabled="listLoading" @click="reloadPlans">
          {{ listLoading ? '불러오는 중...' : '다시 시도' }}
        </button>
      </div>

      <!-- ── Empty state ──────────────────────────────────────────────── -->
      <div v-else-if="plans.length === 0" class="empty-state">
        <div class="empty-icon">
          <svg width="52" height="52" viewBox="0 0 24 24" fill="none" stroke="var(--color-line)" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round">
            <rect x="3" y="4" width="18" height="18" rx="2" />
            <path d="M16 2v4M8 2v4M3 10h18" />
          </svg>
        </div>
        <p class="empty-title">아직 여행 계획이 없어요</p>
        <p class="empty-sub">AI가 최적의 일정을 만들어 드려요</p>
        <button class="create-ai-btn" @click="goNewTrip">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="3"/>
            <path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42"/>
          </svg>
          새 여행 만들기 (AI)
        </button>
      </div>

      <!-- ── Plan list ────────────────────────────────────────────────── -->
      <div v-else class="plan-list">
        <div
          v-for="plan in plans"
          :key="plan.id"
          class="plan-card"
          :class="{ expanded: selectedPlanId === plan.id, selectable: compareMode, selected: isCompareSelected(plan.id) }"
        >
          <!-- Card header — click to expand/collapse (or select in compare mode) -->
          <!-- 대표 이미지가 있으면 썸네일 배경으로, 없으면 기존 그라데이션(기본 이미지) 유지 -->
          <div class="plan-thumb" :class="{ 'has-img': !!plan.imageUrl }" @click="onCardTap(plan)">
            <img
              v-if="plan.imageUrl"
              :src="plan.imageUrl"
              class="thumb-img"
              alt=""
              loading="lazy"
              @error="(e) => (e.target.style.display = 'none')"
            />
            <div class="thumb-gradient" />
            <div v-if="compareMode" class="compare-check" :class="{ on: isCompareSelected(plan.id) }">
              <svg v-if="isCompareSelected(plan.id)" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12" /></svg>
            </div>
            <!-- 대표 이미지 변경(소유 계획) — 펼친 카드에서만 노출 -->
            <button
              v-if="!compareMode && selectedPlanId === plan.id"
              class="thumb-edit-btn"
              :disabled="imageUploading"
              title="대표 이미지 변경"
              @click.stop="pickPlanImage(plan)"
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M23 19a2 2 0 01-2 2H3a2 2 0 01-2-2V8a2 2 0 012-2h4l2-3h6l2 3h4a2 2 0 012 2z" /><circle cx="12" cy="13" r="4" />
              </svg>
              {{ imageUploading && imageEditPlanId === plan.id ? '업로드 중…' : '사진' }}
            </button>
            <div class="plan-dates">
              <span class="date-label">{{ formatDate(plan.startDate) }}</span>
              <span class="date-sep">–</span>
              <span class="date-label">{{ formatDate(plan.endDate) }}</span>
            </div>
          </div>
          <div class="plan-info">
            <div class="plan-info-main" @click="onCardTap(plan)">
              <h3 class="plan-name">{{ plan.title }}</h3>
              <p class="plan-sub">{{ plan.destination }} · {{ dayCount(plan.startDate, plan.endDate) }}박 {{ dayCount(plan.startDate, plan.endDate) + 1 }}일</p>
              <div class="plan-spots">
                <span v-for="spot in plan.spots?.slice(0, 3)" :key="spot" class="spot-chip">{{ spot }}</span>
              </div>
            </div>
            <button v-if="!compareMode" class="plan-delete-btn" title="여행 계획 삭제" @click.stop="confirmDeletePlan(plan)">
              <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="3 6 5 6 21 6" />
                <path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2" />
                <line x1="10" y1="11" x2="10" y2="17" />
                <line x1="14" y1="11" x2="14" y2="17" />
              </svg>
            </button>
          </div>

          <!-- Expanded detail panel -->
          <Transition name="expand">
            <div v-if="selectedPlanId === plan.id" class="plan-detail">
              <div class="detail-divider" />

              <!-- 편집 도움말 — 한 번만(세션) 노출 -->
              <div v-if="showEditHint" class="edit-hint">
                <span class="edit-hint-text">
                  <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><line x1="4" y1="8" x2="20" y2="8" /><line x1="4" y1="14" x2="20" y2="14" /></svg>
                  끌어서 순서 변경 · <strong>⋯</strong> 눌러 다른 날로 이동·삭제
                </span>
                <button class="edit-hint-close" aria-label="도움말 닫기" @click="dismissEditHint">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" /></svg>
                </button>
              </div>

              <!-- Days list from planStore.current if loaded -->
              <div v-if="planStore.loading" class="detail-loading">불러오는 중...</div>
              <template v-else-if="planStore.current?.id === plan.id">
                <div
                  v-for="day in planStore.current.days ?? []"
                  :key="day.dayNo"
                  class="detail-day"
                >
                  <div class="detail-day-header">
                    <span class="detail-day-pill">{{ day.dayNo }}일차</span>
                    <span v-if="day.summary" class="detail-day-summary">{{ day.summary }}</span>
                  </div>
                  <div class="detail-places">
                    <!-- TransitionGroup: 이동/삭제/순서변경 시 행이 부드럽게 미끄러지도록(FLIP).
                         드래그 중(dragDay)에는 트랜지션을 끄는 클래스를 줘 따라다니는 잔상/버벅임을 막는다. -->
                    <TransitionGroup
                      name="place"
                      tag="div"
                      class="detail-places-group"
                      :class="{ 'is-dragging': dragDay === day.dayNo }"
                    >
                    <div
                      v-for="(place, idx) in day.places ?? []"
                      :key="place.id ?? place.attraction?.contentId ?? idx"
                      class="detail-place-row"
                      :class="{
                        dragging: dragDay === day.dayNo && dragFrom === idx,
                        'drop-target': dragOver === day.dayNo + ':' + idx && dragFrom !== idx,
                      }"
                      data-place-row
                      :data-day="day.dayNo"
                      :data-idx="idx"
                    >
                      <!-- 드래그 핸들 — 포인터 기반(마우스+터치) 정렬 -->
                      <span
                        class="drag-handle"
                        title="끌어서 순서 변경"
                        @pointerdown="onHandlePointerDown(day, idx, $event)"
                      >
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                          <line x1="4" y1="8" x2="20" y2="8" /><line x1="4" y1="14" x2="20" y2="14" />
                        </svg>
                      </span>
                      <span class="detail-place-num">{{ idx + 1 }}</span>
                      <span class="detail-place-name">{{ place.attraction?.title ?? place.title ?? '장소' }}</span>
                      <span v-if="place.visitTime" class="detail-place-time">{{ place.visitTime }}</span>
                      <button
                        class="place-edit-btn"
                        title="더보기 (다른 날로 이동·삭제)"
                        :disabled="planStore.loading"
                        @click="openRowSheet(plan.id, day.dayNo, place)"
                      >
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">
                          <circle cx="12" cy="5" r="1" /><circle cx="12" cy="12" r="1" /><circle cx="12" cy="19" r="1" />
                        </svg>
                      </button>
                    </div>
                    </TransitionGroup>
                    <div v-if="!(day.places?.length)" class="detail-empty">일정이 없어요</div>
                    <!-- 장소 추가 — 검색해서 이 일차에 바로 담기 -->
                    <button
                      class="add-place-btn"
                      :disabled="planStore.loading"
                      @click="openAddPlace(plan.id, day.dayNo)"
                    >
                      <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" /></svg>
                      {{ day.dayNo }}일차에 장소 추가
                    </button>
                  </div>
                </div>
                <div v-if="!(planStore.current.days?.length)" class="detail-empty-plan">
                  아직 일정이 없어요. AI로 동선을 최적화해보세요!
                </div>
              </template>
              <div v-else class="detail-loading">상세 정보를 불러오는 중...</div>

              <!-- 일정 지도 — 일차별로 분리. 선택한 일차의 장소+경로만 표시 -->
              <div v-if="planDayNumbers.length" class="plan-map-section">
                <!-- 일차 선택 (이전 ‹ · 칩 · › 다음) -->
                <div class="day-nav">
                  <button class="day-nav-arrow" :disabled="!canPrevDay" title="이전 일차" @click="stepDay(-1)">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6" /></svg>
                  </button>
                  <div class="day-chips">
                    <button
                      v-for="dn in planDayNumbers"
                      :key="dn"
                      :class="['day-chip', { active: selectedDay === dn }]"
                      @click="selectedDay = dn"
                    >{{ dn }}일차</button>
                  </div>
                  <button class="day-nav-arrow" :disabled="!canNextDay" title="다음 일차" @click="stepDay(1)">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6" /></svg>
                  </button>
                </div>

                <div v-if="currentDayPlaces.length" class="plan-map-wrap">
                  <TripMap :places="currentDayPlaces" :path="currentDayLine" :path-dashed="currentDayDashed" :numbered="true" />
                </div>
                <p v-else class="day-empty-map">{{ selectedDay }}일차는 지도에 표시할 장소(좌표)가 없어요.</p>

                <div class="map-actions">
                  <span v-if="currentDaySummary" class="route-summary">
                    {{ selectedDay }}일차 · 차량 {{ currentDaySummary.distanceKm }}km · 약 {{ currentDaySummary.durationMin }}분
                  </span>
                  <span v-else-if="routePathLoading" class="route-summary muted">경로 계산 중…</span>
                  <span v-else-if="currentDayDashed" class="route-summary muted">{{ selectedDay }}일차 · 직선 동선(도로경로 없음)</span>
                  <span v-else-if="currentDayPlaces.length" class="route-summary muted">{{ selectedDay }}일차 경로 없음</span>
                  <button
                    v-if="currentDayPlaces.length >= 1"
                    class="navi-btn"
                    @click="openKakaoNavi"
                  >
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <polygon points="3 11 22 2 13 21 11 13 3 11" />
                    </svg>
                    카카오맵 길안내
                  </button>
                </div>
              </div>

              <!-- 예산 패널 -->
              <div v-if="budget && budget.planId === plan.id" class="budget-panel">
                <div class="budget-head">
                  <span class="budget-title">예상 예산</span>
                  <span class="budget-total">{{ formatWon(budget.totalEstimated) }}</span>
                </div>
                <div class="budget-days">
                  <div v-for="d in budget.dayBudgets" :key="d.dayNo" class="budget-day-row">
                    <span class="budget-day-label">{{ d.dayNo }}일차</span>
                    <span class="budget-day-cost">{{ formatWon(d.estimatedCost) }}</span>
                  </div>
                </div>
                <div v-if="budget.plannedBudget != null" class="budget-planned-row">
                  <span>설정 예산 {{ formatWon(budget.plannedBudget) }}</span>
                  <span
                    v-if="budget.difference != null"
                    class="budget-diff"
                    :class="{ over: budget.difference < 0 }"
                  >
                    {{ budget.difference >= 0 ? '여유 ' : '초과 ' }}{{ formatWon(Math.abs(budget.difference)) }}
                  </span>
                </div>
                <p v-if="budget.note" class="budget-note">{{ budget.note }}</p>
              </div>

              <!-- 공유 링크 -->
              <div v-if="shareInfo && shareInfo.planId === plan.id" class="share-panel">
                <span class="share-icon">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M4 12v8a2 2 0 002 2h12a2 2 0 002-2v-8" /><polyline points="16 6 12 2 8 6" /><line x1="12" y1="2" x2="12" y2="15" />
                  </svg>
                </span>
                <code class="share-url">{{ shareInfo.url }}</code>
                <button class="share-copy-btn" @click="copyShareUrl">{{ shareCopied ? '복사됨' : '복사' }}</button>
              </div>

              <!-- 챗봇으로 다듬기 — 상세의 주요 액션. 이 계획 맥락으로 어시스턴트 진입 -->
              <div class="detail-actions">
                <button class="detail-action-btn assistant-btn" @click="goAssistant(plan.id)">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z" />
                  </svg>
                  이 계획을 챗봇과 다듬기
                </button>
              </div>

              <!-- Action buttons -->
              <div class="detail-actions">
                <button class="detail-action-btn optimize-btn" @click="goReport(plan.id)">
                  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <circle cx="12" cy="12" r="3"/>
                    <path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42"/>
                  </svg>
                  AI 동선 최적화
                </button>
              </div>
              <!-- 평가 받기 -->
              <div class="detail-actions secondary">
                <button class="detail-sub-btn" @click="goReport(plan.id)">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M9 11l3 3L22 4" /><path d="M21 12v7a2 2 0 01-2 2H5a2 2 0 01-2-2V5a2 2 0 012-2h11" />
                  </svg>
                  평가 받기
                </button>
              </div>
              <div class="detail-actions secondary">
                <button class="detail-sub-btn" :disabled="budgetLoading" @click="loadBudget(plan.id)">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <line x1="12" y1="1" x2="12" y2="23" /><path d="M17 5H9.5a3.5 3.5 0 000 7h5a3.5 3.5 0 010 7H6" />
                  </svg>
                  {{ budgetLoading ? '계산 중...' : '예산 보기' }}
                </button>
                <button class="detail-sub-btn" :disabled="shareLoading" @click="sharePlan(plan.id)">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <circle cx="18" cy="5" r="3" /><circle cx="6" cy="12" r="3" /><circle cx="18" cy="19" r="3" /><line x1="8.59" y1="13.51" x2="15.42" y2="17.49" /><line x1="15.41" y1="6.51" x2="8.59" y2="10.49" />
                  </svg>
                  {{ shareLoading ? '생성 중...' : '공유하기' }}
                </button>
              </div>
            </div>
          </Transition>
        </div>
      </div>

      <!-- ── Companion section ────────────────────────────────────────── -->
      <div class="companion-section">
        <div class="section-header">
          <h2 class="section-title">동행 구하기</h2>
          <button class="see-all" @click="router.push({ path: '/community', query: { tab: 'companion' } })">전체보기</button>
        </div>
        <div v-if="companions.length" class="companion-list">
          <div
            v-for="comp in companions.slice(0, 3)"
            :key="comp.id"
            class="companion-card"
            @click="router.push(`/companion/${comp.id}`)"
          >
            <!-- 대표 이미지 썸네일 — 없으면 기본 그라데이션(comp-thumb-ph) -->
            <div class="comp-thumb" :class="{ ph: !comp.imageUrl }">
              <img
                v-if="comp.imageUrl"
                :src="comp.imageUrl"
                alt=""
                loading="lazy"
                @error="(e) => { e.target.style.display = 'none'; e.target.parentElement.classList.add('ph') }"
              />
            </div>
            <div class="comp-header">
              <span class="comp-badge" :class="{ urgent: comp.status === '마감임박' }">{{ comp.status }}</span>
              <span
                v-if="companionDday(comp.dateRange) != null"
                class="comp-dday"
                :class="{ urgent: companionDday(comp.dateRange) <= 3 }"
              >
                D-{{ companionDday(comp.dateRange) }}
              </span>
            </div>
            <h4 class="comp-title">{{ comp.title }}</h4>
            <p class="comp-sub">{{ comp.location }}<span v-if="comp.dateRange"> · {{ comp.dateRange }}</span></p>
            <div class="comp-footer">
              <div class="comp-members">
                <div v-for="i in Math.min(comp.currentCount, 6)" :key="i" class="member-dot" />
                <span class="member-text">{{ comp.currentCount }}/{{ comp.maxCount }}명</span>
              </div>
              <button class="join-btn" @click.stop="router.push(`/companion/${comp.id}`)">참여하기</button>
            </div>
          </div>
        </div>
        <!-- 동행 목록 비어있음(미로그인/없음/로드 실패) — 가짜 목업 대신 빈상태 노출 -->
        <div v-else class="companion-empty">
          <p class="companion-empty-text">아직 모집 중인 동행이 없어요</p>
          <button class="companion-empty-btn" @click="router.push({ path: '/community', query: { tab: 'companion' } })">
            동행 둘러보기
          </button>
        </div>
      </div>

      <div class="bottom-spacer" />
    </div>

    <!-- 비교 결과 모달 — N개 계획을 가로 스크롤 표로 -->
    <Transition name="fade">
      <div v-if="compareResult" class="compare-overlay" @click.self="closeCompare">
        <div class="compare-sheet">
          <div class="compare-sheet-head">
            <h3 class="compare-sheet-title">계획 비교 ({{ compareResult.length }}개)</h3>
            <button class="compare-sheet-close" @click="closeCompare">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" /></svg>
            </button>
          </div>
          <!-- 가로 스크롤: 첫 열(지표 라벨) 고정 + 계획별 N열 -->
          <div class="compare-scroll">
            <table class="compare-table">
              <thead>
                <tr>
                  <th class="compare-th-metric" />
                  <th
                    v-for="p in compareResult"
                    :key="p.planId"
                    class="compare-th-plan"
                  >{{ p.title }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in compareRows" :key="row.key">
                  <td class="compare-td-metric">{{ row.label }}</td>
                  <td
                    v-for="cell in row.cells"
                    :key="cell.planId"
                    class="compare-td-val"
                    :class="{ better: cell.best }"
                  >{{ cell.text }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </Transition>

    <!-- 장소 추가 바텀시트 — 검색해서 선택한 일차에 담기 -->
    <Transition name="fade">
      <div v-if="addPlace.open" class="addp-overlay" @click.self="closeAddPlace">
        <div class="addp-sheet">
          <div class="addp-head">
            <h3 class="addp-title">{{ addPlace.dayNo }}일차에 장소 추가</h3>
            <button class="addp-close" @click="closeAddPlace" aria-label="닫기">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" /></svg>
            </button>
          </div>
          <div class="addp-search">
            <input
              v-model="addPlace.query"
              class="addp-input"
              type="search"
              placeholder="장소 이름 검색 (예: 경복궁, 해운대)"
              enterkeyhint="search"
              @keydown.enter="searchPlaces"
            />
            <button class="addp-search-btn" :disabled="addPlace.searching || !addPlace.query.trim()" @click="searchPlaces">
              {{ addPlace.searching ? '검색 중…' : '검색' }}
            </button>
          </div>
          <p v-if="addPlace.error" class="addp-error">{{ addPlace.error }}</p>
          <div class="addp-results">
            <div v-if="addPlace.searching" class="addp-hint">검색 중…</div>
            <div v-else-if="addPlace.searched && !addPlace.results.length" class="addp-hint">검색 결과가 없어요.</div>
            <button
              v-for="item in addPlace.results"
              :key="item.contentId"
              class="addp-result"
              :disabled="addPlace.adding"
              @click="addPlaceToDay(item)"
            >
              <img v-if="item.firstimage" :src="item.firstimage" class="addp-thumb" alt="" loading="lazy" @error="(e) => (e.target.style.display = 'none')" />
              <span v-else class="addp-thumb addp-thumb-ph">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z" /><circle cx="12" cy="10" r="3" /></svg>
              </span>
              <span class="addp-result-body">
                <span class="addp-result-name">{{ item.title }}</span>
                <span v-if="item.addr1" class="addp-result-addr">{{ item.addr1 }}</span>
              </span>
              <svg class="addp-plus" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--color-peach)" stroke-width="2.4" stroke-linecap="round"><line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" /></svg>
            </button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- 장소 행 액션 시트 — 다른 날로 이동 / 삭제 -->
    <Transition name="fade">
      <div v-if="rowSheet.open" class="rs-overlay" @click.self="closeRowSheet">
        <div class="rs-sheet">
          <div class="rs-head">
            <button v-if="rowSheet.step !== 'menu'" class="rs-back" aria-label="뒤로" @click="rowSheet.step = 'menu'">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M15 18l-6-6 6-6" /></svg>
            </button>
            <span class="rs-title">{{ rowSheet.step === 'move' ? '며칠차로 옮길까요?' : rowSheet.step === 'time' ? '방문 시간' : (rowSheet.place?.attraction?.title ?? '장소') }}</span>
            <button class="rs-close" aria-label="닫기" @click="closeRowSheet">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" /></svg>
            </button>
          </div>

          <p v-if="rowSheet.error" class="rs-error">{{ rowSheet.error }}</p>

          <!-- 메뉴 -->
          <div v-if="rowSheet.step === 'menu'" class="rs-menu">
            <button class="rs-item" :disabled="rowSheet.moving" @click="openTimeStep">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9" /><polyline points="12 7 12 12 15 14" /></svg>
              방문 시간 설정
              <span v-if="rowSheet.place?.visitTime" class="rs-item-val">{{ shortTime(rowSheet.place.visitTime) }}</span>
            </button>
            <button
              v-if="moveTargets.length"
              class="rs-item"
              :disabled="rowSheet.moving"
              @click="rowSheet.step = 'move'"
            >
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="17 1 21 5 17 9" /><path d="M3 11V9a4 4 0 0 1 4-4h14" /><polyline points="7 23 3 19 7 15" /><path d="M21 13v2a4 4 0 0 1-4 4H3" /></svg>
              다른 날로 이동
            </button>
            <button class="rs-item danger" :disabled="rowSheet.moving" @click="removeFromSheet">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6" /><path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2" /></svg>
              삭제
            </button>
          </div>

          <!-- 방문 시간 -->
          <div v-else-if="rowSheet.step === 'time'" class="rs-time">
            <div class="rs-time-chips">
              <button
                v-for="q in timeQuickChips"
                :key="q.label"
                :class="['rs-time-chip', { active: rowSheet.time === q.value }]"
                @click="rowSheet.time = q.value"
              >{{ q.label }}<span class="rs-time-chip-sub">{{ q.value }}</span></button>
            </div>
            <input v-model="rowSheet.time" type="time" class="rs-time-input" />
            <div class="rs-time-actions">
              <button class="rs-time-clear" :disabled="rowSheet.moving" @click="confirmTime(null)">시간 지우기</button>
              <button class="rs-time-save" :disabled="rowSheet.moving || !rowSheet.time" @click="confirmTime(rowSheet.time)">
                {{ rowSheet.moving ? '저장 중…' : '저장' }}
              </button>
            </div>
          </div>

          <!-- 일차 선택 -->
          <div v-else class="rs-days">
            <button
              v-for="dn in moveTargets"
              :key="dn"
              class="rs-day-chip"
              :disabled="rowSheet.moving"
              @click="confirmMove(dn)"
            >{{ dn }}일차</button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- 대표 이미지 업로드용 숨김 파일 입력 -->
    <input ref="planImgInput" type="file" accept="image/*" class="hidden-file" @change="onPlanImageChange" />

    <!-- 편집 토스트(순서변경/이동 결과) -->
    <Transition name="fade">
      <div v-if="planToast.show" class="plan-toast">{{ planToast.text }}</div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useRouter } from 'vue-router'
import { usePlanStore } from '@/stores/plan.js'
import { useCompanionStore } from '@/stores/companion.js'
import { planApi, attractionApi, communityApi } from '@/api/index.js'
import TripMap from '@/components/common/TripMap.vue'
import { useConfirm } from '@/composables/useConfirm.js'

const $confirm = useConfirm().confirm

const router = useRouter()
const planStore = usePlanStore()
const companionStore = useCompanionStore()
// storeToRefs로 반응성 유지 — 비반응적 destructure 시 loadPlans() 후 목록이 갱신되지 않음
const { plans } = storeToRefs(planStore)
// 동행 목록도 store 기준으로 반응성 유지(하드코딩 목업 제거 → 실 API 연동)
const { companions } = storeToRefs(companionStore)

const selectedPlanId = ref(null)

// ── 계획 목록 로드 상태 ───────────────────────────────────────────────────────
// planStore.loading은 상세/편집 등에서도 켜지므로, 목록 로드 전용 상태를 따로 둔다.
// 실패를 "계획 없어요" 빈상태로 위장하지 않고 에러/로딩/빈상태를 분리해 노출한다.
const listLoading = ref(false)
const listError = ref(null)

// ── 예산 보기 ────────────────────────────────────────────────────────────────
const budget = ref(null)        // { planId, dayBudgets, totalEstimated, plannedBudget, difference, note }
const budgetLoading = ref(false)

// ── 공유 ─────────────────────────────────────────────────────────────────────
const shareInfo = ref(null)     // { planId, url }
const shareLoading = ref(false)
const shareCopied = ref(false)

// ── 비교 ─────────────────────────────────────────────────────────────────────
const COMPARE_MAX = 5              // 한 번에 비교 가능한 최대 계획 수
const compareMode = ref(false)
const compareSelection = ref([])   // 선택된 planId (2~COMPARE_MAX개)
const compareResult = ref(null)    // PlanStat[] (요청한 순서 유지) 또는 null
const compareLoading = ref(false)

// ── 길찾기(도로 경로) ──────────────────────────────────────────────────────────
// route-path 응답: { planId, enabled, days:[{ dayNo, distanceMeters, durationSeconds, path:[[lat,lng],...] }] }
const routePath = ref(null)        // { planId, enabled, days }
const routePathLoading = ref(false)
const selectedDay = ref(1)         // 지도에 표시 중인 일차(1부터). 계획 펼칠 때 첫 일차로 초기화

// ── 장소 드래그 정렬 상태 ──────────────────────────────────────────────────────
const dragDay = ref(null)          // 드래그 중인 일차 dayNo (같은 일차 내에서만 정렬 허용)
const dragFrom = ref(null)         // 드래그 시작 인덱스
const dragOver = ref(null)         // 현재 드롭 후보 'dayNo:idx' (삽입 위치 하이라이트용)

// ── 장소 추가(검색 바텀시트) ───────────────────────────────────────────────────
const addPlace = ref({
  open: false,
  planId: null,
  dayNo: null,
  query: '',
  results: [],
  searching: false,
  searched: false,
  adding: false,
  error: '',
})

function openAddPlace(planId, dayNo) {
  addPlace.value = {
    open: true, planId, dayNo,
    query: '', results: [], searching: false, searched: false, adding: false, error: '',
  }
}
function closeAddPlace() {
  addPlace.value.open = false
}

// ── 장소 행 액션(다른 날로 이동·삭제) + 편집 토스트 ────────────────────────────
const rowSheet = ref({ open: false, planId: null, dayNo: null, place: null, step: 'menu', moving: false, error: '', time: '' })
const timeQuickChips = [
  { label: '오전', value: '09:00' },
  { label: '점심', value: '12:00' },
  { label: '오후', value: '15:00' },
  { label: '저녁', value: '19:00' },
]
// 'HH:mm:ss' → 'HH:mm'
function shortTime(t) {
  return t ? String(t).slice(0, 5) : ''
}
const planToast = ref({ show: false, text: '' })
let planToastTimer = null

// 편집 도움말 — 세션당 한 번만
const HINT_KEY = 'triip.planEditHintSeen'
const showEditHint = ref((() => { try { return !sessionStorage.getItem(HINT_KEY) } catch { return true } })())
function dismissEditHint() {
  showEditHint.value = false
  try { sessionStorage.setItem(HINT_KEY, '1') } catch { /* noop */ }
}

function showPlanToast(text) {
  planToast.value = { show: true, text }
  if (planToastTimer) clearTimeout(planToastTimer)
  planToastTimer = setTimeout(() => { planToast.value.show = false }, 2600)
}

// ── 계획 대표 이미지 변경 ───────────────────────────────────────────────────────
// 펼친 계획 카드의 썸네일에서 사진을 고르면 /community/images 로 업로드 후
// PATCH /api/plans/{id} (imageUrl)로 저장한다. 업로드 엔드포인트는 동행/커뮤니티와 동일.
const planImgInput = ref(null)
const imageUploading = ref(false)
const imageEditPlanId = ref(null)   // 현재 업로드 대상 계획 id(버튼 라벨 표시용)

/** 사진 선택 트리거 — 어떤 계획에 적용할지 기억하고 파일 picker를 연다. */
function pickPlanImage(plan) {
  if (imageUploading.value) return
  imageEditPlanId.value = plan.id
  planImgInput.value?.click()
}

/** 파일 선택 → 업로드 → 계획 PATCH(imageUrl) → 목록/상세 반영 */
async function onPlanImageChange(e) {
  const file = e.target.files?.[0]
  e.target.value = ''   // 같은 파일 재선택 허용
  const planId = imageEditPlanId.value
  if (!file || planId == null) return
  if (file.size > 10 * 1024 * 1024) {
    showPlanToast('이미지 크기는 10MB를 초과할 수 없어요.')
    return
  }
  imageUploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', file)
    const { data } = await communityApi.uploadImage(fd)
    const imageUrl = data?.imageUrl
    if (!imageUrl) {
      showPlanToast('업로드 응답이 올바르지 않아요.')
      return
    }
    // 낙관적 버전: 상세가 로드돼 있으면 그 version을, 없으면 목록의 version을 사용
    const fromList = plans.value.find((p) => p.id === planId)
    const expectedVersion = (planStore.current?.id === planId
      ? planStore.current.version
      : fromList?.version)
    await planApi.updatePlan(planId, {
      expectedVersion,
      title: fromList?.title ?? planStore.current?.title,
      startDate: fromList?.startDate ?? planStore.current?.startDate,
      endDate: fromList?.endDate ?? planStore.current?.endDate,
      companions: fromList?.companions ?? planStore.current?.companions ?? null,
      budget: fromList?.budget ?? planStore.current?.budget ?? null,
      imageUrl,
    })
    // 목록/상세 새로고침으로 새 imageUrl 반영
    await planStore.loadPlans()
    if (planStore.current?.id === planId) await planStore.loadPlan(planId)
    showPlanToast('대표 이미지를 변경했어요')
  } catch (err) {
    showPlanToast(err?.response?.data?.message ?? '이미지 변경에 실패했어요.')
  } finally {
    imageUploading.value = false
    imageEditPlanId.value = null
  }
}

// 이동 가능한 일차(현재 일차 제외)
const moveTargets = computed(() =>
  planDayNumbers.value.filter((dn) => dn !== rowSheet.value.dayNo),
)

function openRowSheet(planId, dayNo, place) {
  rowSheet.value = { open: true, planId, dayNo, place, step: 'menu', moving: false, error: '', time: shortTime(place?.visitTime) }
}
function closeRowSheet() {
  rowSheet.value.open = false
}

function openTimeStep() {
  rowSheet.value.time = shortTime(rowSheet.value.place?.visitTime)
  rowSheet.value.step = 'time'
}

async function confirmTime(time) {
  const s = rowSheet.value
  if (s.moving || !s.place) return
  s.moving = true
  s.error = ''
  try {
    await planStore.setPlaceVisitTime(s.planId, s.dayNo, s.place.id, time || null)
    closeRowSheet()
    showPlanToast(time ? `방문 시간 ${time} 설정` : '방문 시간을 지웠어요')
  } catch {
    s.error = planStore.error ?? '시간 저장에 실패했어요.'
  } finally {
    s.moving = false
  }
}

async function removeFromSheet() {
  const s = rowSheet.value
  if (s.moving || !s.place) return
  s.moving = true
  s.error = ''
  try {
    await removePlace(s.planId, s.dayNo, s.place)
    closeRowSheet()
    showPlanToast('장소를 삭제했어요')
  } catch {
    s.error = planStore.error ?? '삭제에 실패했어요.'
  } finally {
    s.moving = false
  }
}

async function confirmMove(toDay) {
  const s = rowSheet.value
  if (s.moving || !s.place) return
  s.moving = true
  s.error = ''
  try {
    await planStore.movePlaceToDay(s.planId, s.dayNo, toDay, s.place)
    loadRoutePath(s.planId)
    selectedDay.value = toDay
    closeRowSheet()
    showPlanToast(`${toDay}일차로 옮겼어요`)
  } catch {
    s.error = planStore.error ?? '이동에 실패했어요.'
  } finally {
    s.moving = false
  }
}

async function searchPlaces() {
  const q = addPlace.value.query.trim()
  if (!q || addPlace.value.searching) return
  addPlace.value.searching = true
  addPlace.value.error = ''
  try {
    const { data } = await attractionApi.list({ keyword: q, size: 15, page: 1 })
    const raw = Array.isArray(data) ? data : (data?.items ?? data?.content ?? [])
    addPlace.value.results = Array.isArray(raw)
      ? raw.filter((i) => i && (i.contentId ?? i.contentid))
      : []
    addPlace.value.searched = true
  } catch (e) {
    addPlace.value.results = []
    addPlace.value.error = e?.response?.data?.message ?? '검색에 실패했어요. 잠시 후 다시 시도해 주세요.'
  } finally {
    addPlace.value.searching = false
  }
}

async function addPlaceToDay(item) {
  if (addPlace.value.adding) return
  const { planId, dayNo } = addPlace.value
  const contentId = item.contentId ?? item.contentid
  const contentType = Number(item.contentTypeId ?? item.contenttypeid ?? item.contentType)
  if (!contentId || !Number.isFinite(contentType)) {
    addPlace.value.error = '이 장소는 추가할 수 없어요(정보 부족).'
    return
  }
  // 같은 일차에 이미 있는 장소면 API 호출 없이 안내
  const day = (planStore.current?.days ?? []).find((d) => d.dayNo === dayNo)
  const dup = (day?.places ?? []).some(
    (p) => String(p.attraction?.contentId ?? p.contentId ?? '') === String(contentId),
  )
  if (dup) {
    addPlace.value.error = '이미 이 일차에 추가된 장소예요.'
    return
  }
  addPlace.value.adding = true
  addPlace.value.error = ''
  try {
    await planStore.addPlace(planId, dayNo, { contentId: String(contentId), contentType })
    if (planStore.error) {
      addPlace.value.error = planStore.error
      return
    }
    loadRoutePath(planId)   // 추가 후 도로 경로 갱신
    selectedDay.value = dayNo // 추가한 일차를 지도에 보여준다
    closeAddPlace()
  } catch (e) {
    addPlace.value.error = planStore.error ?? e?.response?.data?.message ?? '추가에 실패했어요.'
  } finally {
    addPlace.value.adding = false
  }
}

onMounted(() => {
  reloadPlans()
  // 실제 동행 목록 로드(하드코딩 목업 제거). 실패 시 store가 조용히 빈 배열 유지 → 빈상태 노출.
  companionStore.fetchCompanions()
})

/** 계획 목록 로드 — 로딩/에러를 분리 추적해 빈상태 위장을 막는다 */
async function reloadPlans() {
  if (listLoading.value) return
  listLoading.value = true
  listError.value = null
  try {
    await planStore.loadPlans()
    // store가 에러를 삼키고 plans를 []로 두는 경우(로그인 만료/서버 오류)를 에러로 노출
    if (planStore.error) listError.value = planStore.error
  } catch (e) {
    listError.value = planStore.error ?? e?.message ?? '계획을 불러오지 못했어요.'
  } finally {
    listLoading.value = false
  }
}

function formatDate(str) {
  if (!str) return ''
  const d = new Date(str)
  return `${d.getMonth() + 1}/${d.getDate()}`
}

function dayCount(start, end) {
  if (!start || !end) return 0
  return Math.round((new Date(end) - new Date(start)) / 86400000)
}

// ── 펼친 계획의 지도 마커 ─────────────────────────────────────────────────────
// planStore.current(상세)의 days[].places[].attraction(latitude/longitude)에서 좌표를 모은다.
// TripMap props 형태({ id, name, lat, lng })로 변환하고, 좌표 없는 항목은 제외한다.
// dayNo를 함께 담아 day별 구분(향후 색/라벨)에 활용한다.
const currentPlanPlaces = computed(() => {
  const cur = planStore.current
  if (!cur || cur.id !== selectedPlanId.value) return []
  const out = []
  for (const day of cur.days ?? []) {
    for (const place of day.places ?? []) {
      const a = place.attraction
      const lat = Number(a?.latitude)
      const lng = Number(a?.longitude)
      if (!Number.isFinite(lat) || !Number.isFinite(lng)) continue
      out.push({
        id: place.id ?? a?.contentId ?? `${day.dayNo}-${out.length}`,
        name: a?.title ?? place.title ?? '장소',
        lat,
        lng,
        dayNo: day.dayNo,
      })
    }
  }
  return out
})

// ── 일차 네비게이션 ───────────────────────────────────────────────────────────
// 펼친 계획의 일차 번호 목록(장소 유무와 무관하게 day 단위로 분리해서 보여준다).
const planDayNumbers = computed(() => {
  const cur = planStore.current
  if (!cur || cur.id !== selectedPlanId.value) return []
  return (cur.days ?? []).map((d) => d.dayNo)
})

// 선택한 일차의 장소만(지도 마커용).
const currentDayPlaces = computed(() =>
  currentPlanPlaces.value.filter((p) => p.dayNo === selectedDay.value),
)

// 선택한 일차의 도로 경로선([lat,lng]…). 그 날 도로경로가 없으면 빈 배열.
const currentDayPath = computed(() => {
  const rp = routePath.value
  if (!rp || rp.planId !== selectedPlanId.value || !rp.enabled) return []
  const day = (rp.days ?? []).find((d) => d.dayNo === selectedDay.value)
  return day?.path ?? []
})

// 지도에 그릴 동선 — 도로경로가 있으면 그것, 없으면(카카오 길찾기가 비도로 지점으로 실패한 날)
// 장소들을 순서대로 잇는 '직선 근사 동선'으로 폴백한다(항상 동선이 보이도록).
const currentDayLine = computed(() => {
  const road = currentDayPath.value
  if (road.length >= 2) return road
  const pts = currentDayPlaces.value
  if (pts.length >= 2) return pts.map((p) => [p.lat, p.lng])
  return []
})
// 직선 폴백일 때만 true → 지도에서 점선으로 표시(도로경로가 아님을 명확히).
const currentDayDashed = computed(
  () => currentDayPath.value.length < 2 && currentDayPlaces.value.length >= 2,
)

// 선택한 일차의 차량 거리/시간 요약. 경로 없으면 null.
const currentDaySummary = computed(() => {
  const rp = routePath.value
  if (!rp || rp.planId !== selectedPlanId.value || !rp.enabled) return null
  const day = (rp.days ?? []).find((d) => d.dayNo === selectedDay.value)
  if (!day || !day.distanceMeters) return null
  return {
    distanceKm: Math.round(day.distanceMeters / 100) / 10,
    durationMin: Math.round(day.durationSeconds / 60),
  }
})

const canPrevDay = computed(() => {
  const i = planDayNumbers.value.indexOf(selectedDay.value)
  return i > 0
})
const canNextDay = computed(() => {
  const i = planDayNumbers.value.indexOf(selectedDay.value)
  return i >= 0 && i < planDayNumbers.value.length - 1
})
function stepDay(delta) {
  const nums = planDayNumbers.value
  const i = nums.indexOf(selectedDay.value)
  const next = nums[i + delta]
  if (next != null) selectedDay.value = next
}

/** 펼친 계획의 도로 경로 조회. 좌표 2곳 미만/키 미설정이면 조용히 빈 경로 유지(마커만 표시). */
async function loadRoutePath(planId) {
  routePathLoading.value = true
  try {
    const { data } = await planApi.getRoutePath(planId)
    // 빠르게 다른 계획을 펼쳤다면(planId가 더 이상 선택된 계획이 아님) 결과를 버린다(레이스 방지).
    if (selectedPlanId.value === planId) routePath.value = data
  } catch {
    if (selectedPlanId.value === planId) routePath.value = null
  } finally {
    if (selectedPlanId.value === planId) routePathLoading.value = false
  }
}

/** 카카오맵으로 길안내 — 선택한 일차의 첫 장소를 목적지로 길찾기를 새 탭에 연다. */
function openKakaoNavi() {
  const first = currentDayPlaces.value[0]
  if (!first) return
  const name = encodeURIComponent(first.name || '목적지')
  // map.kakao.com 길찾기 링크: 도착지(이름,위도,경도) — 카카오맵 앱/웹이 길안내를 띄운다.
  window.open(`https://map.kakao.com/link/to/${name},${first.lat},${first.lng}`, '_blank', 'noopener')
}

// ── 동행 카드용 D-day 계산 ─────────────────────────────────────────────────────
// store companion의 dateRange(=travelDate, 'YYYY-MM-DD')에서 남은 일수를 구한다.
// 날짜 파싱 불가/과거면 null → 배지 미표시.
function companionDday(dateStr) {
  if (!dateStr) return null
  const target = new Date(dateStr)
  if (Number.isNaN(target.getTime())) return null
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  target.setHours(0, 0, 0, 0)
  const diff = Math.round((target - today) / 86400000)
  return diff >= 0 ? diff : null
}

/** Navigate to /ai/plan (new AI trip flow) */
function goNewTrip() {
  router.push('/ai/plan')
}

/** 카드 탭 — 비교 모드면 선택, 아니면 펼치기/접기 */
function onCardTap(plan) {
  if (compareMode.value) {
    toggleCompareSelect(plan.id)
    return
  }
  togglePlan(plan)
}

/** Expand/collapse a plan card and load its detail */
async function togglePlan(plan) {
  if (selectedPlanId.value === plan.id) {
    selectedPlanId.value = null
    return
  }
  selectedPlanId.value = plan.id
  selectedDay.value = 1   // 새 계획을 펼치면 1일차부터 보여준다
  // 다른 계획을 펼치면 이전 계획의 예산/공유/경로 패널은 감춘다
  if (budget.value && budget.value.planId !== plan.id) budget.value = null
  if (shareInfo.value && shareInfo.value.planId !== plan.id) shareInfo.value = null
  if (routePath.value && routePath.value.planId !== plan.id) routePath.value = null
  // Load plan detail if not already loaded or stale
  if (planStore.current?.id !== plan.id) {
    try {
      await planStore.loadPlan(plan.id)
    } catch {
      // error shown via planStore.error
    }
  }
  // 상세 로드 후 실제 첫 일차로 지도를 맞춘다(일차 번호가 1부터가 아닐 수 있음).
  if (planStore.current?.id === plan.id) {
    selectedDay.value = planStore.current?.days?.[0]?.dayNo ?? 1
  }
  // 도로 경로(길찾기) 조회 — 실패해도 마커는 그대로 표시
  loadRoutePath(plan.id)
}

/** Navigate to the 동선 리포트(평가) screen */
function goReport(planId) {
  router.push(`/plan/${planId}/report`)
}

/** 챗봇으로 수정 — AssistantView로 planId 쿼리와 함께 이동 */
function goAssistant(planId) {
  router.push({ path: '/assistant', query: { planId } })
}

/** 원(₩) 포맷 */
function formatWon(n) {
  if (n == null) return '-'
  return `${Number(n).toLocaleString('ko-KR')}원`
}

// ── 예산 보기 ────────────────────────────────────────────────────────────────
async function loadBudget(planId) {
  if (budgetLoading.value) return
  // 토글: 이미 같은 계획 예산이 열려있으면 닫는다
  if (budget.value && budget.value.planId === planId) {
    budget.value = null
    return
  }
  budgetLoading.value = true
  try {
    const { data } = await planApi.getBudget(planId)
    budget.value = data
  } catch {
    budget.value = null
  } finally {
    budgetLoading.value = false
  }
}

// ── 공유 ─────────────────────────────────────────────────────────────────────
async function sharePlan(planId) {
  if (shareLoading.value) return
  // 토글: 이미 같은 계획 공유링크가 열려있으면 닫는다
  if (shareInfo.value && shareInfo.value.planId === planId) {
    shareInfo.value = null
    return
  }
  shareLoading.value = true
  shareCopied.value = false
  try {
    const { data } = await planApi.share(planId)
    // BE는 상대경로(/plan/shared/{token})를 주므로 절대 URL로 변환
    const path = data?.shareUrl ?? `/plan/shared/${data?.shareToken}`
    const url = `${window.location.origin}${path}`
    shareInfo.value = { planId, url }
    // 생성 직후 클립보드에 자동 복사 시도
    await copyToClipboard(url)
  } catch {
    shareInfo.value = null
  } finally {
    shareLoading.value = false
  }
}

async function copyShareUrl() {
  if (!shareInfo.value) return
  await copyToClipboard(shareInfo.value.url)
}

async function copyToClipboard(text) {
  try {
    await navigator.clipboard.writeText(text)
    shareCopied.value = true
    setTimeout(() => { shareCopied.value = false }, 1500)
  } catch {
    // 클립보드 접근 불가 — 사용자가 링크를 직접 복사하면 됨
  }
}

// ── 비교 ─────────────────────────────────────────────────────────────────────
function toggleCompareMode() {
  compareMode.value = !compareMode.value
  compareSelection.value = []
  compareResult.value = null
  compareLoading.value = false
  if (compareMode.value) {
    // 비교 모드 진입 시 펼친 카드/패널 정리
    selectedPlanId.value = null
    budget.value = null
    shareInfo.value = null
  }
}

function isCompareSelected(planId) {
  return compareSelection.value.includes(planId)
}

function toggleCompareSelect(planId) {
  const idx = compareSelection.value.indexOf(planId)
  if (idx >= 0) {
    compareSelection.value.splice(idx, 1)
    return
  }
  if (compareSelection.value.length >= COMPARE_MAX) return  // 최대 COMPARE_MAX개
  compareSelection.value.push(planId)
  // 자동 실행하지 않고 "비교하기" 버튼으로 실행 — N개 선택을 마칠 시간을 준다.
}

async function runCompare() {
  if (compareSelection.value.length < 2 || compareLoading.value) return
  compareLoading.value = true
  try {
    const { data } = await planApi.compareMany(compareSelection.value)
    // 백엔드 응답: { plans: PlanStat[] }
    compareResult.value = Array.isArray(data?.plans) ? data.plans : []
  } catch {
    compareResult.value = null
  } finally {
    compareLoading.value = false
  }
}

function closeCompare() {
  compareResult.value = null
  // 모달만 닫고 비교 모드는 유지 — 선택은 초기화해 다시 고를 수 있게
  compareSelection.value = []
}

/**
 * 비교 표 행 — 지표별로 N개 계획의 값을 만들고, 각 행에서 최적값을 강조한다.
 * lowerBetter=true 인 지표(이동거리·소요)는 최소값이, false(장소 수)는 최대값이 best.
 * 동률이거나 비교 의미가 없는 지표(일정·예산)는 강조하지 않는다.
 */
const compareRows = computed(() => {
  const list = compareResult.value
  if (!Array.isArray(list) || !list.length) return []
  const dur = (m) => {
    const h = Math.floor(m / 60)
    const min = m % 60
    return h > 0 ? `${h}시간${min > 0 ? ' ' + min + '분' : ''}` : `${min}분`
  }
  // metric: 각 계획에서 수치를 뽑는 함수, format: 표시 문자열, lowerBetter: 작을수록 좋은지(null=강조 없음)
  const defs = [
    { key: 'days', label: '일정', metric: (p) => p.totalDays, format: (p) => `${p.totalDays}일`, lowerBetter: null },
    { key: 'places', label: '장소 수', metric: (p) => p.totalPlaces, format: (p) => `${p.totalPlaces}곳`, lowerBetter: false },
    { key: 'dist', label: '총 이동거리', metric: (p) => p.totalDistanceKm, format: (p) => `${p.totalDistanceKm}km`, lowerBetter: true },
    { key: 'dur', label: '예상 소요', metric: (p) => p.totalDurationMin, format: (p) => dur(p.totalDurationMin), lowerBetter: true },
    { key: 'budget', label: '예산', metric: (p) => p.budget, format: (p) => (p.budget != null ? formatWon(p.budget) : '-'), lowerBetter: null },
  ]
  return defs.map((def) => {
    let bestVal = null
    if (def.lowerBetter != null) {
      const nums = list.map(def.metric).filter((v) => v != null && Number.isFinite(v))
      if (nums.length) bestVal = def.lowerBetter ? Math.min(...nums) : Math.max(...nums)
    }
    // 최적값이 둘 이상이면(동률) 강조하지 않는다.
    const bestCount = bestVal == null ? 0 : list.filter((p) => def.metric(p) === bestVal).length
    const cells = list.map((p) => {
      const v = def.metric(p)
      const best = bestVal != null && bestCount === 1 && v === bestVal
      return { planId: p.planId, text: def.format(p), best }
    })
    return { key: def.key, label: def.label, cells }
  })
})

/** 여행 계획 삭제 — 확인 후 deletePlan 호출 */
async function confirmDeletePlan(plan) {
  if (!await $confirm(`'${plan.title}' 여행 계획을 삭제할까요?`)) return
  try {
    await planStore.deletePlan(plan.id)
    if (selectedPlanId.value === plan.id) selectedPlanId.value = null
  } catch {
    // 오류는 planStore.error에 반영됨
  }
}

// ── 드래그 정렬 (포인터 기반: 마우스 + 터치, 같은 일차 내에서만) ──────────────────
// HTML5 draggable은 모바일 터치에서 동작하지 않으므로 Pointer Events로 직접 구현한다.
// 핸들에서 pointerdown → window pointermove로 현재 행 추적 → pointerup에서 확정.
let pointerActive = false

function onHandlePointerDown(day, idx, e) {
  if (planStore.loading) return
  if (e.pointerType === 'mouse' && e.button !== 0) return // 마우스는 좌클릭만
  e.preventDefault() // 마우스: 텍스트 선택/네이티브 드래그 방지
  // 포인터 캡처 — 터치에서 브라우저가 이 제스처를 '스크롤'로 가로채 pointercancel 시키는 것을 막는다.
  // (이게 없으면 실제 폰에서 손가락을 떼기도 전에 드래그가 취소돼 "안 되는 것처럼" 보인다.)
  try { e.currentTarget.setPointerCapture(e.pointerId) } catch { /* 미지원 브라우저 무시 */ }
  dragDay.value = day.dayNo
  dragFrom.value = idx
  dragOver.value = day.dayNo + ':' + idx
  pointerActive = true
  // passive:false 라야 pointermove에서 preventDefault(스크롤 차단)가 먹힌다.
  window.addEventListener('pointermove', onPointerMove, { passive: false })
  window.addEventListener('pointerup', onPointerUp, { once: true })
  window.addEventListener('pointercancel', onPointerUp, { once: true })
}

function onPointerMove(e) {
  if (!pointerActive) return
  e.preventDefault() // 드래그 중 화면 스크롤 방지(터치)
  const el = document.elementFromPoint(e.clientX, e.clientY)
  const row = el && el.closest('[data-place-row]')
  if (!row) return
  const dayNo = Number(row.getAttribute('data-day'))
  const idx = Number(row.getAttribute('data-idx'))
  if (dayNo !== dragDay.value) return // 같은 일차 안에서만 정렬
  dragOver.value = dayNo + ':' + idx
}

async function onPointerUp() {
  pointerActive = false
  window.removeEventListener('pointermove', onPointerMove)
  // 둘 다 명시적으로 제거 — {once:true}는 '발생한' 이벤트만 자동 해제하므로
  // (pointerup이 터지면 pointercancel이, 반대면 pointerup이) 한쪽이 남는다.
  window.removeEventListener('pointerup', onPointerUp)
  window.removeEventListener('pointercancel', onPointerUp)
  const from = dragFrom.value
  const day = dragDay.value
  let toIdx = null
  if (dragOver.value) {
    const [d, i] = dragOver.value.split(':')
    if (Number(d) === day) toIdx = Number(i)
  }
  clearDrag()
  if (from == null || toIdx == null || from === toIdx || day == null) return
  const cur = planStore.current
  const dayObj = (cur?.days ?? []).find((dd) => dd.dayNo === day)
  if (!cur || !dayObj) return
  const places = [...(dayObj.places ?? [])]
  const [moved] = places.splice(from, 1)
  places.splice(toIdx, 0, moved)
  try {
    // 낙관적 — 화면은 즉시 바뀌고 저장은 백그라운드(목록이 "불러오는 중"으로 안 사라짐)
    await planStore.reorderDayPlaces(cur.id, day, places)
    loadRoutePath(cur.id) // 순서 변경 → 도로 경로 갱신(백그라운드)
    showPlanToast('순서를 변경했어요')
  } catch {
    // 실패 시 자동 되돌림 + 안내
    showPlanToast(planStore.error || '순서 저장에 실패했어요')
  }
}

function clearDrag() {
  dragDay.value = null
  dragFrom.value = null
  dragOver.value = null
}

// 드래그 도중 화면을 떠나면(컴포넌트 언마운트) window 리스너가 남지 않게 정리한다.
onUnmounted(() => {
  window.removeEventListener('pointermove', onPointerMove)
  window.removeEventListener('pointerup', onPointerUp)
  window.removeEventListener('pointercancel', onPointerUp)
})

/** 장소 한 개 삭제 */
async function removePlace(planId, dayNo, place) {
  if (place.id == null) return
  try {
    await planStore.removePlace(planId, dayNo, place.id)
    loadRoutePath(planId) // 장소 변경 → 도로 경로 갱신
  } catch {
    // 오류는 planStore.error에 반영됨
  }
}
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  background: var(--color-white);
}

.plan-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 52px 20px 16px;
}

.header-title {
  font-size: 22px;
  font-weight: 800;
  color: var(--color-ink);
  letter-spacing: -0.5px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.compare-toggle {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--color-surface);
  color: var(--color-ink-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.15s, color 0.15s;
}

.compare-toggle.active {
  background: var(--color-peach);
  color: white;
}

.add-btn {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--color-peach);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* ── Compare mode ─────────────────────────────────────────────────────────── */
.compare-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 20px;
  background: var(--color-peach-light);
  border-bottom: 1px solid rgba(247, 143, 87, 0.15);
}

.compare-banner-text {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--color-peach-pressed);
}

.compare-banner-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.compare-banner-run {
  font-size: 12.5px;
  font-weight: 700;
  color: #fff;
  background: var(--color-peach);
  padding: 6px 14px;
  border-radius: var(--radius-full);
  cursor: pointer;
  transition: opacity 0.15s;
}
.compare-banner-run:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.compare-banner-cancel {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--color-ink-muted);
}

.plan-card.selectable {
  outline: 2px solid transparent;
  transition: outline-color 0.15s;
}

.plan-card.selected {
  outline-color: var(--color-peach);
}

.compare-check {
  position: absolute;
  top: 10px;
  right: 12px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: 2px solid rgba(255, 255, 255, 0.8);
  background: rgba(0, 0, 0, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-peach);
  z-index: 2;
}

.compare-check.on {
  background: white;
  border-color: white;
}

.scroll-content {
  flex: 1;
  overflow-y: auto;
  padding: 0 20px;
}

/* ── Loading / error state ──────────────────────────────────────────────── */
.state-block {
  padding: 16px 0 40px;
}

.skeleton-card {
  height: 96px;
  border-radius: var(--radius-lg);
  background: var(--color-surface);
  margin-bottom: 12px;
  animation: plan-shimmer 1.2s infinite;
}

@keyframes plan-shimmer {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.45; }
}

.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 0 40px;
  gap: 8px;
}

/* ── Empty state ─────────────────────────────────────────────────────────── */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 0 40px;
  gap: 8px;
}

.empty-icon {
  margin-bottom: 8px;
}

.empty-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-ink);
  letter-spacing: -0.3px;
}

.empty-sub {
  font-size: 13.5px;
  color: var(--color-ink-muted);
  margin-bottom: 16px;
}

.create-ai-btn {
  background: linear-gradient(90deg, var(--color-peach) 0%, #f9a96a 100%);
  color: white;
  font-size: 14px;
  font-weight: 700;
  padding: 13px 28px;
  border-radius: var(--radius-full);
  letter-spacing: -0.2px;
  display: flex;
  align-items: center;
  gap: 8px;
  box-shadow: 0 4px 14px rgba(247, 143, 87, 0.3);
}

.create-ai-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  box-shadow: none;
}

/* ── Plan list ────────────────────────────────────────────────────────────── */
.plan-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-bottom: 24px;
}

.plan-card {
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-card);
  cursor: pointer;
}

.plan-thumb {
  height: 100px;
  background: linear-gradient(135deg, #f78f57 0%, #e0743a 100%);
  position: relative;
  display: flex;
  align-items: flex-end;
  padding: 12px 16px;
  overflow: hidden;
}

/* 대표 이미지 — 썸네일 배경을 채운다. 깨지면 onerror로 숨겨 기본 그라데이션이 드러난다. */
.thumb-img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  z-index: 0;
}

.thumb-gradient {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, transparent 40%, rgba(0, 0, 0, 0.25) 100%);
  z-index: 1;
}
/* 이미지가 있으면 텍스트 가독성을 위해 하단 그늘을 더 진하게 */
.plan-thumb.has-img .thumb-gradient {
  background: linear-gradient(180deg, rgba(0, 0, 0, 0.05) 0%, rgba(0, 0, 0, 0.45) 100%);
}

/* 대표 이미지 변경 버튼 — 썸네일 우상단 */
.thumb-edit-btn {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 2;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 5px 10px;
  border-radius: var(--radius-full);
  background: rgba(0, 0, 0, 0.45);
  color: #fff;
  font-size: 11.5px;
  font-weight: 700;
  backdrop-filter: blur(3px);
  cursor: pointer;
}
.thumb-edit-btn:disabled { opacity: 0.6; cursor: not-allowed; }

.hidden-file { position: absolute; width: 1px; height: 1px; opacity: 0; pointer-events: none; }

.plan-dates {
  display: flex;
  align-items: center;
  gap: 6px;
  position: relative;
  z-index: 2;
}

.date-label {
  font-size: 13px;
  font-weight: 600;
  color: white;
}

.date-sep {
  color: rgba(255, 255, 255, 0.7);
}

.plan-info {
  padding: 14px 16px 16px;
  background: var(--color-white);
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.plan-info-main {
  flex: 1;
  min-width: 0;
}

.plan-delete-btn {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-ink-muted);
  flex-shrink: 0;
  cursor: pointer;
}

.plan-delete-btn:hover {
  background: var(--color-peach-light);
  color: var(--color-error);
}

.plan-name {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-ink);
  letter-spacing: -0.3px;
  margin-bottom: 4px;
}

.plan-sub {
  font-size: 13px;
  color: var(--color-ink-muted);
  margin-bottom: 10px;
}

.plan-spots {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.spot-chip {
  background: var(--color-peach-light);
  color: var(--color-peach-pressed);
  font-size: 12px;
  font-weight: 500;
  padding: 4px 10px;
  border-radius: var(--radius-full);
}

/* ── Plan detail panel ────────────────────────────────────────────────────── */
.plan-detail {
  background: var(--color-surface);
  padding: 0 16px 16px;
  overflow: hidden;
}

.detail-divider {
  height: 1px;
  background: var(--color-line-light);
  margin-bottom: 14px;
}

.detail-loading {
  font-size: 13px;
  color: var(--color-ink-muted);
  text-align: center;
  padding: 12px 0;
}

.detail-day {
  margin-bottom: 14px;
}

.detail-day-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.detail-day-pill {
  background: var(--color-peach);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  padding: 3px 10px;
  border-radius: var(--radius-full);
}

.detail-day-summary {
  font-size: 12.5px;
  color: var(--color-ink-secondary);
}

.detail-places {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

/* TransitionGroup 래퍼 — 기존 .detail-places의 세로 스택 레이아웃을 이어받는다.
   position:relative 라야 leave 시 position:absolute로 빠지는 행이 이 안에 머문다. */
.detail-places-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
  position: relative;
}

.detail-place-row {
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--color-white);
  border-radius: var(--radius-md);
  padding: 9px 12px;
  box-shadow: var(--shadow-card);
  transition: opacity 0.12s, box-shadow 0.12s;
}

/* ── 장소 행 트랜지션 (이동/추가/삭제 시 부드럽게) ───────────────────────────── */
/* FLIP 이동 — 순서변경/다른 날 이동/삭제로 위치가 바뀌면 행이 미끄러진다 */
.place-move {
  transition: transform 0.32s cubic-bezier(0.22, 0.61, 0.36, 1);
}
/* 진입 — 위에서 살짝 슬라이드 + 페이드 인 */
.place-enter-active {
  transition: transform 0.28s cubic-bezier(0.22, 0.61, 0.36, 1), opacity 0.28s ease;
}
/* 이탈 — 페이드 + 슬라이드 아웃. position:absolute로 흐름에서 빠져 나머지가 자연스럽게 채워진다 */
.place-leave-active {
  transition: transform 0.26s ease, opacity 0.26s ease;
  position: absolute;
  width: 100%;
  z-index: 0;
}
.place-enter-from {
  opacity: 0;
  transform: translateY(-10px);
}
.place-leave-to {
  opacity: 0;
  transform: translateX(16px);
}
/* 드래그 중에는 트랜지션을 꺼 포인터 추적과 충돌(잔상/버벅임)을 막는다 */
.detail-places-group.is-dragging .detail-place-row,
.detail-places-group.is-dragging.place-move {
  transition: none;
}
/* 드래그 중인 행 — 들린 느낌(반투명 + 강조 테두리) */
.detail-place-row.dragging {
  opacity: 0.55;
  outline: 2px solid var(--color-peach);
  outline-offset: -2px;
}
/* 드롭 후보 — 위쪽에 굵은 주황 삽입선 */
.detail-place-row.drop-target {
  box-shadow: inset 0 4px 0 0 var(--color-peach), var(--shadow-card);
}

/* 드래그 핸들 — 탭하기 쉽게 키운 히트영역 + 또렷한 색 */
.drag-handle {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  margin-left: -4px;
  color: var(--color-ink-secondary);
  cursor: grab;
  flex-shrink: 0;
  touch-action: none;          /* 핸들에서 시작한 터치는 스크롤이 아닌 드래그로 처리 */
  border-radius: var(--radius-sm);
}
.drag-handle:hover { background: var(--color-surface); color: var(--color-peach-pressed); }
.drag-handle:active { cursor: grabbing; }

.detail-place-num {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: var(--color-peach-light);
  color: var(--color-peach-pressed);
  font-size: 11px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.detail-place-name {
  flex: 1;
  font-size: 13.5px;
  font-weight: 600;
  color: var(--color-ink);
  letter-spacing: -0.2px;
}

.detail-place-time {
  font-size: 11.5px;
  font-weight: 500;
  color: var(--color-peach);
  background: var(--color-peach-light);
  padding: 2px 8px;
  border-radius: var(--radius-full);
  white-space: nowrap;
}

.place-edit-actions {
  display: flex;
  align-items: center;
  gap: 2px;
  flex-shrink: 0;
}

.place-edit-btn {
  width: 26px;
  height: 26px;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-ink-muted);
  cursor: pointer;
}

.place-edit-btn:hover:not(:disabled) {
  background: var(--color-peach-light);
  color: var(--color-peach-pressed);
}

.place-edit-btn.danger:hover:not(:disabled) {
  color: var(--color-error);
}

.place-edit-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.detail-empty {
  font-size: 12.5px;
  color: var(--color-ink-muted);
  padding: 6px 0;
}

.detail-empty-plan {
  font-size: 13px;
  color: var(--color-ink-muted);
  text-align: center;
  padding: 8px 0 4px;
}

.detail-actions {
  display: flex;
  gap: 8px;
  margin-top: 14px;
}

.detail-action-btn {
  flex: 1;
  height: 42px;
  border-radius: var(--radius-xl);
  font-size: 13.5px;
  font-weight: 700;
  letter-spacing: -0.2px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  cursor: pointer;
}

.optimize-btn {
  background: linear-gradient(90deg, var(--color-peach) 0%, #f9a96a 100%);
  color: #fff;
  box-shadow: 0 3px 10px rgba(247, 143, 87, 0.3);
}

/* 챗봇으로 다듬기 — 주요 액션이지만 최적화 버튼과 위계 구분(보라 톤 그라데이션) */
.assistant-btn {
  background: linear-gradient(90deg, var(--color-ai, #7c5cf0) 0%, #9b7cf6 100%);
  color: #fff;
  box-shadow: 0 3px 10px rgba(124, 92, 240, 0.28);
}

.detail-actions.secondary {
  margin-top: 8px;
}

.detail-sub-btn {
  flex: 1;
  height: 38px;
  border-radius: var(--radius-xl);
  font-size: 12.5px;
  font-weight: 700;
  letter-spacing: -0.2px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  cursor: pointer;
  background: var(--color-white);
  color: var(--color-ink-secondary);
  border: 1px solid var(--color-line-light);
}

.detail-sub-btn:hover:not(:disabled) {
  background: var(--color-peach-light);
  color: var(--color-peach-pressed);
  border-color: var(--color-peach-light);
}

.detail-sub-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* ── Plan map (일정 지도) ─────────────────────────────────────────────────── */
.plan-map-section {
  margin-top: 14px;
}

/* ── 장소 추가 버튼 ─────────────────────────────────────────────────────────── */
.add-place-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100%;
  margin-top: 2px;
  padding: 10px;
  border: 1px dashed var(--color-line);
  border-radius: var(--radius-md);
  background: transparent;
  color: var(--color-peach-pressed);
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s;
}
.add-place-btn:hover:not(:disabled) {
  background: var(--color-peach-light);
  border-color: var(--color-peach);
}
.add-place-btn:disabled { opacity: 0.5; cursor: not-allowed; }

/* ── 장소 추가 바텀시트 ───────────────────────────────────────────────────────── */
.addp-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: flex-end;
  justify-content: center;
  z-index: 1000;
}
.addp-sheet {
  width: 100%;
  max-width: 480px;
  background: var(--color-white);
  border-radius: var(--radius-xl) var(--radius-xl) 0 0;
  padding: 16px 18px calc(env(safe-area-inset-bottom, 0px) + 18px);
  max-height: 78%;
  display: flex;
  flex-direction: column;
}
.addp-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.addp-title { font-size: 16px; font-weight: 800; color: var(--color-ink); letter-spacing: -0.3px; }
.addp-close {
  width: 32px; height: 32px;
  display: flex; align-items: center; justify-content: center;
  color: var(--color-ink-muted);
}
.addp-search { display: flex; gap: 8px; margin-bottom: 10px; }
.addp-input {
  flex: 1;
  padding: 10px 14px;
  background: var(--color-surface);
  border-radius: var(--radius-full);
  font-size: 14px;
  color: var(--color-ink);
}
.addp-input::placeholder { color: var(--color-ink-muted); }
.addp-search-btn {
  flex-shrink: 0;
  padding: 0 16px;
  border-radius: var(--radius-full);
  background: var(--color-peach);
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}
.addp-search-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.addp-error { font-size: 12.5px; color: var(--color-error); margin: 2px 2px 8px; }
.addp-results { flex: 1; overflow-y: auto; display: flex; flex-direction: column; gap: 6px; }
.addp-hint { font-size: 13px; color: var(--color-ink-muted); text-align: center; padding: 24px 0; }
.addp-result {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px;
  border-radius: var(--radius-md);
  background: var(--color-surface);
  text-align: left;
  cursor: pointer;
  transition: background 0.12s;
}
.addp-result:hover:not(:disabled) { background: var(--color-peach-light); }
.addp-result:disabled { opacity: 0.6; cursor: not-allowed; }
.addp-thumb {
  width: 44px; height: 44px;
  border-radius: var(--radius-sm);
  object-fit: cover;
  flex-shrink: 0;
}
.addp-thumb-ph {
  display: flex; align-items: center; justify-content: center;
  background: var(--color-white);
}
.addp-result-body { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.addp-result-name {
  font-size: 13.5px; font-weight: 700; color: var(--color-ink);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.addp-result-addr {
  font-size: 11.5px; color: var(--color-ink-muted);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.addp-plus { flex-shrink: 0; }

/* ── 일차 네비게이션 ─────────────────────────────────────────────────────────── */
.day-nav {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
}
.day-nav-arrow {
  width: 30px;
  height: 30px;
  flex-shrink: 0;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-ink-secondary);
  background: var(--color-surface);
  cursor: pointer;
}
.day-nav-arrow:disabled { opacity: 0.35; cursor: not-allowed; }
.day-chips {
  display: flex;
  gap: 6px;
  overflow-x: auto;
  flex: 1;
  scrollbar-width: none;
}
.day-chips::-webkit-scrollbar { display: none; }
.day-chip {
  flex-shrink: 0;
  font-size: 12.5px;
  font-weight: 700;
  padding: 6px 14px;
  border-radius: var(--radius-full);
  background: var(--color-surface);
  color: var(--color-ink-secondary);
  cursor: pointer;
}
.day-chip.active {
  background: var(--color-peach);
  color: #fff;
}
.day-empty-map {
  font-size: 12.5px;
  color: var(--color-ink-muted);
  text-align: center;
  padding: 24px 0;
  background: var(--color-surface);
  border-radius: var(--radius-md);
}

.plan-map-wrap {
  width: 100%;
  height: 200px;
  border-radius: var(--radius-md);
  overflow: hidden;
  border: 1px solid var(--color-line-light);
  background: var(--color-surface);
}

.map-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: 8px;
}

.route-summary {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-ink-secondary);
}

.route-summary.muted {
  color: var(--color-ink-muted);
  font-weight: 500;
}

.navi-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-left: auto;
  font-size: 12.5px;
  font-weight: 700;
  color: var(--color-peach-pressed);
  background: var(--color-peach-light);
  border-radius: var(--radius-full);
  padding: 7px 14px;
  cursor: pointer;
}

.navi-btn:hover {
  background: var(--color-peach);
  color: #fff;
}

/* ── Budget panel ─────────────────────────────────────────────────────────── */
.budget-panel {
  background: var(--color-white);
  border-radius: var(--radius-md);
  padding: 12px 14px;
  margin-top: 14px;
  box-shadow: var(--shadow-card);
}

.budget-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 10px;
}

.budget-title {
  font-size: 13px;
  font-weight: 700;
  color: var(--color-ink);
}

.budget-total {
  font-size: 16px;
  font-weight: 800;
  color: var(--color-peach-pressed);
  letter-spacing: -0.3px;
}

.budget-days {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.budget-day-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12.5px;
}

.budget-day-label {
  color: var(--color-ink-muted);
}

.budget-day-cost {
  color: var(--color-ink-secondary);
  font-weight: 600;
}

.budget-planned-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed var(--color-line-light);
  font-size: 12.5px;
  color: var(--color-ink-muted);
}

.budget-diff {
  font-weight: 700;
  color: var(--color-peach-pressed);
}

.budget-diff.over {
  color: var(--color-error);
}

.budget-note {
  font-size: 11px;
  color: var(--color-ink-muted);
  margin-top: 8px;
}

/* ── Share panel ──────────────────────────────────────────────────────────── */
.share-panel {
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--color-peach-light);
  border-radius: var(--radius-md);
  padding: 10px 12px;
  margin-top: 10px;
}

.share-icon {
  flex-shrink: 0;
  color: var(--color-peach-pressed);
  display: flex;
}

.share-url {
  flex: 1;
  min-width: 0;
  font-size: 11.5px;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  color: var(--color-ink-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.share-copy-btn {
  flex-shrink: 0;
  font-size: 12px;
  font-weight: 700;
  color: var(--color-peach-pressed);
  padding: 4px 8px;
}

/* ── Compare modal ────────────────────────────────────────────────────────── */
.compare-overlay {
  position: fixed;          /* .page(overflow:hidden)에 갇히지 않게 뷰포트 기준 */
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: flex-end;
  justify-content: center;
  z-index: 1000;            /* BottomNav(z-index:100) 위로 — 시트 하단이 가려지지 않게 */
}

.compare-sheet {
  width: 100%;
  background: var(--color-white);
  border-radius: var(--radius-xl) var(--radius-xl) 0 0;
  padding: 18px 20px calc(env(safe-area-inset-bottom, 0px) + 24px);
  max-height: 80%;
  overflow-y: auto;
}

.compare-sheet-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}

.compare-sheet-title {
  font-size: 17px;
  font-weight: 800;
  color: var(--color-ink);
  letter-spacing: -0.4px;
}

.compare-sheet-close {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-ink-muted);
}

/* N열 비교 표 — 가로 스크롤. 첫 열(지표 라벨)은 sticky로 고정 */
.compare-scroll {
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

.compare-table {
  border-collapse: collapse;
  width: 100%;
  min-width: max-content;
}

.compare-th-metric,
.compare-td-metric {
  position: sticky;
  left: 0;
  z-index: 1;
  background: var(--color-white);
  text-align: left;
  white-space: nowrap;
  padding: 8px 12px 8px 2px;
  font-size: 11.5px;
  font-weight: 600;
  color: var(--color-ink-muted);
}

.compare-th-plan {
  min-width: 96px;
  padding: 0 8px 8px;
  font-size: 12.5px;
  font-weight: 700;
  color: var(--color-ink);
  text-align: center;
  line-height: 1.35;
  border-bottom: 2px solid var(--color-peach-light);
  white-space: nowrap;
}

.compare-table tbody tr {
  border-bottom: 1px solid var(--color-line-light);
}

.compare-td-val {
  padding: 9px 8px;
  text-align: center;
  font-size: 13.5px;
  font-weight: 600;
  color: var(--color-ink-secondary);
  white-space: nowrap;
}

.compare-td-val.better {
  color: var(--color-peach-pressed);
  font-weight: 800;
}

/* fade transition for overlay */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* expand/collapse transition */
.expand-enter-active,
.expand-leave-active {
  transition: max-height 0.35s ease, opacity 0.25s ease;
  /* 지도+일차네비+예산+공유 패널이 들어가면 600px를 쉽게 넘겨 콘텐츠가 잘렸다.
     넉넉한 상한으로 올려 잘림을 막는다(트랜지션은 약간 빨라지지만 클리핑 없음). */
  max-height: 3000px;
}

.expand-enter-from,
.expand-leave-to {
  max-height: 0;
  opacity: 0;
}

/* ── Companion section ────────────────────────────────────────────────────── */
.companion-section {
  padding-bottom: 16px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.section-title {
  font-size: 17px;
  font-weight: 750;
  color: var(--color-ink);
  letter-spacing: -0.4px;
}

.see-all {
  font-size: 13px;
  color: var(--color-ink-muted);
}

.companion-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.companion-card {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  padding: 16px;
  cursor: pointer;
  overflow: hidden;
}

/* 동행 카드 대표 이미지 — 카드 패딩을 상쇄해 상단 전체 폭 배너로 */
.comp-thumb {
  margin: -16px -16px 12px;
  height: 96px;
  position: relative;
  overflow: hidden;
  background: var(--color-line-light);
}
.comp-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
/* 이미지 없음(또는 깨짐) — 기본 그라데이션 placeholder */
.comp-thumb.ph {
  background: linear-gradient(135deg, #f7b690 0%, #e89a6c 100%);
}

.comp-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.comp-badge {
  background: var(--color-peach-light);
  color: var(--color-peach-pressed);
  font-size: 11.5px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: var(--radius-full);
}

.comp-badge.urgent {
  background: #fff0e8;
  color: #d04010;
}

/* ── Companion empty state ────────────────────────────────────────────────── */
.companion-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 28px 16px;
  background: var(--color-surface);
  border-radius: var(--radius-lg);
}

.companion-empty-text {
  font-size: 13px;
  color: var(--color-ink-muted);
}

.companion-empty-btn {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-peach-pressed);
  background: var(--color-white);
  border: 1px solid var(--color-line-light);
  padding: 8px 18px;
  border-radius: var(--radius-full);
  cursor: pointer;
}

.comp-dday {
  font-size: 12px;
  font-weight: 700;
  color: var(--color-ink-muted);
}

.comp-dday.urgent {
  color: var(--color-error);
}

.comp-title {
  font-size: 14.5px;
  font-weight: 700;
  color: var(--color-ink);
  letter-spacing: -0.3px;
  margin-bottom: 4px;
}

.comp-sub {
  font-size: 12.5px;
  color: var(--color-ink-muted);
  margin-bottom: 12px;
}

.comp-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.comp-members {
  display: flex;
  align-items: center;
  gap: 4px;
}

.member-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--color-peach);
}

.member-text {
  font-size: 12px;
  color: var(--color-ink-muted);
  margin-left: 4px;
}

.join-btn {
  background: var(--color-peach);
  color: white;
  font-size: 13px;
  font-weight: 600;
  padding: 7px 16px;
  border-radius: var(--radius-full);
  letter-spacing: -0.2px;
}

.bottom-spacer {
  height: 24px;
}

/* ── 편집 도움말 ─────────────────────────────────────────────────────────────── */
.edit-hint {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  padding: 8px 10px 8px 12px;
  background: var(--color-peach-light);
  border-radius: var(--radius-md);
}
.edit-hint-text {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11.5px;
  color: var(--color-peach-pressed);
  letter-spacing: -0.2px;
}
.edit-hint-text strong { font-weight: 800; }
.edit-hint-close {
  width: 22px; height: 22px;
  display: flex; align-items: center; justify-content: center;
  color: var(--color-peach-pressed);
  flex-shrink: 0;
}

/* ── 장소 행 액션 시트 ───────────────────────────────────────────────────────── */
.rs-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: flex-end;
  justify-content: center;
  z-index: 1000;
}
.rs-sheet {
  width: 100%;
  max-width: 480px;
  background: var(--color-white);
  border-radius: var(--radius-xl) var(--radius-xl) 0 0;
  padding: 12px 16px calc(env(safe-area-inset-bottom, 0px) + 16px);
}
.rs-head {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
}
.rs-title {
  flex: 1;
  font-size: 15px;
  font-weight: 800;
  color: var(--color-ink);
  letter-spacing: -0.3px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.rs-back, .rs-close {
  width: 30px; height: 30px;
  display: flex; align-items: center; justify-content: center;
  color: var(--color-ink-muted);
  flex-shrink: 0;
}
.rs-error { font-size: 12px; color: var(--color-error); margin: 0 2px 8px; }
.rs-menu { display: flex; flex-direction: column; gap: 4px; }
.rs-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 10px;
  border-radius: var(--radius-md);
  font-size: 14.5px;
  font-weight: 600;
  color: var(--color-ink);
  text-align: left;
}
.rs-item:active { background: var(--color-surface); }
.rs-item.danger { color: var(--color-error); }
.rs-item:disabled { opacity: 0.5; }
.rs-item-val {
  margin-left: auto;
  font-size: 12.5px;
  font-weight: 700;
  color: var(--color-peach);
  background: var(--color-peach-light);
  padding: 2px 9px;
  border-radius: var(--radius-full);
}

/* ── 방문 시간 ───────────────────────────────────────────────────────────────── */
.rs-time { padding: 2px 0 4px; }
.rs-time-chips {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  margin-bottom: 12px;
}
.rs-time-chip {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 10px 4px;
  border-radius: var(--radius-md);
  background: var(--color-surface);
  border: 2px solid transparent;
  font-size: 13px;
  font-weight: 700;
  color: var(--color-ink);
}
.rs-time-chip.active { border-color: var(--color-peach); background: var(--color-peach-light); }
.rs-time-chip-sub { font-size: 10.5px; font-weight: 500; color: var(--color-ink-muted); font-family: var(--font-mono); }
.rs-time-input {
  width: 100%;
  height: 44px;
  border: 1px solid var(--color-line);
  border-radius: var(--radius-md);
  padding: 0 12px;
  font-size: 15px;
  color: var(--color-ink);
  background: var(--color-white);
  margin-bottom: 12px;
}
.rs-time-actions { display: flex; gap: 8px; }
.rs-time-clear {
  flex: 1;
  height: 44px;
  border-radius: var(--radius-xl);
  background: var(--color-white);
  border: 1px solid var(--color-line);
  font-size: 13.5px;
  font-weight: 700;
  color: var(--color-ink-secondary);
}
.rs-time-save {
  flex: 1.4;
  height: 44px;
  border-radius: var(--radius-xl);
  background: var(--color-peach);
  color: #fff;
  font-size: 14px;
  font-weight: 800;
}
.rs-time-save:disabled, .rs-time-clear:disabled { opacity: 0.5; }
.rs-days {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(72px, 1fr));
  gap: 8px;
  padding: 4px 0 6px;
}
.rs-day-chip {
  padding: 14px 8px;
  border-radius: var(--radius-md);
  background: var(--color-surface);
  border: 2px solid transparent;
  font-size: 13.5px;
  font-weight: 800;
  color: var(--color-ink);
}
.rs-day-chip:active { border-color: var(--color-peach); background: var(--color-peach-light); }
.rs-day-chip:disabled { opacity: 0.5; }

/* ── 편집 토스트 ─────────────────────────────────────────────────────────────── */
.plan-toast {
  position: fixed;
  left: 50%;
  bottom: calc(72px + env(safe-area-inset-bottom, 0px));
  transform: translateX(-50%);
  z-index: 1100;
  padding: 10px 18px;
  background: var(--color-ink);
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: -0.2px;
  border-radius: var(--radius-full);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.25);
  white-space: nowrap;
}
</style>
