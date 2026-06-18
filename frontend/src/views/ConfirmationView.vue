<template>
  <div class="page">
    <div class="scroll-content">
      <!-- Hero success section -->
      <div class="success-top">
        <div class="success-icon">
          <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2.5">
            <polyline points="20 6 9 17 4 12" />
          </svg>
        </div>
        <h1 class="success-title">예약이 완료되었어요!</h1>
        <p class="success-sub">예약 확인 이메일을 발송했습니다</p>
        <div class="booking-no">예약번호 BK202406150023</div>
      </div>

      <!-- Ticket card -->
      <div class="ticket-wrap">
        <div class="ticket">
          <div class="ticket-top">
            <div class="ticket-badge">
              <span class="badge-dot" />
              예약 확정
            </div>
            <h2 class="ticket-name">해운대 오션뷰 호텔</h2>
            <p class="ticket-sub">부산 해운대구 해운대해변로 234</p>
            <div class="notch" />
            <div class="notch right" />
          </div>
          <div class="ticket-details">
            <div
              v-for="item in ticketDetails"
              :key="item.label"
              class="ticket-detail-item"
            >
              <span class="ticket-detail-label">{{ item.label }}</span>
              <span class="ticket-detail-value" style="white-space: pre-line">{{ item.value }}</span>
            </div>
          </div>
          <div class="ticket-total">
            <span class="ticket-total-label">결제 완료</span>
            <span class="ticket-total-value">₩358,000</span>
          </div>
        </div>
      </div>

      <!-- Action buttons -->
      <div class="actions">
        <button class="btn-primary" @click="$router.push('/plan')">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="4" width="18" height="18" rx="2" />
            <path d="M16 2v4M8 2v4M3 10h18" />
          </svg>
          여행 계획 보기
        </button>
        <div class="btn-row">
          <button class="btn-outline" @click="$router.push('/home')">
            <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2z" />
              <polyline points="9 22 9 12 15 12 15 22" />
            </svg>
            홈으로
          </button>
          <button class="btn-outline" @click="shareBooking">
            <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="18" cy="5" r="3" />
              <circle cx="6" cy="12" r="3" />
              <circle cx="18" cy="19" r="3" />
              <line x1="8.59" y1="13.51" x2="15.42" y2="17.49" />
              <line x1="15.41" y1="6.51" x2="8.59" y2="10.49" />
            </svg>
            {{ shareLabel }}
          </button>
        </div>
      </div>

      <!-- Notice -->
      <div class="notice">
        <div class="notice-title">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10" />
            <line x1="12" y1="8" x2="12" y2="12" />
            <line x1="12" y1="16" x2="12.01" y2="16" />
          </svg>
          안내사항
        </div>
        <ul class="notice-list">
          <li v-for="item in notices" :key="item" class="notice-item">{{ item }}</li>
        </ul>
      </div>

      <div class="bottom-spacer" />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const shareLabel = ref('공유하기')

async function shareBooking() {
  const shareData = {
    title: '관통여행 예약 확정',
    text: '해운대 오션뷰 호텔 · 예약번호 BK202406150023 — 관통여행에서 예약했어요!',
    url: window.location.origin,
  }
  try {
    if (navigator.share) {
      await navigator.share(shareData)
    } else if (navigator.clipboard) {
      await navigator.clipboard.writeText(`${shareData.text} ${shareData.url}`)
      shareLabel.value = '복사됨!'
      setTimeout(() => (shareLabel.value = '공유하기'), 1500)
    }
  } catch {
    // 사용자가 공유 시트를 취소한 경우 — 무시
  }
}

const ticketDetails = [
  { label: '체크인',   value: '2026.06.15 (토)\n15:00 이후' },
  { label: '체크아웃', value: '2026.06.17 (월)\n11:00 이전' },
  { label: '객실',     value: '오션뷰 디럭스' },
  { label: '인원',     value: '성인 2명' },
]

const notices = [
  '체크인 시 예약자 본인 신분증을 지참해 주세요.',
  '취소는 체크인 3일 전까지 무료 취소 가능합니다.',
  '문의: 관통여행 고객센터 1588-0000 (24시간)',
]
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  background: var(--color-surface);
}

