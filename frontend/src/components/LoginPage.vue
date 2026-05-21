<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { message } from '../utils/message'
import IconSvg from './ui/IconSvg.vue'

const router = useRouter()
const emit = defineEmits(['login-success'])

const isLoading = ref(false)
const loginMode = ref('password')
const showPassword = ref(false)
const rememberMe = ref(false)

const pwdForm = reactive({
  account: '',
  password: ''
})

const codeForm = reactive({
  phone: '',
  code: ''
})

const countdown = ref(0)

const switchLoginMode = (mode) => {
  loginMode.value = mode
}

const handleSendCode = async () => {
  if (!codeForm.phone) {
    message.error('请输入手机号')
    return
  }
  
  if (countdown.value > 0) return
  
  countdown.value = 60
  const timer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(timer)
    }
  }, 1000)
  
  message.success('验证码已发送')
}

const handleLogin = async () => {
  if (loginMode.value === 'password') {
    if (!pwdForm.account) {
      message.error('请输入邮箱或手机号')
      return
    }
    if (!pwdForm.password) {
      message.error('请输入密码')
      return
    }
    
    isLoading.value = true
    
    try {
      const response = await fetch('/api/v0/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          username: pwdForm.account,
          password: pwdForm.password,
          rememberMe: rememberMe.value
        })
      })
      
      const data = await response.json()
      
      if (data.code === 200 || data.code === 0) {
        localStorage.setItem('token', data.data.token)
        localStorage.setItem('user', JSON.stringify(data.data.user))
        message.success('登录成功')
        router.push('/')
      } else {
        message.error(data.msg || '登录失败')
      }
    } catch (error) {
      message.error('网络错误，请稍后重试')
      console.error('登录失败:', error)
    } finally {
      isLoading.value = false
    }
  } else {
    if (!codeForm.phone) {
      message.error('请输入手机号')
      return
    }
    if (!codeForm.code) {
      message.error('请输入验证码')
      return
    }
    message.error('验证码登录暂未开放')
  }
}
</script>

<template>
  <div class="login-page">
    <div class="bg-dots"></div>
    
    <div class="particles">
      <div class="particle p1"></div>
      <div class="particle p2"></div>
      <div class="particle p3"></div>
      <div class="particle p4"></div>
    </div>
    
    <div class="login-card">
      <div class="brand">
        <div class="brand-logo">
          <IconSvg name="brain" :size="32" color="#fff" />
        </div>
        <h1 class="brand-title">知库·灵枢</h1>
        <p class="brand-desc">企业知识中枢 · 智能管理平台</p>
      </div>
      
      <div class="login-form-card">
        <div class="login-tabs">
          <button 
            :class="['tab-btn', { active: loginMode === 'password' }]"
            @click="switchLoginMode('password')"
          >
            密码登录
          </button>
          <button 
            :class="['tab-btn', { active: loginMode === 'code' }]"
            @click="switchLoginMode('code')"
          >
            验证码登录
          </button>
        </div>
        
        <div v-if="loginMode === 'password'" class="form-content">
          <div class="form-item">
            <label class="form-label">邮箱 / 手机号</label>
            <div class="input-wrapper">
              <IconSvg name="mail" :size="14" color="#9ca3af" class="input-icon" />
              <input 
                v-model="pwdForm.account"
                type="text" 
                placeholder="请输入邮箱或手机号" 
                class="form-input"
                @keyup.enter="handleLogin"
              />
            </div>
          </div>
          
          <div class="form-item">
            <label class="form-label">密码</label>
            <div class="input-wrapper">
              <IconSvg name="lock" :size="14" color="#9ca3af" class="input-icon" />
              <input 
                v-model="pwdForm.password"
                :type="showPassword ? 'text' : 'password'" 
                placeholder="请输入密码" 
                class="form-input"
                @keyup.enter="handleLogin"
              />
              <button type="button" class="toggle-pwd" @click="showPassword = !showPassword">
                <IconSvg :name="showPassword ? 'eye' : 'eye-off'" :size="14" color="#9ca3af" />
              </button>
            </div>
          </div>
        </div>
        
        <div v-else class="form-content">
          <div class="form-item">
            <label class="form-label">手机号</label>
            <div class="input-wrapper">
              <IconSvg name="phone" :size="14" color="#9ca3af" class="input-icon" />
              <input 
                v-model="codeForm.phone"
                type="tel" 
                placeholder="请输入手机号" 
                class="form-input"
              />
            </div>
          </div>
          
          <div class="form-item">
            <label class="form-label">验证码</label>
            <div class="input-wrapper code-input">
              <div class="input-wrapper-inner">
                <IconSvg name="key" :size="14" color="#9ca3af" class="input-icon" />
                <input 
                  v-model="codeForm.code"
                  type="text" 
                  placeholder="请输入验证码" 
                  class="form-input"
                  @keyup.enter="handleLogin"
                />
              </div>
              <button 
                class="send-code-btn" 
                :disabled="countdown > 0"
                @click="handleSendCode"
              >
                {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
              </button>
            </div>
          </div>
        </div>
        
        <div class="form-options">
          <label class="remember-me">
            <input v-model="rememberMe" type="checkbox" />
            <span>记住密码</span>
          </label>
          <a href="#" class="forgot-pwd">忘记密码？</a>
        </div>
        
        <button 
          class="login-btn" 
          :disabled="isLoading"
          @click="handleLogin"
        >
          {{ isLoading ? '登录中...' : '登 录' }}
        </button>
        
        <p class="register-link">
          还没有账号？
          <a href="#">立即注册</a>
        </p>
      </div>
      
      <p class="copyright">© 2025 知库·灵枢 企业知识管理系统 | 安全 · 智能 · 高效</p>
    </div>
  </div>
</template>

<style lang="scss" scoped>


.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #f5f3ff 0%, #ede9fe 50%, #fae8ff 100%);
}

