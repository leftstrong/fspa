// +----------------------------------------------------------------------
// | CRMEB [ CRMEB赋能开发者，助力企业发展 ]
// +----------------------------------------------------------------------
// | Copyright (c) 2016~2021 https://www.crmeb.com All rights reserved.
// +----------------------------------------------------------------------
// | Licensed CRMEB并不是自由软件，未经许可不能去掉CRMEB相关版权
// +----------------------------------------------------------------------
// | Author: CRMEB Team <admin@crmeb.com>
// +----------------------------------------------------------------------

import Layout from '@/layout'

const appointmentRouter = {
  path: '/appointment',
  component: Layout,
  redirect: '/appointment/appointmentIndex',
  name: 'Appointment',
  meta: {
    title: '预约管理',
    icon: 'clipboard'
  },
  children: [
    {
      path: 'appointmentIndex',
      component: () => import('@/views/appointment/appointmentIndex'),
      name: 'AppointmentIndex',
      meta: { title: '预约信息', icon: '' }
    },
    {
      path: 'artisanIndex',
      component: () => import('@/views/appointment/artisanIndex'),
      name: 'ArtisanIndex',
      meta: { title: '手艺人信息', icon: '' }
    },
  ]
}
  //   path: '/appointment',
  //   component: Layout,
  //   redirect: '/appointment/index',
  //   name: 'Appointment',
  //   meta: {
  //     title: '预约管理',
  //     icon: 'clipboard'
  //   },
  //   children: [
  //     {
  //       path: 'appointmentIndex',
  //       component: () => import('@/views/appointment/appointmentIndex'),
  //       name: 'AppointmentIndex',
  //       meta: { title: '预约信息', icon: '' }
  //     },
  //     {
  //       path: 'artisanIndex',
  //       component: () => import('@/views/appointment/artisanIndex'),
  //       name: 'AppointmentIndex',
  //       meta: { title: '手艺人信息', icon: '' }
  //     }
  //   ]
  // }

export default appointmentRouter
