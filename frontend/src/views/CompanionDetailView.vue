# Created: 2026-06-16 14:05:43
<template>
  <div class="page">
    <!-- Hero photo -->
    <div class="hero">
      <div class="hero-img" :style="heroStyle" />
      <div class="hero-overlay" />
      <div class="hero-top">
        <button class="ghost-btn" @click="$router.back()">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M19 12H5M12 5l-7 7 7 7" />
          </svg>
        </button>
        <div class="hero-top-right">
          <!-- 수정 화면(/companion/:id/edit) 라우트가 없어 깨진 이동을 방지하기 위해 수정 버튼 제거 -->
          <button class="ghost-btn" @click="share">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="18" cy="5" r="3" /><circle cx="6" cy="12" r="3" /><circle cx="18" cy="19" r="3" />
              <line x1="8.59" y1="13.51" x2="15.42" y2="17.49" /><line x1="15.41" y1="6.51" x2="8.59" y2="10.49" />
            </svg>
          </button>
        </div>
      </div>
    </div>

    <div class="content-scroll">
      <!-- 로딩 -->
      <div v-if="detailState === 'loading'" class="detail-state">
        <div class="detail-skeleton w70" />
        <div class="detail-skeleton w90" />
        <div class="detail-skeleton w50" />
      </div>

      <!-- 없는 글(404) -->
      <div v-else-if="detailState === 'not-found'" class="detail-state detail-state-msg">
        <div class="detail-state-icon">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="var(--color-line)" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" />
          </svg>
        </div>
        <p class="detail-state-title">모집글을 찾을 수 없어요</p>
        <p class="detail-state-sub">삭제되었거나 잘못된 링크일 수 있어요.</p>
        <button class="detail-state-btn" @click="$router.back()">돌아가기</button>
      </div>

      <!-- 로드 실패 -->
      <div v-else-if="detailState === 'error'" class="detail-state detail-state-msg">
        <div class="detail-state-icon">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="var(--color-line)" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10" /><line x1="12" y1="8" x2="12" y2="12" /><line x1="12" y1="16" x2="12.01" y2="16" />
          </svg>
        </div>
        <p class="detail-state-title">정보를 불러오지 못했어요</p>
        <p class="detail-state-sub">{{ companionStore.detailError ?? '잠시 후 다시 시도해 주세요.' }}</p>
        <button class="detail-state-btn" :disabled="companionStore.detailLoading" @click="reloadDetail">
          {{ companionStore.detailLoading ? '불러오는 중...' : '다시 시도' }}
        </button>
      </div>

      <!-- 정상 로드 -->
      <template v-else>
      <!-- Status & title -->
      <div class="title-area">
        <div class="badges-row">
          <span v-if="comp.isOwner" class="badge owner-badge">내 모집글 · 방장</span>
          <span v-else :class="['badge', comp.status === '마감임박' ? 'badge-urgent' : 'badge-open']">{{ comp.status }}</span>
          <span class="date-range">{{ comp.dateRange }}</span>
        </div>
        <h1 class="comp-title">{{ comp.title }}</h1>

        <!-- Author -->
        <div class="author-row">
          <div class="author-avatar">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2" /><circle cx="12" cy="7" r="4" />
            </svg>
          </div>
          <div class="author-info">
            <span class="author-name">{{ comp.author?.nickname }}</span>
            <span class="author-sub">방장 · 동행 {{ comp.author?.tripCount }}회</span>
          </div>
          <!-- 팔로우 버튼 — 방장 본인이 아닐 때만. 낙관적 토글 + 실패 롤백 -->
          <button
            v-if="!comp.isOwner && authorUserId != null"
            class="follow-btn"
            :class="{ following: isFollowing }"
            :disabled="followLoading"
            @click.stop="toggleFollow"
          >
            <svg v-if="isFollowing" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12" /></svg>
            <svg v-else width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" /></svg>
            {{ isFollowing ? '팔로잉' : '팔로우' }}
          </button>
          <span v-else-if="!comp.isOwner" class="seat-count">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2" /><circle cx="9" cy="7" r="4" /></svg>
            {{ comp.currentCount }}/{{ comp.maxCount }} 모집 인원
          </span>
        </div>

        <!-- Owner: applicant status -->
        <div v-if="comp.isOwner" class="owner-status-row">
          <span class="owner-status-label">신청 현황</span>
          <span class="owner-status-val">대기 {{ comp.pendingCount }} · 승인 {{ comp.approvedCount }} / 정원 {{ comp.maxCount }}</span>
          <div class="avatar-stack">
            <div v-for="n in Math.min(comp.approvedCount + comp.pendingCount, 3)" :key="n" class="stack-avatar">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2" /><circle cx="12" cy="7" r="4" />
              </svg>
            </div>
          </div>
        </div>
      </div>

      <!-- Pending state -->
      <div v-if="myApplicationStatus === 'PENDING' && !comp.isOwner" class="pending-banner">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--color-peach)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="10" /><polyline points="12 6 12 12 16 14" />
        </svg>
        <div>
          <div class="pending-title">승인 대기 중이에요</div>
          <div class="pending-sub">방장이 신청을 확인하면 채팅방에 입장할 수 있어요.</div>
        </div>
      </div>

      <!-- Approved state (origin) -->
      <div v-if="myApplicationStatus === 'APPROVED' && !comp.isOwner" class="approved-banner">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#2a7a4b" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M22 11.08V12a10 10 0 11-5.93-9.14" /><polyline points="22 4 12 14.01 9 11.01" />
        </svg>
        <div>
          <div class="approved-title">동행이 확정됐어요!</div>
          <div class="approved-sub">채팅방에서 일정을 조율해보세요.</div>
        </div>
      </div>

      <!-- 모집 조건 (HEAD) -->
      <div class="cond-head">
        <h3 class="section-title">모집 조건</h3>
        <span v-if="seatsLeft > 0" class="cond-seats">남은 자리 {{ seatsLeft }}명</span>
        <span v-else class="cond-seats cond-seats-full">모집 마감</span>
      </div>

      <!-- Info grid -->
      <div class="info-grid">
        <div class="info-cell">
          <div class="info-icon-row">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="2" stroke-linecap="round"><rect x="3" y="4" width="18" height="18" rx="2" /><path d="M16 2v4M8 2v4M3 10h18" /></svg>
            <span class="info-label">일정</span>
          </div>
          <span class="info-val">{{ comp.dateRange || '-' }}</span>
        </div>
        <div class="info-cell">
          <div class="info-icon-row">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="2" stroke-linecap="round"><circle cx="12" cy="12" r="10" /><polyline points="12 6 12 12 16 14" /></svg>
            <span class="info-label">기간</span>
          </div>
          <span class="info-val">{{ comp.period || '-' }}</span>
        </div>
        <div class="info-cell">
          <div class="info-icon-row">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="2" stroke-linecap="round"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z" /></svg>
            <span class="info-label">지역</span>
          </div>
          <span class="info-val">{{ comp.location || '-' }}</span>
        </div>
        <div class="info-cell">
          <div class="info-icon-row">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="2" stroke-linecap="round"><path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2" /><circle cx="9" cy="7" r="4" /></svg>
            <span class="info-label">모집 인원</span>
          </div>
          <span class="info-val">{{ comp.currentCount }}/{{ comp.maxCount }}명</span>
        </div>
        <div class="info-cell info-cell-wide">
          <div class="info-icon-row">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="2" stroke-linecap="round"><line x1="12" y1="1" x2="12" y2="23" /><path d="M17 5H9.5a3.5 3.5 0 000 7h5a3.5 3.5 0 010 7H6" /></svg>
            <span class="info-label">예상 비용</span>
          </div>
          <span class="info-val">{{ comp.estimatedCost || '-' }}</span>
        </div>
      </div>

      <!-- Tags -->
      <div v-if="comp.tags?.length" class="tags-row">
        <span v-for="tag in comp.tags" :key="tag" class="tag-chip">{{ tag }}</span>
      </div>

      <!-- Intro -->
      <div class="section">
        <h3 class="section-title">소개</h3>
        <p class="intro-text">{{ comp.intro }}</p>
      </div>

      <!-- 모집 위치/일정 지도 — 연결된 계획 장소(좌표) 또는 모집 위치 단일 마커 -->
      <!-- 표시할 좌표가 하나도 없으면 지도 섹션 자체를 숨긴다(가짜 핀 금지). -->
      <div v-if="hasMapPlaces" class="section plan-section">
        <div class="cond-head">
          <h3 class="section-title">{{ linkedPlan ? '정해진 일정' : '모집 위치' }}</h3>
          <span v-if="linkedPlan" class="plan-fixed-pill">계획 연동</span>
        </div>
        <p v-if="linkedPlan?.title" class="plan-meta">
          {{ linkedPlan.title }}
          <span v-if="planDateRange" class="plan-date">· {{ planDateRange }}</span>
        </p>

        <!-- Kakao 지도(공통 TripMap 컴포넌트) -->
        <div class="plan-map-wrap">
          <TripMap :places="tripMapPlaces" :numbered="hasPlaces" />
        </div>

        <!-- Day-by-day place list (연결된 계획이 있을 때만) -->
        <div v-for="day in placesByDay" :key="day.dayNo" class="plan-day">
          <div class="plan-day-head">
            <span class="plan-day-pill">{{ day.dayNo }}일차</span>
          </div>
          <div class="plan-route">
            <div
              v-for="(place, idx) in day.places"
              :key="`${day.dayNo}-${idx}`"
              class="plan-stop"
            >
              <div class="plan-stop-left">
                <div class="plan-stop-dot">{{ idx + 1 }}</div>
                <div v-if="idx < day.places.length - 1" class="plan-stop-line" />
              </div>
              <div class="plan-stop-name">{{ place.title }}</div>
            </div>
          </div>
        </div>
      </div>

      <div style="height: 100px" />
      </template>
    </div>

    <!-- Bottom CTA — 정상 로드된 경우에만 노출(가짜 신청 CTA 방지) -->
    <div v-if="detailState === 'ready'" class="cta-bar">
      <div v-if="applyError" class="apply-error">{{ applyError }}</div>
      <!-- Visitor: not applied — 남은 자리가 있을 때만 신청 CTA 노출 -->
      <template v-if="!comp.isOwner && !isApplied && seatsLeft > 0">
        <div class="seats-left">남은 자리 {{ seatsLeft }}명</div>
        <button class="cta-main" :disabled="companionStore.loading" @click="apply">참여 신청하기</button>
      </template>

      <!-- Visitor: not applied & 정원 마감 — 신청 CTA 숨기고 마감 안내(sec2와 정합) -->
      <template v-else-if="!comp.isOwner && !isApplied">
        <button class="cta-main" disabled>모집이 마감되었어요</button>
      </template>

      <!-- Visitor: approved — 채팅방 입장 + ... 더보기(동행 탈퇴) -->
      <template v-else-if="!comp.isOwner && isApplied && isApproved">
        <button class="cta-main" :disabled="comp.chatRoomId == null" @click="openChat">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z" /></svg>
          채팅방 입장
        </button>
        <div class="cta-more-wrap">
          <div v-if="ctaMoreOpen" class="cta-more-backdrop" @click="ctaMoreOpen = false" />
          <button class="cta-more-btn" @click.stop="ctaMoreOpen = !ctaMoreOpen" aria-label="더보기">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><circle cx="12" cy="5" r="1.5"/><circle cx="12" cy="12" r="1.5"/><circle cx="12" cy="19" r="1.5"/></svg>
          </button>
          <Transition name="fade">
            <div v-if="ctaMoreOpen" class="cta-more-menu">
              <button class="cta-more-item danger" @click="withdrawCompanion">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
                동행 탈퇴
              </button>
            </div>
          </Transition>
        </div>
      </template>

      <!-- Visitor: applied (pending) — 취소 가능 -->
      <template v-else-if="!comp.isOwner && isApplied">
        <button class="cta-cancel" @click="cancelApply">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" /></svg>
          신청 취소
        </button>
      </template>

      <!-- Owner -->
      <template v-else>
        <button class="cta-chat" :disabled="comp.chatRoomId == null" @click="openChat">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z" /></svg>
        </button>
        <button class="cta-main" @click="$router.push(`/companion/${comp.id}/applicants`)">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round"><path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2" /><circle cx="9" cy="7" r="4" /></svg>
          신청자 관리 {{ comp.pendingCount }}
        </button>
      </template>
    </div>

    <!-- 공유 토스트 -->
    <div v-if="shareToast" class="share-toast">{{ shareToast }}</div>

    <!-- 신청 메시지 팝업 -->
    <Transition name="fade">
      <div v-if="applySheet.open" class="apply-overlay" @click.self="applySheet.open = false">
        <div class="apply-popup">
          <div class="apply-popup-head">
            <h3 class="apply-popup-title">참여 신청</h3>
            <button class="apply-popup-close" @click="applySheet.open = false" aria-label="닫기">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" /></svg>
            </button>
          </div>
          <p class="apply-popup-desc">방장에게 전달할 한마디를 남겨보세요. (선택)</p>
          <textarea
            v-model="applySheet.message"
            class="apply-popup-textarea"
            rows="4"
            placeholder="간단한 자기소개나 참여 이유를 적어주세요"
            maxlength="200"
          />
          <div class="apply-popup-count">{{ applySheet.message.length }} / 200</div>
          <button
            class="apply-popup-confirm"
            :disabled="applySheet.submitting"
            @click="confirmApply"
          >
            {{ applySheet.submitting ? '신청 중…' : '신청하기' }}
          </button>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCompanionStore } from '@/stores/companion.js'
