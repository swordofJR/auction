<template>
  <div class="traditional-login">
    <div class="login-box">
      <h2>用户登录</h2>
      <form @submit.prevent="handleLogin">
        <div class="form-group">
          <label>用户名</label>
          <input v-model="username" type="text" required />
        </div>
        <div class="form-group">
          <label>密码</label>
          <input v-model="password" type="password" required />
        </div>
        <div class="form-group">
          <button type="submit" :disabled="loading">
            {{ loading ? '登录中...' : '登录' }}
          </button>
        </div>
        <div class="form-group register-link">
          <span>还没有账号？</span>
          <router-link to="/register">注册</router-link>
        </div>
        <div v-if="error" class="error">{{ error }}</div>
      </form>
    </div>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'TraditionalLogin',
  data() {
    return {
      username: '',
      password: '',
      error: '',
      loading: false
    }
  },
  methods: {
    handleLogin() {
      this.error = ''
      this.loading = true
      const params = new URLSearchParams()
      params.append('username', this.username)
      params.append('password', this.password)
      axios.post('/api/user/login', params)
        .then(response => {
          const user = response.data
          if (this.username === 'root') {
            user.role = 'admin'
          } else {
            user.role = user.role || 'user'
          }
          sessionStorage.setItem('user', JSON.stringify(user))
          if (user.role === 'admin') {
            this.$router.push('/editable-table')
          } else {
            this.$router.push('/panels')
          }
        })
        .catch(error => {
          console.error('登录失败:', error)
          if (error.response && error.response.data) {
            console.error('错误详情:', error.response.data)
            this.error = error.response.data.message || '登录失败，请检查用户名和密码'
          } else {
            this.error = '登录失败，服务器连接异常'
          }
        })
        .finally(() => {
          this.loading = false
        })
    }
  }
}
</script>

<style scoped>
.traditional-login {
  width: 100vw;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f6fa;
}
.login-box {
  background: #fff;
  padding: 40px 30px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  width: 350px;
}
.login-box h2 {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
}
.form-group {
  margin-bottom: 20px;
}
.form-group label {
  display: block;
  margin-bottom: 8px;
  color: #333;
  font-weight: 500;
}
.form-group input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 16px;
  transition: border-color 0.3s;
}
.form-group input:focus {
  border-color: #409eff;
  outline: none;
}
button[type="submit"] {
  width: 100%;
  padding: 12px;
  background: #409eff;
  color: #fff;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
  transition: background-color 0.3s;
}
button[type="submit"]:hover:not(:disabled) {
  background: #66b1ff;
}
button[type="submit"]:disabled {
  background: #a0cfff;
  cursor: not-allowed;
}
.error {
  color: #f56c6c;
  text-align: center;
  margin-top: 10px;
  font-size: 14px;
}
.register-link {
  text-align: center;
  font-size: 14px;
}
.register-link a {
  color: #409eff;
  margin-left: 5px;
  text-decoration: none;
}
.register-link a:hover {
  text-decoration: underline;
}
</style>
 