import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

// 定义路由记录
const routes: Array<RouteRecordRaw> = [
    // 1. 登录页面 (独立页面，不需要布局容器)
    {
        path: '/login',
        name: 'Login',
        component: () => import('../views/Login.vue'),
        meta: {
            title: '登录',
            requiresAuth: false
        }
    },

    // 2. 公共页面区域 (包含公共子页面)
    {
        path: '/',
        name: 'PublicLayout',
        redirect: '/home', // 默认跳转到子页面
        children: [
            {
                path: 'home', // 访问地址: /home
                name: 'PublicHome',
                component: () => import('../views/custom/Index.vue'),
                meta: {
                    title: '首页',
                    requiresAuth: false
                }
            },
        ]
    },

    // 3. 后台管理区域 (包含后台子页面)
    {
        path: '/admin',
        name: 'AdminLayout',
        component: () => import('../views/admin/Index.vue'),
        meta: {
            title: '后台管理',
            requiresAuth: true // 标记：需要登录才能访问
        },
        redirect: '/admin/dashboard',
        children: [
            {
                path: 'dashboard', // 访问地址: /admin/dashboard
                name: 'AdminDashboard',
                component: () => import('../views/admin/Dashboard.vue'),
                meta: {
                    title: '仪表盘'
                }
            },
            {
                path: 'player',
                meta: {
                    title: '玩家管理'
                },
                children: [
                    {
                        path: 'accounts',
                        name: 'PlayerAccounts',
                        component: () => import('../views/admin/player/PlayerAccounts.vue'),
                        meta: {
                            title: '玩家账号管理'
                        }
                    },
                    {
                        path: 'roles',
                        name: 'PlayerRoles',
                        component: () => import('../views/admin/player/PlayerRoles.vue'),
                        meta: {
                            title: '玩家角色管理'
                        }
                    }
                ]
            },
            {
                path: 'pvf',
                meta: {
                    title: 'PVF管理'
                },
                children: [
                    {
                        path: 'manager',
                        name: 'PvfManager',
                        component: () => import('../views/admin/pvf/PvfManager.vue'),
                        meta: {
                            title: 'PVF管理'
                        }
                    }
                ]
            },
            {
                path: 'login',
                meta: {
                    title: '登录器管理'
                },
                children: [
                    {
                        path: 'config',
                        name: 'LauncherConfig',
                        component: () => import('../views/admin/login/LauncherConfig.vue'),
                        meta: {
                            title: '登录器配置'
                        }
                    },
                    {
                        path: 'versions',
                        name: 'LauncherVersions',
                        component: () => import('../views/admin/login/LauncherVersions.vue'),
                        meta: {
                            title: '版本管理'
                        }
                    }
                ]
            }
        ]
    },

    // 4. 404 页面 (捕获所有未定义的路由)
    {
        path: '/:pathMatch(.*)*',
        name: 'NotFound',
        // 这里简单演示，你可以指向一个专门的 404 组件
        redirect: '/'
    }
]

// 创建路由实例
const router = createRouter({
    // 使用 HTML5 History 模式
    history: createWebHistory(),
    routes
})

// 全局前置守卫 (简单的权限控制)
router.beforeEach((to) => {
    // 1. 设置标题
    document.title = (to.meta.title as string) || '默认标题'

    const isAuthenticated = !!localStorage.getItem('token')

    // 2. 鉴权逻辑
    if (to.meta.requiresAuth && !isAuthenticated) {
        // 返回要跳转的路径对象，相当于 next(...)
        return {
            path: '/login',
            query: { redirect: to.fullPath }
        }
    }
})

export default router