import { followApi } from '@/api/index.js'
import TripMap from '@/components/common/TripMap.vue'

const route = useRoute()
const router = useRouter()
const companionStore = useCompanionStore()

// 실제 상세 데이터(없으면 null). 실패/404를 가짜 "동행 모집" 객체로 위장하지 않는다.
const realComp = computed(() => companionStore.getById(route.params.id) ?? null)

// 화면 상태: 로딩/404/오류/정상 분기.
// - 데이터가 있으면 ready
// - 없고 로딩 중이면 loading
// - 없고 404면 not-found, 그 외 실패면 error
const detailState = computed(() => {
  if (realComp.value) return 'ready'
  if (companionStore.detailLoading) return 'loading'
  if (companionStore.detailNotFound) return 'not-found'
  if (companionStore.detailError) return 'error'
  return 'loading'
})

// 템플릿이 comp.value.* 를 참조하므로, 정상 로드 전/실패 시에도 null 접근이 없게
// 중립 placeholder를 제공한다. 단 이 값은 화면 렌더(본문/CTA)에는 쓰지 않고,
// detailState === 'ready' 일 때만 본문/CTA를 노출한다.
const comp = computed(() => realComp.value ?? {
  id: route.params.id, title: '', location: '-', dateRange: '-',
  status: '', currentCount: 0, maxCount: 0, author: { nickname: '-', tripCount: 0 },
  period: '-', estimatedCost: '-', tags: [], intro: '',
  isOwner: false, pendingCount: 0, approvedCount: 0,
  isApplied: false, myApplicationId: null, myApplicationStatus: null, chatRoomId: null,
})

