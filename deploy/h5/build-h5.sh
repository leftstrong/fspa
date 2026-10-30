#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# 一键构建 fspa_uni 的 H5 生产包（等价于 HBuilderX「发行 → 网站-H5」）
# 产物输出到 fspa_uni/unpackage/dist/build/h5，供 web 镜像 / nginx 直接托管
#
# 用法：bash deploy/h5/build-h5.sh
#       找不到 HBuilderX 时可用 HBUILDERX_HOME 指定安装目录
# ---------------------------------------------------------------------------
set -euo pipefail

cd "$(dirname "$0")/../.."
ROOT="$(pwd)"
H5_DIR="fspa_uni/unpackage/dist/build/h5"

# ------------------------------ 1. 定位 HBuilderX ------------------------------
# 只用它自带的 node 与 @dcloudio 编译器，避免自己装一套 uni-app 依赖
find_node() {
  local home
  for home in "${HBUILDERX_HOME:-}" \
              "/d/HBuilderX/HBuilderX" \
              "/c/HBuilderX" \
              "/c/Program Files/HBuilderX" \
              "$HOME/AppData/Local/Programs/HBuilderX" \
              "/Applications/HBuilderX.app/Contents/HBuilderX"; do
    [ -n "$home" ] || continue
    if [ -x "$home/plugins/node/node.exe" ]; then
      printf '%s' "$home/plugins/node/node.exe"; return 0
    fi
    if [ -x "$home/plugins/node/bin/node" ]; then
      printf '%s' "$home/plugins/node/bin/node"; return 0
    fi
  done
  return 1
}

NODE_BIN="$(find_node)" || {
  echo "[x] 没找到 HBuilderX（需要它自带的 uni-app 编译器）" >&2
  echo "    请设置 HBUILDERX_HOME，例如：" >&2
  echo "    HBUILDERX_HOME=/d/HBuilderX/HBuilderX bash deploy/h5/build-h5.sh" >&2
  exit 1
}

echo "==> HBuilderX node: $NODE_BIN"
echo "==> 开始构建 H5 生产包（首次约 2~5 分钟）"

# ------------------------------ 2. 构建 ------------------------------
if ! "$NODE_BIN" deploy/h5/build-h5.js; then
  echo "[x] 构建失败，详见上方日志" >&2
  exit 1
fi

# ------------------------------ 3. 产物校验 ------------------------------
if [ ! -f "$H5_DIR/index.html" ]; then
  echo "[x] 构建完成但没有产物：$H5_DIR/index.html" >&2
  exit 1
fi

# 生产包必须是同源请求。命中这些说明打包时 config/app.js 又走回了局域网/占位域名
if grep -rql "192\.168\.\|127\.0\.0\.1:20012\|flowerspa\.cn" "$H5_DIR/static" 2>/dev/null; then
  echo "[x] 产物里还写着内网地址或占位域名，线上会请求失败：" >&2
  grep -rl "192\.168\.\|127\.0\.0\.1:20012\|flowerspa\.cn" "$H5_DIR/static" 2>/dev/null | sed 's/^/    /' >&2
  echo "    检查 fspa_uni/config/app.js 的 H5 分支是否仍取 window.location.origin" >&2
  exit 1
fi

# 部署路径是 /h5/ 子路径，资源和路由都必须是 /h5/ 前缀，
# 否则线上页面能打开但 JS/CSS 全 404（白屏）
if ! grep -q "/h5/static/" "$H5_DIR/index.html" 2>/dev/null; then
  echo "[x] 产物不是按 /h5/ 子路径构建的（index.html 里没有 /h5/static/）" >&2
  echo "    检查 fspa_uni/manifest.json → h5.router.base 是否为 /h5/" >&2
  exit 1
fi

SIZE=$(du -sh "$H5_DIR" | cut -f1)
COUNT=$(find "$H5_DIR" -type f | wc -l | tr -d ' ')
echo ""
echo "[✓] H5 构建完成：$H5_DIR（$COUNT 个文件，$SIZE）"
echo "    接口走同源 /api/front，静态资源与路由基址为 /h5/。"
