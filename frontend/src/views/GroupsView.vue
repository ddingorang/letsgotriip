# Created: 2026-06-19 (그룹 · 단체할인 화면)
<template>
  <div class="page">
    <!-- Header -->
    <header class="grp-header">
      <h1 class="header-title">그룹</h1>
      <div class="header-right">
        <button class="icon-btn bell-wrap" @click="$router.push('/notifications')">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9" /><path d="M13.73 21a2 2 0 01-3.46 0" />
          </svg>
          <span v-if="notifStore.hasUnread" class="notif-dot" />
        </button>
      </div>
    </header>

    <div class="scroll-content">
      <!-- ── 내 그룹 ───────────────────────────────────────── -->
      <section class="section">
        <div class="section-header">
          <span class="section-title">내 그룹</span>
          <button class="text-action" @click="openCreate">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" /></svg>
            새 그룹
          </button>
        </div>

        <!-- Loading -->
        <div v-if="loading && groups.length === 0" class="loading-row">
          <div class="spinner" />
        </div>

        <!-- Empty -->
        <div v-else-if="groups.length === 0" class="empty-card">
          <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2" /><circle cx="9" cy="7" r="4" /><path d="M23 21v-2a4 4 0 00-3-3.87M16 3.13a4 4 0 010 7.75" />
          </svg>
          <p class="empty-title">아직 속한 그룹이 없어요</p>
          <p class="empty-sub">새 그룹을 만들거나 그룹 코드로 참여해 보세요.</p>
          <button class="empty-cta" @click="openCreate">새 그룹 만들기</button>
        </div>

        <!-- Group list -->
        <div v-else class="group-list">
          <div
            v-for="g in groups"
            :key="g.id"
            :class="['group-card', { expanded: expandedId === g.id }]"
          >
            <div class="group-main" @click="toggleExpand(g)">
              <div class="group-avatar">{{ initial(g.name) }}</div>
              <div class="group-body">
                <div class="group-name-row">
                  <span class="group-name">{{ g.name }}</span>
                  <span :class="['role-badge', isOwner(g) ? 'owner' : 'member']">
                    {{ isOwner(g) ? '방장' : '멤버' }}
                  </span>
                </div>
                <p v-if="g.description" class="group-desc">{{ g.description }}</p>
                <div class="group-meta">
                  <span class="meta-pill">
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2" /><circle cx="9" cy="7" r="4" /></svg>
                    {{ g.memberCount }}/{{ g.maxMembers }}명
                  </span>
                  <span v-if="g.memberCount >= g.maxMembers" class="meta-pill full">정원 마감</span>
                </div>
              </div>
              <svg class="chevron" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9" /></svg>
            </div>

            <!-- Expanded: members + actions -->
            <div v-if="expandedId === g.id" class="group-detail">
              <div class="member-head">
                <span class="member-head-title">멤버</span>
                <span v-if="membersLoading" class="member-head-sub">불러오는 중…</span>
              </div>
              <ul v-if="members.length" class="member-list">
                <li v-for="m in members" :key="m.userId" class="member-item">
                  <div class="member-avatar">{{ initial(memberName(m)) }}</div>
                  <div class="member-info">
                    <span class="member-name">
                      {{ m.userId === myUserId ? '나' : memberName(m) }}
                    </span>
                    <span class="member-joined">{{ formatDate(m.joinedAt) }} 참여</span>
                  </div>
                  <span :class="['role-badge', 'sm', m.role === 'OWNER' ? 'owner' : 'member']">
                    {{ m.role === 'OWNER' ? '방장' : '멤버' }}
                  </span>
                </li>
              </ul>

              <p v-if="detailError" class="inline-error">{{ detailError }}</p>

              <div class="detail-actions">
                <button
                  v-if="!isOwner(g)"
                  class="leave-btn"
                  :disabled="actionBusy"
                  @click="leave(g)"
                >
                  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4" /><polyline points="16 17 21 12 16 7" /><line x1="21" y1="12" x2="9" y2="12" /></svg>
                  그룹 나가기
                </button>
                <p v-else class="owner-hint">방장은 그룹을 나갈 수 없어요.</p>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- ── 그룹 코드로 참여 ───────────────────────────────── -->
      <section class="section">
        <div class="section-header">
          <span class="section-title">그룹 참여</span>
        </div>
        <div class="join-card">
          <p class="join-hint">친구에게 받은 그룹 번호로 바로 참여할 수 있어요.</p>
          <div class="join-row">
            <input
              v-model="joinId"
              type="number"
              min="1"
              inputmode="numeric"
              class="join-input"
              placeholder="그룹 번호 입력"
              @keyup.enter="joinByCode"
            />
            <button class="join-btn" :disabled="!joinId || actionBusy" @click="joinByCode">참여</button>
          </div>
          <p v-if="joinError" class="inline-error">{{ joinError }}</p>
        </div>
      </section>

      <!-- ── 단체 할인 ─────────────────────────────────────── -->
      <section class="section">
        <div class="section-header">
          <span class="section-title">단체 할인</span>
          <span class="demo-tag">DEMO</span>
        </div>
        <p class="discount-lead">인원이 모이면 더 저렴하게! 그룹 멤버를 위한 제휴 할인 정보예요.</p>

        <div v-if="discountsLoading && discounts.length === 0" class="loading-row">
          <div class="spinner" />
        </div>
        <div v-else class="discount-list">
          <div v-for="(d, i) in discounts" :key="i" class="discount-card">
            <div class="discount-rate">
              <span class="rate-num">{{ d.discountRate }}</span><span class="rate-pct">%</span>
            </div>
            <div class="discount-body">
              <div class="discount-title-row">
                <span class="discount-title">{{ d.title }}</span>
                <span v-if="d.demo" class="demo-badge">데모</span>
              </div>
              <p class="discount-desc">{{ d.description }}</p>
              <span class="discount-partner">제휴 · {{ d.partner }}</span>
            </div>
          </div>
        </div>
      </section>

      <div class="bottom-spacer" />
    </div>

    <!-- ── 새 그룹 만들기 시트 ───────────────────────────────── -->
    <div v-if="showCreate" class="sheet-backdrop" @click.self="closeCreate">
      <div class="sheet">
        <div class="sheet-handle" />
        <h2 class="sheet-title">새 그룹 만들기</h2>

        <div class="field">
          <label class="field-label">그룹 이름 <span class="req">*</span></label>
          <input v-model="createForm.name" maxlength="100" class="field-input" placeholder="예) 제주 여름 여행팀" />
        </div>

        <div class="field">
          <label class="field-label">소개</label>
          <textarea v-model="createForm.description" maxlength="2000" rows="3" class="field-textarea" placeholder="그룹 목적이나 일정을 적어주세요" />
        </div>

        <div class="field">
          <label class="field-label">최대 인원</label>
          <div class="chips-row">
            <button
              v-for="n in [4, 6, 10, 15, 20]"
              :key="n"
              :class="['chip-btn', { active: createForm.maxMembers === n }]"
              @click="createForm.maxMembers = n"
            >
              {{ n }}명
            </button>
          </div>
        </div>

        <p v-if="createError" class="inline-error">{{ createError }}</p>

        <div class="sheet-actions">
          <button class="sheet-cancel" @click="closeCreate">취소</button>
          <button class="sheet-submit" :disabled="!createForm.name.trim() || actionBusy" @click="submitCreate">
            만들기
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { groupApi } from '@/api/index.js'
import { useAuthStore } from '@/stores/auth.js'
import { useNotificationStore } from '@/stores/notification.js'

