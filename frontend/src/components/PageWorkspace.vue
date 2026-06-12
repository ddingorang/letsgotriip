<script setup>
import { computed, ref, watch } from 'vue';
import { pageWorkflows } from '../data.js';

const props = defineProps({
  page: { type: String, required: true },
  detailContentOpen: { type: Boolean, required: true }
});

const selectedAction = ref(0);
const workflow = computed(() => pageWorkflows[props.page] ?? pageWorkflows.home);

watch(() => props.page, () => {
  selectedAction.value = 0;
});
</script>

<template>
  <section class="panel">
    <div class="focus-head">
      <div>
        <h2>{{ workflow.title }}</h2>
        <p class="deck-lead">{{ workflow.lead }}</p>
      </div>
    </div>
    <div class="now-grid">
      <article v-for="item in workflow.now" :key="item[0]" class="now-card">
        <span>먼저 확인</span>
        <b>{{ item[0] }}</b>
        <small>{{ item[1] }}</small>
      </article>
    </div>
    <div v-if="detailContentOpen" class="detail-expansion">
      <div class="task-layout">
        <div>
          <h3>{{ workflow.detailTitle }}</h3>
          <p class="deck-lead">현재 화면에서 바로 할 수 있는 작업을 선택하세요.</p>
          <div class="task-list" role="listbox" aria-label="화면 작업 선택">
            <button
              v-for="(action, index) in workflow.actions"
              :key="action"
              class="task-choice"
              type="button"
              :aria-selected="selectedAction === index"
              @click="selectedAction = index"
            >
              <span>{{ String(index + 1).padStart(2, '0') }}</span>
              <b>{{ action }}</b>
            </button>
          </div>
        </div>
        <aside class="task-preview">
          <span class="contract-label">SELECTED</span>
          <h3>{{ workflow.actions[selectedAction] }}</h3>
          <p>이 선택 영역에 실제 API 연동, 입력 폼, 지도, 비교표 같은 상세 기능을 붙일 수 있습니다.</p>
          <div class="detail-actions">
            <button class="btn btn-primary" type="button">시작</button>
            <button class="btn" type="button">초기화</button>
          </div>
        </aside>
      </div>
    </div>
  </section>
</template>
