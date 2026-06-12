<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import AppNavBar from '../components/layout/AppNavBar.vue';
import { usePlanStore } from '../stores/plan.js';

const router = useRouter();
const planStore = usePlanStore();

// ── 날짜 유틸 ─────────────────────────────────────────────────────────────────
function toDateStr(d) {
  return d.toISOString().slice(0, 10);
}
function todayStr() {
  return toDateStr(new Date());
}
function plusDaysStr(n) {
  const d = new Date();
  d.setDate(d.getDate() + n);
  return toDateStr(d);
}
// "YYYY-MM-DD" → "M/D"
function fmtShort(dateStr) {
  if (!dateStr) return '';
  const [, m, d] = dateStr.split('-');
  return `${parseInt(m)}/${parseInt(d)}`;
}
// "HH:mm:ss" → "HH:mm"
function fmtTime(t) {
  if (!t) return '';
  return t.slice(0, 5);
}

// ── 계획 없음 → 신규 생성 폼 ──────────────────────────────────────────────────
const showCreateForm = ref(false);
const newTitle = ref('나의 여행');
const newStart = ref(todayStr());
const newEnd = ref(plusDaysStr(2));
const createError = ref('');

// ── 일자 탭 ───────────────────────────────────────────────────────────────────
const activeDay = ref(0);

const days = computed(() => {
  const c = planStore.current;
  if (!c?.days) return [];
  return c.days.map((day) => ({
    id: day.id,
    dayNo: day.dayNo,
    label: `${day.dayNo}일차`,
    date: fmtShort(dateForDayNo(day.dayNo)),
    places: day.places ?? [],
    memo: day.memo,
  }));
});

function dateForDayNo(dayNo) {
  const c = planStore.current;
  if (!c?.startDate) return null;
  const d = new Date(c.startDate);
  d.setDate(d.getDate() + (dayNo - 1));
  return toDateStr(d);
}

const activeDayData = computed(() => days.value[activeDay.value] ?? null);

// ── 로컬 places 배열 (순서 변경 작업용) ─────────────────────────────────────
const localPlaces = ref([]);

watch(activeDayData, (day) => {
  localPlaces.value = day ? [...day.places] : [];
}, { immediate: true });

// 탭 전환 시에도 동기화
watch(activeDay, () => {
  const day = activeDayData.value;
  localPlaces.value = day ? [...day.places] : [];
});

// ── 순서 변경 ─────────────────────────────────────────────────────────────────
function moveUp(idx) {
  if (idx === 0) return;
  const arr = [...localPlaces.value];
  [arr[idx - 1], arr[idx]] = [arr[idx], arr[idx - 1]];
  localPlaces.value = arr;
}
function moveDown(idx) {
  if (idx === localPlaces.value.length - 1) return;
  const arr = [...localPlaces.value];
  [arr[idx], arr[idx + 1]] = [arr[idx + 1], arr[idx]];
  localPlaces.value = arr;
}

// ── 저장 ─────────────────────────────────────────────────────────────────────
const saveMsg = ref('');

async function saveOrder() {
  const c = planStore.current;
  const day = activeDayData.value;
  if (!c || !day) return;
  saveMsg.value = '';
  try {
    await planStore.replacePlaces(c.id, day.dayNo, localPlaces.value);
    saveMsg.value = '순서를 저장했어요.';
    setTimeout(() => { saveMsg.value = ''; }, 2500);
  } catch {
    // planStore.error already set (conflict 포함)
  }
}

// ── 장소 삭제 ─────────────────────────────────────────────────────────────────
async function deletePlace(placeId) {
  const c = planStore.current;
  const day = activeDayData.value;
  if (!c || !day) return;
  try {
    await planStore.removePlace(c.id, day.dayNo, placeId);
  } catch {
    // planStore.error already set
  }
}

// ── 계획 생성 ─────────────────────────────────────────────────────────────────
async function createPlan() {
  createError.value = '';
  if (!newTitle.value.trim()) { createError.value = '계획 이름을 입력해 주세요.'; return; }
  if (!newStart.value || !newEnd.value) { createError.value = '날짜를 입력해 주세요.'; return; }
  if (newEnd.value < newStart.value) { createError.value = '종료일이 시작일보다 빠를 수 없어요.'; return; }
  try {
    await planStore.createPlan({ title: newTitle.value.trim(), startDate: newStart.value, endDate: newEnd.value });
    showCreateForm.value = false;
  } catch {
    createError.value = planStore.error ?? '계획 생성에 실패했어요.';
  }
}

