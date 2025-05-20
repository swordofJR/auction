<template>
  <div class="home">
    <Row class="header" type="flex" align="middle">
      <div class="logo">
        <img src="../assets/img/logo2.png" height="30" alt="">
        <span></span>基于区块链的竞拍品存证溯源应用管理系统<Tag></Tag>
      </div>
      <Dropdown class="login-info" placement="bottom-end">
        <Button type="ghost">
            <img src="../assets/img/photo1.jpg" height="30" alt="">{{ currentUser.username }}
            <Icon type="arrow-down-b"></Icon>
        </Button> 
        <Dropdown-menu slot="list">
            <Dropdown-item @click.native="handleLogout"><i class="fa fa-key"></i>退出登录</Dropdown-item>
        </Dropdown-menu>
      </Dropdown>
    </Row>   
    <div class="sidebar">
      <Menu theme="dark" width="100%" class="menu" :active-name="activeName" :accordion="true">
          <Menu-item name="Panels" v-if="isUser && !isAdmin">
              <router-link to="/panels">
                <i class="fa fa-database"></i>
                上传竞品信息
              </router-link>
          </Menu-item>
          <Menu-item name="Marketplace" v-if="canAccessMarket">
              <router-link to="/marketplace">
                <i class="fa fa-gavel"></i>
                拍卖市场  
              </router-link>
          </Menu-item>
          <Menu-item name="Traceability">
              <router-link to="/traceability">
                <i class="fa fa-search"></i>
                竞品溯源  
              </router-link>
          </Menu-item>
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
      return this.user && this.user.role === 'user'
    },
    canAccessMarket() {
      return this.user && (this.user.role === 'user' || this.user.role === 'admin')
    }
  },
  mounted: function () {
    this.activeName = this.$route.name
    const userStr = sessionStorage.getItem('user')
    if (userStr) {
      try {
        this.user = JSON.parse(userStr)
        this.activeName = this.$route.name || (this.isAdmin ? 'EditableTable' : 'Panels')
      } catch (e) {
        console.error('Failed to parse user info:', e)
        this.logout()
      }
    } else {
      this.$router.push('/login')
    }
  },
  methods: {
    handleLogout() {
      this.$Modal.confirm({
        title: '提示',
        content: '确定要退出登录吗？',
        onOk: () => {
          this.logout();
        }
      });
    },
    logout() {
      sessionStorage.removeItem('user');
      localStorage.removeItem('user');
      this.$Message.success('已成功退出登录');
      setTimeout(() => {
        this.$router.push('/login');
      }, 500);
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
