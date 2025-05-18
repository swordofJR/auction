<template>
  <div class="panel-container">
    <Card style="width: 100%">
      <p slot="title">
        <Icon type="md-people"></Icon>
        用户管理
      </p>
      <div class="operation-bar">
        <Button type="primary" @click="showAddUserModal">新增用户</Button>
        <Input v-model="searchQuery" placeholder="搜索用户名..." style="width: 200px; margin-left: 10px;" />
        <Button type="primary" icon="md-refresh" @click="fetchUsers" style="margin-left: 10px;">刷新</Button>
      </div>
      <Table border :columns="columns" :data="filteredUsers" :loading="loading"></Table>
    </Card>
    <!-- 添加用户对话框 -->
    <Modal v-model="addUserModal" title="新增用户" @on-ok="addUser" :loading="modalLoading">
      <Form :model="newUser" :rules="rules" ref="newUserForm">
        <FormItem label="用户名" prop="username">
          <Input v-model="newUser.username" placeholder="请输入用户名"></Input>
        </FormItem>
        <FormItem label="邮箱" prop="email">
          <Input v-model="newUser.email" placeholder="请输入邮箱"></Input>
        </FormItem>
        <FormItem label="密码" prop="password">
          <Input type="password" v-model="newUser.password" placeholder="请输入密码"></Input>
        </FormItem>
        <FormItem label="确认密码" prop="confirmPassword">
          <Input type="password" v-model="newUser.confirmPassword" placeholder="请再次输入密码"></Input>
        </FormItem>
      </Form>
      <div slot="footer">
        <Button @click="addUserModal = false">取消</Button>
        <Button type="primary" :loading="modalLoading" @click="addUser">确定</Button>
      </div>
    </Modal>
  </div>
