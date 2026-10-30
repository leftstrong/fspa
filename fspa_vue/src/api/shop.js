/**
 * @author Chennong
 * @date 2023-05-10
 * @description 门店api
 */
import request from '@/utils/request'


/**
 * 获取产品详情
 * @param int id
 *
 */
export function getProductDetail(params) {
  return request({
    url: '/admin/shop/product/detail',
    method: 'GET',
    params
  })
}

/**
 * @author Chennong
 * @date 2023-05-11
 * @description 查询用户信息
 */
export function getUserInfoByPhone(params) {
  return request({
    url: '/admin/shop/getUserInfoByPhone',
    method: 'GET',
    params
  })
}

/**
 * 提货点分页列表
 * @param pram
 */
export function shopListApi(params) {
  return request({
    url: '/admin/shop/list',
    method: 'get',
    params: params
  })
}