// 대표 이미지 — 있으면 hero 배경으로 표시(없으면 기존 그라데이션 placeholder 유지).
const heroStyle = computed(() => {
  const url = comp.value.imageUrl ?? comp.value.thumbnail
  if (!url) return {}
  return {
    backgroundImage: `url("${url}")`,
    backgroundSize: 'cover',
    backgroundPosition: 'center',
  }
})

// ── 작성자 팔로우 ────────────────────────────────────────────────────────────
// 작성자 userId — 상세 응답(normalizeDetail 가 ...item 보존)의 authorId.
const authorUserId = computed(() => comp.value.authorId ?? comp.value.author?.userId ?? null)
const isFollowing = ref(false)
const followLoading = ref(false)

// 작성자가 바뀌면(상세 로드/이동) 팔로우 상태를 다시 조회한다. 방장 본인은 조회 생략.
async function refreshFollowStatus() {
  isFollowing.value = false
  const uid = authorUserId.value
  if (uid == null || comp.value.isOwner) return
  try {
    const { data } = await followApi.status(uid)
    isFollowing.value = !!data?.following
  } catch {
    // 미로그인/실패 — 기본 false 유지(버튼은 보이되 누르면 안내)
  }
}

// 낙관적 토글 — 즉시 UI 반영 후 실패 시 롤백.
async function toggleFollow() {
  const uid = authorUserId.value
  if (uid == null || followLoading.value) return
  const prev = isFollowing.value
  isFollowing.value = !prev
  followLoading.value = true
  try {
    const { data } = await followApi.toggle(uid)
    if (typeof data?.following === 'boolean') isFollowing.value = data.following
  } catch (e) {
    isFollowing.value = prev   // 롤백
    showShareToast(e?.response?.status === 401 ? '로그인이 필요해요.' : '잠시 후 다시 시도해 주세요.')
  } finally {
    followLoading.value = false
  }
}

