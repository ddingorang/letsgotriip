# Created: 2026-06-16 14:26:29
<template>
  <div class="page">
    <div class="scroll-content">
      <!-- Top icons -->
      <div class="top-bar">
        <div style="flex:1" />
        <button class="icon-btn bell-wrap" @click="$router.push('/notifications')">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9" /><path d="M13.73 21a2 2 0 01-3.46 0" />
          </svg>
          <span v-if="notifStore.hasUnread" class="notif-dot" />
        </button>
        <button class="icon-btn" @click="$router.push('/mypage/edit')">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7" />
            <path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z" />
          </svg>
        </button>
      </div>

      <!-- Profile -->
      <div class="profile-section">
        <div class="avatar-col">
          <div class="avatar-circle">
            <img v-if="authStore.user?.profileImageUrl" :src="authStore.user.profileImageUrl" :alt="authStore.user.nickname" class="avatar-img" />
            <span v-else class="avatar-text">프로필</span>
          </div>
          <div class="camera-icon">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M23 19a2 2 0 01-2 2H3a2 2 0 01-2-2V8a2 2 0 012-2h4l2-3h6l2 3h4a2 2 0 012 2z" /><circle cx="12" cy="13" r="4" />
            </svg>
          </div>
        </div>
        <div class="profile-info">
          <h2 class="profile-name">{{ authStore.user?.nickname ?? '프로필' }}</h2>
          <p class="profile-bio">{{ authStore.user?.bio || authStore.user?.email || '' }}</p>
        </div>
      </div>

      <!-- Stats -->
      <div class="stats-row">
        <div class="stat">
          <span class="stat-num">{{ gami?.stats?.plans ?? planCount }}</span>
          <span class="stat-label">여행 계획</span>
        </div>
        <div class="stat-divider" />
        <div class="stat">
          <span class="stat-num">{{ gami?.stats?.completedPlans ?? completedCount }}</span>
          <span class="stat-label">다녀온 여행</span>
        </div>
        <div class="stat-divider" />
        <div class="stat">
          <span class="stat-num">{{ gami?.stats?.badges ?? unlockedBadgeCount }}</span>
          <span class="stat-label">뱃지</span>
        </div>
      </div>

      <!-- Challenge card -->
      <div class="challenge-card" @click="$router.push('/mypage/challenge')">
        <div class="challenge-header">
          <div class="challenge-left">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="var(--color-peach)" stroke="none">
              <path d="M12 2l2.4 7.4H22l-6.2 4.5 2.4 7.4L12 17 5.8 21.3l2.4-7.4L2 9.4h7.6L12 2z" />
            </svg>
            <span class="challenge-title">이달의 챌린지 · {{ gami?.challenge?.month ?? '' }}</span>
          </div>
          <span class="challenge-phase">{{ gami?.challenge?.current ?? 0 }} / {{ gami?.challenge?.goal ?? 10 }}곳</span>
        </div>
        <div class="challenge-bar">
          <div class="challenge-fill" :style="{ width: (gami?.challenge?.percent ?? 0) + '%' }" />
        </div>
        <p class="challenge-hint">{{ gami?.challenge?.hint ?? '계획을 만들어 챌린지를 시작해보세요!' }}</p>
      </div>

      <!-- Main tabs -->
      <div class="main-tab-bar">
        <button
          v-for="(tab, i) in mainTabs"
          :key="i"
          :class="['main-tab', { active: activeMain === i }]"
          @click="activeMain = i"
        >
          {{ tab }}
        </button>
      </div>

      <!-- ① 내 계획 -->
      <div v-show="activeMain === 0" class="tab-content">
        <div class="sub-filter">
          <button
            v-for="f in planFilters"
            :key="f"
            :class="['sub-chip', { active: planFilter === f }]"
            @click="planFilter = f"
          >
            {{ f }}
          </button>
        </div>

        <template v-if="filteredPlans.length > 0">
          <div
            v-for="plan in filteredPlans"
            :key="plan.id"
            class="plan-item"
            @click="$router.push('/plan')"
          >
            <div class="plan-thumb">
              <span class="plan-thumb-label">{{ plan.thumbLabel }}</span>
            </div>
            <div class="plan-info">
              <div class="plan-title-row">
                <span class="plan-title">{{ plan.title }}</span>
                <span :class="['plan-badge', plan.status === '예정' ? 'badge-upcoming' : 'badge-done']">{{ plan.status }}</span>
              </div>
              <div class="plan-meta">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><rect x="3" y="4" width="18" height="18" rx="2" /><path d="M16 2v4M8 2v4M3 10h18" /></svg>
                {{ plan.dateRange }}
              </div>
              <button v-if="plan.status === '완료'" class="album-link" @click.stop="$router.push(`/mypage/album/${plan.id}`)">
                <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><rect x="3" y="3" width="7" height="7" /><rect x="14" y="3" width="7" height="7" /><rect x="14" y="14" width="7" height="7" /><rect x="3" y="14" width="7" height="7" /></svg>
                앨범 보기
              </button>
            </div>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--color-line)" stroke-width="2" stroke-linecap="round"><path d="M9 18l6-6-6-6" /></svg>
          </div>
        </template>

        <div v-else class="empty-plans">
          <div class="empty-icon">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="18" cy="5" r="3" /><circle cx="6" cy="12" r="3" /><circle cx="18" cy="19" r="3" />
              <line x1="8.59" y1="13.51" x2="15.42" y2="17.49" /><line x1="15.41" y1="6.51" x2="8.59" y2="10.49" />
            </svg>
          </div>
          <h3 class="empty-title">아직 여행 계획이 없어요</h3>
          <p class="empty-sub">첫 여행을 계획하고<br />나만의 일정을 만들어보세요!</p>
          <button class="create-plan-btn" @click="$router.push('/plan')">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2.5" stroke-linecap="round"><line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" /></svg>
            여행 계획 만들기
          </button>
        </div>
      </div>

      <!-- ② 앨범 -->
      <div v-show="activeMain === 1" class="tab-content">
        <!-- Phase 2 lock state -->
        <div v-if="albumPhase2" class="phase2-placeholder">
          <div class="phase2-icon">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2" /><circle cx="12" cy="12" r="3" /></svg>
            <div class="lock-badge">
              <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="2.5" stroke-linecap="round"><rect x="3" y="11" width="18" height="11" rx="2" /><path d="M7 11V7a5 5 0 0110 0v4" /></svg>
            </div>
          </div>
          <span class="phase2-label">Phase 2</span>
          <h3 class="phase2-title">앨범은 곧 만나요</h3>
          <p class="phase2-sub">여행이 끝나면 사진이 자동으로<br />앨범에 정리될 예정이에요.</p>
        </div>

        <!-- Albums grid -->
        <div v-else>
          <div class="album-section-header">
            <span class="album-count">앨범 {{ albums.length }}</span>
            <button class="make-album-btn">+ 앨범 만들기</button>
          </div>
          <div class="albums-grid">
            <div
              v-for="album in albums"
              :key="album.id"
              class="album-card"
              @click="$router.push(`/mypage/album/${album.id}`)"
            >
              <div class="album-thumb">
                <div class="album-photo-count">
                  <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round"><rect x="3" y="3" width="18" height="18" rx="2" /><circle cx="8.5" cy="8.5" r="1.5" /><polyline points="21 15 16 10 5 21" /></svg>
                  {{ album.photoCount }}
                </div>
                <span class="album-place-label">{{ album.location }}</span>
              </div>
              <div class="album-info">
                <span class="album-title">{{ album.title }}</span>
                <span class="album-auto">자동</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- ③ 뱃지 -->
      <div v-show="activeMain === 2" class="tab-content">
        <div class="badge-header">획득한 뱃지 <strong>{{ gami?.stats?.badges ?? 0 }}</strong> / {{ badgeList.length }}</div>
        <div class="badges-grid">
          <div v-for="badge in badgeList" :key="badge.key" class="badge-item">
            <div :class="['badge-circle', badge.unlocked ? 'unlocked' : 'locked']">
              <svg v-if="badge.unlocked" width="20" height="20" viewBox="0 0 24 24" fill="white" stroke="none"><path d="M12 2l2.4 7.4H22l-6.2 4.5 2.4 7.4L12 17 5.8 21.3l2.4-7.4L2 9.4h7.6L12 2z"/></svg>
              <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--color-line)" stroke-width="2" stroke-linecap="round"><rect x="3" y="11" width="18" height="11" rx="2" /><path d="M7 11V7a5 5 0 0110 0v4" /></svg>
            </div>
            <span class="badge-name">{{ badge.name }}</span>
            <span v-if="!badge.unlocked && badge.progressText" class="badge-progress">{{ badge.progressText }}</span>
          </div>
        </div>
      </div>

      <!-- Logout -->
      <div class="logout-section">
        <button class="logout-btn" @click="handleLogout">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4" /><polyline points="16 17 21 12 16 7" /><line x1="21" y1="12" x2="9" y2="12" />
          </svg>
          로그아웃
        </button>
      </div>

      <div class="bottom-spacer" />
    </div>

  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth.js'
