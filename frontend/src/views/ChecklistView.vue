<template>
  <div class="page">
    <!-- Header -->
    <header class="nav-bar">
      <button class="icon-btn" @click="$router.back()">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M19 12H5M12 5l-7 7 7 7" />
        </svg>
      </button>
      <h1 class="nav-title">여행 체크리스트</h1>
      <button class="icon-btn" @click="reload" :disabled="loading">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="23 4 23 10 17 10" /><polyline points="1 20 1 14 7 14" />
          <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15" />
        </svg>
      </button>
    </header>

    <div class="scroll-content">
      <!-- 여행(계획) 선택 — 칩으로 여행별 체크리스트 전환 -->
      <div class="plan-section">
        <p class="plan-label">여행 선택</p>
        <div class="plan-chips">
          <!-- 전체(여행 미지정) -->
          <div
            class="plan-chip"
            :class="{ active: selectedPlanId == null }"
            @click="selectPlan(null)"
          >
            전체
          </div>
          <div
            v-for="p in plans"
            :key="p.id"
            class="plan-chip"
            :class="{ active: selectedPlanId === p.id }"
            @click="selectPlan(p.id)"
          >
            {{ p.title || '제목 없는 여행' }}
          </div>
          <div v-if="plansLoading" class="plan-empty">여행 목록 불러오는 중…</div>
          <div v-else-if="!plans.length" class="plan-empty">저장된 여행이 없어요</div>
        </div>
      </div>

      <!-- Progress -->
      <div class="progress-section">
        <div class="progress-header">
          <span class="progress-label">{{ selectedPlanTitle }} 준비 현황</span>
          <span class="progress-count">{{ doneItems }}/{{ totalItems }} 완료</span>
        </div>
        <div class="progress-bar">
          <div class="progress-fill" :style="{ width: progressPct + '%' }" />
        </div>
        <div class="progress-sub">
          <div class="progress-sub-item">
            <div class="progress-dot" style="background:var(--color-peach)" />완료 {{ doneItems }}개
          </div>
          <div class="progress-sub-item">
            <div class="progress-dot" style="background:var(--color-line)" />미완료 {{ totalItems - doneItems }}개
          </div>
        </div>
      </div>

      <!-- Template apply chips -->
      <div class="template-section">
        <p class="template-label">템플릿으로 시작</p>
        <div class="template-chips">
          <div
            v-for="tpl in templates"
            :key="tpl.key"
            class="template-chip"
            :class="{ disabled: applyingKey }"
            @click="applyTemplate(tpl)"
          >
            <svg v-if="applyingKey === tpl.key" class="spin" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
              <path d="M21 12a9 9 0 1 1-6.219-8.56" />
            </svg>
            <svg v-else width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
              <line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" />
            </svg>
            {{ tpl.name }}
          </div>
          <div v-if="!templates.length && !loading" class="template-empty">템플릿이 없습니다</div>
        </div>
      </div>

      <!-- Error / loading banners -->
      <div v-if="error" class="state-banner error">
        {{ error }}
        <button class="state-retry" @click="reload">다시 시도</button>
      </div>
      <div v-if="loading" class="state-banner">불러오는 중…</div>

      <!-- Empty state -->
      <div v-if="!loading && !error && !items.length" class="empty-state">
        <p class="empty-title">아직 체크리스트가 비어 있어요</p>
        <p class="empty-sub">위 템플릿을 적용하거나 항목을 직접 추가해 보세요.</p>
      </div>

      <!-- Checklist groups -->
      <div v-if="!loading && groups.length" class="checklist">
        <div v-for="group in groups" :key="group.category" class="checklist-group">
          <div class="group-header" @click="toggleGroup(group.category)">
            <div class="group-icon" :style="`background:${group.bg}`">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" :stroke="group.fg" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="9 11 12 14 22 4" /><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11" />
              </svg>
            </div>
            <span class="group-title">{{ group.category }}</span>
            <span class="group-count">{{ group.done }}/{{ group.items.length }} 완료</span>
            <svg class="group-chevron" :class="{ collapsed: collapsed[group.category] }" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="6 9 12 15 18 9" />
            </svg>
          </div>
          <template v-if="!collapsed[group.category]">
            <div
              v-for="item in group.items"
              :key="item.id"
              class="check-item"
              :class="{ busy: pendingIds.has(item.id) }"
            >
              <div class="checkbox" :class="{ checked: item.checked }" @click="toggleItem(item)">
                <svg v-if="item.checked" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="3">
                  <polyline points="20 6 9 17 4 12" />
                </svg>
              </div>
              <span class="check-text" :class="{ done: item.checked }" @click="toggleItem(item)">{{ item.title }}</span>
              <button class="item-delete" @click.stop="removeItem(item)" aria-label="삭제">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                  <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
                </svg>
              </button>
            </div>

            <!-- Inline add row for this group -->
            <div v-if="addingCategory === group.category" class="add-item-row">
              <input
                ref="addInputRef"
                v-model="newTitle"
                class="add-input"
                placeholder="항목 입력 후 Enter"
                @keydown.enter.prevent="confirmAdd(group.category)"
                @keydown.esc="cancelAdd"
              />
              <button class="add-confirm" :disabled="!newTitle.trim() || creating" @click="confirmAdd(group.category)">
                {{ creating ? '추가 중…' : '추가' }}
              </button>
              <button class="add-cancel" @click="cancelAdd">취소</button>
            </div>
            <div v-else class="add-item-btn" @click="startAdd(group.category)">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                <line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" />
              </svg>
              항목 추가
            </div>
          </template>
        </div>
      </div>

      <div class="bottom-spacer" />
    </div>

    <!-- Bottom action bar -->
    <div class="bottom-action-bar">
      <button class="btn-add" @click="startAdd(defaultCategory)">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
          <line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" />
        </svg>
        항목 추가
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, nextTick, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { storeToRefs } from 'pinia'
import { checklistApi } from '@/api/index.js'
import { usePlanStore } from '@/stores/plan.js'

