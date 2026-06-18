# Created: 2026-06-16 13:29:01
<template>
  <div class="page">
    <header class="plan-header">
      <h1 class="header-title">내 여행 계획</h1>
      <button class="add-btn" @click="showCreate = true">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round">
          <line x1="12" y1="5" x2="12" y2="19" />
          <line x1="5" y1="12" x2="19" y2="12" />
        </svg>
      </button>
    </header>

    <div class="scroll-content">
      <div v-if="plans.length === 0" class="empty-state">
        <div class="empty-icon">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="var(--color-line)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <rect x="3" y="4" width="18" height="18" rx="2" />
            <path d="M16 2v4M8 2v4M3 10h18" />
          </svg>
        </div>
        <p class="empty-title">아직 여행 계획이 없어요</p>
        <p class="empty-sub">새 계획을 만들어 여행을 준비해보세요</p>
        <button class="create-btn" @click="showCreate = true">계획 만들기</button>
      </div>

      <div v-else class="plan-list">
        <div v-for="plan in plans" :key="plan.id" class="plan-card" @click="selectPlan(plan)">
          <div class="plan-thumb">
            <div class="thumb-gradient" />
            <div class="plan-dates">
              <span class="date-label">{{ formatDate(plan.startDate) }}</span>
              <span class="date-sep">–</span>
              <span class="date-label">{{ formatDate(plan.endDate) }}</span>
            </div>
          </div>
          <div class="plan-info">
            <h3 class="plan-name">{{ plan.title }}</h3>
            <p class="plan-sub">{{ plan.destination }} · {{ dayCount(plan.startDate, plan.endDate) }}박 {{ dayCount(plan.startDate, plan.endDate) + 1 }}일</p>
            <div class="plan-spots">
              <span v-for="spot in plan.spots?.slice(0, 3)" :key="spot" class="spot-chip">{{ spot }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="companion-section">
        <div class="section-header">
          <h2 class="section-title">동행 구하기</h2>
          <button class="see-all">전체보기</button>
        </div>
        <div class="companion-list">
          <div v-for="comp in companions" :key="comp.id" class="companion-card">
            <div class="comp-header">
              <span class="comp-badge">{{ comp.category }}</span>
              <span class="comp-dday" :class="{ urgent: comp.dday <= 3 }">D-{{ comp.dday }}</span>
            </div>
            <h4 class="comp-title">{{ comp.title }}</h4>
            <p class="comp-sub">{{ comp.destination }} · {{ comp.dates }}</p>
            <div class="comp-footer">
              <div class="comp-members">
                <div v-for="i in comp.currentCount" :key="i" class="member-dot" />
                <span class="member-text">{{ comp.currentCount }}/{{ comp.maxCount }}명</span>
              </div>
              <button class="join-btn">참여하기</button>
            </div>
          </div>
        </div>
      </div>

      <div class="bottom-spacer" />
    </div>

    <Transition name="slide-up">
      <div v-if="showCreate" class="modal-overlay" @click.self="showCreate = false">
        <div class="modal">
          <div class="modal-header">
            <h3 class="modal-title">새 여행 계획</h3>
            <button class="close-btn" @click="showCreate = false">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="18" y1="6" x2="6" y2="18" />
                <line x1="6" y1="6" x2="18" y2="18" />
              </svg>
            </button>
          </div>
          <div class="modal-body">
            <input v-model="newPlan.title" class="modal-input" placeholder="계획 이름 (예: 제주 2박 3일)" />
            <input v-model="newPlan.destination" class="modal-input" placeholder="여행지" />
            <div class="date-row">
              <input v-model="newPlan.startDate" type="date" class="modal-input" />
              <span class="date-sep-label">~</span>
              <input v-model="newPlan.endDate" type="date" class="modal-input" />
            </div>
          </div>
          <button class="modal-submit" @click="createPlan">계획 만들기</button>
        </div>
      </div>
    </Transition>

  </div>
</template>

<script setup>
import { ref } from 'vue'

const showCreate = ref(false)
const plans = ref([
  {
    id: 1,
    title: '제주 힐링 여행',
    destination: '제주도',
    startDate: '2024-12-20',
    endDate: '2024-12-23',
    spots: ['성산일출봉', '협재해변', '우도'],
  },
])

const newPlan = ref({ title: '', destination: '', startDate: '', endDate: '' })

const companions = ref([
  { id: 1, category: '관광', title: '제주 동부 일주 같이 해요!', destination: '제주도', dates: '12월 28-30일', currentCount: 2, maxCount: 4, dday: 5 },
  { id: 2, category: '맛집', title: '부산 로컬 맛집 투어 동행 구해요', destination: '부산', dates: '1월 4-5일', currentCount: 1, maxCount: 3, dday: 12 },
  { id: 3, category: '액티비티', title: '한라산 백록담 등반 파티 모집', destination: '제주도', dates: '1월 10일', currentCount: 3, maxCount: 6, dday: 2 },
])

function formatDate(str) {
  if (!str) return ''
  const d = new Date(str)
  return `${d.getMonth() + 1}/${d.getDate()}`
}

function dayCount(start, end) {
  if (!start || !end) return 0
  return Math.round((new Date(end) - new Date(start)) / 86400000)
}

function selectPlan(plan) {
  console.log('plan selected', plan.id)
}

function createPlan() {
  if (!newPlan.value.title || !newPlan.value.destination) return
  plans.value.unshift({ id: Date.now(), ...newPlan.value, spots: [] })
  newPlan.value = { title: '', destination: '', startDate: '', endDate: '' }
  showCreate.value = false
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

.scroll-content {
  flex: 1;
  overflow-y: auto;
  padding: 0 20px;
}

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

.create-btn {
  background: var(--color-peach);
  color: white;
  font-size: 14px;
  font-weight: 600;
  padding: 12px 28px;
  border-radius: var(--radius-full);
  letter-spacing: -0.2px;
}

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
}

.thumb-gradient {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, transparent 40%, rgba(0, 0, 0, 0.25) 100%);
}

.plan-dates {
  display: flex;
  align-items: center;
  gap: 6px;
  position: relative;
  z-index: 1;
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

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: flex-end;
  z-index: 200;
}

.modal {
  background: var(--color-white);
  border-radius: 24px 24px 0 0;
  padding: 24px 20px 40px;
  width: 100%;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.modal-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-ink);
  letter-spacing: -0.4px;
}

.close-btn {
  color: var(--color-ink-muted);
}

.modal-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 24px;
}

.modal-input {
  width: 100%;
  background: var(--color-surface);
  border-radius: var(--radius-md);
  padding: 14px 16px;
  font-size: 14px;
  color: var(--color-ink);
  letter-spacing: -0.2px;
}

.modal-input::placeholder {
  color: var(--color-ink-muted);
}

.date-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.date-row .modal-input {
  flex: 1;
}

.date-sep-label {
  color: var(--color-ink-muted);
  flex-shrink: 0;
}

.modal-submit {
  width: 100%;
  background: var(--color-peach);
  color: white;
  font-size: 15px;
  font-weight: 700;
  padding: 16px;
  border-radius: var(--radius-xl);
  letter-spacing: -0.3px;
}

.slide-up-enter-active,
.slide-up-leave-active {
  transition: opacity 0.2s;
}

.slide-up-enter-from,
.slide-up-leave-to {
  opacity: 0;
}

.slide-up-enter-active .modal,
.slide-up-leave-active .modal {
  transition: transform 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.slide-up-enter-from .modal,
.slide-up-leave-to .modal {
  transform: translateY(100%);
}
</style>