import { useNotificationStore } from '@/stores/notification.js'
import { useGamificationStore } from '@/stores/gamification.js'
import { http } from '@/api/http.js'

const router = useRouter()
const authStore = useAuthStore()
const notifStore = useNotificationStore()
const gamiStore = useGamificationStore()

const gami = computed(() => gamiStore.summary)

async function handleLogout() {
  await authStore.logout()
  router.push('/login')
}

const activeMain = ref(0)

// ── 내 계획 (GET /api/plans) ────────────────────────────────────────────────
const planFilters = ['전체', '예정', '완료']
const planFilter = ref('전체')
const allPlans = ref([])

// 날짜 포맷 'M.D' (예: 6.12)
function fmtDate(iso) {
  if (!iso) return ''
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return ''
  return `${d.getMonth() + 1}.${d.getDate()}`
}

// 종료일이 오늘보다 과거면 '완료', 아니면 '예정'
function deriveStatus(endDate) {
  if (!endDate) return '예정'
  const end = new Date(endDate)
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return end < today ? '완료' : '예정'
}

function mapPlan(p) {
  const dateRange = p.startDate && p.endDate
    ? `${fmtDate(p.startDate)}-${fmtDate(p.endDate)}`
    : fmtDate(p.startDate) || fmtDate(p.endDate)
  return {
    id: p.id,
    title: p.title,
    status: deriveStatus(p.endDate),
    dateRange,
    // 썸네일은 일정 제목 앞부분을 사용 (이미지 미보유)
    thumbLabel: (p.title || '여행') + '\n일정',
  }
}