const route = useRoute()

// ── 여행(계획) 선택 상태 ──────────────────────────────────────────────────────
// selectedPlanId: null = 전체(여행 미지정 포함), 숫자 = 해당 여행만.
// /checklist?planId=12 로 들어오면 그 여행을 초기 선택값으로 사용한다.
const planStore = usePlanStore()
const { plans } = storeToRefs(planStore)
const plansLoading = ref(false)

function parsePlanId(raw) {
  if (raw == null || raw === '') return null
  const n = Number(raw)
  return Number.isFinite(n) ? n : null
}
const selectedPlanId = ref(parsePlanId(route.query.planId))

// 진행 현황 라벨용 — 선택한 여행 제목(미지정이면 "전체")
const selectedPlanTitle = computed(() => {
  if (selectedPlanId.value == null) return '전체'
  const p = plans.value.find((x) => x.id === selectedPlanId.value)
  return p?.title || '선택한 여행'
})

const items = ref([])
const templates = ref([])
const loading = ref(false)
const error = ref('')
const applyingKey = ref('')
const creating = ref(false)
const pendingIds = ref(new Set())

const collapsed = reactive({})

// 카테고리별 그룹 아이콘 색상(라운드로빈)
const PALETTE = [
  { bg: 'var(--color-peach-light)', fg: 'var(--color-peach-pressed)' },
  { bg: '#FFF3E0', fg: '#E65100' },
  { bg: 'var(--color-surface)', fg: 'var(--color-ink-secondary)' },
  { bg: '#E8F5E9', fg: '#2E7D32' },
  { bg: '#E3F2FD', fg: '#1565C0' },
]
const FALLBACK_CATEGORY = '기타'

const groups = computed(() => {
  const map = new Map()
  for (const it of items.value) {
    const cat = it.category && it.category.trim() ? it.category : FALLBACK_CATEGORY
    if (!map.has(cat)) map.set(cat, [])
    map.get(cat).push(it)
  }
  let i = 0
  return [...map.entries()].map(([category, list]) => {
    const palette = PALETTE[i++ % PALETTE.length]
    return {
      category,
      items: list,
      done: list.filter((x) => x.checked).length,
      bg: palette.bg,
      fg: palette.fg,
    }
  })
})

const defaultCategory = computed(() => groups.value[0]?.category || FALLBACK_CATEGORY)

const totalItems = computed(() => items.value.length)
const doneItems = computed(() => items.value.filter((i) => i.checked).length)
const progressPct = computed(() => (totalItems.value ? Math.round((doneItems.value / totalItems.value) * 100) : 0))

