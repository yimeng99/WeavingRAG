<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import MainLayout from './layout/MainLayout.vue'

const router = useRouter()
const route = useRoute()
const isLoggedIn = ref(false)

const checkAuth = () => {
  const token = localStorage.getItem('token')
  isLoggedIn.value = !!token
}

onMounted(() => {
  checkAuth()
})

watch(() => route.path, () => {
  checkAuth()
})
</script>

<template>
  <div id="app">
    <router-view v-if="route.path === '/login'" />
    <MainLayout v-else-if="isLoggedIn" />
    <router-view v-else />
  </div>
</template>

<style>
#app {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #2c3e50;
}
</style>
