<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
import { storeToRefs } from 'pinia';
import { getFestivalDateRange } from '../api/festival.js';
import { useKakaoMap } from '../composables/useKakaoMap.js';
import { useFestivalStore } from '../stores/festival.js';

defineProps({
  detailContentOpen: { type: Boolean, required: true }
});

const store = useFestivalStore();
const { festivals, loading, error } = storeToRefs(store);

const selectedArea = ref('');
const selectedId = ref('');
const statusFilter = ref('all');
const viewMode = ref('list');
const mapEl = ref(null);

const { today } = getFestivalDateRange();

const areaOptions = [
  { code: '', label: '전체 지역' },
  { code: '11', label: '서울' },
  { code: '26', label: '부산' },
  { code: '27', label: '대구' },
  { code: '28', label: '인천' },
  { code: '29', label: '광주' },
  { code: '30', label: '대전' },
  { code: '31', label: '울산' },
  { code: '36', label: '세종' },
  { code: '41', label: '경기' },
  { code: '51', label: '강원' },
  { code: '43', label: '충북' },
  { code: '44', label: '충남' },
  { code: '52', label: '전북' },
  { code: '46', label: '전남' },
  { code: '47', label: '경북' },
  { code: '48', label: '경남' },
  { code: '50', label: '제주' },
];

function fmtDate(s) {
  if (!s || s.length < 8) return s ?? '';
  return `${s.slice(0, 4)}.${s.slice(4, 6)}.${s.slice(6, 8)}`;
}

function getStatus(fest) {
  return fest.startDate <= today && fest.endDate >= today ? 'ongoing' : 'upcoming';
}

function statusLabel(fest) {
  if (getStatus(fest) === 'ongoing') return '진행중';
  const d = new Date(`${fest.startDate.slice(0, 4)}-${fest.startDate.slice(4, 6)}-${fest.startDate.slice(6, 8)}`);
  const now = new Date();
  now.setHours(0, 0, 0, 0);
  const days = Math.ceil((d - now) / 86400000);
  return days === 0 ? 'D-Day' : `D-${days}`;
}

const filtered = computed(() => {
  if (statusFilter.value === 'ongoing') return festivals.value.filter((f) => getStatus(f) === 'ongoing');
  if (statusFilter.value === 'upcoming') return festivals.value.filter((f) => getStatus(f) === 'upcoming');
  return festivals.value;
});

const topCards = computed(() => filtered.value.slice(0, 3));
const selected = computed(() => filtered.value.find((f) => f.id === selectedId.value) ?? filtered.value[0]);

// ── Kakao Map ──────────────────────────────────────────────────────────────
const { setMarkers, setOverlay, panTo } = useKakaoMap(mapEl, {
  getCenter: () => selected.value?.latitude
    ? { lat: selected.value.latitude, lng: selected.value.longitude }
    : null,
  onReady: () => {
    syncMarkers();
    setOverlay(selected.value?.title, selected.value?.latitude, selected.value?.longitude);
  },
});

function syncMarkers() {
  setMarkers(filtered.value, {
    getId: (f) => f.id,
    getLat: (f) => f.latitude,
    getLng: (f) => f.longitude,
    onClickItem: (f) => { selectedId.value = f.id; },
  });
}

watch(filtered, syncMarkers);

watch(selected, (fest) => {
  setOverlay(fest?.title, fest?.latitude, fest?.longitude);
  panTo(fest?.latitude, fest?.longitude);
});

// ── Area filter ────────────────────────────────────────────────────────────
async function changeArea() {
  selectedId.value = '';
  await store.loadFestivals(selectedArea.value);
}

onMounted(() => {
  if (store.loadedArea === null) store.loadFestivals();
});
</script>