async function loadPlans() {
  try {
    const { data } = await http.get('/api/plans', { params: { page: 0, size: 50 } })
    // Page 응답: { content: [...] } 또는 배열 모두 허용
    const list = Array.isArray(data) ? data : (data?.content ?? [])
    allPlans.value = list.map(mapPlan)
  } catch {
    allPlans.value = []
  }
}

const filteredPlans = computed(() => {
  if (planFilter.value === '전체') return allPlans.value
  return allPlans.value.filter((p) => p.status === planFilter.value)
})

const planCount = computed(() => allPlans.value.length)
const completedCount = computed(
  () => allPlans.value.filter((p) => p.status === '완료').length,
)

// ── 앨범 (GET /users/me/albums) ─────────────────────────────────────────────
const albums = ref([])
// 앨범이 0개면 Phase 2 안내 플레이스홀더를 보여준다.
const albumPhase2 = computed(() => albums.value.length === 0)

async function loadAlbums() {
  try {
    const { data } = await http.get('/users/me/albums')
    const list = Array.isArray(data) ? data : (data?.content ?? [])
    albums.value = list.map((a) => ({
      id: a.id,
      title: a.name,
      location: a.name,
      photoCount: a.photoCount ?? 0,
      thumbnailUrl: a.thumbnailUrl ?? null,
    }))
  } catch {
    albums.value = []
  }
}

// ── 뱃지 ────────────────────────────────────────────────────────────────────
// 게임화 API(GET /api/gamification/summary)의 실데이터 뱃지 목록.
// 미로그인/로딩 시 빈 배열 → 화면은 빈 상태로 정직하게 표시.
const badgeList = computed(() => gami.value?.badges ?? [])
const unlockedBadgeCount = computed(
  () => badgeList.value.filter((b) => b.unlocked).length,
)

// 탭 라벨에 실제 개수를 표시
const mainTabs = computed(() => [
  '내 계획',
  `앨범 ${albums.value.length}`,
  `뱃지 ${gami.value?.stats?.badges ?? unlockedBadgeCount.value}`,
])

onMounted(() => {
  notifStore.load()
  gamiStore.load()
  loadPlans()
  loadAlbums()
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
.scroll-content {
  flex: 1;
  overflow-y: auto;
}

/* Top bar */
.top-bar {
  display: flex;
  align-items: center;
  padding: 52px 16px 4px;
}
.icon-btn {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-ink);
}
.bell-wrap { position: relative; }
.notif-dot {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--color-peach);
  border: 1.5px solid white;
}