function toggleGroup(category) {
  collapsed[category] = !collapsed[category]
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    // 선택한 여행이 있으면 그 여행 항목만, 없으면 전체 조회
    const params = selectedPlanId.value != null ? { planId: selectedPlanId.value } : {}
    const [listRes, tplRes] = await Promise.all([
      checklistApi.list(params),
      templates.value.length ? Promise.resolve({ data: templates.value }) : checklistApi.templates(),
    ])
    items.value = listRes.data ?? []
    templates.value = tplRes.data ?? []
  } catch (e) {
    error.value = '체크리스트를 불러오지 못했어요. 잠시 후 다시 시도해 주세요.'
  } finally {
    loading.value = false
  }
}

function reload() {
  if (loading.value) return
  load()
}

async function refreshItems() {
  const params = selectedPlanId.value != null ? { planId: selectedPlanId.value } : {}
  const { data } = await checklistApi.list(params)
  items.value = data ?? []
}

// 여행 칩 선택 — 선택 즉시 해당 여행 체크리스트로 갈아끼운다
function selectPlan(id) {
  if (selectedPlanId.value === id || loading.value) return
  selectedPlanId.value = id
  cancelAdd() // 다른 여행의 입력 행이 남지 않도록 초기화
  load()
}

async function toggleItem(item) {
  if (pendingIds.value.has(item.id)) return
  const next = new Set(pendingIds.value)
  next.add(item.id)
  pendingIds.value = next
  const prev = item.checked
  item.checked = !prev // 낙관적 업데이트
  try {
    const { data } = await checklistApi.toggle(item.id)
    if (data && typeof data.checked === 'boolean') item.checked = data.checked
  } catch (e) {
    item.checked = prev // 롤백
    error.value = '변경에 실패했어요. 다시 시도해 주세요.'
  } finally {
    const after = new Set(pendingIds.value)
    after.delete(item.id)
    pendingIds.value = after
  }
}

async function removeItem(item) {
  if (pendingIds.value.has(item.id)) return
  const next = new Set(pendingIds.value)
  next.add(item.id)
  pendingIds.value = next
  try {
    await checklistApi.remove(item.id)
    items.value = items.value.filter((x) => x.id !== item.id)
  } catch (e) {
    error.value = '삭제에 실패했어요. 다시 시도해 주세요.'
  } finally {
    const after = new Set(pendingIds.value)
    after.delete(item.id)
    pendingIds.value = after
  }
}

// ── 인라인 추가 ───────────────────────────────────────────────────────────────
const addingCategory = ref('')
const newTitle = ref('')
const addInputRef = ref(null)

async function startAdd(category) {
  addingCategory.value = category
  newTitle.value = ''
  if (collapsed[category]) collapsed[category] = false
  await nextTick()
  const el = Array.isArray(addInputRef.value) ? addInputRef.value[0] : addInputRef.value
  el?.focus?.()
}

function cancelAdd() {
  addingCategory.value = ''
  newTitle.value = ''
}

async function confirmAdd(category) {
  const title = newTitle.value.trim()
  if (!title || creating.value) return
  creating.value = true
  error.value = ''
  try {
    const payload = {
      title,
      category: category === FALLBACK_CATEGORY ? null : category,
    }
    if (selectedPlanId.value != null) payload.planId = selectedPlanId.value
    const { data } = await checklistApi.create(payload)
    if (data) items.value = [...items.value, data]
    newTitle.value = ''
    // 연속 추가 편의를 위해 입력 행 유지
    await nextTick()
    const el = Array.isArray(addInputRef.value) ? addInputRef.value[0] : addInputRef.value
    el?.focus?.()
  } catch (e) {
    error.value = '항목 추가에 실패했어요. 다시 시도해 주세요.'
  } finally {
    creating.value = false
  }
}

// ── 템플릿 적용 ───────────────────────────────────────────────────────────────
async function applyTemplate(tpl) {
  if (applyingKey.value) return
  applyingKey.value = tpl.key
  error.value = ''
  try {
    const params = { templateKey: tpl.key }
    if (selectedPlanId.value != null) params.planId = selectedPlanId.value
    await checklistApi.applyTemplate(params)
    await refreshItems()
  } catch (e) {
    error.value = '템플릿 적용에 실패했어요. 다시 시도해 주세요.'
  } finally {
    applyingKey.value = ''
  }
}

