<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import AppNavBar from '../components/layout/AppNavBar.vue';

const router = useRouter();

const selectedPayment = ref('card');

const bookingInfo = [
  { label: '숙소', value: '해운대 오션뷰 호텔' },
  { label: '체크인', value: '2026.06.15 (토) 15:00' },
  { label: '체크아웃', value: '2026.06.17 (월) 11:00' },
  { label: '인원', value: '성인 2명' },
  { label: '객실', value: '오션뷰 디럭스 · 1박' },
];

const travelerInfo = [
  { label: '이름', value: '김관통' },
  { label: '연락처', value: '010-1234-5678' },
  { label: '이메일', value: 'travel@example.com' },
];

const priceRows = [
  { label: '숙박비 (2박)', value: '₩360,000', style: '' },
  { label: '서비스 수수료', value: '₩18,000', style: '' },
  { label: '쿠폰 할인', value: '−₩20,000', style: 'color:var(--color-error)' },
];

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
];

const agreed = ref(true);
</script>

<template>
  <div class="payment">
    <AppNavBar title="예약 / 결제" />

    <!-- Steps -->
    <div class="steps">
      <div class="step">
        <div class="step-circle done">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><polyline points="20 6 9 17 4 12"/></svg>
        </div>
        <span class="step-label">일정 선택</span>
      </div>
      <div class="step-line done"></div>
      <div class="step">
        <div class="step-circle active">2</div>
        <span class="step-label active">예약 정보</span>
      </div>
      <div class="step-line"></div>
      <div class="step">
        <div class="step-circle pending">3</div>
        <span class="step-label">결제</span>
      </div>
      <div class="step-line"></div>
      <div class="step">
        <div class="step-circle pending">4</div>
        <span class="step-label">완료</span>
      </div>
    </div>

    <div class="content">
      <!-- Booking Summary -->
      <div class="section-card">
        <div class="section-title">예약 정보</div>
        <div v-for="row in bookingInfo" :key="row.label" class="info-row">
          <div class="info-label">{{ row.label }}</div>
          <div class="info-value">{{ row.value }}</div>
        </div>
      </div>

      <!-- Traveler Info -->
      <div class="section-card">
        <div class="section-title">예약자 정보</div>
        <div v-for="row in travelerInfo" :key="row.label" class="form-group">
          <label class="form-label">{{ row.label }}</label>
          <input class="form-input filled" :value="row.value" readonly />
        </div>
      </div>

      <!-- Payment Options -->
      <div class="section-card">
        <div class="section-title">결제 수단</div>
        <div class="pay-options">
          <div
            v-for="opt in paymentOptions"
            :key="opt.key"
            class="pay-option"
            :class="{ selected: selectedPayment === opt.key }"
            @click="selectedPayment = opt.key"
          >
            <div class="pay-radio" :class="{ checked: selectedPayment === opt.key }"></div>
            <div class="pay-icon" :style="opt.iconStyle">
              <template v-if="opt.isText">{{ opt.iconContent }}</template>
              <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 14.5v-9l6 4.5-6 4.5z"/></svg>
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
        <div class="section-title">결제 금액</div>
        <div v-for="row in priceRows" :key="row.label" class="price-row">
          <div class="price-label">{{ row.label }}</div>
          <div class="price-value" :style="row.style">{{ row.value }}</div>
        </div>
        <div class="price-total-row">
          <div class="price-total-label">최종 결제 금액</div>
          <div class="price-total-value">₩358,000</div>
        </div>
      </div>

      <!-- Agreement -->
      <div class="section-card">
        <div class="agree-row" @click="agreed = !agreed">
          <div class="checkbox" :class="{ checked: agreed }">
            <svg v-if="agreed" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="3"><polyline points="20 6 9 17 4 12"/></svg>
          </div>
          <div class="agree-text">예약 조건 및 <span class="agree-link">이용약관</span>, <span class="agree-link">개인정보 처리방침</span>에 동의합니다.</div>
        </div>
      </div>
    </div>

    <!-- Bottom Bar -->
    <div class="bottom-bar">
      <button class="pay-btn" @click="router.push('/confirmation')">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="1" y="4" width="22" height="16" rx="2" ry="2"/><line x1="1" y1="10" x2="23" y2="10"/></svg>
        ₩358,000 결제하기
      </button>
      <div class="pay-note">SSL 보안 결제 · 개인정보는 안전하게 보호됩니다</div>
    </div>
  </div>
</template>

<style scoped>
.payment { background: var(--surface-subtle); min-height: 100%; }

