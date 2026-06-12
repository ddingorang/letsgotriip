<script setup>
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth.js';

const router = useRouter();
const auth = useAuthStore();

const displayName = computed(() => auth.user?.nickname || auth.user?.name || '여행자');
const displayEmail = computed(() => auth.user?.email || '');
const avatarChar = computed(() => (auth.user?.nickname || auth.user?.name || '여')[0]);

const stats = [
  { val: '12', label: '총 여행' },
  { val: '5', label: '찜한 곳' },
  { val: '8', label: '리뷰' },
  { val: '3', label: '쿠폰' },
];

const activeTab = ref('예정');
const bookingTabs = ['예정', '진행중', '완료', '취소'];

const bookings = [
  {
    id: 1,
    status: '예약 확정',
    statusClass: 'confirmed',
    bookingNo: 'BK202406150023',
    name: '해운대 오션뷰 호텔',
    detail: '2026.06.15 ~ 06.17 · 2박 · 성인 2명',
    price: '₩358,000',
  },
  {
    id: 2,
    status: '예약 확정',
    statusClass: 'confirmed',
    bookingNo: 'BK202407010011',
    name: '서울 → 부산 KTX',
    detail: '2026.07.01 09:00 출발 · 좌석 12A',
    price: '₩59,800',
  },
];

const menuGroups = [
  [
    { key: 'wish', label: '찜한 여행지', orange: true, badge: null, svg: '<path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>' },
    { key: 'review', label: '내가 쓴 리뷰', orange: true, badge: null, svg: '<path d="M14 9V5a3 3 0 0 0-3-3l-4 9v11h11.28a2 2 0 0 0 2-1.7l1.38-9a2 2 0 0 0-2-2.3H14z"/><path d="M7 22H4a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2h3"/>' },
    { key: 'coupon', label: '쿠폰함', orange: true, badge: '3', svg: '<path d="M20 12V22H4V12"/><path d="M22 7H2v5h20V7z"/><path d="M12 22V7"/><path d="M12 7H7.5a2.5 2.5 0 0 1 0-5C11 2 12 7 12 7z"/><path d="M12 7h4.5a2.5 2.5 0 0 0 0-5C13 2 12 7 12 7z"/>' },
  ],
  [
    { key: 'notification', label: '알림 설정', orange: false, badge: null, svg: '<path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/>' },
    { key: 'security', label: '보안 설정', orange: false, badge: null, svg: '<rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/>' },
    { key: 'support', label: '고객센터', orange: false, badge: null, svg: '<circle cx="12" cy="12" r="10"/><path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/><line x1="12" y1="17" x2="12.01" y2="17"/>' },
  ],
  [
    { key: 'logout', label: '로그아웃', orange: false, badge: null, error: true, svg: '<path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/>' },
  ],
];
</script>

<template>
  <div class="mypage">
    <!-- Profile Header -->
    <div class="profile-header">
      <div class="profile-row">
        <div class="avatar">{{ avatarChar }}</div>
        <div class="profile-info">
          <div class="profile-name">{{ displayName }}</div>
          <div class="profile-email">{{ displayEmail }}</div>
        </div>
        <button class="edit-btn">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
          수정
        </button>
      </div>
    </div>

    <div class="content-wrap">
      <!-- Stats -->
      <div class="stats-outer">
        <div class="stats-wrap">
          <template v-for="(s, i) in stats" :key="s.label">
            <div class="stat-item">
              <div class="stat-val">{{ s.val }}</div>
              <div class="stat-label">{{ s.label }}</div>
            </div>
            <div v-if="i < stats.length - 1" class="stat-divider"></div>
          </template>
        </div>
      </div>

      <!-- Booking History -->
      <div class="booking-section">
        <div class="section-header">
          <div class="section-title">예약 내역</div>
          <span class="section-more">전체보기 ›</span>
        </div>
        <div class="booking-tabs">
          <div
            v-for="tab in bookingTabs"
            :key="tab"
            class="booking-tab"
            :class="{ active: activeTab === tab }"
            @click="activeTab = tab"
          >{{ tab }}</div>
        </div>
        <div
          v-for="booking in bookings"
          :key="booking.id"
          class="booking-card"
          @click="router.push('/confirmation')"
        >
          <div class="booking-card-top">
            <div class="booking-status" :class="booking.statusClass">
              <span class="status-dot"></span>
              {{ booking.status }}
            </div>
            <div class="booking-date">예약번호 {{ booking.bookingNo }}</div>
          </div>
          <div class="booking-name">{{ booking.name }}</div>
          <div class="booking-detail">{{ booking.detail }}</div>
          <div class="booking-footer">
            <div class="booking-price">{{ booking.price }}</div>
            <div class="booking-action">예약 상세 →</div>
          </div>
        </div>
      </div>

      <!-- Menu -->
      <div class="menu-section">
        <div v-for="(group, gi) in menuGroups" :key="gi" class="menu-group">
          <div
            v-for="item in group"
            :key="item.key"
            class="menu-item"
            @click="item.key === 'logout' ? auth.logout().then(() => router.push('/')) : undefined"
          >
            <div class="menu-icon" :class="{ orange: item.orange, error: item.error }">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" v-html="item.svg"></svg>
            </div>
            <div class="menu-label" :class="{ error: item.error }">{{ item.label }}</div>
            <span v-if="item.badge" class="menu-badge">{{ item.badge }}</span>
            <svg v-if="!item.error" class="menu-arrow" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 18l6-6-6-6"/></svg>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.mypage { background: var(--surface-subtle); min-height: 100%; }

