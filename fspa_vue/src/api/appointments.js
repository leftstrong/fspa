import request from '@/utils/request'

/**
 * 新增EbAppointments
 * @param pram
 */
export function appointmentsCreate(data) {
  return request({
    url: 'admin/appointments/save',
    method: 'POST',
    data
  })
}

/**
 * appointments更新
 * @param pram
 */
export function appointmentsUpdate(data) {
  return request({
    url: 'admin/appointments/update',
    method: 'POST',
    data
  })
}

/**
 * appointments详情
 * @param pram
 */
export function appointmentsDetail(pram) {
  const data = {
    id: pram.id
  }
  return request({
    url: `admin/appointments/info`,
    method: 'GET',
    params: data
  })
}

/**
 * appointments删除
 * @param pram
 */
export function appointmentsDelete(pram) {
  const data = {
    id: pram.id
  }
  return request({
    url: `admin/appointments/delete`,
    method: 'get',
    params: data
  })
}


/**
 * appointments列表
 * @param pram
 */
export function appointmentsList(params) {
  return request({
    url: '/admin/appointments/list',
    method: 'GET',
    params
  })
}