// ── 마운트 ────────────────────────────────────────────────────────────────────
onMounted(async () => {
  await planStore.loadPlans();
  if (planStore.plans.length > 0) {
    await planStore.loadPlan(planStore.plans[0].id);
    activeDay.value = 0;
  } else {
    showCreateForm.value = true;
  }
});
</script>

<template>
  <div class="plan-edit">
    <AppNavBar title="여행 계획 편집" @back="router.back()">
      <template #action>
        <div class="nav-actions">
          <button class="icon-btn" aria-label="공유">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="18" cy="5" r="3"/><circle cx="6" cy="12" r="3"/><circle cx="18" cy="19" r="3"/><line x1="8.59" y1="13.51" x2="15.42" y2="17.49"/><line x1="15.41" y1="6.51" x2="8.59" y2="10.49"/></svg>
          </button>
          <button class="nav-save" @click="saveOrder" :disabled="planStore.loading">저장</button>
        </div>
      </template>
    </AppNavBar>

    <!-- ── 로딩 ── -->
    <div v-if="planStore.loading && !planStore.current && !showCreateForm" class="loading-state">
      <div class="loading-spinner"></div>
      <div class="loading-text">계획을 불러오는 중이에요…</div>
    </div>

    <!-- ── 계획 없음 → 생성 폼 ── -->
    <div v-else-if="showCreateForm" class="create-form-wrap">
      <div class="create-form-icon">
        <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="var(--color-primary-400)" stroke-width="1.5"><path d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2"/><rect x="9" y="3" width="6" height="4" rx="1"/><line x1="9" y1="12" x2="15" y2="12"/><line x1="9" y1="16" x2="13" y2="16"/></svg>
      </div>
      <div class="create-form-title">새 여행 계획 만들기</div>
      <div class="create-form-sub">아직 계획이 없어요. 지금 바로 만들어 보세요!</div>

      <div class="create-form-card">
        <div class="form-field">
          <label class="form-label">계획 이름</label>
          <input class="form-input" v-model="newTitle" placeholder="예) 부산 2박 3일 여행" maxlength="50" />
        </div>
        <div class="form-field">
          <label class="form-label">시작일</label>
          <input class="form-input" type="date" v-model="newStart" />
        </div>
        <div class="form-field">
          <label class="form-label">종료일</label>
          <input class="form-input" type="date" v-model="newEnd" />
        </div>
        <div v-if="createError" class="form-error">{{ createError }}</div>
        <button class="btn-create" @click="createPlan" :disabled="planStore.loading">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
          계획 만들기
        </button>
      </div>
    </div>

    <!-- ── 계획 있음 ── -->
    <template v-else-if="planStore.current">
      <!-- 헤더 -->
      <div class="plan-header">
        <div class="plan-title-row">
          <input class="plan-title-input" :value="planStore.current.title" readonly>
          <div class="plan-edit-icon">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
          </div>
        </div>
        <div class="plan-meta">
          <div class="plan-chip orange">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
            {{ fmtShort(planStore.current.startDate) }} ~ {{ fmtShort(planStore.current.endDate) }}
          </div>
          <div v-if="planStore.current.companions" class="plan-chip">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/></svg>
            {{ planStore.current.companions }}
          </div>
        </div>
      </div>

      <!-- 일자 탭 -->
      <div class="day-tabs">
        <div
          v-for="(day, i) in days"
          :key="day.id"
          class="day-tab"
          :class="{ active: activeDay === i }"
          @click="activeDay = i"
        >
          <div class="day-tab-label">{{ day.label }}</div>
          <div class="day-tab-date">{{ day.date }}</div>
        </div>
      </div>

      <!-- 에러/저장 메시지 -->
      <div v-if="planStore.error" class="status-msg status-error">{{ planStore.error }}</div>
      <div v-if="saveMsg" class="status-msg status-ok">{{ saveMsg }}</div>

      <!-- 본문 -->
      <div class="content">
        <!-- 지도 미리보기 영역 (Kakao 실연동 다음 단계 — 현재 places lat/lng 주석으로 연결 지점 표시) -->
        <!-- places[i].attraction.latitude, places[i].attraction.longitude 를 Kakao 마커로 사용할 것 -->
        <div class="map-preview">
          <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="rgba(0,100,180,0.3)" stroke-width="1"><path d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l4.553 2.276A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7"/></svg>
          <template v-for="(p, pi) in localPlaces" :key="p.id">
            <!-- 연결 지점: p.attraction.latitude, p.attraction.longitude -->
            <div class="map-dot" :style="`top:${30 + pi * 20}px;left:${60 + pi * 60}px;background:var(--color-primary-400)`"></div>
          </template>
          <div class="map-expand">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 3 21 3 21 9"/><polyline points="9 21 3 21 3 15"/><line x1="21" y1="3" x2="14" y2="10"/><line x1="3" y1="21" x2="10" y2="14"/></svg>
            지도 크게 보기
          </div>
        </div>

        <!-- 일자 요약 -->
        <div class="day-summary">
          <div class="day-summary-item">
            <div class="day-summary-val">{{ localPlaces.length }}곳</div>
            <div class="day-summary-label">방문 장소</div>
          </div>
          <div class="day-summary-item">
            <div class="day-summary-val">{{ activeDayData?.memo || '메모 없음' }}</div>
            <div class="day-summary-label">일정 메모</div>
          </div>
        </div>

        <!-- 장소 리스트 -->
        <div class="place-list">
          <!-- 빈 일자 -->
          <div v-if="localPlaces.length === 0" class="empty-state">
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="var(--text-tertiary)" stroke-width="1.5"><path d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"/><circle cx="12" cy="10" r="3"/></svg>
            <div class="empty-text">장소를 추가해 보세요</div>
          </div>

          <template v-for="(place, i) in localPlaces" :key="place.id">
            <div class="place-item">
              <div class="place-num-wrap">
                <div class="place-num">{{ i + 1 }}</div>
              </div>
              <div class="place-card">
                <!-- ▲▼ 순서 변경 (drag handle 자리) -->
                <div class="place-order-btns">
                  <button class="order-btn" :disabled="i === 0" @click="moveUp(i)" aria-label="위로">
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="18 15 12 9 6 15"/></svg>
                  </button>
                  <button class="order-btn" :disabled="i === localPlaces.length - 1" @click="moveDown(i)" aria-label="아래로">
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="6 9 12 15 18 9"/></svg>
                  </button>
                </div>
                <div class="place-icon">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"/><circle cx="12" cy="10" r="3"/></svg>
                </div>
                <div class="place-body">
                  <div class="place-name">{{ place.attraction?.title ?? '(이름 없음)' }}</div>
                  <div class="place-addr">{{ place.attraction?.addr ?? '' }}</div>
                  <div v-if="place.visitTime" class="place-time">{{ fmtTime(place.visitTime) }}</div>
                </div>
                <button class="place-del" @click="deletePlace(place.id)" aria-label="삭제">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
                </button>
              </div>
            </div>
          </template>

          <div class="add-place-btn" @click="router.push('/search')">
            <div class="add-place-icon">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
            </div>
            <div class="add-place-text">장소 추가하기</div>
          </div>
        </div>
      </div>

      <!-- 하단 바 -->
      <div class="bottom-bar">
        <button class="btn-route">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l4.553 2.276A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7"/></svg>
          경로 보기
        </button>
        <button class="btn-primary" @click="saveOrder" :disabled="planStore.loading">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"/><polyline points="17 21 17 13 7 13 7 21"/><polyline points="7 3 7 8 15 8"/></svg>
          저장하기
        </button>
      </div>
    </template>
  </div>
