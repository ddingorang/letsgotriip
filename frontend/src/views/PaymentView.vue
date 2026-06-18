<template>
  <div class="page">
    <!-- Header -->
    <header class="nav-bar">
      <button class="icon-btn" @click="$router.back()">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M19 12H5M12 5l-7 7 7 7" />
        </svg>
      </button>
      <h1 class="nav-title">예약 / 결제</h1>
      <span class="nav-spacer" />
    </header>

    <!-- Step indicator -->
    <div class="steps">
      <div class="step">
        <div class="step-circle done">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="3">
            <polyline points="20 6 9 17 4 12" />
          </svg>
        </div>
        <span class="step-label">일정 선택</span>
      </div>
      <div class="step-line done" />
      <div class="step">
        <div class="step-circle active">2</div>
        <span class="step-label active">예약 정보</span>
      </div>
      <div class="step-line" />
      <div class="step">
        <div class="step-circle pending">3</div>
        <span class="step-label">결제</span>
      </div>
      <div class="step-line" />
      <div class="step">
        <div class="step-circle pending">4</div>
        <span class="step-label">완료</span>
      </div>
    </div>

    <div class="scroll-content">
      <!-- Booking Info -->
      <div class="section-card">
        <p class="section-title">예약 정보</p>
        <div v-for="row in bookingInfo" :key="row.label" class="info-row">
          <span class="info-label">{{ row.label }}</span>
          <span class="info-value">{{ row.value }}</span>
        </div>
      </div>

      <!-- Traveler Info -->
      <div class="section-card">
        <p class="section-title">예약자 정보</p>
        <div v-for="row in travelerInfo" :key="row.label" class="form-group">
          <label class="form-label">{{ row.label }}</label>
          <input class="form-input" :value="row.value" readonly />
        </div>
      </div>

      <!-- Payment Options -->
      <div class="section-card">
        <p class="section-title">결제 수단</p>
        <div class="pay-options">
          <div
            v-for="opt in paymentOptions"
            :key="opt.key"
            class="pay-option"
            :class="{ selected: selectedPayment === opt.key }"
            @click="selectedPayment = opt.key"
          >
            <div class="pay-radio" :class="{ checked: selectedPayment === opt.key }" />
            <div class="pay-icon" :style="opt.iconStyle">
              <template v-if="opt.isText">{{ opt.iconContent }}</template>
              <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 14.5v-9l6 4.5-6 4.5z" />
              </svg>
            </div>
            <div class="pay-text">
              <div class="pay-name">{{ opt.name }}</div>
              <div class="pay-sub">{{ opt.sub }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- Price Summary -->
      <div class="section-card">
        <p class="section-title">결제 금액</p>
        <div v-for="row in priceRows" :key="row.label" class="price-row">
          <span class="price-label">{{ row.label }}</span>
          <span class="price-value" :style="row.style">{{ row.value }}</span>
        </div>
        <div class="price-total-row">
          <span class="price-total-label">최종 결제 금액</span>
          <span class="price-total-value">₩358,000</span>
        </div>
      </div>

      <!-- Agreement -->
      <div class="section-card">
        <div class="agree-row" @click="agreed = !agreed">
          <div class="checkbox" :class="{ checked: agreed }">
            <svg v-if="agreed" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="3">
              <polyline points="20 6 9 17 4 12" />
            </svg>
          </div>
          <p class="agree-text">
            예약 조건 및 <span class="agree-link">이용약관</span>, <span class="agree-link">개인정보 처리방침</span>에 동의합니다.
          </p>
        </div>
      </div>

      <div class="bottom-spacer" />
    </div>

    <!-- Bottom CTA -->
    <div class="bottom-bar">
      <button class="pay-btn" :disabled="!agreed" @click="$router.push('/confirmation')">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <rect x="1" y="4" width="22" height="16" rx="2" ry="2" />
          <line x1="1" y1="10" x2="23" y2="10" />
        </svg>
        ₩358,000 결제하기
      </button>
      <p class="pay-note">SSL 보안 결제 · 개인정보는 안전하게 보호됩니다</p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const selectedPayment = ref('card')
const agreed = ref(true)

const bookingInfo = [
  { label: '숙소', value: '해운대 오션뷰 호텔' },
  { label: '체크인', value: '2026.06.15 (토) 15:00' },
  { label: '체크아웃', value: '2026.06.17 (월) 11:00' },
  { label: '인원', value: '성인 2명' },
  { label: '객실', value: '오션뷰 디럭스 · 1박' },
]

const travelerInfo = [
  { label: '이름', value: '김관통' },
  { label: '연락처', value: '010-1234-5678' },
  { label: '이메일', value: 'travel@example.com' },
]

const priceRows = [
  { label: '숙박비 (2박)', value: '₩360,000', style: '' },
  { label: '서비스 수수료', value: '₩18,000', style: '' },
  { label: '쿠폰 할인', value: '−₩20,000', style: 'color:var(--color-error)' },
]

const paymentOptions = [
  {
    key: 'card',
    name: '신용카드',
    sub: 'VISA **** 1234',
    iconStyle: 'background:#1A1F71;color:#fff',
    iconContent: 'VISA',
    isText: true,
  },
  {
    key: 'naverpay',
    name: '네이버페이',
    sub: '포인트 12,500원 보유',
    iconStyle: 'background:#03C75A;color:#fff',
    iconContent: '',
    isText: false,
  },
  {
    key: 'kakaopay',
    name: '카카오페이',
    sub: '간편 결제',
    iconStyle: 'background:#FFCD00;color:#3C1E1E',
    iconContent: '',
    isText: false,
  },
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

/* Nav bar */
.nav-bar {
  display: flex;
  align-items: center;
  height: 56px;
  padding: 0 8px 0 4px;
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
.nav-spacer {
  width: 44px;
  flex-shrink: 0;
}

/* Steps */
.steps {
  display: flex;
  align-items: center;
  padding: 14px 20px;
  background: var(--color-white);
  border-bottom: 1px solid var(--color-line-light);
  flex-shrink: 0;
}
.step {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}
.step-circle {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
}
.step-circle.done {
  background: var(--color-peach);
  color: white;
}
.step-circle.active {
  background: var(--color-peach);
  color: white;
}
.step-circle.pending {
  background: var(--color-surface);
  color: var(--color-ink-muted);
  border: 1.5px solid var(--color-line);
}
.step-label {
  font-size: 10px;
  font-weight: 500;
  color: var(--color-ink-muted);
  letter-spacing: -0.1px;
}
.step-label.active {
  color: var(--color-peach-pressed);
  font-weight: 700;
}
.step-line {
  flex: 1;
  height: 2px;
  background: var(--color-line);
  max-width: 36px;
  margin: 0 2px 12px;
}
.step-line.done {
  background: var(--color-peach);
}

/* Scroll area */
.scroll-content {
  flex: 1;
  overflow-y: auto;
  padding: 14px 20px 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* Section card */
.section-card {
  background: var(--color-white);
  border-radius: var(--radius-lg);
  padding: 18px;
}
.section-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-ink);
  letter-spacing: -0.3px;
  margin-bottom: 14px;
}

/* Info rows */
.info-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 9px 0;
  border-bottom: 1px solid var(--color-line-light);
}
.info-row:last-child {
  border-bottom: none;
  padding-bottom: 0;
}
.info-label {
  font-size: 13.5px;
  color: var(--color-ink-secondary);
}
.info-value {
  font-size: 13.5px;
  font-weight: 500;
  color: var(--color-ink);
  text-align: right;
}

/* Form group */
.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 14px;
}
.form-group:last-child {
  margin-bottom: 0;
}
.form-label {
  font-size: 12.5px;
  font-weight: 500;
  color: var(--color-ink-secondary);
}
.form-input {
  height: 46px;
  padding: 0 14px;
  background: var(--color-surface);
  border: 1.5px solid var(--color-line);
  border-radius: var(--radius-md);
  font-size: 14px;
  color: var(--color-ink);
  width: 100%;
}

