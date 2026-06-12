<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import AppNavBar from '../components/layout/AppNavBar.vue';

const router = useRouter();

const activeFilter = ref(0);
const filters = [
  { label: '전체', icon: null },
  { label: '날짜 선택', icon: 'calendar' },
  { label: '부산', icon: null },
  { label: '제주', icon: null },
  { label: '서울', icon: null },
  { label: '모집중만', icon: null },
];

const companions = [
  {
    id: 1,
    avatarChar: '김',
    avatarStyle: 'background:var(--color-primary-100);color:var(--color-primary-600)',
    name: '김관통 · 30대 남성',
    rating: '4.9',
    trips: '12',
    statusClass: 'status-open',
    statusLabel: '모집중',
    title: '부산 2박 3일 같이 가실 분 구해요!',
    info: [
      { icon: 'calendar', val: '06/15 ~ 06/17' },
      { icon: 'location', val: '부산' },
      { icon: 'people', val: '20~30대 환영' },
      { icon: 'money', val: '10~30만원' },
    ],
    tags: ['맛집탐방', '해변', '자유여행'],
    people: [
      { char: '김', style: 'background:var(--color-primary-100);color:var(--color-primary-600)' },
      { char: '박', style: 'background:#E8F5E9;color:#2E7D32' },
    ],
    peopleText: '2/4명 · 2자리 남음',
    btnClass: 'open',
    btnLabel: '참여 신청',
    opacity: 1,
  },
  {
    id: 2,
    avatarChar: '이',
    avatarStyle: 'background:#E8F5E9;color:#2E7D32',
    name: '이여행러 · 20대 여성',
    rating: '4.7',
    trips: '6',
    statusClass: 'status-almost',
    statusLabel: '마감임박',
    title: '제주 올레길 3박4일 함께해요',
    info: [
      { icon: 'calendar', val: '07/01 ~ 07/04' },
      { icon: 'location', val: '제주' },
      { icon: 'people', val: '여성만' },
      { icon: 'money', val: '30만원 내외' },
    ],
    tags: ['올레길', '하이킹', '자연'],
    people: [
      { char: '이', style: 'background:#E8F5E9;color:#2E7D32' },
      { char: '최', style: 'background:#FFF3E0;color:#E65100' },
      { char: '박', style: 'background:var(--color-primary-100);color:var(--color-primary-600)' },
    ],
    peopleText: '3/4명 · 1자리 남음',
    btnClass: 'almost',
    btnLabel: '마감임박',
    opacity: 1,
  },
  {
    id: 3,
    avatarChar: '박',
    avatarStyle: 'background:var(--color-neutral-100);color:var(--text-secondary)',
    name: '박솔로 · 30대 남성',
    rating: '4.8',
    trips: '18',
    statusClass: 'status-closed',
    statusLabel: '모집완료',
    title: '경주 1박 2일 역사탐방 동행',
    info: [],
    tags: ['역사/문화', '혼행'],
    people: [],
    peopleText: '4/4명 · 모집완료',
    btnClass: 'closed',
    btnLabel: '마감됨',
    opacity: 0.7,
  },
];
</script>

<template>
  <div class="companion">
    <AppNavBar title="동행 모집" @back="router.back()">
      <template #action>
        <div class="nav-write">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
          글쓰기
        </div>
      </template>
    </AppNavBar>

    <div class="filter-bar">
      <div
        v-for="(f, i) in filters"
        :key="i"
        class="filter-chip"
        :class="activeFilter === i ? 'active' : 'default'"
        @click="activeFilter = i"
      >
        <svg v-if="f.icon === 'calendar'" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
        {{ f.label }}
      </div>
    </div>

    <div class="companion-list">
      <div
        v-for="card in companions"
        :key="card.id"
        class="companion-card"
        :style="{ opacity: card.opacity }"
      >
        <div class="card-header">
          <div class="host-avatar" :style="card.avatarStyle">{{ card.avatarChar }}</div>
          <div class="host-info">
            <div class="host-name">{{ card.name }}</div>
            <div class="host-rating">
              <svg width="11" height="11" viewBox="0 0 24 24" fill="var(--color-warning)" stroke="none"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>
              {{ card.rating }} · 동행 {{ card.trips }}회
            </div>
          </div>
          <div class="status-badge" :class="card.statusClass">{{ card.statusLabel }}</div>
        </div>

        <div class="card-title">{{ card.title }}</div>

        <div v-if="card.info.length" class="card-info-grid">
          <div v-for="item in card.info" :key="item.val" class="card-info-item">
            <div class="card-info-icon">
              <svg v-if="item.icon === 'calendar'" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
              <svg v-else-if="item.icon === 'location'" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"/></svg>
              <svg v-else-if="item.icon === 'people'" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/></svg>
              <svg v-else-if="item.icon === 'money'" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="1" x2="12" y2="23"/><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/></svg>
            </div>
            <div class="card-info-val">{{ item.val }}</div>
          </div>
        </div>

        <div class="card-tags">
          <span v-for="tag in card.tags" :key="tag" class="card-tag">{{ tag }}</span>
        </div>

        <div class="people-row">
          <div v-if="card.people.length" class="people-avatars">
            <div
              v-for="(p, i) in card.people"
              :key="i"
              class="people-avatar"
              :style="p.style"
            >{{ p.char }}</div>
          </div>
          <div class="people-text">{{ card.peopleText }}</div>
          <button class="apply-btn" :class="card.btnClass">{{ card.btnLabel }}</button>
        </div>
      </div>
    </div>

    <div class="fab">
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
      동행 모집하기
    </div>
  </div>