const authStore = useAuthStore()
const notifStore = useNotificationStore()

const myUserId = authStore.user?.userId ?? null

// ── state ───────────────────────────────────────────────────────────────────
const groups = ref([])
const loading = ref(false)

const discounts = ref([])
const discountsLoading = ref(false)

const expandedId = ref(null)
const members = ref([])
const membersLoading = ref(false)
const detailError = ref('')

const actionBusy = ref(false)

const joinId = ref('')
const joinError = ref('')

const showCreate = ref(false)
const createForm = reactive({ name: '', description: '', maxMembers: 10 })
const createError = ref('')

// ── helpers ─────────────────────────────────────────────────────────────────
function isOwner(g) {
  return myUserId != null && Number(g.ownerId) === Number(myUserId)
}
function initial(name) {
  return (name || '?').trim().charAt(0).toUpperCase()
}
// 멤버 표시명 — 닉네임 우선, 없으면 사용자 번호 폴백
function memberName(m) {
  return m?.nickname?.trim() || `사용자 #${m?.userId}`
}
function formatDate(iso) {
  if (!iso) return '-'
  const d = new Date(iso)
  if (isNaN(d.getTime())) return '-'
  return `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, '0')}.${String(d.getDate()).padStart(2, '0')}`
}
function errMsg(e, fallback) {
  return e?.response?.data?.message ?? e?.message ?? fallback
}