// 여행 목록을 불러와 칩으로 보여준다(이미 로드돼 있으면 재사용).
async function loadPlans() {
  if (plans.value.length) return
  plansLoading.value = true
  try {
    await planStore.loadPlans()
  } catch (e) {
    // 여행 목록 실패는 체크리스트 본체를 막지 않는다(전체 보기로 동작)
  } finally {
    plansLoading.value = false
  }
}

onMounted(() => {
  loadPlans()
  load()
})
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  background: var(--color-surface);
}

/* Nav bar */
.nav-bar {
  display: flex;
  align-items: center;
  height: 56px;
  padding: 0 4px;
  background: var(--color-white);
  border-bottom: 1px solid var(--color-line-light);
  flex-shrink: 0;
}
.icon-btn {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-ink);
  border-radius: 50%;
  flex-shrink: 0;
}
.icon-btn:disabled {
  opacity: 0.4;
}
.nav-title {
  flex: 1;
  text-align: center;
  font-size: 16px;
  font-weight: 600;
  color: var(--color-ink);
  letter-spacing: -0.3px;
}

/* Scroll */
.scroll-content {
  flex: 1;
  overflow-y: auto;
}

/* 여행 선택 칩 */
.plan-section {
  padding: 14px 20px;
  background: var(--color-white);
  border-bottom: 1px solid var(--color-line-light);
}
.plan-label {
  font-size: 11px;
  font-weight: 500;
  color: var(--color-ink-muted);
  margin-bottom: 8px;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}
.plan-chips {
  display: flex;
  gap: 8px;
  overflow-x: auto;
}
.plan-chip {
  flex-shrink: 0;
  padding: 7px 14px;
  border-radius: var(--radius-full);
  font-size: 12.5px;
  font-weight: 600;
  background: var(--color-surface);
  color: var(--color-ink-secondary);
  border: 1.5px solid var(--color-line-light);
  cursor: pointer;
  white-space: nowrap;
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
}
.plan-chip.active {
  background: var(--color-peach);
  color: white;
  border-color: var(--color-peach);
}
.plan-empty {
  font-size: 12.5px;
  color: var(--color-ink-muted);
  padding: 7px 2px;
  white-space: nowrap;
}

/* Progress */
.progress-section {
  background: var(--color-white);
  padding: 16px 20px;
  border-bottom: 1px solid var(--color-line-light);
}
.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.progress-label {
  font-size: 14.5px;
  font-weight: 600;
  color: var(--color-ink);
  letter-spacing: -0.3px;
}
.progress-count {
  font-size: 13.5px;
  font-weight: 700;
  color: var(--color-peach-pressed);
}
.progress-bar {
  height: 8px;
  background: var(--color-surface);
  border-radius: var(--radius-full);
  overflow: hidden;
}
.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--color-peach), #f9a96a);
  border-radius: var(--radius-full);
  transition: width 0.4s;
}
.progress-sub {
  display: flex;
  gap: 14px;
  margin-top: 10px;
}
.progress-sub-item {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 11.5px;
  font-weight: 500;
  color: var(--color-ink-secondary);
}
.progress-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

/* Template chips */
.template-section {
  padding: 14px 20px;
  background: var(--color-white);
  border-bottom: 1px solid var(--color-line-light);
}
.template-label {
  font-size: 11px;
  font-weight: 500;
  color: var(--color-ink-muted);
  margin-bottom: 8px;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}
.template-chips {
  display: flex;
  gap: 8px;
  overflow-x: auto;
}
.template-chip {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 7px 12px;
  border-radius: var(--radius-full);
  font-size: 12.5px;
  font-weight: 500;
  background: var(--color-surface);
  color: var(--color-ink-secondary);
  border: 1.5px solid var(--color-line-light);
  cursor: pointer;
  white-space: nowrap;
}
.template-chip.disabled {
  opacity: 0.5;
  pointer-events: none;
}
.template-chip svg {
  color: var(--color-peach-pressed);
}
.template-empty {
  font-size: 12.5px;
  color: var(--color-ink-muted);
  padding: 7px 2px;
}
.spin {
  animation: spin 0.8s linear infinite;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}