</template>

<style scoped>
.companion { background: var(--surface-subtle); }

.nav-write { display: flex; align-items: center; gap: 6px; height: 36px; padding: 0 14px; background: var(--color-primary-500); color: #fff; border-radius: var(--radius-full); font: var(--weight-semibold) var(--text-sm)/1 var(--font-sans); cursor: pointer; white-space: nowrap; }

.filter-bar { background: #fff; padding: 12px 20px; border-bottom: 1px solid var(--border-subtle); display: flex; gap: 8px; overflow-x: auto; scrollbar-width: none; }
.filter-bar::-webkit-scrollbar { display: none; }
.filter-chip { flex-shrink: 0; display: flex; align-items: center; gap: 5px; padding: 7px 13px; border-radius: var(--radius-full); font: var(--weight-medium) var(--text-sm)/1 var(--font-sans); white-space: nowrap; cursor: pointer; border: 1.5px solid transparent; }
.filter-chip.active { background: var(--color-primary-50); color: var(--color-primary-500); border-color: var(--color-primary-300); }
.filter-chip.default { background: var(--surface-subtle); color: var(--text-secondary); border-color: var(--border-default); }

.companion-list { padding: 14px 20px 100px; display: flex; flex-direction: column; gap: 12px; }

.companion-card { background: var(--surface-bg); border-radius: var(--radius-xl); padding: 18px; box-shadow: var(--shadow-sm); cursor: pointer; }
.card-header { display: flex; align-items: flex-start; gap: 10px; margin-bottom: 12px; }
.host-avatar { width: 44px; height: 44px; border-radius: var(--radius-full); display: flex; align-items: center; justify-content: center; font: var(--weight-bold) var(--text-base)/1 var(--font-sans); flex-shrink: 0; }
.host-info { flex: 1; }
.host-name { font: var(--weight-semibold) var(--text-sm)/1 var(--font-sans); color: var(--text-primary); }
.host-rating { display: flex; align-items: center; gap: 4px; font: var(--weight-medium) var(--text-xs)/1 var(--font-sans); color: var(--text-secondary); margin-top: 3px; }
.status-badge { font: var(--weight-semibold) var(--text-xs)/1 var(--font-sans); padding: 4px 10px; border-radius: var(--radius-full); flex-shrink: 0; }
.status-open { background: #E8F5E9; color: #2E7D32; }
.status-almost { background: #FFF3E0; color: #E65100; }
.status-closed { background: var(--surface-subtle); color: var(--text-tertiary); }

.card-title { font: var(--weight-bold) var(--text-lg)/var(--leading-snug) var(--font-sans); color: var(--text-primary); margin-bottom: 8px; letter-spacing: -0.02em; }
.card-info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; margin-bottom: 12px; }
.card-info-item { display: flex; align-items: center; gap: 6px; }
.card-info-icon { color: var(--text-tertiary); flex-shrink: 0; }
.card-info-val { font: var(--weight-medium) var(--text-sm)/1 var(--font-sans); color: var(--text-secondary); }
.card-tags { display: flex; gap: 6px; flex-wrap: wrap; margin-bottom: 12px; }
.card-tag { font: var(--weight-medium) var(--text-xs)/1 var(--font-sans); background: var(--surface-subtle); color: var(--text-secondary); padding: 4px 10px; border-radius: var(--radius-full); }

.people-row { display: flex; align-items: center; gap: 10px; padding-top: 12px; border-top: 1px solid var(--border-subtle); }
.people-avatars { display: flex; }
.people-avatar { width: 26px; height: 26px; border-radius: var(--radius-full); border: 2px solid #fff; display: flex; align-items: center; justify-content: center; font: var(--weight-bold) 10px/1 var(--font-sans); flex-shrink: 0; }
.people-avatar + .people-avatar { margin-left: -8px; }
.people-text { flex: 1; font: var(--weight-medium) var(--text-xs)/1 var(--font-sans); color: var(--text-secondary); }
.apply-btn { padding: 8px 16px; border-radius: var(--radius-full); font: var(--weight-semibold) var(--text-sm)/1 var(--font-sans); border: none; cursor: pointer; }
.apply-btn.open { background: var(--color-primary-500); color: #fff; }
.apply-btn.almost { background: var(--color-primary-100); color: var(--color-primary-600); }
.apply-btn.closed { background: var(--surface-subtle); color: var(--text-tertiary); cursor: default; }

.fab {
  position: fixed;
  bottom: 24px;
  right: max(20px, calc(50% - 195px));
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--color-primary-500);
  color: #fff;
  padding: 14px 20px;
  border-radius: var(--radius-full);
  font: var(--weight-semibold) var(--text-sm)/1 var(--font-sans);
  box-shadow: 0 4px 16px rgba(247,143,87,.4);
  cursor: pointer;
}
</style>