.scroll-content {
  flex: 1;
  overflow-y: auto;
}

/* Hero */
.success-top {
  background: linear-gradient(160deg, var(--color-peach) 0%, #ff9a00 100%);
  padding: 52px 20px 60px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 12px;
}
.success-icon {
  width: 80px;
  height: 80px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid rgba(255, 255, 255, 0.4);
}
.success-title {
  font-size: 26px;
  font-weight: 800;
  color: white;
  letter-spacing: -0.6px;
  line-height: 1.2;
}
.success-sub {
  font-size: 15px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.85);
}
.booking-no {
  background: rgba(255, 255, 255, 0.2);
  border-radius: var(--radius-full);
  padding: 8px 18px;
  font-size: 13px;
  font-weight: 600;
  color: white;
}

/* Ticket */
.ticket-wrap {
  margin: -30px 20px 0;
  position: relative;
}
.ticket {
  background: var(--color-white);
  border-radius: var(--radius-xl);
  overflow: hidden;
  box-shadow: var(--shadow-card);
}
.ticket-top {
  padding: 20px;
  border-bottom: 1px dashed var(--color-line);
  position: relative;
}
.ticket-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 11.5px;
  font-weight: 600;
  background: var(--color-peach-light);
  color: var(--color-peach-pressed);
  padding: 4px 10px;
  border-radius: var(--radius-full);
  margin-bottom: 10px;
}
.badge-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-peach);
  display: inline-block;
}
.ticket-name {
  font-size: 19px;
  font-weight: 800;
  color: var(--color-ink);
  letter-spacing: -0.5px;
  margin-bottom: 5px;
}
.ticket-sub {
  font-size: 13px;
  color: var(--color-ink-secondary);
}
.notch {
  position: absolute;
  bottom: -16px;
  left: -16px;
  width: 32px;
  height: 32px;
  background: var(--color-surface);
  border-radius: 50%;
}
.notch.right {
  left: auto;
  right: -16px;
}

.ticket-details {
  padding: 20px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
.ticket-detail-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.ticket-detail-label {
  font-size: 11.5px;
  color: var(--color-ink-muted);
}
.ticket-detail-value {
  font-size: 13.5px;
  font-weight: 600;
  color: var(--color-ink);
  line-height: 1.5;
}

.ticket-total {
  padding: 16px 20px;
  background: var(--color-peach-light);
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.ticket-total-label {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-ink);
}
.ticket-total-value {
  font-size: 22px;
  font-weight: 800;
  color: var(--color-peach-pressed);
  letter-spacing: -0.5px;
}

/* Actions */
.actions {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.btn-primary {
  width: 100%;
  height: 54px;
  background: var(--color-peach);
  color: white;
  border-radius: var(--radius-lg);
  font-size: 15px;
  font-weight: 700;
  letter-spacing: -0.3px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: background 0.15s;
}
.btn-primary:active {
  background: var(--color-peach-pressed);
}
.btn-row {
  display: flex;
  gap: 10px;
}
.btn-outline {
  flex: 1;
  height: 50px;
  background: transparent;
  color: var(--color-ink);
  border: 1.5px solid var(--color-line);
  border-radius: var(--radius-lg);
  font-size: 14px;
  font-weight: 600;
  letter-spacing: -0.2px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
}

/* Notice */
.notice {
  margin: 0 20px 20px;
  background: var(--color-surface);
  border-radius: var(--radius-md);
  padding: 14px 16px;
}
.notice-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-ink-secondary);
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 6px;
}
.notice-list {
  display: flex;
  flex-direction: column;
  gap: 5px;
  list-style: none;
}
.notice-item {
  font-size: 12.5px;
  color: var(--color-ink-muted);
  padding-left: 10px;
  position: relative;
  line-height: 1.5;
}
.notice-item::before {
  content: '·';
  position: absolute;
  left: 0;
}

.bottom-spacer {
  height: 24px;
}
</style>