</template>

<style scoped>
.plan-edit { background: var(--surface-subtle); }

/* ── 로딩 ── */
.loading-state { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 16px; height: 60vh; }
.loading-spinner { width: 36px; height: 36px; border: 3px solid var(--border-subtle); border-top-color: var(--color-primary-500); border-radius: 50%; animation: spin 0.8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.loading-text { font: var(--weight-medium) var(--text-sm)/1 var(--font-sans); color: var(--text-tertiary); }

/* ── 계획 생성 폼 ── */
.create-form-wrap { display: flex; flex-direction: column; align-items: center; padding: 48px 20px 120px; gap: 12px; }
.create-form-icon { width: 80px; height: 80px; background: var(--color-primary-50); border-radius: 50%; display: flex; align-items: center; justify-content: center; }
.create-form-title { font: var(--weight-bold) var(--text-xl)/1 var(--font-sans); color: var(--text-primary); letter-spacing: -0.02em; }
.create-form-sub { font: var(--type-body-sm); color: var(--text-tertiary); }
.create-form-card { width: 100%; background: var(--surface-bg); border-radius: var(--radius-lg); padding: 20px; box-shadow: var(--shadow-sm); display: flex; flex-direction: column; gap: 14px; margin-top: 12px; }
.form-field { display: flex; flex-direction: column; gap: 6px; }
.form-label { font: var(--weight-semibold) var(--text-sm)/1 var(--font-sans); color: var(--text-secondary); }
.form-input { height: 46px; border: 1.5px solid var(--border-default); border-radius: var(--radius-md); padding: 0 14px; font: var(--weight-medium) var(--text-base)/1 var(--font-sans); color: var(--text-primary); background: var(--surface-bg); outline: none; }
.form-input:focus { border-color: var(--color-primary-400); }
.form-error { font: var(--type-caption); color: var(--color-error, #e53935); }
.btn-create { width: 100%; height: 52px; background: var(--color-primary-500); color: #fff; border: none; border-radius: var(--radius-md); font: var(--weight-bold) var(--text-base)/1 var(--font-sans); cursor: pointer; display: flex; align-items: center; justify-content: center; gap: 8px; margin-top: 4px; }
.btn-create:disabled { opacity: 0.6; cursor: not-allowed; }

/* ── 네비 ── */
.nav-actions { display: flex; align-items: center; gap: 4px; }
.icon-btn { width: 36px; height: 36px; display: flex; align-items: center; justify-content: center; color: var(--text-primary); border-radius: var(--radius-sm); }
.nav-save { padding: 0 14px; height: 34px; background: var(--color-primary-500); color: #fff; border: none; border-radius: var(--radius-full); font: var(--weight-semibold) var(--text-sm)/1 var(--font-sans); cursor: pointer; }
.nav-save:disabled { opacity: 0.6; cursor: not-allowed; }

/* ── 헤더 ── */
.plan-header { background: #fff; padding: 14px 20px; border-bottom: 1px solid var(--border-subtle); }
.plan-title-row { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }
.plan-title-input { flex: 1; font: var(--weight-bold) var(--text-xl)/1 var(--font-sans); color: var(--text-primary); border: none; background: none; outline: none; letter-spacing: -0.02em; }
.plan-edit-icon { color: var(--text-tertiary); flex-shrink: 0; }
.plan-meta { display: flex; gap: 8px; flex-wrap: wrap; }
.plan-chip { display: inline-flex; align-items: center; gap: 5px; background: var(--surface-subtle); border-radius: var(--radius-full); padding: 5px 10px; font: var(--weight-medium) var(--text-xs)/1 var(--font-sans); color: var(--text-secondary); }
.plan-chip.orange { background: var(--color-primary-50); color: var(--color-primary-600); }

/* ── 일자 탭 ── */
.day-tabs { display: flex; background: #fff; border-bottom: 1px solid var(--border-subtle); overflow-x: auto; scrollbar-width: none; }
.day-tabs::-webkit-scrollbar { display: none; }
.day-tab { flex-shrink: 0; display: flex; flex-direction: column; align-items: center; gap: 2px; padding: 10px 18px; border-bottom: 2.5px solid transparent; cursor: pointer; }
.day-tab.active { border-bottom-color: var(--color-primary-500); }
.day-tab-label { font: var(--weight-semibold) var(--text-sm)/1 var(--font-sans); color: var(--text-tertiary); }
.day-tab.active .day-tab-label { color: var(--color-primary-500); }
.day-tab-date { font: var(--type-caption); color: var(--text-tertiary); }

/* ── 상태 메시지 ── */
.status-msg { padding: 10px 20px; font: var(--weight-medium) var(--text-sm)/1 var(--font-sans); }
.status-error { background: #fff3f3; color: #c62828; }
.status-ok { background: #f0fdf4; color: #2e7d32; }

/* ── 본문 ── */
.content { padding: 16px 20px 130px; }

.map-preview { height: 120px; background: linear-gradient(135deg, #e8f4fd 0%, #d0e8f7 100%); border-radius: var(--radius-lg); margin-bottom: 14px; position: relative; overflow: hidden; display: flex; align-items: center; justify-content: center; cursor: pointer; }
.map-dot { position: absolute; width: 10px; height: 10px; background: var(--color-primary-500); border-radius: 50%; border: 2px solid #fff; box-shadow: var(--shadow-sm); }
.map-expand { position: absolute; bottom: 10px; right: 10px; display: flex; align-items: center; gap: 5px; background: #fff; border-radius: var(--radius-full); padding: 5px 10px; font: var(--weight-semibold) var(--text-xs)/1 var(--font-sans); color: var(--text-secondary); box-shadow: var(--shadow-sm); }

.day-summary { display: flex; gap: 10px; padding: 12px; background: var(--color-primary-50); border-radius: var(--radius-md); margin-bottom: 14px; }
.day-summary-item { flex: 1; display: flex; flex-direction: column; align-items: center; gap: 3px; }
.day-summary-val { font: var(--weight-bold) var(--text-base)/1 var(--font-sans); color: var(--color-primary-600); }
.day-summary-label { font: var(--type-caption); color: var(--color-primary-400); }

/* ── 장소 리스트 ── */
.place-list { display: flex; flex-direction: column; gap: 2px; position: relative; }
.place-list::before { content: ''; position: absolute; left: 19px; top: 24px; bottom: 60px; width: 2px; background: var(--border-subtle); z-index: 0; }

.empty-state { display: flex; flex-direction: column; align-items: center; gap: 10px; padding: 40px 0; }
.empty-text { font: var(--weight-medium) var(--text-sm)/1 var(--font-sans); color: var(--text-tertiary); }

.place-item { display: flex; gap: 12px; align-items: flex-start; position: relative; }
.place-num-wrap { display: flex; flex-direction: column; align-items: center; gap: 0; flex-shrink: 0; width: 40px; z-index: 1; }
.place-num { width: 26px; height: 26px; background: var(--color-primary-500); border-radius: var(--radius-full); display: flex; align-items: center; justify-content: center; font: var(--weight-bold) var(--text-xs)/1 var(--font-sans); color: #fff; flex-shrink: 0; }
.place-card { flex: 1; background: var(--surface-bg); border-radius: var(--radius-md); padding: 12px 14px; margin-bottom: 8px; display: flex; align-items: center; gap: 10px; box-shadow: var(--shadow-xs); }
.place-order-btns { display: flex; flex-direction: column; gap: 2px; flex-shrink: 0; }
.order-btn { width: 22px; height: 22px; display: flex; align-items: center; justify-content: center; background: var(--surface-subtle); border: none; border-radius: var(--radius-sm); color: var(--text-secondary); cursor: pointer; padding: 0; }
.order-btn:disabled { opacity: 0.3; cursor: not-allowed; }
.place-icon { width: 38px; height: 38px; background: var(--color-primary-50); border-radius: var(--radius-sm); display: flex; align-items: center; justify-content: center; color: var(--color-primary-500); flex-shrink: 0; }
.place-body { flex: 1; min-width: 0; }
.place-name { font: var(--weight-semibold) var(--text-sm)/1 var(--font-sans); color: var(--text-primary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.place-addr { font: var(--type-caption); color: var(--text-tertiary); margin-top: 3px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.place-time { font: var(--weight-medium) var(--text-xs)/1 var(--font-sans); color: var(--text-tertiary); margin-top: 4px; }
.place-del { width: 28px; height: 28px; display: flex; align-items: center; justify-content: center; color: var(--text-tertiary); cursor: pointer; flex-shrink: 0; background: none; border: none; }

.add-place-btn { display: flex; align-items: center; gap: 10px; padding: 14px; background: var(--surface-bg); border-radius: var(--radius-md); border: 1.5px dashed var(--border-default); cursor: pointer; margin-top: 6px; }
.add-place-icon { width: 36px; height: 36px; background: var(--color-primary-50); border-radius: var(--radius-sm); display: flex; align-items: center; justify-content: center; color: var(--color-primary-500); flex-shrink: 0; }
.add-place-text { font: var(--weight-semibold) var(--text-sm)/1 var(--font-sans); color: var(--color-primary-500); }

/* ── 하단 바 ── */
.bottom-bar { position: fixed; bottom: 0; left: 50%; transform: translateX(-50%); width: 100%; max-width: 430px; background: var(--surface-bg); border-top: 1px solid var(--border-subtle); padding: 14px 20px 34px; display: flex; gap: 10px; }
.btn-route { flex: 1; height: 52px; background: var(--surface-subtle); color: var(--text-primary); border: none; border-radius: var(--radius-md); font: var(--weight-semibold) var(--text-base)/1 var(--font-sans); cursor: pointer; display: flex; align-items: center; justify-content: center; gap: 8px; }
.btn-primary { flex: 1; height: 52px; background: var(--color-primary-500); color: #fff; border: none; border-radius: var(--radius-md); font: var(--weight-bold) var(--text-base)/1 var(--font-sans); cursor: pointer; display: flex; align-items: center; justify-content: center; gap: 8px; }
.btn-primary:disabled { opacity: 0.6; cursor: not-allowed; }
</style>
