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
  const alertError = ref(false)

  // SSE 구독 핸들 — 구독 취소(이탈) 시 abort
  let streamController = null
  // 사용자 의도(unsubscribe) 여부 — 의도적 종료면 재연결하지 않는다
  let intentionalClose = false
  // 지수 백오프 재연결 타이머/시도 횟수
  let reconnectTimer = null
  let reconnectAttempts = 0
  // 스트림 단절 대비 폴링 타이머
  let pollTimer = null

  const RECONNECT_BASE_MS = 2000   // 첫 재시도 지연
  const RECONNECT_MAX_MS = 30000   // 재시도 지연 상한
  const POLL_INTERVAL_MS = 60000   // 폴링 주기(1분)

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
    // 알림 — 실제 BE(/api/notifications, 인증). 실패 시 무음 시드 폴백이 아니라
    // 빈/에러 상태로 노출(가짜 데이터로 실패를 숨기지 않음).
    try {
      const { data } = await notificationApi.list()
      notifications.value = Array.isArray(data) ? data.map(mapAlert) : []
      alertError.value = false
    } catch {
      notifications.value = []
      alertError.value = true
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

  /** 단건 읽음 처리 — 알림 클릭(위치 이동) 시 호출. 로컬 즉시 반영 + 서버 동기화 */
  async function markRead(id) {
    const n = notifications.value.find((x) => String(x.id) === String(id))
    if (!n || n.read) return
    n.read = true
    try {
      await notificationApi.markRead(id)
    } catch {
      // 미로그인/오류 — 로컬 상태만 유지
    }
  }

  function noticeById(id) {
    return notices.value.find((n) => String(n.id) === String(id)) ?? null
  }

  /** SSE data 프레임(JSON 문자열) 1건 → 목록 상단 추가(동일 id 중복 방지) */
  function handleStreamFrame(raw) {
    // 프레임 수신 = 연결 정상 → 백오프 카운터 리셋(다음 단절 시 빠르게 재시도)
    reconnectAttempts = 0
    let payload
    try {
      payload = JSON.parse(raw)
    } catch {
      return // 비 JSON(핑/주석 등) 무시
    }
    if (!payload || payload.id == null) return
    if (notifications.value.some((n) => String(n.id) === String(payload.id))) return
    notifications.value.unshift(mapAlert(payload))
  }

  /**
   * 서버에서 알림 목록을 다시 받아 동기화 — 폴링/재연결 보강용.
   * load()/refresh() 의 loaded 가드와 무관하게 항상 최신화한다.
   * 실패는 무음(스트림 단절 중 일시 오류) — 기존 목록을 보존하고 다음 주기에 재시도.
   */
  async function syncFromServer() {
    try {
      const { data } = await notificationApi.list()
      notifications.value = Array.isArray(data) ? data.map(mapAlert) : []
      alertError.value = false
    } catch {
      // 일시 오류 — 기존 목록 유지, 상태는 건드리지 않음
    }
  }

  /** 폴링 시작 — 스트림이 끊겨도 주기적으로 최신화. 중복 시작 방지. */
  function startPolling() {
    if (pollTimer) return
    pollTimer = setInterval(syncFromServer, POLL_INTERVAL_MS)
  }

  /** 폴링 정지 */
  function stopPolling() {
    if (!pollTimer) return
    clearInterval(pollTimer)
    pollTimer = null
  }

  /** 대기 중인 재연결 타이머 취소 */
  function clearReconnect() {
    if (!reconnectTimer) return
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }

  /** 실제 스트림 연결 1회. 자연 종료(사용자 의도 X) 시 지수 백오프로 재연결한다. */
  function openStream() {
    if (streamController) return
    intentionalClose = false
    streamController = new AbortController()
    const controller = streamController
    notificationApi
      .connectStream(handleStreamFrame, { signal: controller.signal })
      .catch(() => {
        // 미로그인/네트워크/스트림 오류 — 무음(목록은 load()/폴링 결과 유지)
      })
      .finally(() => {
        if (streamController === controller) streamController = null
        // 사용자 의도(unsubscribe/abort) 없이 자연 종료된 경우에만 재연결
        if (!intentionalClose) scheduleReconnect()
      })
  }

  /** 지수 백오프 재연결 예약 — 단절 동안 폴링으로 최신화를 보장한다. */
  function scheduleReconnect() {
    if (reconnectTimer || intentionalClose) return
    // 스트림이 끊긴 동안에는 폴링으로 최신화를 보강한다
    startPolling()
    const delay = Math.min(RECONNECT_BASE_MS * 2 ** reconnectAttempts, RECONNECT_MAX_MS)
    reconnectAttempts++
    reconnectTimer = setTimeout(() => {
      reconnectTimer = null
      if (intentionalClose) return
      // 재연결 직전 1회 즉시 동기화로 누락분 메우기
      syncFromServer()
      openStream()
    }, delay)
  }

  /**
   * 실시간 알림 구독 — 진입 시 1회. 신규 알림 도착마다 목록 상단에 추가되고
   * (read:false 이므로) unreadCount 가 자동 증가한다. 중복 구독은 방지.
   * 스트림이 자연 종료되면 지수 백오프로 자동 재구독하고, 단절 시에도 폴링으로 최신화한다.
   */
  function subscribe() {
    if (streamController || reconnectTimer) return
    intentionalClose = false
    reconnectAttempts = 0
    startPolling()
    openStream()
  }

  /** 구독 해제 — 페이지 이탈 시 호출. 재연결/폴링까지 모두 정리(충돌 방지). */
  function unsubscribe() {
    // 사용자 의도 종료 표시 → finally 의 재연결 분기 차단
    intentionalClose = true
    clearReconnect()
    stopPolling()
    reconnectAttempts = 0
    if (streamController) {
      streamController.abort()
      streamController = null
    }
  }

  return {
    notifications,
    notices,
    loaded,
    alertError,
    unreadCount,
    hasUnread,
    load,
    refresh,
    markAllRead,
    markRead,
    subscribe,
    unsubscribe,
    noticeById,
  }
})