/* Payment options */
.pay-options {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.pay-option {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  border: 1.5px solid var(--color-line);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s;
}
.pay-option.selected {
  border-color: var(--color-peach);
  background: var(--color-peach-light);
}
.pay-radio {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: 2px solid var(--color-line);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.15s;
}
.pay-radio.checked {
  border-color: var(--color-peach);
  background: var(--color-peach);
}
.pay-radio.checked::after {
  content: '';
  display: block;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: white;
}
.pay-icon {
  width: 38px;
  height: 26px;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  font-weight: 700;
  flex-shrink: 0;
}
.pay-text {
  flex: 1;
}
.pay-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-ink);
  letter-spacing: -0.2px;
}
.pay-sub {
  font-size: 11.5px;
  color: var(--color-ink-muted);
  margin-top: 2px;
}

/* Price rows */
.price-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
}
.price-label {
  font-size: 13.5px;
  color: var(--color-ink-secondary);
}
.price-value {
  font-size: 13.5px;
  font-weight: 500;
  color: var(--color-ink);
}
.price-total-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 0 0;
  border-top: 1.5px solid var(--color-line-light);
  margin-top: 6px;
}
.price-total-label {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-ink);
}
.price-total-value {
  font-size: 20px;
  font-weight: 800;
  color: var(--color-peach-pressed);
  letter-spacing: -0.5px;
}

/* Agreement */
.agree-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  cursor: pointer;
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
.agree-text {
  font-size: 13px;
  color: var(--color-ink-secondary);
  line-height: 1.5;
}
.agree-link {
  color: var(--color-peach-pressed);
  text-decoration: underline;
}

.bottom-spacer {
  height: 120px;
}

/* Bottom bar */
.bottom-bar {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: var(--color-white);
  border-top: 1px solid var(--color-line-light);
  padding: 14px 20px 32px;
  flex-shrink: 0;
}
.pay-btn {
  width: 100%;
  height: 54px;
  background: var(--color-peach);
  color: white;
  border-radius: var(--radius-lg);
  font-size: 16px;
  font-weight: 700;
  letter-spacing: -0.3px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: background 0.15s;
}
.pay-btn:active {
  background: var(--color-peach-pressed);
}
.pay-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}
.pay-note {
  text-align: center;
  font-size: 11.5px;
  color: var(--color-ink-muted);
  margin-top: 8px;
  letter-spacing: -0.1px;
}
</style>
