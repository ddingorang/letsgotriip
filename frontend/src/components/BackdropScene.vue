<script setup>
defineProps({
  layers: { type: Array, required: true },
  activeLayer: { type: Number, required: true }
});
</script>

<template>
  <div class="backdrop" aria-hidden="true">
    <div
      v-for="layer in layers"
      :key="layer.id"
      class="backdrop-scene"
      :class="{ 'is-active': layer.id === activeLayer }"
      :style="{ backgroundImage: `url('${layer.scene.image}')`, backgroundPosition: layer.scene.focus }"
    />
  </div>
</template>
