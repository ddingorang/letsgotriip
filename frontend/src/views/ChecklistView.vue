<script setup>
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import AppNavBar from '../components/layout/AppNavBar.vue';

const router = useRouter();

const transportItems = ref([
  { id: 1, text: 'KTX 예약 (서울→부산)', checked: true, link: '예약 확인', alarm: false, alarmSet: false, urgent: false },
  { id: 2, text: '숙소 예약 (해운대 오션뷰)', checked: true, link: '예약 확인', alarm: false, alarmSet: false, urgent: false },
  { id: 3, text: '귀환 KTX 예약', checked: true, link: '예약 확인', alarm: false, alarmSet: false, urgent: false },
]);

const essentialItems = ref([
  { id: 4, text: '신분증', checked: true, link: '', alarm: true, alarmSet: true, urgent: false },
  { id: 5, text: '충전기 + 보조배터리', checked: true, link: '', alarm: true, alarmSet: false, urgent: false },
  { id: 6, text: '여행자 보험 가입 ⚠️', checked: false, link: '가입하기', alarm: false, alarmSet: false, urgent: true },
  { id: 7, text: '상비약 챙기기', checked: false, link: '', alarm: true, alarmSet: false, urgent: false },
]);

const transportExpanded = ref(true);
const essentialExpanded = ref(true);
const dayExpanded = ref(false);

function toggleItem(list, id) {
  const item = list.find(i => i.id === id);
  if (item) item.checked = !item.checked;
}

const totalItems = computed(() => transportItems.value.length + essentialItems.value.length + 4);
const doneItems = computed(() =>
  [...transportItems.value, ...essentialItems.value].filter(i => i.checked).length
);
const progressPct = computed(() =>
  totalItems.value ? Math.round((doneItems.value / totalItems.value) * 100) : 0
);
const transportDone = computed(() => transportItems.value.filter(i => i.checked).length);
const essentialDone = computed(() => essentialItems.value.filter(i => i.checked).length);
</script>

