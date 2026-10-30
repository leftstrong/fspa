// +----------------------------------------------------------------------
// | 图片地址解析：把后端返回的相对路径拼成可访问的绝对地址
// +----------------------------------------------------------------------
// | 背景：后台 dev 跑在 9527，接口在 20012。后端返回的 /static/... 或
// | /crmebimage/... 若直接丢给 <img>，浏览器会去请求 9527 而拿到 404。
// | 这里统一拼上接口域名，前端所有图片引用都走它。
// +----------------------------------------------------------------------

import SettingMer from '@/utils/settingMer'

const FALLBACK =
  'data:image/svg+xml;charset=utf-8,' +
  encodeURIComponent(
    '<svg xmlns="http://www.w3.org/2000/svg" width="80" height="80">' +
      '<rect width="80" height="80" fill="#f5f5f5"/>' +
      '<path d="M24 52l10-12 7 8 6-5 9 9H24z" fill="#d9d9d9"/>' +
      '<circle cx="30" cy="30" r="4" fill="#d9d9d9"/>' +
      '</svg>'
  )

/**
 * 相对路径 → 绝对地址
 * @param {string} path 后端返回的图片路径
 * @returns {string} 可直接用于 <img src> 的地址
 */
export function imgUrl(path) {
  if (!path) return FALLBACK
  const p = String(path).trim()
  if (!p) return FALLBACK
  // 已经是完整地址或内联图，原样返回
  if (/^(https?:)?\/\//i.test(p) || /^data:/i.test(p) || /^blob:/i.test(p)) return p
  const host = (SettingMer.httpUrl || '').replace(/\/+$/, '')
  return host + (p.charAt(0) === '/' ? p : '/' + p)
}

/**
 * 逗号分隔的多图字符串 → 绝对地址数组
 * @param {string} images 形如 "/a.jpg,/b.jpg"
 * @returns {string[]}
 */
export function imgList(images) {
  if (!images) return []
  return String(images)
    .split(',')
    .map(s => s.trim())
    .filter(Boolean)
    .map(imgUrl)
}

/** 占位图，供 v-if 兜底 */
export const IMG_FALLBACK = FALLBACK

export default imgUrl