.profile-header { background: var(--color-primary-500); padding: 16px 20px 28px; }
.profile-row { display: flex; align-items: center; gap: 14px; }
.avatar { width: 64px; height: 64px; border-radius: var(--radius-full); background: rgba(255,255,255,0.2); display: flex; align-items: center; justify-content: center; color: #fff; font: var(--weight-bold) 24px/1 var(--font-sans); flex-shrink: 0; border: 2px solid rgba(255,255,255,0.4); }
.profile-info { flex: 1; }
.profile-name { font: var(--weight-bold) var(--text-xl)/1 var(--font-sans); color: #fff; margin-bottom: 5px; }
.profile-email { font: var(--type-body-sm); color: rgba(255,255,255,0.8); }
.edit-btn { display: flex; align-items: center; gap: 4px; font: var(--weight-medium) var(--text-sm)/1 var(--font-sans); color: rgba(255,255,255,0.85); cursor: pointer; background: rgba(255,255,255,0.15); padding: 8px 12px; border-radius: var(--radius-full); border: none; }

.content-wrap { padding-bottom: 100px; }

.stats-outer { padding: 0 20px; }
.stats-wrap { margin-top: -20px; background: var(--surface-bg); border-radius: var(--radius-xl); padding: 18px; box-shadow: var(--shadow-md); display: flex; }
.stat-item { flex: 1; display: flex; flex-direction: column; align-items: center; gap: 5px; }
.stat-divider { width: 1px; background: var(--border-subtle); margin: 4px 0; }
.stat-val { font: var(--weight-bold) var(--text-2xl)/1 var(--font-sans); color: var(--color-primary-500); }
.stat-label { font: var(--type-caption); color: var(--text-secondary); }

.booking-section { padding: 24px 20px 0; }
.section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.section-title { font: var(--weight-bold) var(--text-lg)/1 var(--font-sans); color: var(--text-primary); }
.section-more { font: var(--weight-medium) var(--text-sm)/1 var(--font-sans); color: var(--text-secondary); cursor: pointer; }
.booking-tabs { display: flex; background: var(--surface-subtle); border-radius: var(--radius-sm); padding: 4px; margin-bottom: 14px; }
.booking-tab { flex: 1; text-align: center; padding: 8px; font: var(--weight-medium) var(--text-sm)/1 var(--font-sans); color: var(--text-tertiary); border-radius: var(--radius-xs); cursor: pointer; }
.booking-tab.active { background: var(--surface-bg); color: var(--text-primary); font-weight: var(--weight-semibold); box-shadow: var(--shadow-xs); }

.booking-card { background: var(--surface-bg); border-radius: var(--radius-lg); padding: 16px; margin-bottom: 10px; cursor: pointer; }
.booking-card-top { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 10px; }
.booking-status { display: inline-flex; align-items: center; gap: 5px; font: var(--weight-semibold) var(--text-xs)/1 var(--font-sans); padding: 4px 9px; border-radius: var(--radius-full); }
.booking-status.confirmed { background: var(--color-primary-50); color: var(--color-primary-600); }
.booking-status.completed { background: var(--surface-subtle); color: var(--text-secondary); }
.status-dot { width: 6px; height: 6px; border-radius: 50%; background: var(--color-primary-500); flex-shrink: 0; display: inline-block; }
.booking-date { font: var(--type-caption); color: var(--text-tertiary); }
.booking-name { font: var(--weight-semibold) var(--text-base)/1 var(--font-sans); color: var(--text-primary); margin-bottom: 4px; }
.booking-detail { font: var(--type-body-sm); color: var(--text-secondary); }
.booking-footer { display: flex; justify-content: space-between; align-items: center; padding-top: 12px; border-top: 1px solid var(--border-subtle); margin-top: 12px; }
.booking-price { font: var(--weight-bold) var(--text-base)/1 var(--font-sans); color: var(--text-primary); }
.booking-action { font: var(--weight-semibold) var(--text-sm)/1 var(--font-sans); color: var(--color-primary-500); cursor: pointer; }

.menu-section { padding: 24px 20px 0; }
.menu-group { background: var(--surface-bg); border-radius: var(--radius-lg); overflow: hidden; margin-bottom: 12px; }
.menu-item { display: flex; align-items: center; gap: 12px; padding: 16px; border-bottom: 1px solid var(--border-subtle); cursor: pointer; }
.menu-item:last-child { border-bottom: none; }
.menu-icon { width: 36px; height: 36px; border-radius: var(--radius-sm); background: var(--surface-subtle); display: flex; align-items: center; justify-content: center; color: var(--text-secondary); flex-shrink: 0; }
.menu-icon.orange { background: var(--color-primary-50); color: var(--color-primary-500); }
.menu-icon.error { color: var(--color-error); }
.menu-label { flex: 1; font: var(--weight-medium) var(--text-base)/1 var(--font-sans); color: var(--text-primary); }
.menu-label.error { color: var(--color-error); }
.menu-badge { background: var(--color-error); color: #fff; font: var(--weight-bold) 10px/1 var(--font-sans); padding: 2px 6px; border-radius: var(--radius-full); }
.menu-arrow { color: var(--text-tertiary); }
</style>