<template>
  <div class="checklist-view">
    <AppNavBar title="여행 체크리스트" @back="router.back()">
      <template #action>
        <button class="icon-btn" aria-label="더보기">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="5" r="1" fill="currentColor"/><circle cx="12" cy="12" r="1" fill="currentColor"/><circle cx="12" cy="19" r="1" fill="currentColor"/></svg>
        </button>
      </template>
    </AppNavBar>

    <div class="trip-banner">
      <div class="trip-icon">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2"><path d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l4.553 2.276A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7"/></svg>
      </div>
      <div>
        <div class="trip-name">부산 2박 3일 여행</div>
        <div class="trip-date">06월 15일(토) ~ 06월 17일(월)</div>
      </div>
      <div class="trip-dday">D-3</div>
    </div>

    <div class="progress-section">
      <div class="progress-header">
        <div class="progress-label">전체 준비 현황</div>
        <div class="progress-count">{{ doneItems }}/{{ totalItems }} 완료</div>
      </div>
      <div class="progress-bar">
        <div class="progress-fill" :style="{ width: progressPct + '%' }"></div>
      </div>
      <div class="progress-sub">
        <div class="progress-sub-item">
          <div class="progress-dot" style="background:var(--color-primary-500)"></div>완료 {{ doneItems }}개
        </div>
        <div class="progress-sub-item">
          <div class="progress-dot" style="background:var(--border-strong)"></div>미완료 {{ totalItems - doneItems }}개
        </div>
        <div class="progress-sub-item">
          <div class="progress-dot" style="background:var(--color-error)"></div>긴급 2개
        </div>
      </div>
    </div>

    <div class="template-section">
      <div class="template-label">빠른 추가</div>
      <div class="template-chips">
        <div v-for="label in ['신분증','보조배터리','상비약','선크림','우산']" :key="label" class="template-chip">
          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
          {{ label }}
        </div>
      </div>
    </div>

    <div class="checklist">
      <!-- 교통/예약 -->
      <div class="checklist-group">
        <div class="group-header" @click="transportExpanded = !transportExpanded">
          <div class="group-icon" style="background:var(--color-primary-50)">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--color-primary-500)" stroke-width="2"><rect x="1" y="3" width="15" height="13"/><polygon points="16 8 20 8 23 11 23 16 16 16 16 8"/><circle cx="5.5" cy="18.5" r="2.5"/><circle cx="18.5" cy="18.5" r="2.5"/></svg>
          </div>
          <div class="group-title">교통 / 예약</div>
          <div class="group-count">{{ transportDone }}/{{ transportItems.length }} 완료</div>
          <svg class="group-chevron" :class="{ collapsed: !transportExpanded }" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 12 15 18 9"/></svg>
        </div>
        <template v-if="transportExpanded">
          <div
            v-for="item in transportItems"
            :key="item.id"
            class="check-item"
            @click="toggleItem(transportItems, item.id)"
          >
            <div class="checkbox" :class="{ checked: item.checked }">
              <svg v-if="item.checked" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="3"><polyline points="20 6 9 17 4 12"/></svg>
            </div>
            <div class="check-text" :class="{ done: item.checked }">{{ item.text }}</div>
            <div v-if="item.link" class="check-link">{{ item.link }}</div>
          </div>
        </template>
      </div>

      <!-- 필수 지참물 -->
      <div class="checklist-group">
        <div class="group-header" @click="essentialExpanded = !essentialExpanded">
          <div class="group-icon" style="background:#FFF3E0">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#E65100" stroke-width="2"><rect x="2" y="7" width="20" height="14" rx="2" ry="2"/><path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"/></svg>
          </div>
          <div class="group-title">필수 지참물</div>
          <div class="group-count">{{ essentialDone }}/{{ essentialItems.length }} 완료</div>
          <svg class="group-chevron" :class="{ collapsed: !essentialExpanded }" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 12 15 18 9"/></svg>
        </div>
        <template v-if="essentialExpanded">
          <div
            v-for="item in essentialItems"
            :key="item.id"
            class="check-item"
            @click="toggleItem(essentialItems, item.id)"
          >
            <div class="checkbox" :class="{ checked: item.checked }">
              <svg v-if="item.checked" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="3"><polyline points="20 6 9 17 4 12"/></svg>
            </div>
            <div
              class="check-text"
              :class="{ done: item.checked }"
              :style="item.urgent && !item.checked ? 'color:var(--color-error);font-weight:var(--weight-semibold)' : ''"
            >{{ item.text }}</div>
            <div v-if="item.link" class="check-link">{{ item.link }}</div>
            <svg
              v-else-if="item.alarm"
              class="check-alarm"
              :class="{ set: item.alarmSet }"
              width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
            ><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/></svg>
          </div>
          <div class="add-item-btn">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
            항목 추가
          </div>
        </template>
      </div>

      <!-- 여행 당일 -->
      <div class="checklist-group">
        <div class="group-header" @click="dayExpanded = !dayExpanded">
          <div class="group-icon" style="background:var(--surface-subtle)">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--text-secondary)" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
          </div>
          <div class="group-title">여행 당일 (D-3)</div>
          <div class="group-count">0/4 완료</div>
          <svg class="group-chevron" :class="{ collapsed: !dayExpanded }" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 12 15 18 9"/></svg>
        </div>
      </div>
    </div>

    <div class="bottom-bar">
      <button class="btn-outline">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/></svg>
        알림 설정
      </button>
      <button class="btn-add">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
        항목 추가
      </button>
    </div>
  </div>
</template>

<style scoped>
.checklist-view { background: var(--surface-subtle); }

.icon-btn { width: 36px; height: 36px; display: flex; align-items: center; justify-content: center; color: var(--text-secondary); border-radius: var(--radius-sm); }

