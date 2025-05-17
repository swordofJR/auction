<template>
  <Row class="vm-login vm-panel">
   <Col span="10" class="login-form">
     <div class="login-header">
       <img src="../assets/img/logo.png" height="200" alt="">
       <p class="title"><span></span>基于区块链的</p>
       <p class="title"><span></span>数字藏品版权保护系统</p>
     </div>
     <div class="login-form">
       <div v-if="!isConnected" class="wallet-connect">
         <Button type="primary" @click="connectWallet" :loading="connecting" size="large">
           <template v-if="!connecting">
             <Icon type="logo-bitcoin" />
             连接钱包
           </template>
           <template v-else>
             Connecting...
           </template>
         </Button>
         <p v-if="error" class="error-message">{{ error }}</p>
       </div>
       <div v-else class="wallet-info">
         <div class="wallet-address">
           <Icon type="ios-wallet" />
           <span>{{ shortAddress }}</span>
         </div>
         <Button type="primary" @click="disconnectWallet" size="large">Disconnect</Button>
       </div>
     </div>
   </Col>
   <Col span="14" class="login-ad">
     <span class="photo-author"></span>
   </Col>
  </Row>
</template>
<script>
export default {
  name: 'VmLogin',
  data() {
    return {
      isConnected: false,
      connecting: false,
      error: '',
      account: ''
    }
  },
  computed: {
    shortAddress() {
      if (!this.account) return ''
      return `${this.account.slice(0, 6)}...${this.account.slice(-4)}`
    }
  },
  methods: {
    async connectWallet() {
      if (!window.ethereum) {
        this.error = '请安装 MetaMask 钱包'
        return
      }
      this.connecting = true
      this.error = ''
      try {
        // 使用 enable() 方法连接钱包
        const accounts = await window.ethereum.enable()
        this.account = accounts[0]
        this.isConnected = true
        // 连接成功后跳转
        this.$router.push('/imagelist')
      } catch (error) {
        this.error = error.message || '连接钱包失败'
      } finally {
        this.connecting = false
      }
    },
    disconnectWallet() {
      this.isConnected = false
      this.account = ''
    }
  }
}
</script>
<style lang="less" scoped>
.vm-login {
  width: 1200px;
  height: 600px;
  margin: 0 auto;
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  &>div {
    padding: 40px;
    height: 100%;
  }
  .login-form {
    display: flex;
    flex-direction: column;
    justify-content: center;
    background-color: #fff;
    border-radius: 8px 0 0 8px;
    .login-header {
      text-align: center;
      margin-bottom: 40px;
      img {
        margin-bottom: 20px;
      }
      .title {
        font-size: 24px;
        font-weight: bold;
        margin: 10px 0;
        color: #333;
      }
    }
    .wallet-connect {
      text-align: center;
      button {
        width: 100%;
        height: 50px;
        font-size: 18px;
      }
    }
    .wallet-info {
      text-align: center;
      .wallet-address {
        margin-bottom: 20px;
        font-size: 16px;
        i {
          margin-right: 10px;
        }
      }
      button {
        width: 100%;
        height: 50px;
        font-size: 18px;
      }
    }
  }
  .login-ad {
    height: 100%;
    font-weight: bold;
    font-size: 14px;
    border-radius: 0 8px 8px 0;
    background: url("../assets/img/login-bg.jpg") no-repeat center center;
    background-size: cover;
    position: relative;
    .photo-author {
      position: absolute;
      right: 20px;
      bottom: 20px;
      color: rgba(255, 255, 255, 0.8);
    }
  }
}
</style>
