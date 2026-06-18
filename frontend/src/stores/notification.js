/**
 * Notification / Notice store — 프론트엔드 전용(정적 시드).
 *
 * BE에 알림(/notifications)·공지(/notices) 엔드포인트가 아직 없어, 화면 흐름을
 * 완성하기 위한 로컬 시드 데이터로 동작한다. 추후 BE가 생기면 loadNotifications /
 * loadNotices 내부만 API 호출로 교체하면 된다.
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { noticeApi, notificationApi } from '@/api/index.js'

// ISO 시각 → "방금 전 / N분 전 / N시간 전 / N일 전 / YYYY.MM.DD"
function relativeTime(iso) {
  if (!iso) return ''
  const then = new Date(iso).getTime()
  if (Number.isNaN(then)) return ''
  const diffMin = Math.floor((Date.now() - then) / 60000)
  if (diffMin < 1) return '방금 전'
  if (diffMin < 60) return `${diffMin}분 전`
  const diffH = Math.floor(diffMin / 60)
  if (diffH < 24) return `${diffH}시간 전`
  const diffD = Math.floor(diffH / 24)
  if (diffD < 7) return `${diffD}일 전`
  const d = new Date(iso)
  return `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, '0')}.${String(d.getDate()).padStart(2, '0')}`
}

// ── 알림 시드 ─────────────────────────────────────────────────────────────────
const SEED_NOTIFICATIONS = [
  {
    id: 'n-1',
    type: 'companion',
    title: '동행 신청이 도착했어요',
    body: '‘제주 한 달 살기’ 모집글에 새 신청자가 있어요.',
    time: '방금 전',
    read: false,
  },
  {
    id: 'n-2',
    type: 'community',
    title: '내 글에 댓글이 달렸어요',
    body: '‘부산 2박 3일 후기’에 댓글이 1개 달렸어요.',
    time: '1시간 전',
    read: false,
  },
  {
    id: 'n-3',
    type: 'badge',
    title: '뱃지를 획득했어요 🎉',
    body: '‘미식가’ 뱃지를 획득했어요. 마이페이지에서 확인하세요.',
    time: '어제',
    read: true,
  },
]

// ── 공지 시드 ─────────────────────────────────────────────────────────────────
const SEED_NOTICES = [
  {
    id: 'notice-3',
    category: '업데이트',
    title: 'AI 일정 생성 기능이 개선되었어요',
    date: '2026-06-15',
    body: '이제 동행인·예산·테마를 입력하면 더 정교한 일정을 추천해 드려요. 마이페이지 > AI 여행에서 사용해 보세요.',
  },
  {
    id: 'notice-2',
    category: '안내',
    title: '카카오 지도 연동 안내',
    date: '2026-06-10',
    body: '탐색 화면의 지도가 카카오 지도로 전환되었습니다. 현재 위치 기반 주변 관광지 정렬을 지원해요.',
  },
  {
    id: 'notice-1',
    category: '필독',
    title: '서비스 베타 오픈 안내',
    date: '2026-06-05',
    body: '관통 여행 서비스가 베타로 오픈했습니다. 이용 중 불편한 점은 언제든 알려 주세요. 감사합니다.',
  },
]

export const useNotificationStore = defineStore('notification', () => {
  const notifications = ref([])
  const notices = ref([])
  const loaded = ref(false)

  const unreadCount = computed(
    () => notifications.value.filter((n) => !n.read).length,
  )
  const hasUnread = computed(() => unreadCount.value > 0)

  function mapNotice(n) {
    return {
      id: n.id,
      category: n.category,
      title: n.title,
      date: (n.createdAt ?? '').slice(0, 10),   // YYYY-MM-DD
      body: n.content,
    }
  }

  function mapAlert(n) {
    return {
      id: n.id,
      type: n.type,
      title: n.title,
      body: n.body,
      link: n.link,
      time: relativeTime(n.createdAt),
      read: n.read,
    }
  }

  async function load() {
    if (loaded.value) return
    loaded.value = true
    // 알림 — 실제 BE(/api/notifications, 인증). 실패(미로그인/오류) 시 시드 폴백
    try {
      const { data } = await notificationApi.list()
      notifications.value = Array.isArray(data) ? data.map(mapAlert) : []
    } catch {
      notifications.value = SEED_NOTIFICATIONS.map((n) => ({ ...n }))
    }
    // 공지 — 실제 BE(/api/notices). 실패 시 시드 폴백
    try {
      const { data } = await noticeApi.list()
      notices.value = Array.isArray(data) && data.length
        ? data.map(mapNotice)
        : SEED_NOTICES.map((n) => ({ ...n }))
    } catch {
      notices.value = SEED_NOTICES.map((n) => ({ ...n }))
    }
  }

  /** 강제 새로고침 — 알림 페이지 진입 시 최신화 */
  async function refresh() {
    loaded.value = false
    await load()
  }

  async function markAllRead() {
    notifications.value.forEach((n) => {
      n.read = true
    })
    try {
      await notificationApi.markAllRead()
    } catch {
      // 미로그인/오류 — 로컬 상태만 유지
    }
  }

  function noticeById(id) {
    return notices.value.find((n) => String(n.id) === String(id)) ?? null
  }

  return {
    notifications,
    notices,
    loaded,
    unreadCount,
    hasUnread,
    load,
    refresh,
    markAllRead,
    noticeById,
  }
})
