/**
 * @author Chennong
 * @date 2023-05-10
 * @description 门店购物车api
 */
import request from "@/utils/request";

/**
 * 获取购物车列表
 * @param numType boolean true 购物车数量,false=购物车产品数量
 */
export function getCartCounts(numType,type) {
  return request.get("cart/count?numType=" + numType + "&type=" + type);
}
/**
 * 获取购物车列表
 *
 */
export function getCartList(params) {
  return request({
    url: '/admin/shop/cart/list',
    method: 'GET',
    params
  })
}

/**
 * 购车添加
 *
 */
export function postCartAdd(data) {
  return request.post('/admin/shop/cart/save', data, {});
}

/**
 * 修改购物车商品数量
 */
export function changeCartNum(data) {
  return request({
    url: '/admin/shop/cart/num',
    method: 'POST',
    data
  })
}
/**
 * 清除购物车
 * @param object ids join(',') 切割成字符串
 */
export function cartDel(ids) {
  if (typeof ids === 'object')
    ids = ids.join(',');
  return request.post('cart/delete', {
    ids: ids
  }, {}, 1);
}

/**
 * 购物车重选提交
 *
 */
export function getResetCart(data) {
  return request.post('cart/resetcart', data);
}

/**
 * 将字符串 转base64
 * @param object data
 */
export function qrcodeApi(data) {
  return request.post('qrcode/str2base64', data, {}, 1);
}

/**
 * 清空购物车
 */
export function cleanCart() {
  return request({
    url: '/admin/shop/cart/flush',
    method: 'DELETE',
  })
}

/**
 * 商品移出购物车
 */
export function removeProductForCart(data) {
  return request({
    url: '/admin/shop/cart/delete',
    method: 'DELETE',
    data
  })
}
