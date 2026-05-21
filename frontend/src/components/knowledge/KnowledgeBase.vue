<script setup>
</script>

<template>
  <div class="knowledge-management">
    <div class="kb-content">
      <router-view></router-view>
    </div>
  </div>
</template>

<style scoped lang="scss">
.knowledge-management {
  display: flex;
  height: 100%;
  width: 100%;
  overflow: hidden;
}

.kb-content {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
</style>