/* State banners */
.state-banner {
  margin: 12px 20px 0;
  padding: 12px 14px;
  background: var(--color-white);
  border-radius: var(--radius-md);
  font-size: 13px;
  font-weight: 500;
  color: var(--color-ink-secondary);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.state-banner.error {
  color: var(--color-error);
  background: #FDECEC;
}
.state-retry {
  flex-shrink: 0;
  font-size: 12.5px;
  font-weight: 700;
  color: var(--color-peach-pressed);
}

/* Empty */
.empty-state {
  margin: 40px 20px;
  text-align: center;
}
.empty-title {
  font-size: 14.5px;
  font-weight: 700;
  color: var(--color-ink);
  margin-bottom: 6px;
  letter-spacing: -0.3px;
}
.empty-sub {
  font-size: 12.5px;
  font-weight: 500;
  color: var(--color-ink-muted);
  line-height: 1.5;
}

/* Checklist groups */
.checklist {
  padding: 16px 20px 12px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.checklist-group {
  background: var(--color-white);
  border-radius: var(--radius-lg);
  overflow: hidden;
}
.group-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  border-bottom: 1px solid var(--color-line-light);
  cursor: pointer;
}
.group-icon {
  width: 30px;
  height: 30px;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.group-title {
  flex: 1;
  font-size: 13.5px;
  font-weight: 700;
  color: var(--color-ink);
  letter-spacing: -0.2px;
}
.group-count {
  font-size: 11.5px;
  font-weight: 500;
  color: var(--color-ink-muted);
}
.group-chevron {
  color: var(--color-ink-muted);
  transition: transform 0.2s;
}
.group-chevron.collapsed {
  transform: rotate(-90deg);
}

.check-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 13px 16px;
  border-bottom: 1px solid var(--color-line-light);
}
.check-item:last-child {
  border-bottom: none;
}
.check-item.busy {
  opacity: 0.55;
  pointer-events: none;
}
.checkbox {
  width: 22px;
  height: 22px;
  border-radius: var(--radius-sm);
  border: 2px solid var(--color-line);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.15s;
  cursor: pointer;
}
.checkbox.checked {
  background: var(--color-peach);
  border-color: var(--color-peach);
}
.check-text {
  flex: 1;
  font-size: 13.5px;
  font-weight: 500;
  color: var(--color-ink);
  letter-spacing: -0.2px;
  cursor: pointer;
}
.check-text.done {
  text-decoration: line-through;
  color: var(--color-ink-muted);
}
.item-delete {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-ink-muted);
  border-radius: 50%;
}
.item-delete:active {
  background: var(--color-surface);
  color: var(--color-error);
}

.add-item-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  color: var(--color-peach-pressed);
  font-size: 13.5px;
  font-weight: 600;
  cursor: pointer;
  letter-spacing: -0.2px;
}
.add-item-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
}
.add-input {
  flex: 1;
  height: 38px;
  border: 1.5px solid var(--color-line);
  border-radius: var(--radius-md);
  padding: 0 12px;
  font-size: 13.5px;
  color: var(--color-ink);
  background: var(--color-white);
}
.add-input:focus {
  border-color: var(--color-peach);
  outline: none;
}
.add-confirm {
  flex-shrink: 0;
  height: 38px;
  padding: 0 14px;
  background: var(--color-peach);
  color: white;
  border-radius: var(--radius-md);
  font-size: 13px;
  font-weight: 700;
}
.add-confirm:disabled {
  opacity: 0.5;
}
.add-cancel {
  flex-shrink: 0;
  height: 38px;
  padding: 0 10px;
  color: var(--color-ink-muted);
  font-size: 13px;
  font-weight: 600;
}

.bottom-spacer {
  height: 100px;
}

/* Bottom action bar — sits above BottomNav (shell provides the nav) */
.bottom-action-bar {
  position: absolute;
  bottom: var(--bottom-nav-height);
  left: 0;
  right: 0;
  background: var(--color-white);
  border-top: 1px solid var(--color-line-light);
  padding: 12px 20px;
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}
.btn-add {
  flex: 1;
  height: 50px;
  background: var(--color-peach);
  color: white;
  border-radius: var(--radius-lg);
  font-size: 14.5px;
  font-weight: 700;
  letter-spacing: -0.3px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: background 0.15s;
}
.btn-add:active {
  background: var(--color-peach-pressed);
}
</style>