// ── load ────────────────────────────────────────────────────────────────────
async function loadGroups() {
  loading.value = true
  try {
    const { data } = await groupApi.list()
    groups.value = Array.isArray(data) ? data : []
  } catch {
    groups.value = []
  } finally {
    loading.value = false
  }
}

async function loadDiscounts() {
  discountsLoading.value = true
  try {
    const { data } = await groupApi.discounts()
    discounts.value = Array.isArray(data) ? data : []
  } catch {
    discounts.value = []
  } finally {
    discountsLoading.value = false
  }
}

// ── detail (members) ────────────────────────────────────────────────────────
async function toggleExpand(g) {
  if (expandedId.value === g.id) {
    expandedId.value = null
    return
  }
  expandedId.value = g.id
  members.value = []
  detailError.value = ''
  membersLoading.value = true
  try {
    const { data } = await groupApi.members(g.id)
    members.value = Array.isArray(data) ? data : []
  } catch (e) {
    detailError.value = errMsg(e, '멤버를 불러오지 못했어요.')
  } finally {
    membersLoading.value = false
  }
}

// ── actions ─────────────────────────────────────────────────────────────────
async function leave(g) {
  if (actionBusy.value) return
  detailError.value = ''
  actionBusy.value = true
  try {
    await groupApi.leave(g.id)
    expandedId.value = null
    await loadGroups()
  } catch (e) {
    detailError.value = errMsg(e, '그룹을 나가지 못했어요.')
  } finally {
    actionBusy.value = false
  }
}

async function joinByCode() {
  const id = Number(joinId.value)
  if (!id || actionBusy.value) return
  joinError.value = ''
  actionBusy.value = true
  try {
    await groupApi.join(id)
    joinId.value = ''
    await loadGroups()
    const joined = groups.value.find((g) => Number(g.id) === id)
    if (joined) toggleExpand(joined)
  } catch (e) {
    joinError.value = errMsg(e, '참여하지 못했어요. 그룹 번호와 정원을 확인해 주세요.')
  } finally {
    actionBusy.value = false
  }
}

// ── create sheet ────────────────────────────────────────────────────────────
function openCreate() {
  createForm.name = ''
  createForm.description = ''
  createForm.maxMembers = 10
  createError.value = ''
  showCreate.value = true
}
function closeCreate() {
  showCreate.value = false
}
async function submitCreate() {
  const name = createForm.name.trim()
  if (!name || actionBusy.value) return
  createError.value = ''
  actionBusy.value = true
  try {
    const { data } = await groupApi.create({
      name,
      description: createForm.description.trim() || null,
      maxMembers: createForm.maxMembers,
    })
    showCreate.value = false
    await loadGroups()
    if (data?.id) {
      const created = groups.value.find((g) => Number(g.id) === Number(data.id))
      if (created) toggleExpand(created)
    }
  } catch (e) {
    createError.value = errMsg(e, '그룹을 만들지 못했어요.')
  } finally {
    actionBusy.value = false
  }
}

onMounted(() => {
  loadGroups()
  loadDiscounts()
})
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  background: var(--color-white, #fff);
}

/* Header */
.grp-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 52px 16px 12px;
  border-bottom: 1px solid var(--color-line-light);
  flex-shrink: 0;
}
.header-title { font-size: 21px; font-weight: 800; color: var(--color-ink); letter-spacing: -0.5px; }
.header-right { display: flex; align-items: center; gap: 4px; }
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
  right: 9px;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--color-peach);
  border: 1.5px solid var(--color-white, #fff);
}

.scroll-content { flex: 1; overflow-y: auto; }

/* Section */
.section { padding: 20px 16px 4px; }
.section + .section { padding-top: 8px; }
.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}
.section-title { font-size: 16px; font-weight: 700; color: var(--color-ink); letter-spacing: -0.3px; }
.text-action {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 3px;
  font-size: 13.5px;
  font-weight: 600;
  color: var(--color-peach);
}
.demo-tag {
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.5px;
  color: var(--color-ink-muted);
  border: 1px solid var(--color-line);
  border-radius: var(--radius-full);
  padding: 2px 7px;
}

/* Loading / empty */
.loading-row { display: flex; justify-content: center; padding: 28px 0; }
.spinner {
  width: 22px;
  height: 22px;
  border: 2.5px solid var(--color-line);
  border-top-color: var(--color-peach);
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

.empty-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 4px;
  padding: 32px 20px;
  background: var(--color-surface);
  border-radius: var(--radius-lg);
}
.empty-title { font-size: 14.5px; font-weight: 700; color: var(--color-ink); margin-top: 8px; }
.empty-sub { font-size: 13px; color: var(--color-ink-muted); line-height: 1.5; }
.empty-cta {
  margin-top: 12px;
  padding: 10px 22px;
  border-radius: var(--radius-full);
  background: var(--color-peach);
  color: white;
  font-size: 14px;
  font-weight: 700;
}

