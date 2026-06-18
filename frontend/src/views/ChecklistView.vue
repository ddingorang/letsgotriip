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
      <button class="icon-btn">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="5" r="1" fill="currentColor" />
          <circle cx="12" cy="12" r="1" fill="currentColor" />
          <circle cx="12" cy="19" r="1" fill="currentColor" />
        </svg>
      </button>
    </header>

    <div class="scroll-content">
      <!-- Trip banner -->
      <div class="trip-banner">
        <div class="trip-icon">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l4.553 2.276A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7" />
          </svg>
        </div>
        <div class="trip-text">
          <p class="trip-name">부산 2박 3일 여행</p>
          <p class="trip-date">06월 15일(토) ~ 06월 17일(월)</p>
        </div>
        <div class="trip-dday">D-3</div>
      </div>

      <!-- Progress -->
      <div class="progress-section">
        <div class="progress-header">
          <span class="progress-label">전체 준비 현황</span>
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
          <div class="progress-sub-item">
            <div class="progress-dot" style="background:var(--color-error)" />긴급 2개
          </div>
        </div>
      </div>

      <!-- Quick add chips -->
      <div class="template-section">
        <p class="template-label">빠른 추가</p>
        <div class="template-chips">
          <div v-for="label in quickItems" :key="label" class="template-chip">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
              <line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" />
            </svg>
            {{ label }}
          </div>
        </div>
      </div>

      <!-- Checklist groups -->
      <div class="checklist">
        <!-- 교통/예약 -->
        <div class="checklist-group">
          <div class="group-header" @click="transportExpanded = !transportExpanded">
            <div class="group-icon" style="background:var(--color-peach-light)">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--color-peach-pressed)" stroke-width="2">
                <rect x="1" y="3" width="15" height="13" /><polygon points="16 8 20 8 23 11 23 16 16 16 16 8" />
                <circle cx="5.5" cy="18.5" r="2.5" /><circle cx="18.5" cy="18.5" r="2.5" />
              </svg>
            </div>
            <span class="group-title">교통 / 예약</span>
            <span class="group-count">{{ transportDone }}/{{ transportItems.length }} 완료</span>
            <svg class="group-chevron" :class="{ collapsed: !transportExpanded }" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="6 9 12 15 18 9" />
            </svg>
          </div>
          <template v-if="transportExpanded">
            <div
              v-for="item in transportItems"
              :key="item.id"
              class="check-item"
              @click="toggleItem(transportItems, item.id)"
            >
              <div class="checkbox" :class="{ checked: item.checked }">
                <svg v-if="item.checked" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="3">
                  <polyline points="20 6 9 17 4 12" />
                </svg>
              </div>
              <span class="check-text" :class="{ done: item.checked }">{{ item.text }}</span>
              <span v-if="item.link" class="check-link">{{ item.link }}</span>
            </div>
          </template>
        </div>

        <!-- 필수 지참물 -->
        <div class="checklist-group">
          <div class="group-header" @click="essentialExpanded = !essentialExpanded">
            <div class="group-icon" style="background:#FFF3E0">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#E65100" stroke-width="2">
                <rect x="2" y="7" width="20" height="14" rx="2" /><path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16" />
              </svg>
            </div>
            <span class="group-title">필수 지참물</span>
            <span class="group-count">{{ essentialDone }}/{{ essentialItems.length }} 완료</span>
            <svg class="group-chevron" :class="{ collapsed: !essentialExpanded }" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="6 9 12 15 18 9" />
            </svg>
          </div>
          <template v-if="essentialExpanded">
            <div
              v-for="item in essentialItems"
              :key="item.id"
              class="check-item"
              @click="toggleItem(essentialItems, item.id)"
            >
              <div class="checkbox" :class="{ checked: item.checked }">
                <svg v-if="item.checked" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="3">
                  <polyline points="20 6 9 17 4 12" />
                </svg>
              </div>
              <span
                class="check-text"
                :class="{ done: item.checked }"
                :style="item.urgent && !item.checked ? 'color:var(--color-error);font-weight:600' : ''"
              >{{ item.text }}</span>
              <span v-if="item.link" class="check-link">{{ item.link }}</span>
              <svg
                v-else-if="item.alarm"
                class="check-alarm"
                :class="{ set: item.alarmSet }"
                width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
              >
                <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" /><path d="M13.73 21a2 2 0 0 1-3.46 0" />
              </svg>
            </div>
            <div class="add-item-btn">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                <line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" />
              </svg>
              항목 추가
            </div>
          </template>
        </div>

        <!-- 여행 당일 -->
        <div class="checklist-group">
          <div class="group-header" @click="dayExpanded = !dayExpanded">
            <div class="group-icon" style="background:var(--color-surface)">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-secondary)" stroke-width="2">
                <circle cx="12" cy="12" r="10" /><polyline points="12 6 12 12 16 14" />
              </svg>
            </div>
            <span class="group-title">여행 당일 (D-3)</span>
            <span class="group-count">0/4 완료</span>
            <svg class="group-chevron" :class="{ collapsed: !dayExpanded }" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="6 9 12 15 18 9" />
            </svg>
          </div>
        </div>
      </div>

      <div class="bottom-spacer" />
    </div>

    <!-- Bottom action bar (note: BottomNav is provided by the shell; this is the page's own action bar above it) -->
    <div class="bottom-action-bar">
      <button class="btn-outline">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" /><path d="M13.73 21a2 2 0 0 1-3.46 0" />
        </svg>
        알림 설정
      </button>
      <button class="btn-add">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
          <line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" />
        </svg>
        항목 추가
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const transportExpanded = ref(true)
const essentialExpanded = ref(true)
const dayExpanded = ref(false)

const quickItems = ['신분증', '보조배터리', '상비약', '선크림', '우산']

const transportItems = ref([
  { id: 1, text: 'KTX 예약 (서울→부산)', checked: true,  link: '예약 확인', alarm: false, alarmSet: false, urgent: false },
  { id: 2, text: '숙소 예약 (해운대 오션뷰)', checked: true, link: '예약 확인', alarm: false, alarmSet: false, urgent: false },
  { id: 3, text: '귀환 KTX 예약', checked: true, link: '예약 확인', alarm: false, alarmSet: false, urgent: false },
])

const essentialItems = ref([
  { id: 4, text: '신분증', checked: true,  link: '', alarm: true,  alarmSet: true,  urgent: false },
  { id: 5, text: '충전기 + 보조배터리', checked: true, link: '', alarm: true, alarmSet: false, urgent: false },
  { id: 6, text: '여행자 보험 가입 ⚠️', checked: false, link: '가입하기', alarm: false, alarmSet: false, urgent: true },
  { id: 7, text: '상비약 챙기기', checked: false, link: '', alarm: true, alarmSet: false, urgent: false },
])

function toggleItem(list, id) {
  const item = list.find((i) => i.id === id)
  if (item) item.checked = !item.checked
}

const totalItems = computed(() => transportItems.value.length + essentialItems.value.length)
const doneItems = computed(() => [...transportItems.value, ...essentialItems.value].filter((i) => i.checked).length)
const progressPct = computed(() => totalItems.value ? Math.round((doneItems.value / totalItems.value) * 100) : 0)
const transportDone = computed(() => transportItems.value.filter((i) => i.checked).length)
const essentialDone = computed(() => essentialItems.value.filter((i) => i.checked).length)
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

/* Trip banner */
.trip-banner {
  background: var(--color-peach);
  padding: 14px 20px;
  display: flex;
  align-items: center;
  gap: 12px;
}
.trip-icon {
  width: 40px;
  height: 40px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.trip-text {
  flex: 1;
}
.trip-name {
  font-size: 14.5px;
  font-weight: 700;
  color: white;
  letter-spacing: -0.3px;
  margin-bottom: 3px;
}
.trip-date {
  font-size: 11.5px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.8);
}
.trip-dday {
  background: rgba(255, 255, 255, 0.2);
  border-radius: var(--radius-full);
  padding: 5px 12px;
  font-size: 13px;
  font-weight: 700;
  color: white;
  flex-shrink: 0;
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
.template-chip svg {
  color: var(--color-peach-pressed);
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
  cursor: pointer;
}
.check-item:last-child {
  border-bottom: none;
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
}
.check-text.done {
  text-decoration: line-through;
  color: var(--color-ink-muted);
}
.check-link {
  font-size: 12px;
  font-weight: 500;
  color: var(--color-peach-pressed);
  flex-shrink: 0;
}
.check-alarm {
  color: var(--color-ink-muted);
  flex-shrink: 0;
}
.check-alarm.set {
  color: var(--color-peach-pressed);
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
.btn-outline {
  height: 50px;
  padding: 0 16px;
  background: transparent;
  color: var(--color-ink-secondary);
  border: 1.5px solid var(--color-line);
  border-radius: var(--radius-lg);
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
  display: flex;
  align-items: center;
  gap: 6px;
}
</style>
