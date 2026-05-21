<script setup>
import { ref, watch } from 'vue'
import IconSvg from './IconSvg.vue'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  title: {
    type: String,
    default: '提示'
  },
  content: {
    type: String,
    default: ''
  },
  confirmText: {
    type: String,
    default: '确定'
  },
  cancelText: {
    type: String,
    default: '取消'
  },
  type: {
    type: String,
    default: 'warning', // warning, danger, info, success
    validator: (value) => ['warning', 'danger', 'info', 'success'].includes(value)
  },
  showCancel: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['update:modelValue', 'confirm', 'cancel'])

const visible = ref(props.modelValue)

watch(() => props.modelValue, (val) => {
  visible.value = val
})

const close = () => {
  visible.value = false
  emit('update:modelValue', false)
}

const handleConfirm = () => {
  emit('confirm')
  close()
}

const handleCancel = () => {
  emit('cancel')
  close()
}

const getIconName = () => {
  const icons = {
    warning: 'info',
    danger: 'trash',
    info: 'info',
    success: 'check'
  }
  return icons[props.type] || 'info'
}

const getIconColor = () => {
  const colors = {
    warning: '#f59e0b',
    danger: '#ef4444',
    info: '#3b82f6',
    success: '#10b981'
  }
  return colors[props.type] || '#7c3aed'
}

const getIconBg = () => {
  const bgs = {
    warning: '#fef3c7',
    danger: '#fef2f2',
    info: '#eff6ff',
    success: '#f0fdf4'
  }
  return bgs[props.type] || '#f3e8ff'
}
</script>

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="visible" class="modal-overlay" @click.self="handleCancel">
        <div class="modal-container">
          <div class="modal-header">
            <div class="modal-icon" :style="{ background: getIconBg() }">
              <IconSvg :name="getIconName()" :size="20" :color="getIconColor()" />
            </div>
            <h3 class="modal-title">{{ title }}</h3>
          </div>
          
          <div class="modal-body">
            <p class="modal-content">{{ content }}</p>
          </div>
          
          <div class="modal-footer">
            <button 
              v-if="showCancel"
              class="btn btn-cancel" 
              @click="handleCancel"
            >
              {{ cancelText }}
            </button>
            <button 
              class="btn btn-confirm"
              :class="`btn-${type}`"
              @click="handleConfirm"
            >
              {{ confirmText }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style lang="scss" scoped>


.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}

.modal-container {
  background: #fff;
  border-radius: $radius-xl;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.15);
  max-width: 400px;
  width: 90%;
  overflow: hidden;
}

.modal-header {
  padding: $spacing-lg;
  display: flex;
  align-items: center;
  gap: $spacing-md;
}

.modal-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.modal-title {
  font-size: $font-size-lg;
  font-weight: 600;
  color: $gray-800;
  margin: 0;
}

.modal-body {
  padding: 0 $spacing-lg $spacing-lg;
}

.modal-content {
  font-size: $font-size-sm;
  color: $gray-600;
  line-height: 1.6;
  margin: 0;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: $spacing-sm;
  padding: $spacing-md $spacing-lg;
  background: $gray-50;
  border-top: 1px solid $gray-100;
}

.btn {
  padding: 10px 20px;
  border-radius: $radius-md;
  font-size: $font-size-sm;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  border: none;
}

.btn-cancel {
  background: #fff;
  border: 1px solid $gray-200;
  color: $gray-600;
  
  &:hover {
    background: $gray-50;
    border-color: $gray-300;
  }
}

.btn-confirm {
  color: #fff;
  
  &.btn-warning {
    background: #f59e0b;
    &:hover { background: #d97706; }
  }
  
  &.btn-danger {
    background: #ef4444;
    &:hover { background: #dc2626; }
  }
  
  &.btn-info {
    background: #3b82f6;
    &:hover { background: #2563eb; }
  }
  
  &.btn-success {
    background: $purple-600;
    &:hover { background: $purple-700; }
  }
}

// 动画
.modal-enter-active,
.modal-leave-active {
  transition: all 0.2s ease;
  
  .modal-container {
    transition: all 0.2s ease;
  }
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
  
  .modal-container {
    transform: scale(0.95);
    opacity: 0;
  }
}
</style>