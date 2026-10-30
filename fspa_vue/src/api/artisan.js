import request from '@/utils/request'

/**
 * 新增EbArtisan
 * @param pram
 */
export function artisanCreate(data) {
  return request({
    url: '/admin/artisan/save',
    method: 'POST',
    data
  })
}

/**
 * ebartisan更新
 * @param pram
 */
export function artisanUpdate(data) {
  return request({
    url: '/admin/artisan/update',
    method: 'POST',
    data
  })
}

/**
 * ebartisan详情
 * @param pram
 */
export function artisanDetail(pram) {
  const data = {
    id: pram.id
  }
  return request({
    url: `/admin/artisan/info`,
    method: 'get',
    params: data
  })
}

/**
 * ebartisan删除
 * @param pram
 */
export function artisanDelete(pram) {
   const data = {
    id: pram.id
  }
  return request({
    url: `/admin/artisan/delete`,
    method: 'get',
    params: data
  })
}


/**
 * ebartisan列表
 * @param pram
 */
export function artisanList(params) {
  return request({
    url: '/admin/artisan/list',
    method: 'get',
    params
  })
}
