import Vue from 'vue'
import Router from 'vue-router'
import Home from '@/pages/Home'
import Dashboard from '@/pages/Dashboard'
import BasicTable from '@/pages/BasicTable'
import EditableTable from '@/pages/EditableTable'
import Widget from '@/pages/Widget'
import Panels from '@/pages/Panels'
import Editor from '@/pages/Editor'
import ImageList from '@/pages/ImageList'
import Charts from '@/pages/Charts'
import Login from '@/pages/Login'
import Register from '@/pages/Register'
import LockScreen from '@/pages/LockScreen'
import Loading from '@/pages/Loading'
import AllCopyrights from '@/pages/AllCopyrights'
import UserManagement from '@/pages/UserManagement'
import Marketplace from '@/pages/Marketplace'
import Traceability from '@/pages/Traceability'

Vue.use(Router)

const router = new Router({
    routes: [
        {
            path: '/',
            redirect: '/login'
        },
        {
            path: '/',
            component: Home,
            children: [
                {
                    path: '/',
                    name: 'Dashboard',
                    component: Dashboard
                },
                {
                    path: 'widget',
                    name: 'Widget',
                    component: Widget
                },
                {
                    path: 'panels',
                    name: 'Panels',
                    component: Panels,
                    meta: {
                        title: '上传版权',
                        requiresAuth: true,
                        role: 'user'
                    }
                },
                {
                    path: 'editor',
                    name: 'Editor',
                    component: Editor
                },
                {
                    path: 'imagelist',
                    name: 'ImageList',
                    component: ImageList,
                    meta: {
                        requiresAuth: true,
                        role: 'user'
                    }
                },
                {
                    path: 'basic-table',
                    name: 'BasicTable',
                    component: BasicTable,
                    meta: {
                        keepAlive: true,
                        title: '我的数字藏品版权',
                        requiresAuth: true,
                        role: 'user'
                    }
                },
                {
                    path: 'editable-table',
                    name: 'EditableTable',
                    component: EditableTable,
                    meta: {
                        keepAlive: true,
                        title: '审核版权申请',
                        requiresAuth: true,
                        role: 'admin'
                    }
                },
                {
                    path: 'all-copyrights',
                    name: 'AllCopyrights',
                    component: AllCopyrights,
                    meta: {
                        keepAlive: true,
                        title: '查看版权信息',
                        requiresAuth: true,
                        role: 'admin'
                    }
                },
                {
                    path: 'user-management',
                    name: 'UserManagement',
                    component: UserManagement,
                    meta: {
                        keepAlive: true,
                        title: '用户管理',
                        requiresAuth: true,
                        role: 'admin'
                    }
                },
                {
                    path: 'marketplace',
                    name: 'Marketplace',
                    component: Marketplace,
                    meta: {
                        keepAlive: true,
                        title: '拍卖市场',
                        requiresAuth: true
                    }
                },
                {
                    path: 'traceability',
                    name: 'Traceability',
                    component: Traceability,
                    meta: {
                        keepAlive: true,
                        title: '竞品溯源',
                        requiresAuth: true
                    }
                },
                {
                    path: 'charts',
                    name: 'Charts',
                    component: Charts
                }
            ]
        },
        {
            path: '/login',
            name: 'Login',
            components: {
                blank: Login
            }
        },
        {
            path: '/register',
            name: 'Register',
            components: {
                blank: Register
            }
        },
        {
            path: '/lockscreen',
            name: 'Lockscreen',
            components: {
                blank: LockScreen
            }
        },
        {
            path: '/loading',
            name: 'Loading',
            components: {
                blank: Loading
            }
        }
    ]
})

// 添加路由守卫
router.beforeEach((to, from, next) => {
    // 检查页面是否需要认证
    if (to.matched.some(record => record.meta.requiresAuth)) {
        // 从sessionStorage获取当前会话的用户信息
        const userStr = sessionStorage.getItem('user')
        if (!userStr) {
            // 未登录，跳转到登录页
            next({
                path: '/login'
            })
            return
        }

        // 解析用户信息
        try {
            const user = JSON.parse(userStr)
            const role = user.role

            // 检查页面是否有角色要求，以及用户是否满足要求
            if (to.meta.role) {
                // 如果页面需要user角色，任何登录用户都可以访问
                if (to.meta.role === 'user') {
                    next(); // 允许访问
                    return;
                }

                // 如果页面需要admin角色，只有admin可以访问
                if (to.meta.role === 'admin' && role !== 'admin') {
                    // 非管理员重定向到个人中心
                    next({
                        path: '/panels'
                    });
                    return;
                }
            }

            // 用户已登录且满足角色要求，允许访问
            next()
        } catch (e) {
            console.error('Failed to parse user info:', e)
            // 解析出错，跳转到登录页
            next({
                path: '/login'
            })
        }
    } else {
        // 不需要认证的页面直接放行
        next()
    }
})

export default router