/* Group cards */
.group-list { display: flex; flex-direction: column; gap: 10px; }
.group-card {
  background: var(--color-white, #fff);
  border: 1px solid var(--color-line-light);
  border-radius: var(--radius-lg);
  overflow: hidden;
  transition: border-color 0.15s;
}
.group-card.expanded { border-color: var(--color-peach); }
.group-main {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  cursor: pointer;
}
.group-avatar {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-md);
  background: var(--color-peach-light);
  color: var(--color-peach-pressed);
  font-size: 18px;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.group-body { flex: 1; min-width: 0; }
.group-name-row { display: flex; align-items: center; gap: 8px; margin-bottom: 2px; }
.group-name {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-ink);
  letter-spacing: -0.3px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.group-desc {
  font-size: 12.5px;
  color: var(--color-ink-muted);
  margin-bottom: 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.group-meta { display: flex; align-items: center; gap: 6px; }
.meta-pill {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 600;
  color: var(--color-ink-secondary);
}
.meta-pill.full { color: #d04010; }
.chevron { flex-shrink: 0; transition: transform 0.2s; }
.group-card.expanded .chevron { transform: rotate(180deg); }

.role-badge {
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: var(--radius-full);
  flex-shrink: 0;
}
.role-badge.owner { background: var(--color-peach); color: white; }
.role-badge.member { background: var(--color-surface); color: var(--color-ink-secondary); }
.role-badge.sm { font-size: 10.5px; padding: 1px 7px; }

/* Group detail */
.group-detail {
  border-top: 1px solid var(--color-line-light);
  padding: 14px;
  background: var(--color-surface-alt, #faf9f7);
}
.member-head { display: flex; align-items: baseline; gap: 8px; margin-bottom: 10px; }
.member-head-title { font-size: 13px; font-weight: 700; color: var(--color-ink); }
.member-head-sub { font-size: 12px; color: var(--color-ink-muted); }
.member-list { display: flex; flex-direction: column; gap: 8px; }
.member-item { display: flex; align-items: center; gap: 10px; }
.member-avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: var(--color-line);
  color: var(--color-ink-secondary);
  font-size: 12px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.member-info { flex: 1; display: flex; flex-direction: column; gap: 1px; min-width: 0; }
.member-name { font-size: 13.5px; font-weight: 600; color: var(--color-ink); }
.member-joined { font-size: 11.5px; color: var(--color-ink-muted); }

.detail-actions { margin-top: 14px; }
.leave-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 9px 16px;
  border-radius: var(--radius-full);
  border: 1.5px solid var(--color-line);
  font-size: 13px;
  font-weight: 600;
  color: var(--color-ink-secondary);
  background: var(--color-white, #fff);
}
.leave-btn:disabled { opacity: 0.5; }
.owner-hint { font-size: 12.5px; color: var(--color-ink-muted); }

/* Join by code */
.join-card {
  padding: 16px;
  background: var(--color-surface);
  border-radius: var(--radius-lg);
}
.join-hint { font-size: 13px; color: var(--color-ink-secondary); margin-bottom: 12px; line-height: 1.5; }
.join-row { display: flex; gap: 8px; }
.join-input {
  flex: 1;
  padding: 12px 14px;
  background: var(--color-white, #fff);
  border-radius: var(--radius-md);
  border: 1.5px solid transparent;
  font-size: 14px;
  color: var(--color-ink);
  transition: border-color 0.15s;
}
.join-input:focus { border-color: var(--color-peach); }
.join-input::placeholder { color: var(--color-ink-muted); }
.join-btn {
  padding: 0 22px;
  border-radius: var(--radius-md);
  background: var(--color-peach);
  color: white;
  font-size: 14px;
  font-weight: 700;
  flex-shrink: 0;
}
.join-btn:disabled { opacity: 0.4; }

/* Discounts */
.discount-lead {
  font-size: 13px;
  color: var(--color-ink-secondary);
  line-height: 1.5;
  margin-bottom: 14px;
  letter-spacing: -0.2px;
}
.discount-list { display: flex; flex-direction: column; gap: 10px; }
.discount-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px;
  background: var(--color-white, #fff);
  border: 1px solid var(--color-line-light);
  border-radius: var(--radius-lg);
}
.discount-rate {
  width: 56px;
  height: 56px;
  border-radius: var(--radius-md);
  background: var(--color-peach-light);
  color: var(--color-peach-pressed);
  display: flex;
  align-items: baseline;
  justify-content: center;
  flex-shrink: 0;
  gap: 1px;
}
.rate-num { font-size: 24px; font-weight: 800; letter-spacing: -1px; }
.rate-pct { font-size: 13px; font-weight: 700; }
.discount-body { flex: 1; min-width: 0; }
.discount-title-row { display: flex; align-items: center; gap: 6px; margin-bottom: 3px; }
.discount-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--color-ink);
  letter-spacing: -0.3px;
}
.demo-badge {
  font-size: 10px;
  font-weight: 700;
  color: var(--color-ink-muted);
  background: var(--color-surface);
  border-radius: var(--radius-full);
  padding: 1px 6px;
  flex-shrink: 0;
}
.discount-desc { font-size: 12.5px; color: var(--color-ink-secondary); line-height: 1.45; margin-bottom: 4px; }
.discount-partner { font-size: 11.5px; color: var(--color-ink-muted); }

/* Inline error */
.inline-error {
  font-size: 12.5px;
  color: #e53e3e;
  margin-top: 8px;
  line-height: 1.4;
}

.bottom-spacer { height: calc(80px + var(--safe-bottom)); }

/* Create sheet */
.sheet-backdrop {
  position: fixed;          /* .page(overflow:hidden)에 갇히지 않게 뷰포트 기준 */
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: flex-end;
  z-index: 1000;            /* BottomNav(z-index:100) 위로 — 시트 하단이 가려지지 않게 */
  animation: fade-in 0.18s ease;
}
@keyframes fade-in { from { opacity: 0; } to { opacity: 1; } }
.sheet {
  width: 100%;
  background: var(--color-white, #fff);
  border-radius: var(--radius-xl) var(--radius-xl) 0 0;
  padding: 12px 20px calc(20px + var(--safe-bottom));
  animation: slide-up 0.22s ease;
}
@keyframes slide-up { from { transform: translateY(100%); } to { transform: translateY(0); } }
.sheet-handle {
  width: 40px;
  height: 4px;
  border-radius: var(--radius-full);
  background: var(--color-line);
  margin: 0 auto 16px;
}
.sheet-title { font-size: 18px; font-weight: 800; color: var(--color-ink); margin-bottom: 18px; letter-spacing: -0.4px; }

.field { display: flex; flex-direction: column; gap: 7px; margin-bottom: 16px; }
.field-label { font-size: 13px; font-weight: 600; color: var(--color-ink); }
.req { color: var(--color-peach); }
.field-input {
  width: 100%;
  padding: 12px 14px;
  background: var(--color-surface);
  border-radius: var(--radius-md);
  font-size: 14px;
  color: var(--color-ink);
  border: 1.5px solid transparent;
  transition: border-color 0.15s;
}
.field-input:focus { border-color: var(--color-peach); }
.field-input::placeholder { color: var(--color-ink-muted); }
.field-textarea {
  width: 100%;
  padding: 12px 14px;
  background: var(--color-surface);
  border-radius: var(--radius-md);
  font-size: 14px;
  color: var(--color-ink);
  line-height: 1.55;
  resize: none;
  border: 1.5px solid transparent;
  transition: border-color 0.15s;
}
.field-textarea:focus { border-color: var(--color-peach); }
.field-textarea::placeholder { color: var(--color-ink-muted); }

.chips-row { display: flex; flex-wrap: wrap; gap: 8px; }
.chip-btn {
  padding: 7px 16px;
  border-radius: var(--radius-full);
  font-size: 13.5px;
  font-weight: 500;
  color: var(--color-ink-muted);
  border: 1.5px solid var(--color-line);
  background: var(--color-white, #fff);
  transition: all 0.15s;
}
.chip-btn.active { background: var(--color-peach); color: white; border-color: var(--color-peach); }

.sheet-actions { display: flex; gap: 10px; margin-top: 8px; }
.sheet-cancel {
  flex: 1;
  padding: 14px;
  border-radius: var(--radius-xl);
  border: 1.5px solid var(--color-line);
  font-size: 15px;
  font-weight: 600;
  color: var(--color-ink-secondary);
}
.sheet-submit {
  flex: 2;
  padding: 14px;
  border-radius: var(--radius-xl);
  background: var(--color-peach);
  color: white;
  font-size: 15px;
  font-weight: 700;
}
.sheet-submit:disabled { opacity: 0.4; }
</style>
