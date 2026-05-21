<script setup>
import { computed } from 'vue'

const props = defineProps({
  currentStep: {
    type: Number,
    default: 1
  },
  totalSteps: {
    type: Number,
    default: 3
  },
  steps: {
    type: Array,
    default: () => ['基本信息', 'AI 模型配置', '权限设置']
  }
})

const stepItems = computed(() => {
  return props.steps.map((label, index) => ({
    step: index + 1,
    label,
    active: props.currentStep >= index + 1,
    completed: props.currentStep > index + 1
  }))
})
</script>

<template>
  <div class="steps">
    <template v-for="(item, index) in stepItems" :key="index">
      <div class="step-item" :class="{ active: item.active, completed: item.completed }">
        <div class="step-dot">{{ item.completed ? '✓' : item.step }}</div>
        <span class="step-label">{{ item.label }}</span>
      </div>
      <div 
        v-if="index < stepItems.length - 1" 
        class="step-line" 
        :class="{ completed: props.currentStep > item.step }"
      ></div>
    </template>
  </div>
</template>

<style lang="scss" scoped>


.steps {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: $spacing-lg $spacing-xl;
  border-bottom: 1px solid $gray-100;
  background: $gray-50;
}

.step-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: $spacing-sm;
}

.step-dot {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #fff;
  border: 2px solid $gray-200;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: $font-size-sm;
  font-weight: 600;
  color: $gray-400;
  transition: all 0.2s ease;
}

.step-item.active .step-dot {
  border-color: $purple-600;
  background: $purple-600;
  color: #fff;
}

.step-item.completed .step-dot {
  border-color: $purple-600;
  background: $purple-600;
  color: #fff;
}

.step-label {
  font-size: $font-size-xs;
  font-weight: 500;
  color: $gray-400;
}

.step-item.active .step-label {
  color: $purple-600;
}

.step-line {
  width: 60px;
  height: 2px;
  background: $gray-200;
  margin: 0 $spacing-sm;
  margin-bottom: 24px;
  
  &.completed {
    background: $purple-600;
  }
}
</style>