.bg-dots {
  position: absolute;
  inset: 0;
  background-image: radial-gradient(#c084fc 1px, transparent 1px);
  background-size: 24px 24px;
  pointer-events: none;
}

.particles {
  position: absolute;
  inset: 0;
  pointer-events: none;
  
  .particle {
    position: absolute;
    background: rgba(124, 58, 237, 0.1);
    border-radius: 50%;
    animation: float 8s infinite ease-in-out;
    
    &.p1 {
      width: 256px;
      height: 256px;
      top: 80px;
      left: 40px;
    }
    
    &.p2 {
      width: 384px;
      height: 384px;
      bottom: 80px;
      right: 40px;
      animation-delay: 2s;
      background: rgba(168, 85, 247, 0.08);
    }
    
    &.p3 {
      width: 192px;
      height: 192px;
      top: 50%;
      right: 33%;
      animation-delay: 4s;
      background: rgba(192, 132, 252, 0.1);
    }
    
    &.p4 {
      width: 128px;
      height: 128px;
      bottom: 128px;
      left: 25%;
      animation-delay: 6s;
      background: rgba(124, 58, 237, 0.12);
    }
  }
}

@keyframes float {
  0%, 100% { 
    transform: translateY(0) translateX(0); 
    opacity: 0.3; 
  }
  50% { 
    transform: translateY(-20px) translateX(15px); 
    opacity: 0.6; 
  }
}

.login-card {
  width: 100%;
  max-width: 420px;
  animation: fadeInUp 0.5s ease-out;
  position: relative;
  z-index: 1;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.brand {
  text-align: center;
  margin-bottom: 32px;
  
  .brand-logo {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 64px;
    height: 64px;
    border-radius: 16px;
    background: linear-gradient(135deg, #8b5cf6 0%, #7c3aed 100%);
    box-shadow: 0 10px 30px rgba(124, 58, 237, 0.3);
    margin-bottom: 16px;
  }
  
  .brand-title {
    font-size: 24px;
    font-weight: 700;
    background: linear-gradient(to right, #6b21a8, #9333ea);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
    margin: 0 0 4px 0;
  }
  
  .brand-desc {
    font-size: 12px;
    color: $gray-500;
    margin: 0;
  }
}

.login-form-card {
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(8px);
  border-radius: 16px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(124, 58, 237, 0.1);
  padding: 24px;
}

.login-tabs {
  display: flex;
  gap: 8px;
  padding: 4px;
  background: $gray-100;
  border-radius: 12px;
  margin-bottom: 24px;
  
  .tab-btn {
    flex: 1;
    padding: 8px 0;
    font-size: 14px;
    font-weight: 500;
    border-radius: 8px;
    border: none;
    background: transparent;
    color: $gray-500;
    cursor: pointer;
    transition: all 0.2s;
    
    &.active {
      background: $purple-600;
      color: #fff;
    }
    
    &:hover:not(.active) {
      color: $purple-600;
    }
  }
}

.form-content {
  margin-bottom: 16px;
}

.form-item {
  margin-bottom: 20px;
  
  .form-label {
    display: block;
    font-size: 12px;
    font-weight: 500;
    color: $gray-700;
    margin-bottom: 6px;
  }
  
  .input-wrapper {
    position: relative;
    display: flex;
    align-items: center;
    
    .input-icon {
      position: absolute;
      left: 12px;
      z-index: 1;
    }
    
    .form-input {
      width: 100%;
      padding: 10px 12px 10px 36px;
      border: 1px solid $gray-200;
      border-radius: 12px;
      font-size: 14px;
      transition: all 0.2s;
      
      &:focus {
        outline: none;
        border-color: $purple-400;
        box-shadow: 0 0 0 3px rgba(124, 58, 237, 0.1);
      }
      
      &::placeholder {
        color: $gray-400;
      }
    }
    
    .toggle-pwd {
      position: absolute;
      right: 12px;
      background: none;
      border: none;
      padding: 4px;
      cursor: pointer;
      
      &:hover {
        opacity: 0.7;
      }
    }
  }
  
  .code-input {
    gap: 12px;
    
    .input-wrapper-inner {
      flex: 1;
      position: relative;
    }
    
    .send-code-btn {
      padding: 10px 16px;
      font-size: 14px;
      font-weight: 500;
      color: $purple-600;
      background: $purple-50;
      border: none;
      border-radius: 12px;
      cursor: pointer;
      white-space: nowrap;
      transition: all 0.2s;
      
      &:hover:not(:disabled) {
        background: $purple-100;
      }
      
      &:disabled {
        opacity: 0.6;
        cursor: not-allowed;
      }
    }
  }
}

.form-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
  
  .remember-me {
    display: flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;
    
    input {
      width: 16px;
      height: 16px;
      border-radius: 4px;
      border-color: $gray-300;
      accent-color: $purple-600;
    }
    
    span {
      font-size: 12px;
      color: $gray-500;
    }
  }
  
  .forgot-pwd {
    font-size: 12px;
    color: $purple-600;
    text-decoration: none;
    
    &:hover {
      color: $purple-700;
    }
  }
}

.login-btn {
  width: 100%;
  padding: 12px 0;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  background: $purple-600;
  border: none;
  border-radius: 12px;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(124, 58, 237, 0.3);
  transition: all 0.2s;
  
  &:hover:not(:disabled) {
    background: $purple-700;
    transform: scale(1.02);
  }
  
  &:active:not(:disabled) {
    transform: scale(0.98);
  }
  
  &:disabled {
    opacity: 0.7;
    cursor: not-allowed;
  }
}

.register-link {
  text-align: center;
  font-size: 12px;
  color: $gray-500;
  margin-top: 24px;
  
  a {
    color: $purple-600;
    font-weight: 500;
    text-decoration: none;
    
    &:hover {
      color: $purple-700;
    }
  }
}

.copyright {
  text-align: center;
  font-size: 10px;
  color: $gray-400;
  margin-top: 24px;
}

@media (min-width: 480px) {
  .login-form-card {
    padding: 32px;
  }
}
</style>