// 내 신청 상태/ID — origin 의 /applications/me 조회(getMyApplication) 결과를 보관.
// 신청/취소 직후 낙관적으로 갱신하고, 상세 재조회로 서버 기준 동기화한다.
const fetchedStatus = ref(null) // null | 'PENDING' | 'APPROVED' | 'REJECTED'
const fetchedAppId = ref(null)

// 신청 여부는 서버 응답(comp.isApplied) 또는 /applications/me 조회를 기준으로 하되,
// 신청/취소 직후에는 재조회 전까지 낙관적 오버라이드를 적용한다.
const appliedOverride = ref(null)
const isApplied = computed(() => {
  if (appliedOverride.value !== null) return appliedOverride.value
  if (comp.value.isApplied) return true
  return !!fetchedStatus.value && fetchedStatus.value !== 'REJECTED'
})

// 신청 상태(PENDING/APPROVED/REJECTED) — 상세 응답값을 우선하고, 없으면 /me 조회값 사용.
const myApplicationStatus = computed(
  () => comp.value.myApplicationStatus ?? fetchedStatus.value ?? null,
)
// 신청 ID — 상세 응답값을 우선하고, 없으면 /me 조회값 사용(취소 시 사용).
const myApplicationId = computed(
  () => comp.value.myApplicationId ?? fetchedAppId.value ?? null,
)
// 승인된 신청은 채팅방 멤버십이 생성되어 취소가 불가하다(BE에서 409 반환).
// → 취소 버튼 대신 채팅방 입장 안내를 노출한다.
const isApproved = computed(() => myApplicationStatus.value === 'APPROVED')
const applyError = ref('')
const applySheet = reactive({ open: false, message: '', submitting: false })
const ctaMoreOpen = ref(false)

// 남은 모집 자리 — 모집 조건 카드와 하단 CTA에서 공유(중복 계산 방지)
const seatsLeft = computed(() =>
  Math.max(0, (comp.value.maxCount ?? 0) - (comp.value.currentCount ?? 0)),
)

// ── 연결된 계획(지도/동선) ──────────────────────────────────────────────────
// 상세 응답의 linkedPlan = { planId, title, startDate, endDate, places:[{ dayNo, title, lat, lng }] }
const linkedPlan = computed(() => comp.value.linkedPlan ?? null)
// 좌표(lat,lng)가 모두 있는 장소만 (BE가 이미 걸러주지만 방어적으로 한 번 더)
const mapPlaces = computed(() =>
  (linkedPlan.value?.places ?? []).filter(
    p => Number.isFinite(Number(p.lat)) && Number.isFinite(Number(p.lng)),
  ),
)
const hasPlaces = computed(() => mapPlaces.value.length > 0)
// 일자(dayNo)별 그룹핑 — 순서 보존
const placesByDay = computed(() => {
  const groups = []
  const byDay = new Map()
  for (const p of mapPlaces.value) {
    const dayNo = p.dayNo ?? 1
    if (!byDay.has(dayNo)) {
      const entry = { dayNo, places: [] }
      byDay.set(dayNo, entry)
      groups.push(entry)
    }
    byDay.get(dayNo).places.push(p)
  }
  return groups
})
const planDateRange = computed(() => {
  const lp = linkedPlan.value
  if (!lp?.startDate) return ''
  return lp.endDate && lp.endDate !== lp.startDate
    ? `${lp.startDate} ~ ${lp.endDate}`
    : lp.startDate
})

// ── 지도(공통 TripMap) ───────────────────────────────────────────────────────
// 연결된 계획 장소(좌표)를 TripMap props({ id, name, lat, lng })로 변환.
// 좌표가 하나라도 있으면 그 마커들을, 없으면 모집 위치(region)를 지오코딩한
// 단일 마커를 표시한다. 둘 다 없으면 지도 섹션을 숨긴다.
const KAKAO_KEY = import.meta.env.VITE_KAKAO_MAP_KEY