.trip-banner { background: var(--color-primary-500); padding: 14px 20px; display: flex; align-items: center; gap: 12px; }
.trip-icon { width: 40px; height: 40px; background: rgba(255,255,255,0.2); border-radius: var(--radius-md); display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.trip-name { font: var(--weight-bold) var(--text-base)/1 var(--font-sans); color: #fff; margin-bottom: 3px; }
.trip-date { font: var(--weight-medium) var(--text-xs)/1 var(--font-sans); color: rgba(255,255,255,0.75); }
.trip-dday { margin-left: auto; background: rgba(255,255,255,0.2); border-radius: var(--radius-full); padding: 5px 12px; font: var(--weight-bold) var(--text-sm)/1 var(--font-sans); color: #fff; flex-shrink: 0; }

.progress-section { background: #fff; padding: 16px 20px; border-bottom: 1px solid var(--border-subtle); }
.progress-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.progress-label { font: var(--weight-semibold) var(--text-base)/1 var(--font-sans); color: var(--text-primary); }
.progress-count { font: var(--weight-bold) var(--text-sm)/1 var(--font-sans); color: var(--color-primary-500); }
.progress-bar { height: 8px; background: var(--surface-subtle); border-radius: var(--radius-full); overflow: hidden; }
.progress-fill { height: 100%; background: linear-gradient(90deg, var(--color-primary-400), #F9A96A); border-radius: var(--radius-full); transition: width 0.4s; }
.progress-sub { display: flex; gap: 12px; margin-top: 10px; }
.progress-sub-item { display: flex; align-items: center; gap: 5px; font: var(--weight-medium) var(--text-xs)/1 var(--font-sans); color: var(--text-secondary); }
.progress-dot { width: 8px; height: 8px; border-radius: 50%; }

.template-section { padding: 14px 20px; background: #fff; border-bottom: 1px solid var(--border-subtle); }
.template-label { font: var(--weight-medium) var(--text-xs)/1 var(--font-sans); color: var(--text-tertiary); margin-bottom: 8px; text-transform: uppercase; letter-spacing: 0.06em; }
.template-chips { display: flex; gap: 8px; overflow-x: auto; scrollbar-width: none; }
.template-chips::-webkit-scrollbar { display: none; }
.template-chip { flex-shrink: 0; display: flex; align-items: center; gap: 6px; padding: 7px 12px; border-radius: var(--radius-full); font: var(--weight-medium) var(--text-xs)/1 var(--font-sans); background: var(--surface-subtle); color: var(--text-secondary); border: 1.5px solid var(--border-default); cursor: pointer; white-space: nowrap; }
.template-chip svg { color: var(--color-primary-500); }

.checklist { padding: 16px 20px 120px; display: flex; flex-direction: column; gap: 16px; }
.checklist-group { background: var(--surface-bg); border-radius: var(--radius-lg); overflow: hidden; }
.group-header { display: flex; align-items: center; gap: 10px; padding: 14px 16px; border-bottom: 1px solid var(--border-subtle); cursor: pointer; }
.group-icon { width: 30px; height: 30px; border-radius: var(--radius-sm); display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.group-title { flex: 1; font: var(--weight-bold) var(--text-sm)/1 var(--font-sans); color: var(--text-primary); }
.group-count { font: var(--weight-medium) var(--text-xs)/1 var(--font-sans); color: var(--text-tertiary); }
.group-chevron { color: var(--text-tertiary); transition: transform 0.2s; }
.group-chevron.collapsed { transform: rotate(-90deg); }

.check-item { display: flex; align-items: center; gap: 12px; padding: 13px 16px; border-bottom: 1px solid var(--border-subtle); cursor: pointer; }
.check-item:last-child { border-bottom: none; }
.checkbox { width: 22px; height: 22px; border-radius: var(--radius-xs); border: 2px solid var(--border-strong); display: flex; align-items: center; justify-content: center; flex-shrink: 0; transition: all var(--duration-fast); }
.checkbox.checked { background: var(--color-primary-500); border-color: var(--color-primary-500); }
.check-text { flex: 1; font: var(--weight-medium) var(--text-sm)/1 var(--font-sans); color: var(--text-primary); }
.check-text.done { text-decoration: line-through; color: var(--text-tertiary); }
.check-link { font: var(--weight-medium) var(--text-xs)/1 var(--font-sans); color: var(--color-primary-500); flex-shrink: 0; }
.check-alarm { color: var(--text-tertiary); flex-shrink: 0; }
.check-alarm.set { color: var(--color-primary-500); }
.add-item-btn { display: flex; align-items: center; gap: 8px; padding: 12px 16px; color: var(--color-primary-500); font: var(--weight-semibold) var(--text-sm)/1 var(--font-sans); cursor: pointer; }

.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 430px;
  background: var(--surface-bg);
  border-top: 1px solid var(--border-subtle);
  padding: 14px 20px 34px;
  display: flex;
  gap: 10px;
}
.btn-add { flex: 1; height: 52px; background: var(--color-primary-500); color: #fff; border: none; border-radius: var(--radius-md); font: var(--weight-bold) var(--text-base)/1 var(--font-sans); cursor: pointer; display: flex; align-items: center; justify-content: center; gap: 8px; }
.btn-outline { height: 52px; padding: 0 18px; background: transparent; color: var(--text-secondary); border: 1.5px solid var(--border-default); border-radius: var(--radius-md); font: var(--weight-semibold) var(--text-sm)/1 var(--font-sans); cursor: pointer; white-space: nowrap; display: flex; align-items: center; gap: 6px; }
</style>
