export function setupRouterGuards(router) {
  router.beforeEach((to, from, next) => {
    document.title = to.meta.title ? `${to.meta.title} | 知库·灵枢` : '知库·灵枢'
    
    const token = localStorage.getItem('token')
    
    if (to.path === '/login') {
      if (token) {
        next('/dashboard')
      } else {
        next()
      }
    } else {
      if (!token && to.meta.requiresAuth !== false) {
        next('/login')
      } else {
        next()
      }
    }
  })
  
  router.afterEach((to, from) => {
    window.scrollTo(0, 0)
  })
}