// linkedPlan 장소 → TripMap 형식
const linkedTripPlaces = computed(() =>
  mapPlaces.value.map((p, i) => ({
    id: `${p.dayNo ?? 1}-${i}`,
    name: p.title ?? '',
    lat: Number(p.lat),
    lng: Number(p.lng),
  })),
)

// 모집 위치(region) 지오코딩 결과 단일 마커(연결 계획 좌표가 없을 때만 사용)
const regionPlace = ref(null) // { id, name, lat, lng } | null

// 최종 TripMap에 넘길 장소 목록
const tripMapPlaces = computed(() =>
  hasPlaces.value
    ? linkedTripPlaces.value
    : (regionPlace.value ? [regionPlace.value] : []),
)
// 지도에 찍을 좌표가 하나라도 있는지(없으면 섹션 자체를 숨김)
const hasMapPlaces = computed(() => tripMapPlaces.value.length > 0)

// HotplaceRegisterView 와 동일한 SDK 로딩 패턴(전역 캐시 + autoload=false).
// services 라이브러리(지오코더)를 보장한다.
function loadKakao() {
  if (window.kakao?.maps?.services) return Promise.resolve(window.kakao)
  if (window.__kakaoMapLoading && !window.kakao?.maps?.services) {
    window.__kakaoMapLoading = null
  }
  if (window.__kakaoMapLoading) return window.__kakaoMapLoading
  window.__kakaoMapLoading = new Promise((resolve, reject) => {
    if (!KAKAO_KEY) { reject(new Error('VITE_KAKAO_MAP_KEY 누락')); return }
    const s = document.createElement('script')
    s.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${KAKAO_KEY}&autoload=false&libraries=services`
    s.onload = () => window.kakao.maps.load(() => resolve(window.kakao))
    s.onerror = () => reject(new Error('Kakao 지도 SDK 로드 실패'))
    document.head.appendChild(s)
  })
  return window.__kakaoMapLoading
}

// 모집 위치(region 문자열)를 좌표로 지오코딩 → 단일 마커.
// 연결 계획 좌표가 있으면 불필요하므로 건너뛴다. 실패 시 조용히 무시(지도 숨김).
async function geocodeRegion() {
  if (hasPlaces.value) { regionPlace.value = null; return }
  const region = comp.value.location
  if (!region || region === '-') { regionPlace.value = null; return }
  try {
    const kakao = await loadKakao()
    const geocoder = new kakao.maps.services.Geocoder()
    // 주소 검색 우선, 실패 시 키워드(장소) 검색으로 보강
    const byAddress = await new Promise((resolve) => {
      geocoder.addressSearch(region, (res, status) => {
        if (status === kakao.maps.services.Status.OK && res[0]) {
          resolve({ lat: Number(res[0].y), lng: Number(res[0].x) })
        } else resolve(null)
      })
    })
    let coord = byAddress
    if (!coord && kakao.maps.services.Places) {
      coord = await new Promise((resolve) => {
        const ps = new kakao.maps.services.Places()
        ps.keywordSearch(region, (res, status) => {
          if (status === kakao.maps.services.Status.OK && res[0]) {
            resolve({ lat: Number(res[0].y), lng: Number(res[0].x) })
          } else resolve(null)
        })
      })
    }
    if (coord && Number.isFinite(coord.lat) && Number.isFinite(coord.lng)) {
      regionPlace.value = { id: 'region', name: region, lat: coord.lat, lng: coord.lng }
    } else {
      regionPlace.value = null
    }
  } catch {
    // SDK 로드/키 누락 등 — 지도는 숨김 처리(가짜 마커 금지)
    regionPlace.value = null
  }
}

// 상세 로드 후 좌표 상태가 바뀌면 지오코딩 갱신
watch(
  () => [hasPlaces.value, comp.value.location],
  () => { geocodeRegion() },
)

// 내 신청 상태 조회(origin /applications/me 경로) — 방장이 아닐 때만.
// 상세 응답에 isApplied/myApplicationStatus 가 없을 때 보강한다.
async function refreshMyApplication() {
  if (comp.value.isOwner) return
  const app = await companionStore.getMyApplication(route.params.id)
  if (app) {
    fetchedStatus.value = app.status ?? null
    fetchedAppId.value = app.id ?? null
  } else {
    fetchedStatus.value = null
    fetchedAppId.value = null
  }
}

onMounted(async () => {
  await companionStore.getDetail(route.params.id)
  await refreshMyApplication()
  await refreshFollowStatus()
  // 연결 계획 좌표가 없으면 모집 위치를 지오코딩해 단일 마커 준비
  geocodeRegion()
})

/** 상세 재시도 — 로드 실패 화면의 "다시 시도" 버튼용 */
async function reloadDetail() {
  await companionStore.getDetail(route.params.id)
  await refreshMyApplication()
  await refreshFollowStatus()
  geocodeRegion()
}

function apply() {
  applyError.value = ''
  applySheet.message = ''
  applySheet.open = true
}

async function confirmApply() {
  if (applySheet.submitting) return
  applySheet.submitting = true
  applyError.value = ''
  try {
    const app = await companionStore.join(comp.value.id, applySheet.message)
    fetchedStatus.value = app?.status ?? 'PENDING'
    fetchedAppId.value = app?.id ?? null
    appliedOverride.value = true
    applySheet.open = false
    await companionStore.getDetail(route.params.id)
    await refreshMyApplication()
    appliedOverride.value = null
  } catch {
    applyError.value = companionStore.error || '신청에 실패했어요.'
    applySheet.open = false
  } finally {
    applySheet.submitting = false
  }
}

async function cancelApply() {
  applyError.value = ''
  // 신청 ID를 모르면 최신 상세 + /me 조회로 확인
  if (!myApplicationId.value) {
    await companionStore.getDetail(route.params.id)
    await refreshMyApplication()
  }
  const id = myApplicationId.value
  if (!id) {
    applyError.value = '신청 정보를 찾을 수 없어요.'
    return
  }
  try {
    // BE DELETE /companion/posts/{postId}/applications/{applicationId}
    await companionStore.cancel(comp.value.id, id)
    appliedOverride.value = false
    fetchedStatus.value = null
    fetchedAppId.value = null
    await companionStore.getDetail(route.params.id)
    await refreshMyApplication()
    appliedOverride.value = null
  } catch {
    applyError.value = companionStore.error || '신청 취소에 실패했어요.'
  }
}

async function withdrawCompanion() {
  ctaMoreOpen.value = false
  if (!await $confirm('동행에서 탈퇴하시겠어요? 채팅방에서도 나가게 됩니다.')) return
  applyError.value = ''
  if (!myApplicationId.value) {
    await companionStore.getDetail(route.params.id)
    await refreshMyApplication()
  }
  const id = myApplicationId.value
  if (!id) {
    applyError.value = '신청 정보를 찾을 수 없어요.'
    return
  }
  try {
    await companionStore.cancel(comp.value.id, id)
    appliedOverride.value = false
    fetchedStatus.value = null
    fetchedAppId.value = null
    await companionStore.getDetail(route.params.id)
    await refreshMyApplication()
    appliedOverride.value = null
  } catch {
    applyError.value = companionStore.error || '탈퇴에 실패했어요.'
  }
}

function openChat() {
  const chatRoomId = comp.value.chatRoomId
  if (chatRoomId != null) {
    router.push(`/chat/${chatRoomId}`)
  }
}

// 공유 — Web Share API가 있으면 사용하고, 없으면 클립보드 복사로 폴백한다.
// 어느 경로든 짧은 토스트로 피드백을 주어 버튼이 "먹통"으로 느껴지지 않게 한다.
const shareToast = ref('')
let shareToastTimer = null
function showShareToast(message) {
  shareToast.value = message
  if (shareToastTimer) clearTimeout(shareToastTimer)
  shareToastTimer = setTimeout(() => { shareToast.value = '' }, 2000)
}
async function share() {
  const url = location.href
  if (navigator.share) {
    try {
      await navigator.share({ title: comp.value.title, url })
    } catch {
      // 사용자가 공유 시트를 취소한 경우 등 — 조용히 무시
    }
    return
  }
  // 폴백: 클립보드 복사
  try {
    await navigator.clipboard.writeText(url)
    showShareToast('링크 복사됨')
  } catch {
    showShareToast('링크를 복사하지 못했어요')
  }
}

onBeforeUnmount(() => {
  if (shareToastTimer) clearTimeout(shareToastTimer)
})
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  background: var(--color-white);
}

.hero {
  height: 260px;
  position: relative;
  flex-shrink: 0;
}
.hero-img {
  position: absolute;
  inset: 0;
  background: linear-gradient(160deg, #d8c8b8 0%, #c0b0a0 100%);
}
.hero-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(to bottom, rgba(0,0,0,0.28) 0%, transparent 50%);
}
.hero-top {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 52px 16px 12px;
  z-index: 1;
}
.hero-top-right { display: flex; gap: 8px; }
.ghost-btn {
  width: 38px;
  height: 38px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0,0,0,0.25);
  border-radius: 50%;
  backdrop-filter: blur(4px);
}

.content-scroll { flex: 1; overflow-y: auto; }

.title-area {
  padding: 20px 20px 0;
  border-bottom: 1px solid var(--color-line-light);
  padding-bottom: 16px;
}
.badges-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.badge {
  padding: 3px 10px;
  border-radius: var(--radius-full);
  font-size: 11.5px;
  font-weight: 600;
}
.badge-open { background: var(--color-peach-light); color: var(--color-peach-pressed); }
.badge-urgent { background: #fff0e8; color: #d04010; }
.owner-badge { background: var(--color-peach); color: white; }
.date-range { font-size: 13px; color: var(--color-ink-muted); }

.comp-title {
  font-size: 22px;
  font-weight: 800;
  color: var(--color-ink);
  letter-spacing: -0.5px;
  margin-bottom: 14px;
}
.author-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}
.author-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--color-surface);
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}
.author-info { display: flex; flex-direction: column; gap: 1px; flex: 1; }
.author-name { font-size: 14px; font-weight: 600; color: var(--color-ink); }
.author-sub { font-size: 12px; color: var(--color-ink-muted); }
.seat-count {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-ink-secondary);
}

/* 팔로우 버튼 — 기본은 peach 채움, 팔로잉 상태는 외곽선 */
.follow-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  flex-shrink: 0;
  padding: 7px 14px;
  border-radius: var(--radius-full);
  background: var(--color-peach);
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: -0.2px;
  transition: background 0.15s, color 0.15s, border-color 0.15s;
  cursor: pointer;
}
.follow-btn.following {
  background: var(--color-white);
  color: var(--color-ink-secondary);
  border: 1.5px solid var(--color-line);
}
.follow-btn:disabled { opacity: 0.6; cursor: not-allowed; }

.owner-status-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  background: var(--color-surface);
  border-radius: var(--radius-md);
}
.owner-status-label { font-size: 12px; color: var(--color-ink-muted); white-space: nowrap; }
.owner-status-val { font-size: 13.5px; font-weight: 600; color: var(--color-ink); flex: 1; }
.avatar-stack { display: flex; }
.stack-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--color-surface);
  border: 2px solid white;
  margin-left: -6px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.stack-avatar:first-child { margin-left: 0; }

.pending-banner {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin: 16px 20px;
  padding: 14px;
  background: var(--color-peach-light);
  border-radius: var(--radius-md);
}
.pending-title { font-size: 13.5px; font-weight: 700; color: var(--color-peach-pressed); margin-bottom: 2px; }
.pending-sub { font-size: 12.5px; color: var(--color-ink-secondary); line-height: 1.5; }

.approved-banner {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin: 16px 20px;
  padding: 14px;
  background: #edfaf3;
  border-radius: var(--radius-md);
}
.approved-title { font-size: 13.5px; font-weight: 700; color: #2a7a4b; margin-bottom: 2px; }
.approved-sub { font-size: 12.5px; color: var(--color-ink-secondary); line-height: 1.5; }

.cond-head {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 18px 20px 0;
}
.cond-head .section-title { margin-bottom: 0; flex: 1; }
.cond-seats {
  font-size: 12.5px;
  font-weight: 700;
  color: var(--color-peach-pressed);
  background: var(--color-peach-light);
  padding: 3px 10px;
  border-radius: var(--radius-full);
  white-space: nowrap;
}
.cond-seats-full {
  color: var(--color-ink-muted);
  background: var(--color-surface);
}

.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1px;
  margin: 12px 20px 16px;
  background: var(--color-line-light);
  border-radius: var(--radius-lg);
  overflow: hidden;
  border: 1px solid var(--color-line-light);
}
.info-cell-wide { grid-column: 1 / -1; }
.info-cell {
  padding: 14px 16px;
  background: var(--color-white);
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.info-icon-row { display: flex; align-items: center; gap: 4px; }
.info-label { font-size: 12px; color: var(--color-ink-muted); }
.info-val { font-size: 14.5px; font-weight: 700; color: var(--color-ink); letter-spacing: -0.3px; }

.tags-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 0 20px 20px;
}
.tag-chip {
  padding: 5px 12px;
  border-radius: var(--radius-full);
  background: var(--color-surface);
  font-size: 13px;
  color: var(--color-ink-secondary);
  font-weight: 500;
}

.section { padding: 0 20px 20px; }
.section-title { font-size: 16px; font-weight: 700; color: var(--color-ink); margin-bottom: 10px; letter-spacing: -0.3px; }
.intro-text { font-size: 14.5px; color: var(--color-ink-secondary); line-height: 1.7; letter-spacing: -0.2px; }

.cta-bar {
  position: absolute;
  bottom: 0;
  left: 0;
  z-index: 10;
  right: 0;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  padding: 12px 20px calc(12px + var(--safe-bottom));
  background: white;
  border-top: 1px solid var(--color-line-light);
}
.seats-left {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-ink-secondary);
  white-space: nowrap;
}
.cta-main {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 15px;
  background: var(--color-peach);
  color: white;
  font-size: 15px;
  font-weight: 700;
  border-radius: var(--radius-xl);
  letter-spacing: -0.3px;
}
.cta-main:disabled {
  background: var(--color-line);
  color: var(--color-ink-muted);
  cursor: not-allowed;
}
.cta-cancel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  padding: 15px;
  border: 1.5px solid var(--color-line);
  color: var(--color-ink-secondary);
  font-size: 15px;
  font-weight: 600;
  border-radius: var(--radius-xl);
}
.cta-chat {
  width: 52px;
  height: 52px;
  border-radius: var(--radius-lg);
  border: 1.5px solid var(--color-line);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: var(--color-ink);
}
.apply-error {
  width: 100%;
  font-size: 13px;
  color: var(--color-error);
  font-weight: 500;
  text-align: center;
  padding-bottom: 4px;
}

/* ── Linked plan: map + route ─────────────────────────────────────────────── */
.plan-section { padding-top: 4px; }
.plan-section .cond-head { padding: 0; margin-bottom: 8px; }
.plan-fixed-pill {
  font-size: 11.5px;
  font-weight: 700;
  color: var(--color-peach-pressed);
  background: var(--color-peach-light);
  padding: 3px 9px;
  border-radius: var(--radius-full);
  white-space: nowrap;
}
.plan-meta {
  font-size: 13px;
  color: var(--color-ink-secondary);
  margin: -4px 0 12px;
  font-weight: 600;
}
.plan-date { color: var(--color-ink-muted); font-weight: 500; }

.plan-map-wrap {
  position: relative;
  width: 100%;
  height: 220px;
  border-radius: var(--radius-lg);
  overflow: hidden;
  border: 1px solid var(--color-line-light);
  background: var(--color-surface);
  margin-bottom: 16px;
}

.plan-day { margin-bottom: 14px; }
.plan-day-head { margin-bottom: 8px; }
.plan-day-pill {
  background: var(--color-peach-light);
  color: var(--color-peach-pressed);
  padding: 4px 11px;
  border-radius: var(--radius-full);
  font-size: 12px;
  font-weight: 700;
}
.plan-route { display: flex; flex-direction: column; }
.plan-stop { display: flex; gap: 10px; align-items: stretch; }
.plan-stop-left {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 22px;
  flex-shrink: 0;
}
.plan-stop-dot {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: 2px solid var(--color-peach);
  background: var(--color-white);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 9px;
  font-weight: 800;
  color: var(--color-peach);
  flex-shrink: 0;
  z-index: 1;
}
.plan-stop-line {
  width: 2px;
  flex: 1;
  background: var(--color-line-light);
  margin: 2px 0;
  min-height: 14px;
}
.plan-stop-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-ink);
  letter-spacing: -0.2px;
  padding: 1px 0 12px;
}

/* ── Detail load states (로딩/없음/오류) ──────────────────────────────────── */
.detail-state {
  padding: 24px 20px;
}
.detail-skeleton {
  height: 16px;
  border-radius: var(--radius-full);
  background: var(--color-surface);
  margin-bottom: 14px;
  animation: comp-shimmer 1.2s infinite;
}
.detail-skeleton.w70 { width: 70%; }
.detail-skeleton.w90 { width: 90%; }
.detail-skeleton.w50 { width: 50%; }
@keyframes comp-shimmer {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.45; }
}
.detail-state-msg {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 64px 24px;
  gap: 6px;
}
.detail-state-icon { margin-bottom: 6px; }
.detail-state-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-ink);
  letter-spacing: -0.3px;
}
.detail-state-sub {
  font-size: 13.5px;
  color: var(--color-ink-muted);
  margin-bottom: 12px;
  line-height: 1.5;
}
.detail-state-btn {
  background: var(--color-peach);
  color: white;
  font-size: 14px;
  font-weight: 600;
  padding: 11px 26px;
  border-radius: var(--radius-full);
  letter-spacing: -0.2px;
}
.detail-state-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* ── CTA 더보기 (...) 버튼 ────────────────────────────────────── */
.cta-more-wrap {
  position: relative;
  flex-shrink: 0;
}
.cta-more-backdrop {
  position: fixed;
  inset: 0;
  z-index: 9;
}
.cta-more-btn {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-lg);
  border: 1.5px solid var(--color-line);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-ink-secondary);
  background: #fff;
}
.cta-more-menu {
  position: absolute;
  bottom: calc(100% + 8px);
  right: 0;
  background: #fff;
  border: 1px solid var(--color-line);
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.12);
  overflow: hidden;
  z-index: 10;
  min-width: 140px;
  z-index: 50;
}
.cta-more-item {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 13px 16px;
  font-size: 14px;
  font-weight: 500;
  background: none;
  border: none;
  cursor: pointer;
  white-space: nowrap;
}
.cta-more-item.danger {
  color: var(--color-error, #ef4444);
}
.cta-more-item:hover {
  background: var(--color-surface, #f5f5f5);
}

/* ── 공유 토스트 ──────────────────────────────────────────────── */
.share-toast {
  position: fixed;
  bottom: calc(90px + var(--safe-bottom));
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0,0,0,0.82);
  color: #fff;
  font-size: 13px;
  padding: 10px 16px;
  border-radius: var(--radius-full);
  z-index: 70;
  max-width: 80%;
  text-align: center;
}

/* ── 신청 메시지 팝업 ─────────────────────────────────────────── */
.apply-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  z-index: 200;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}
.apply-popup {
  background: #fff;
  border-radius: 16px;
  padding: 24px 20px 20px;
  width: 100%;
  max-width: 400px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.apply-popup-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.apply-popup-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-gray-900, #111);
  margin: 0;
}
.apply-popup-close {
  background: none;
  border: none;
  padding: 4px;
  cursor: pointer;
  color: var(--color-gray-500, #888);
  line-height: 1;
}
.apply-popup-desc {
  font-size: 13px;
  color: var(--color-gray-500, #888);
  margin: 0;
}
.apply-popup-textarea {
  width: 100%;
  resize: none;
  border: 1.5px solid var(--color-gray-200, #e5e7eb);
  border-radius: 10px;
  padding: 12px;
  font-size: 14px;
  font-family: inherit;
  line-height: 1.6;
  outline: none;
  transition: border-color 0.15s;
  box-sizing: border-box;
}
.apply-popup-textarea:focus {
  border-color: var(--color-primary, #6c63ff);
}
.apply-popup-count {
  font-size: 12px;
  color: var(--color-gray-400, #aaa);
  text-align: right;
  margin-top: -6px;
}
.apply-popup-confirm {
  background: var(--color-primary, #6c63ff);
  color: #fff;
  border: none;
  border-radius: 12px;
  padding: 14px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.15s;
}
.apply-popup-confirm:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* fade transition */
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