.steps { display: flex; align-items: center; padding: 16px 20px; background: #fff; border-bottom: 1px solid var(--border-subtle); }
.step { flex: 1; display: flex; flex-direction: column; align-items: center; gap: 6px; }
.step-circle { width: 28px; height: 28px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font: var(--weight-bold) var(--text-sm)/1 var(--font-sans); }
.step-circle.done { background: var(--color-primary-500); color: #fff; }
.step-circle.active { background: var(--color-primary-500); color: #fff; }
.step-circle.pending { background: var(--surface-subtle); color: var(--text-tertiary); border: 1.5px solid var(--border-default); }
.step-label { font: var(--weight-medium) 10px/1 var(--font-sans); color: var(--text-tertiary); }
.step-label.active { color: var(--color-primary-500); font-weight: var(--weight-semibold); }
.step-line { flex: 1; height: 2px; background: var(--border-default); max-width: 40px; margin: 0 4px; margin-bottom: 14px; }
.step-line.done { background: var(--color-primary-500); }

.content { padding: 16px 20px 140px; display: flex; flex-direction: column; gap: 12px; }

.section-card { background: var(--surface-bg); border-radius: var(--radius-lg); padding: 18px; }
.section-title { font: var(--weight-bold) var(--text-base)/1 var(--font-sans); color: var(--text-primary); margin-bottom: 14px; }
.info-row { display: flex; justify-content: space-between; align-items: flex-start; padding: 8px 0; border-bottom: 1px solid var(--border-subtle); }
.info-row:last-child { border-bottom: none; padding-bottom: 0; }
.info-label { font: var(--type-body-sm); color: var(--text-secondary); }
.info-value { font: var(--weight-medium) var(--text-sm)/var(--leading-snug) var(--font-sans); color: var(--text-primary); text-align: right; }

.form-group { display: flex; flex-direction: column; gap: 6px; margin-bottom: 14px; }
.form-group:last-child { margin-bottom: 0; }
.form-label { font: var(--weight-medium) var(--text-sm)/1 var(--font-sans); color: var(--text-secondary); }
.form-input { height: 46px; padding: 0 14px; border: 1.5px solid var(--border-default); border-radius: var(--radius-sm); font: var(--type-body-lg); font-family: var(--font-sans); color: var(--text-primary); background: var(--surface-bg); outline: none; width: 100%; }
.form-input.filled { background: var(--surface-subtle); border-color: var(--border-default); }

.pay-options { display: flex; flex-direction: column; gap: 10px; }
.pay-option { display: flex; align-items: center; gap: 12px; padding: 14px; border: 1.5px solid var(--border-default); border-radius: var(--radius-md); cursor: pointer; }
.pay-option.selected { border-color: var(--color-primary-500); background: var(--color-primary-50); }
.pay-radio { width: 20px; height: 20px; border-radius: 50%; border: 2px solid var(--border-strong); display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.pay-radio.checked { border-color: var(--color-primary-500); background: var(--color-primary-500); }
.pay-radio.checked::after { content: ''; display: block; width: 8px; height: 8px; border-radius: 50%; background: #fff; }
.pay-icon { width: 38px; height: 26px; border-radius: var(--radius-xs); display: flex; align-items: center; justify-content: center; font: var(--weight-bold) 10px/1 var(--font-sans); flex-shrink: 0; }
.pay-text { flex: 1; }
.pay-name { font: var(--weight-semibold) var(--text-sm)/1 var(--font-sans); color: var(--text-primary); }
.pay-sub { font: var(--type-caption); color: var(--text-tertiary); margin-top: 3px; }

.price-row { display: flex; justify-content: space-between; padding: 8px 0; }
.price-label { font: var(--type-body-sm); color: var(--text-secondary); }
.price-value { font: var(--weight-medium) var(--text-sm)/1 var(--font-sans); color: var(--text-primary); }
.price-total-row { display: flex; justify-content: space-between; padding: 14px 0 0; border-top: 1.5px solid var(--border-default); margin-top: 6px; }
.price-total-label { font: var(--weight-bold) var(--text-base)/1 var(--font-sans); color: var(--text-primary); }
.price-total-value { font: var(--weight-bold) var(--text-xl)/1 var(--font-sans); color: var(--color-primary-500); }

.agree-row { display: flex; align-items: center; gap: 10px; cursor: pointer; }
.checkbox { width: 22px; height: 22px; border-radius: var(--radius-xs); border: 2px solid var(--border-strong); display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.checkbox.checked { background: var(--color-primary-500); border-color: var(--color-primary-500); }
.agree-text { font: var(--type-body-sm); color: var(--text-secondary); }
.agree-link { color: var(--color-primary-500); text-decoration: underline; }

.bottom-bar { position: fixed; bottom: 0; left: 50%; transform: translateX(-50%); width: 100%; max-width: 430px; background: var(--surface-bg); border-top: 1px solid var(--border-subtle); padding: 14px 20px 34px; z-index: var(--z-raised); }
.pay-btn { width: 100%; height: 54px; background: var(--color-primary-500); color: #fff; border: none; border-radius: var(--radius-md); font: var(--weight-bold) var(--text-lg)/1 var(--font-sans); cursor: pointer; display: flex; align-items: center; justify-content: center; gap: 8px; }
.pay-note { text-align: center; font: var(--type-caption); color: var(--text-tertiary); margin-top: 8px; }
</style>
