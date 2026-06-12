<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import { storeToRefs } from 'pinia';
import { areaLabels, contentTypeLabels, contextSummary, placeDetails } from '../data.js';
import { useKakaoMap } from '../composables/useKakaoMap.js';
import { useTripUiStore } from '../stores/tripUi.js';
import FestivalWorkspace from './FestivalWorkspace.vue';

defineProps({
  detailContentOpen: { type: Boolean, required: true }
});

const exploreTab = ref('places');

const query = ref('');
const area = ref('');
const type = ref('');
const viewMode = ref('list');
const selectedId = ref('');
const savedIds = ref(new Set());
const mapEl = ref(null);
const uiStore = useTripUiStore();
const { places } = storeToRefs(uiStore);

const areaOptions = computed(() => [...new Set(places.value.map((place) => place.areaCode))]);
const typeOptions = computed(() => [...new Set(places.value.map((place) => place.contentType))]);
const filteredPlaces = computed(() => places.value.filter((place) => matchesFilters(place)));
const selectedPlace = computed(() => filteredPlaces.value.find((place) => place.id === selectedId.value) ?? filteredPlaces.value[0]);

function labelArea(code) {
  return areaLabels[code] ?? `지역 ${code}`;
}

function labelType(value) {
  return contentTypeLabels[value] ?? value;
}

function normalize(value) {
  return String(value ?? '').trim().toLocaleLowerCase('ko-KR');
}

function matchesFilters(place) {
  const searchText = normalize([place.title, place.address, labelArea(place.areaCode), labelType(place.contentType), place.tags.join(' ')].join(' '));
  if (area.value && place.areaCode !== area.value) return false;
  if (type.value && place.contentType !== type.value) return false;
  return !query.value || searchText.includes(normalize(query.value));
}

function selectPlace(id) {
  selectedId.value = id;
}

function saveSelected() {
  if (!selectedPlace.value) return;
  savedIds.value = new Set([...savedIds.value, selectedPlace.value.id]);
}

// ── Kakao Map ──────────────────────────────────────────────────────────────
const { setMarkers, setOverlay, panTo } = useKakaoMap(mapEl, {
  getCenter: () => selectedPlace.value?.latitude
    ? { lat: selectedPlace.value.latitude, lng: selectedPlace.value.longitude }
    : null,
  onReady: () => {
    syncMarkers();
    setOverlay(selectedPlace.value?.title, selectedPlace.value?.latitude, selectedPlace.value?.longitude);
  },
});

function syncMarkers() {
  setMarkers(filteredPlaces.value, {
    getId: (p) => p.id,
    getLat: (p) => p.latitude,
    getLng: (p) => p.longitude,
    onClickItem: (p) => { selectedId.value = p.id; },
  });
}

watch(filteredPlaces, syncMarkers);

watch(selectedPlace, (place) => {
  setOverlay(place?.title, place?.latitude, place?.longitude);
  panTo(place?.latitude, place?.longitude);
});

onMounted(() => {
  void uiStore.loadPlaces();
});
</script>

<template>
  <section class="panel" data-explore-workspace>
    <div class="explore-tab-bar" role="tablist">
      <button role="tab" type="button" :aria-selected="exploreTab === 'places'" @click="exploreTab = 'places'">장소탐색</button>
      <button role="tab" type="button" :aria-selected="exploreTab === 'festivals'" @click="exploreTab = 'festivals'">지역행사</button>
    </div>

    <FestivalWorkspace v-if="exploreTab === 'festivals'" :detail-content-open="detailContentOpen" />

    <template v-else>
    <div class="focus-head">
      <div>
        <h2>지금 먼저 볼 관광지</h2>
        <p class="deck-lead">검색 전에도 바로 판단할 수 있게 대표 후보와 상태를 먼저 보여줍니다.</p>
      </div>
    </div>
    <div class="now-grid">
      <button
        v-for="place in places.slice(0, 3)"
        :key="place.id"
        class="now-card"
        type="button"
        :aria-pressed="place.id === selectedPlace?.id"
        @click="selectPlace(place.id)"
      >
        <span>{{ labelType(place.contentType) }}</span>
        <b>{{ place.title }}</b>
        <small>{{ place.address }}</small>
      </button>
    </div>
    <div v-if="detailContentOpen" class="detail-expansion">
      <div class="workspace-toolbar">
      <div class="workspace-filters">
        <label class="search-field">검색
          <input v-model="query" type="search" data-place-search placeholder="장소, 지역, 태그" autocomplete="off" spellcheck="false" />
        </label>
        <label>지역
          <select v-model="area" data-filter-area>
            <option value="">전체</option>
            <option v-for="code in areaOptions" :key="code" :value="code">{{ labelArea(code) }}</option>
          </select>
        </label>
        <div class="filter-chips" role="group" aria-label="콘텐츠 유형">
          <button class="filter-chip" type="button" :aria-pressed="type === ''" @click="type = ''">전체</button>
          <button
            v-for="contentType in typeOptions"
            :key="contentType"
            class="filter-chip"
            type="button"
            :aria-pressed="type === contentType"
            @click="type = contentType"
          >
            {{ labelType(contentType) }}
          </button>
        </div>
      </div>
      <div class="view-toggle" role="group" aria-label="보기 방식">
        <button type="button" :aria-pressed="viewMode === 'list'" @click="viewMode = 'list'">목록</button>
        <button type="button" :aria-pressed="viewMode === 'map'" @click="viewMode = 'map'">지도</button>
      </div>
    </div>
    </div>
    <div v-if="detailContentOpen" class="workspace-status">
      <span data-result-count><strong>{{ filteredPlaces.length }}</strong>곳</span>
      <span class="state-pill" :class="{ 'is-empty': !filteredPlaces.length }">{{ filteredPlaces.length ? '살펴보기' : '비어 있음' }}</span>
    </div>
    <div v-if="detailContentOpen && selectedPlace" class="workspace-layout">
      <div class="place-detail-pane">
        <h4 data-detail-title>{{ selectedPlace.title }}</h4>
        <p>{{ selectedPlace.address }}</p>
        <p>{{ placeDetails[selectedPlace.id] ?? `${labelType(selectedPlace.contentType)}에 대한 짧은 소개입니다.` }}</p>
        <div class="chips">
          <span v-for="tag in selectedPlace.tags" :key="tag" class="chip">{{ tag }}</span>
        </div>
        <p>{{ contextSummary }}</p>
        <div class="detail-actions">
          <button class="btn btn-primary" type="button" @click="saveSelected">여행 노트에 담기</button>
          <button class="btn" type="button">일정 이어가기</button>
        </div>
      </div>
      <div ref="mapEl" v-if="viewMode === 'map'" class="festival-map" />
      <div v-if="detailContentOpen" class="place-list">
        <button
          v-for="place in filteredPlaces"
          :key="place.id"
          class="place-card"
          type="button"
          :aria-pressed="place.id === selectedPlace.id"
          @click="selectPlace(place.id)"
        >
          <span class="place-type">{{ labelType(place.contentType) }}</span>
          <span><b>{{ place.title }}</b><span>{{ place.address }} · {{ labelArea(place.areaCode) }}</span></span>
          <span class="save-flag">{{ savedIds.has(place.id) ? '담아둠' : '' }}</span>
        </button>
      </div>
    </div>
    <div v-else-if="detailContentOpen" class="workspace-empty">{{ query ? '검색어와 조건에 맞는 장소가 없습니다.' : '이 조건에 맞는 장소가 아직 없습니다.' }}</div>
    </template>
  </section>
</template>