<template>
  <div class="focus-head">
    <div>
      <h2>지역 행사</h2>
      <p class="deck-lead">현재 기준 앞뒤 한 달 이내 진행 중이거나 예정된 행사를 모아봅니다.</p>
    </div>
    <select v-model="selectedArea" class="festival-area-select" @change="changeArea">
      <option v-for="opt in areaOptions" :key="opt.code" :value="opt.code">{{ opt.label }}</option>
    </select>
  </div>

  <div v-if="loading" class="workspace-skeleton">
    <span /><span /><span />
  </div>

  <template v-else-if="error">
    <div class="workspace-error">
      <p>{{ error }}</p>
      <button class="btn" type="button" style="margin-top: 12px" @click="store.loadFestivals(selectedArea)">
        다시 시도
      </button>
    </div>
  </template>

  <template v-else>
    <div v-if="topCards.length" class="now-grid">
      <button
        v-for="fest in topCards"
        :key="fest.id"
        class="now-card"
        type="button"
        :aria-pressed="String(fest.id === selected?.id)"
        @click="selectedId = fest.id"
      >
        <span :class="getStatus(fest) === 'ongoing' ? 'status-ongoing' : 'status-upcoming'">
          {{ statusLabel(fest) }}
        </span>
        <b>{{ fest.title }}</b>
        <small>{{ fmtDate(fest.startDate) }}–{{ fmtDate(fest.endDate) }} · {{ fest.address }}</small>
      </button>
    </div>
    <div v-else class="workspace-empty">이 기간에 해당하는 행사가 없습니다.</div>

    <div v-if="detailContentOpen && filtered.length > 0" class="detail-expansion">
      <div class="workspace-toolbar">
        <div class="filter-chips" role="group" aria-label="진행 상태 필터">
          <button class="filter-chip" type="button" :aria-pressed="statusFilter === 'all'" @click="statusFilter = 'all'">전체</button>
          <button class="filter-chip" type="button" :aria-pressed="statusFilter === 'ongoing'" @click="statusFilter = 'ongoing'">진행중</button>
          <button class="filter-chip" type="button" :aria-pressed="statusFilter === 'upcoming'" @click="statusFilter = 'upcoming'">예정</button>
        </div>
        <div style="display: flex; align-items: center; gap: 10px">
          <span class="workspace-status" style="margin-bottom: 0">
            <strong>{{ filtered.length }}</strong>건
          </span>
          <div class="view-toggle" role="group" aria-label="보기 방식">
            <button type="button" :aria-pressed="viewMode === 'list'" @click="viewMode = 'list'">목록</button>
            <button type="button" :aria-pressed="viewMode === 'map'" @click="viewMode = 'map'">지도</button>
          </div>
        </div>
      </div>

      <!-- 지도 모드 -->
      <div v-if="viewMode === 'map'" class="festival-map-layout">
        <div ref="mapEl" class="festival-map" />
        <div v-if="selected" class="festival-map-info">
          <span
            class="festival-status-badge"
            :class="getStatus(selected) === 'ongoing' ? 'status-ongoing' : 'status-upcoming'"
          >{{ statusLabel(selected) }}</span>
          <b>{{ selected.title }}</b>
          <span>{{ fmtDate(selected.startDate) }} – {{ fmtDate(selected.endDate) }}</span>
          <span>{{ selected.address }}</span>
        </div>
        <div class="place-list">
          <button
            v-for="fest in filtered"
            :key="fest.id"
            class="place-card"
            type="button"
            :aria-pressed="String(fest.id === selected?.id)"
            @click="selectedId = fest.id"
          >
            <span class="place-type" :class="getStatus(fest) === 'ongoing' ? 'status-ongoing' : 'status-upcoming'">
              {{ statusLabel(fest) }}
            </span>
            <span>
              <b>{{ fest.title }}</b>
              <span>{{ fmtDate(fest.startDate) }} – {{ fmtDate(fest.endDate) }} · {{ fest.address }}</span>
            </span>
          </button>
        </div>
      </div>

      <!-- 목록 모드 -->
      <div v-else-if="selected" class="workspace-layout" style="margin-top: 10px">
        <div class="place-detail-pane">
          <span
            class="festival-status-badge"
            :class="getStatus(selected) === 'ongoing' ? 'status-ongoing' : 'status-upcoming'"
          >{{ statusLabel(selected) }}</span>
          <h4 style="margin-top: 8px; margin-bottom: 6px">{{ selected.title }}</h4>
          <p>{{ selected.address }}</p>
          <p>{{ fmtDate(selected.startDate) }} – {{ fmtDate(selected.endDate) }}</p>
          <a
            v-if="selected.homepage"
            :href="selected.homepage"
            target="_blank"
            rel="noopener noreferrer"
            class="festival-homepage-link"
          >관련 웹사이트 →</a>
          <div class="detail-actions">
            <button class="btn btn-primary" type="button">일정에 추가</button>
            <button
              class="btn"
              type="button"
              :disabled="!selected.latitude"
              @click="viewMode = 'map'"
            >지도 보기</button>
          </div>
        </div>
        <div class="place-list">
          <button
            v-for="fest in filtered"
            :key="fest.id"
            class="place-card"
            type="button"
            :aria-pressed="String(fest.id === selected.id)"
            @click="selectedId = fest.id"
          >
            <span class="place-type" :class="getStatus(fest) === 'ongoing' ? 'status-ongoing' : 'status-upcoming'">
              {{ statusLabel(fest) }}
            </span>
            <span>
              <b>{{ fest.title }}</b>
              <span>{{ fmtDate(fest.startDate) }} – {{ fmtDate(fest.endDate) }} · {{ fest.address }}</span>
            </span>
          </button>
        </div>
      </div>
    </div>
  </template>
</template>