/* Profile */
.profile-section {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 8px 20px 16px;
}
.avatar-col {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}
.avatar-circle {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: linear-gradient(135deg, #efe6e4, #e0d4cc);
  display: flex;
  align-items: center;
  justify-content: center;
}
.avatar-text {
  font-size: 11px;
  font-weight: 500;
  color: var(--color-ink-muted);
}
.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
}
.camera-icon {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--color-surface);
  border: 1px solid var(--color-line-light);
  display: flex;
  align-items: center;
  justify-content: center;
}
.profile-info {
  padding-top: 8px;
  display: flex;
  flex-direction: column;
  gap: 5px;
}
.profile-name {
  font-size: 22px;
  font-weight: 800;
  color: var(--color-ink);
  letter-spacing: -0.6px;
}
.profile-bio {
  font-size: 13.5px;
  color: var(--color-ink-muted);
  letter-spacing: -0.2px;
}

/* Stats */
.stats-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
  padding: 12px 20px 16px;
  border-bottom: 1px solid var(--color-line-light);
}
.stat {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
}
.stat-num {
  font-size: 20px;
  font-weight: 800;
  color: var(--color-ink);
  letter-spacing: -0.5px;
}
.stat-label {
  font-size: 11.5px;
  color: var(--color-ink-muted);
  letter-spacing: -0.1px;
}
.stat-divider {
  width: 1px;
  height: 32px;
  background: var(--color-line-light);
  flex-shrink: 0;
}

/* Challenge card */
.challenge-card {
  margin: 16px 20px;
  padding: 14px 16px;
  background: #fff8f2;
  border: 1px solid #fde8d4;
  border-radius: var(--radius-lg);
  cursor: pointer;
}
.challenge-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}
.challenge-left {
  display: flex;
  align-items: center;
  gap: 7px;
}
.challenge-title {
  font-size: 13.5px;
  font-weight: 700;
  color: var(--color-ink);
  letter-spacing: -0.3px;
}
.challenge-phase {
  font-size: 11.5px;
  color: var(--color-ink-muted);
}
.challenge-bar {
  height: 5px;
  background: var(--color-line-light);
  border-radius: 3px;
  overflow: hidden;
  margin-bottom: 8px;
}
.challenge-fill {
  height: 100%;
  background: var(--color-peach);
  border-radius: 3px;
}
.challenge-hint {
  font-size: 12.5px;
  color: var(--color-ink-secondary);
  letter-spacing: -0.2px;
}

/* Main tabs */
.main-tab-bar {
  display: flex;
  border-bottom: 1px solid var(--color-line-light);
  padding: 0 20px;
}
.main-tab {
  flex: 1;
  padding: 12px 0;
  font-size: 14px;
  font-weight: 500;
  color: var(--color-ink-muted);
  position: relative;
  letter-spacing: -0.3px;
}
.main-tab.active {
  color: var(--color-ink);
  font-weight: 700;
}
.main-tab.active::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  right: 0;
  height: 2px;
  background: var(--color-peach);
  border-radius: 2px 2px 0 0;
}

/* Tab content */
.tab-content { padding: 0 0 20px; }

/* Sub filter chips */
.sub-filter {
  display: flex;
  gap: 8px;
  padding: 14px 20px 8px;
}
.sub-chip {
  padding: 6px 16px;
  border-radius: var(--radius-full);
  font-size: 13.5px;
  font-weight: 500;
  color: var(--color-ink-muted);
  background: var(--color-surface);
  transition: all 0.15s;
}
.sub-chip.active {
  background: var(--color-peach);
  color: white;
}

/* Plan items */
.plan-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 20px;
  border-bottom: 1px solid var(--color-line-light);
  cursor: pointer;
}
.plan-thumb {
  width: 56px;
  height: 56px;
  border-radius: var(--radius-md);
  background: linear-gradient(135deg, #ede0d8, #e0d0c8);
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}
.plan-thumb-label {
  font-size: 10px;
  font-weight: 500;
  color: var(--color-ink-muted);
  text-align: center;
  white-space: pre-line;
  line-height: 1.4;
}
.plan-info { flex: 1; min-width: 0; }
.plan-title-row {
  display: flex;
  align-items: center;
  gap: 7px;
  margin-bottom: 5px;
}
.plan-title {
  font-size: 14.5px;
  font-weight: 700;
  color: var(--color-ink);
  letter-spacing: -0.3px;
}
.plan-badge {
  padding: 2px 8px;
  border-radius: var(--radius-full);
  font-size: 11px;
  font-weight: 600;
  flex-shrink: 0;
}
.badge-upcoming { background: var(--color-peach-light); color: var(--color-peach-pressed); }
.badge-done { background: var(--color-surface); color: var(--color-ink-muted); }
.plan-meta {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12.5px;
  color: var(--color-ink-muted);
  margin-bottom: 4px;
}
.album-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 500;
  color: var(--color-ink-muted);
  margin-top: 2px;
}

