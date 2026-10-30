// +----------------------------------------------------------------------
// | 良物 · 以物换物 管理后台路由
// +----------------------------------------------------------------------
// | 主题：物为人用，方为良物
// +----------------------------------------------------------------------

import Layout from '@/layout'

const barterRouter = {
  path: '/barter',
  component: Layout,
  redirect: '/barter/dashboard/index',
  name: 'Barter',
  meta: {
    title: '良物换物',
    icon: 's-management'
  },
  children: [
    {
      path: 'dashboard/index',
      component: () => import('@/views/barter/dashboard/index'),
      name: 'BarterDashboard',
      meta: { title: '数据驾驶舱', icon: '' }
    },
    {
      path: 'item/index',
      component: () => import('@/views/barter/item/index'),
      name: 'BarterItem',
      meta: { title: '物品审核', icon: '' }
    },
    {
      path: 'order/index',
      component: () => import('@/views/barter/order/index'),
      name: 'BarterOrder',
      meta: { title: '换物订单', icon: '' }
    },
    {
      path: 'level/index',
      component: () => import('@/views/barter/level/index'),
      name: 'BarterLevel',
      meta: { title: '会员等级', icon: '' }
    },
    {
      path: 'category/index',
      component: () => import('@/views/barter/category/index'),
      name: 'BarterCategory',
      meta: { title: '品类管理', icon: '' }
    },
    {
      path: 'report/index',
      component: () => import('@/views/barter/report/index'),
      name: 'BarterReport',
      meta: { title: '举报处理', icon: '' }
    },
    {
      path: 'member/index',
      component: () => import('@/views/barter/member/index'),
      name: 'BarterMember',
      meta: { title: '换物用户', icon: '' }
    },
    {
      path: 'config/index',
      component: () => import('@/views/barter/config/index'),
      name: 'BarterConfig',
      meta: { title: '规则配置', icon: '' }
    }
  ]
}

export default barterRouter