</template>
<script>
import axios from 'axios'
export default {
  name: 'UserManagement',
  data() {
    const validatePassConfirm = (rule, value, callback) => {
      if (value !== this.newUser.password) {
        callback(new Error('两次输入的密码不一致'))
      } else {
        callback()
      }
    }
    return {
      loading: false,
      users: [],
      searchQuery: '',
      addUserModal: false,
      modalLoading: false,
      newUser: {
        username: '',
        email: '',
        password: '',
        confirmPassword: ''
      },
      rules: {
        username: [
          { required: true, message: '请输入用户名', trigger: 'blur' },
          { min: 3, message: '用户名至少3个字符', trigger: 'blur' }
        ],
        email: [
          { required: true, message: '请输入邮箱', trigger: 'blur' },
          { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' },
          { min: 6, message: '密码至少6个字符', trigger: 'blur' }
        ],
        confirmPassword: [
          { required: true, message: '请再次输入密码', trigger: 'blur' },
          { validator: validatePassConfirm, trigger: 'blur' }
        ]
      },
      columns: [
        {
          title: 'ID',
          key: 'id',
          width: 80
        },
        {
          title: '用户名',
          key: 'username'
        },
        {
          title: '钱包地址',
          key: 'address',
          ellipsis: true
        },
        {
          title: '状态',
          key: 'deleted',
          width: 100,
          align: 'center',
          render: (h, params) => {
            const isActive = params.row.deleted === 0
            return h('Tag', {
              props: {
                color: isActive ? 'success' : 'error'
              }
            }, isActive ? '未删除' : '已删除')
          }
        },
        {
          title: '创建时间',
          key: 'createdTime',
          render: (h, params) => {
            return h('div', this.formatDateTime(params.row.createdTime))
          }
        },
        {
          title: '更新时间',
          key: 'updatedTime',
          render: (h, params) => {
            return h('div', this.formatDateTime(params.row.updatedTime))
          }
        },
        {
          title: '操作',
          key: 'action',
          width: 150,
          align: 'center',
          render: (h, params) => {
            const user = params.row
            const isRoot = user.username === 'root'
            const isDeleted = user.deleted === 1
            if (isDeleted) {
              return h('Button', {
                props: {
                  type: 'success',
                  size: 'small'
                },
                on: {
                  click: () => {
                    this.confirmRestore(user)
                  }
                }
              }, '恢复')
            } else if (!isRoot) {
              return h('Button', {
                props: {
                  type: 'error',
                  size: 'small'
                },
                on: {
                  click: () => {
                    this.confirmDelete(user)
                  }
                }
              }, '删除')
            } else {
              return h('span', '系统用户')
            }
          }
        }
      ],
      refreshInterval: null
    }
  },
  computed: {
    filteredUsers() {
      if (!this.searchQuery) return this.users
      const query = this.searchQuery.toLowerCase()
      return this.users.filter(user => {
        return user.username.toLowerCase().includes(query)
      })
    }
  },
  methods: {
    // 格式化日期时间
    formatDateTime(dateTimeStr) {
      if (!dateTimeStr) return ''
      const date = new Date(dateTimeStr)
      return `${date.toLocaleDateString()} ${date.toLocaleTimeString()}`
    },
    // 获取用户列表
    fetchUsers() {
      this.loading = true
      axios.get('/api/admin/users')
        .then(response => {
          console.log('获取到的用户数据:', response.data)
          this.users = response.data || []
        })
        .catch(error => {
          this.$Message.error('获取用户列表失败: ' + (error.response && error.response.data && error.response.data.message ? error.response.data.message : '未知错误'))
          console.error('获取用户列表失败:', error)
        })
        .finally(() => {
          this.loading = false
        })
    },
    // 显示添加用户对话框
    showAddUserModal() {
      this.newUser = {
        username: '',
        email: '',
        password: '',
        confirmPassword: ''
      }
      this.addUserModal = true
      this.$nextTick(() => {
        this.$refs.newUserForm.resetFields()
      })
    },
    // 添加用户
    addUser() {
      this.$refs.newUserForm.validate(valid => {
        if (valid) {
          this.modalLoading = true
          const params = new URLSearchParams()
          params.append('username', this.newUser.username)
          params.append('email', this.newUser.email)
          params.append('password', this.newUser.password)
          axios.post('/api/admin/users', params)
            .then(response => {
              this.$Message.success('添加用户成功')
              this.addUserModal = false
              this.fetchUsers() // 刷新用户列表
            })
            .catch(error => {
              this.$Message.error('添加用户失败: ' + (error.response && error.response.data && error.response.data.message ? error.response.data.message : '未知错误'))
              console.error('添加用户失败:', error)
            })
            .finally(() => {
              this.modalLoading = false
            })
        }
      })
    },
    // 确认删除
    confirmDelete(user) {
      if (user.username === 'root') {
        this.$Message.error('不能删除管理员账号')
        return
      }
      this.$Modal.confirm({
        title: '确认删除',
        content: `确定要删除用户 ${user.username} 吗？删除后该用户将无法登录系统。`,
        onOk: () => {
          this.deleteUser(user.id)
        }
      })
    },
    // 删除用户
    deleteUser(id) {
      axios.delete(`/api/admin/users/${id}`)
        .then(response => {
          this.$Message.success('删除用户成功')
          this.fetchUsers() // 刷新用户列表
        })
        .catch(error => {
          this.$Message.error('删除用户失败: ' + (error.response && error.response.data && error.response.data.message ? error.response.data.message : '未知错误'))
          console.error('删除用户失败:', error)
        })
    },
    confirmRestore(user) {
      this.$Modal.confirm({
        title: '确认恢复',
        content: `确定要恢复用户 ${user.username} 吗？恢复后该用户将可以正常登录系统。`,
        onOk: () => {
          this.restoreUser(user.id)
        }
      })
    },
    restoreUser(id) {
      axios.post(`/api/admin/users/${id}/restore`)
        .then(response => {
          this.$Message.success('恢复用户成功')
          this.fetchUsers()
        })
        .catch(error => {
          this.$Message.error('恢复用户失败: ' + (error.response && error.response.data && error.response.data.message ? error.response.data.message : '未知错误'))
          console.error('恢复用户失败:', error)
        })
    },
    startAutoRefresh() {
      this.refreshInterval = setInterval(() => {
        this.fetchUsers()
      }, 30000) // 每30秒刷新一次
    },
    stopAutoRefresh() {
      if (this.refreshInterval) {
        clearInterval(this.refreshInterval)
        this.refreshInterval = null
      }
    }
  },
  mounted() {
    this.fetchUsers()
    // this.startAutoRefresh()
  },
  beforeDestroy() {
    // this.stopAutoRefresh()
  }
}
</script>
<style scoped>
.panel-container {
  padding: 10px;
}
.operation-bar {
  margin-bottom: 20px;
  display: flex;
  justify-content: flex-start;
}
</style> 