/* Empty plans */
.empty-plans {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 48px 24px 24px;
  text-align: center;
}
.empty-icon {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: var(--color-surface);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
}
.empty-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-ink);
  letter-spacing: -0.4px;
  margin-bottom: 6px;
}
.empty-sub {
  font-size: 13.5px;
  color: var(--color-ink-muted);
  line-height: 1.6;
  margin-bottom: 24px;
}
.create-plan-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 28px;
  background: var(--color-peach);
  color: white;
  font-size: 14.5px;
  font-weight: 700;
  border-radius: var(--radius-xl);
  letter-spacing: -0.3px;
}

/* Album phase 2 */
.phase2-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 48px 24px 24px;
  text-align: center;
}
.phase2-icon {
  position: relative;
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: var(--color-surface);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 10px;
}
.lock-badge {
  position: absolute;
  bottom: -2px;
  right: -2px;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: var(--color-line-light);
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid white;
}
.phase2-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-ink-muted);
  background: var(--color-surface);
  padding: 3px 10px;
  border-radius: var(--radius-full);
  margin-bottom: 12px;
}
.phase2-title {
  font-size: 17px;
  font-weight: 700;
  color: var(--color-ink);
  letter-spacing: -0.4px;
  margin-bottom: 6px;
}
.phase2-sub {
  font-size: 13.5px;
  color: var(--color-ink-muted);
  line-height: 1.6;
}

/* Albums grid */
.album-section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px 10px;
}
.album-count {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-ink);
}
.make-album-btn {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-peach);
}
.albums-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  padding: 0 20px;
}
.album-card { cursor: pointer; }
.album-thumb {
  aspect-ratio: 1;
  background: linear-gradient(135deg, #ede0d8, #e0d0c8);
  border-radius: var(--radius-lg);
  position: relative;
  overflow: hidden;
  margin-bottom: 8px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 8px;
}
.album-photo-count {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: rgba(0,0,0,0.55);
  color: white;
  font-size: 11px;
  font-weight: 600;
  padding: 3px 7px;
  border-radius: var(--radius-full);
  align-self: flex-start;
}
.album-place-label {
  font-size: 11px;
  color: white;
  background: rgba(0,0,0,0.35);
  padding: 2px 8px;
  border-radius: 4px;
  align-self: flex-start;
}
.album-info { display: flex; align-items: center; gap: 6px; }
.album-title {
  font-size: 13.5px;
  font-weight: 600;
  color: var(--color-ink);
  letter-spacing: -0.3px;
}
.album-auto {
  font-size: 11px;
  font-weight: 500;
  color: var(--color-ink-muted);
  background: var(--color-surface);
  padding: 2px 7px;
  border-radius: var(--radius-full);
}

/* Badges */
.badge-header {
  padding: 16px 20px 12px;
  font-size: 14px;
  color: var(--color-ink-secondary);
}
.badge-header strong { color: var(--color-ink); font-weight: 700; }
.badges-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  padding: 0 20px;
}
.badge-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}
.badge-circle {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.badge-circle.unlocked { background: var(--color-peach); }
.badge-circle.locked { background: var(--color-surface); border: 1.5px solid var(--color-line-light); }
.badge-name {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-ink);
  text-align: center;
  letter-spacing: -0.2px;
}
.badge-progress {
  font-size: 11.5px;
  color: var(--color-ink-muted);
}

.logout-section {
  padding: 8px 20px 4px;
  border-top: 1px solid var(--color-line-light);
}
.logout-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 14px 0;
  font-size: 14px;
  font-weight: 500;
  color: var(--color-ink-muted);
  letter-spacing: -0.2px;
}
.logout-btn:active { opacity: 0.6; }

.bottom-spacer { height: 24px; }
</style>
