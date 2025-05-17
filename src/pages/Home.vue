<template>
  <div class="home">
    <Row class="header" type="flex" align="middle">
      <div class="logo">
        <img src="../assets/img/logo2.png" height="30" alt="">
        <span></span>基于区块链的竞拍品存证溯源应用管理系统<Tag></Tag>
      </div>
      <!-- <VmMsgPush style="margin-left:20px" :data="msgPushData"></VmMsgPush> -->
      <Dropdown class="login-info" placement="bottom-end">
        <Button type="ghost">
            <img src="../assets/img/photo1.jpg" height="30" alt="">{{ currentUser.username }}
            <Icon type="arrow-down-b"></Icon>
        </Button> 
        <Dropdown-menu slot="list">
            <Dropdown-item divided @click="logout"><i class="fa fa-key"></i>退出登录</Dropdown-item>
            <Dropdown-item divided><i class="fa fa-cog"></i>用户信息</Dropdown-item>
        </Dropdown-menu>
      </Dropdown>
    </Row>   
    <div class="sidebar">
      <Menu theme="dark" width="100%" class="menu" :active-name="activeName" :accordion="true">
          <!-- <Menu-item name="Dashboard">
              <router-link to="/">
                <i class="fa fa-dashboard"></i>
                仪表盘  
              </router-link>
          </Menu-item> -->
          <!-- <Menu-item name="Widget">
              <router-link to="/widget">
                <i class="fa fa-cogs"></i>
                我的  
              </router-link>
          </Menu-item> -->
          <!-- <Menu-item name="Login">
              <router-link to="/login">
                <i class="fa fa-laptop"></i>
                登录  
              </router-link>
          </Menu-item> -->
          <Menu-item name="Panels" v-if="isUser && !isAdmin">
              <router-link to="/panels">
                <i class="fa fa-database"></i>
                上传竞品信息
              </router-link>
          </Menu-item>
          <!-- <Menu-item name="Editor">
              <router-link to="/editor">
                <i class="fa fa-pencil"></i>
                Editor
              </router-link>
          </Menu-item> -->
          <Menu-item name="Marketplace" v-if="canAccessMarket">
              <router-link to="/marketplace">
                <i class="fa fa-gavel"></i>
                拍卖市场  
              </router-link>
          </Menu-item>
          <!-- <Menu-item name="AuctionMarketplace" v-if="canAccessMarket">
              <router-link to="/auction-marketplace">
                <i class="fa fa-gavel"></i>
                竞品拍卖市场  
              </router-link>
          </Menu-item> -->
          <!-- <Menu-item name="ImageList" v-if="canAccessMarket">
              <router-link to="/imagelist">
                <i class="fa fa-shopping-bag"></i>
                数字藏品交易市场  
              </router-link>
          </Menu-item> -->
          <!-- <Menu-item name="Charts">
              <router-link to="/charts">
                <i class="fa fa-bar-chart"></i>
                Charts  
              </router-link>
          </Menu-item> -->
          <Menu-item name="BasicTable" v-if="isUser && !isAdmin">
              <router-link to="/basic-table">
                <i class="fa fa-id-card"></i>
                我的竞品  
              </router-link>
          </Menu-item>
          <Menu-item name="EditableTable" v-if="isAdmin">
              <router-link to="/editable-table">
                <i class="fa fa-id-card"></i>
                竞品审核信息  
              </router-link>
          </Menu-item>
          <Menu-item name="AllCopyrights" v-if="isAdmin">
              <router-link to="/all-copyrights">
                <i class="fa fa-list"></i>
                查看竞品信息  
              </router-link>
          </Menu-item>
          <Menu-item name="UserManagement" v-if="isAdmin">
              <router-link to="/user-management">
                <i class="fa fa-users"></i>
                用户管理  
              </router-link>
          </Menu-item>
          <!-- <Submenu name="DataTable">
              <template slot="title">
                  <i class="fa fa-id-card"></i>
                  我的
              </template>
              <Menu-item name="BasicTable">
                <router-link to="/basic-table">
                  我的数字藏品 
                </router-link>
              </Menu-item>
              <Menu-item name="EditableTable">
                <router-link to="/editable-table">
                  数字藏品信息  
                </router-link>
              </Menu-item>
          </Submenu> -->
          <!-- <Submenu name="Pages">
              <template slot="title">
                  <i class="fa fa-file"></i>
                  登录
              </template>
              <Menu-item name="Login">
                <router-link to="/login">
                  钱包登录 
                </router-link>
              </Menu-item> -->
              <!-- <Menu-item name="Lockscreen">
                <router-link to="/lockscreen">
                  Lockscreen 
                </router-link>
              </Menu-item>
              <Menu-item name="Loading">
                <router-link to="/loading">
                  Loading
                </router-link>
              </Menu-item> -->
          <!-- </Submenu> -->
      </Menu>
    </div>
    <div class="main-content">
      <keep-alive>
        <router-view v-if="$route.meta.keepAlive"></router-view>
      </keep-alive>
      <router-view v-if="!$route.meta.keepAlive"></router-view>
      <p class="vm-author"><a href="https://github.com/luosijie" target="_blank"></a></p> 
  </div>
  </div>
</template>
<script>
import VmMsgPush from '@/components/vm-msg-push.vue'
export default {
  name: 'home',
  components: {
    VmMsgPush
  },
  computed: {
    currentUser() {
      return this.user || { username: '游客' }
    },
    isAdmin() {
      return this.user && this.user.role === 'admin'
    },
    isUser() {
      // 仅普通用户可以看到上传版权和我的数字藏品菜单
      return this.user && this.user.role === 'user'
    },
    // 管理员和用户都可以看到交易市场
    canAccessMarket() {
      return this.user && (this.user.role === 'user' || this.user.role === 'admin')
    }
  },
  mounted: function () {
    this.activeName = this.$route.name
    // 从sessionStorage获取用户信息，确保每个窗口有独立的会话
    const userStr = sessionStorage.getItem('user')
    if (userStr) {
      try {
        this.user = JSON.parse(userStr)
        // 确保导航栏选中当前页面，不自动跳转
        this.activeName = this.$route.name || (this.isAdmin ? 'EditableTable' : 'Panels')
      } catch (e) {
        console.error('Failed to parse user info:', e)
        this.logout() // 解析失败时注销
      }
    } else {
      // 没有用户信息则重定向到登录页
      this.$router.push('/login')
    }
  },
  methods: {
    logout() {
      sessionStorage.removeItem('user')
      this.$router.push('/login')
    }
  },
  data () {
    return {
      user: null,
      activeName: 'Dashboard',
      msgPushData: [
        {
          image: require('@/assets/img/photo.jpg'),
          from: 'JesseLuo',
          time: '2017-1-8',
          message: 'I like your website very much!'
        },
        {
          image: require('@/assets/img/photo.jpg'),
          from: 'JesseLuo',
          time: '2017-1-8',
          message: 'I like your website very much!'
        },
        {
          image: require('@/assets/img/photo.jpg'),
          from: 'JesseLuo',
          time: '2017-1-8',
          message: 'I like your website very much!'
        },
        {
          image: require('@/assets/img/photo.jpg'),
          from: 'JesseLuo',
          time: '2017-1-8',
          message: 'I like your website very much!'
        },
        {
          image: require('@/assets/img/photo.jpg'),
          from: 'JesseLuo',
          time: '2017-1-8',
          message: 'I like your website very much!'
        }
      ]
    }
  }
}
</script>
