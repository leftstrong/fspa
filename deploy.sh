#!/usr/bin/env bash
# =============================================================================
# 良物换物 · 一键部署脚本
#
#   ./deploy.sh          构建并启动（默认）
#   ./deploy.sh build    只构建镜像
#   ./deploy.sh up       只启动
#   ./deploy.sh ps       查看状态
#   ./deploy.sh logs     跟踪日志
#   ./deploy.sh down     停止并保留数据
#   ./deploy.sh reset    停止并删除所有数据（含数据库，慎用）
# =============================================================================
set -euo pipefail

cd "$(dirname "$0")"

RED=$'\033[31m'; GREEN=$'\033[32m'; YELLOW=$'\033[33m'; BOLD=$'\033[1m'; NC=$'\033[0m'
info() { echo "${GREEN}[ok]${NC} $*"; }
warn() { echo "${YELLOW}[!]${NC} $*"; }
die()  { echo "${RED}[x]${NC} $*" >&2; exit 1; }

# 从 .env 取一个值。不 source，避免 JVM 参数里的空格被 shell 当成命令执行
env_get() {
  local key="$1" def="${2:-}" val
  val=$(grep -E "^[[:space:]]*${key}=" .env 2>/dev/null | tail -n 1 | cut -d= -f2-) || true
  if [ -z "$val" ]; then
    printf '%s' "$def"
    return 0
  fi
  val="${val%\"}"; val="${val#\"}"; val="${val%\'}"; val="${val#\'}"
  printf '%s' "$val"
}

# ------------------------------ 1. 环境检查 ------------------------------
command -v docker >/dev/null 2>&1 || die "没有找到 docker，请先安装 Docker Engine"

if docker compose version >/dev/null 2>&1; then
  dc() { docker compose "$@"; }
elif command -v docker-compose >/dev/null 2>&1; then
  dc() { docker-compose "$@"; }
else
  die "没有找到 docker compose（v2 插件或 docker-compose 均可）"
fi

# ------------------------------ 2. .env ------------------------------
if [ ! -f .env ]; then
  cp .env.example .env
  warn "已生成 .env，但里面的密码还是默认值。"
  warn "请先编辑 .env 修改 MYSQL_ROOT_PASSWORD / REDIS_PASSWORD，然后重新运行："
  echo "        vi .env && ./deploy.sh"
  exit 1
fi

MYSQL_PWD_CHECK=$(env_get MYSQL_ROOT_PASSWORD)
if [ -z "$MYSQL_PWD_CHECK" ]; then
  die ".env 里缺少 MYSQL_ROOT_PASSWORD"
fi
case "$MYSQL_PWD_CHECK" in
  ChangeMe*) die "请先修改 .env 里的默认密码（MYSQL_ROOT_PASSWORD / REDIS_PASSWORD）" ;;
esac
info "配置已加载"

# ------------------------------ 3. H5 产物检查 ------------------------------
H5_DIR="fspa_uni/unpackage/dist/build/h5"
if [ ! -f "$H5_DIR/index.html" ]; then
  die "缺少 H5 构建产物：$H5_DIR
   先在本地跑 bash deploy/h5/build-h5.sh（或用 HBuilderX 发行 网站-H5）。"
fi

if grep -rql "192\.168\.\|127\.0\.0\.1:20012\|flowerspa\.cn" "$H5_DIR/static" 2>/dev/null; then
  die "H5 产物里还写着内网地址或占位域名，线上会请求失败。
   重新构建：bash deploy/h5/build-h5.sh
   并确认 fspa_uni/config/app.js 的 H5 分支仍取 window.location.origin。"
fi

# H5 部署在 /h5/ 子路径下：路由 base 和静态资源路径都必须是 /h5/，
# 否则页面能打开，但 JS/CSS 会打到 /static/ 变成 404（白屏）
if ! grep -q "/h5/static/" "$H5_DIR/index.html" 2>/dev/null; then
  die "H5 产物不是按 /h5/ 子路径构建的（index.html 里没有 /h5/static/）。
   检查 fspa_uni/manifest.json → h5.router.base 是否为 /h5/，然后重新构建：
   bash deploy/h5/build-h5.sh"
fi
info "H5 产物就绪（子路径 /h5/）"

# ------------------------------ 4. 执行 ------------------------------
ACTION="${1:-up-all}"
case "$ACTION" in
  build)  dc build ;;
  up)     dc up -d --remove-orphans ;;
  up-all) dc build && dc up -d --remove-orphans ;;
  ps)     dc ps ;;
  logs)   dc logs -f --tail=200 ;;
  down)   dc down ;;
  reset)
    warn "即将删除数据库、Redis 与上传图片的所有数据。"
    read -r -p "确认继续？输入 yes 回车：" ans
    [ "$ans" = "yes" ] || die "已取消"
    dc down -v
    ;;
  *) die "未知命令：$ACTION" ;;
esac

if [ "$ACTION" = "up-all" ] || [ "$ACTION" = "up" ]; then
  HTTP_PORT=$(env_get HTTP_PORT 13999)
  echo
  echo "${BOLD}服务已启动${NC}  （对外只开 ${HTTP_PORT} 一个端口）"
  echo "  入口导航  http://<服务器IP>:${HTTP_PORT}/"
  echo "  品牌官网  http://<服务器IP>:${HTTP_PORT}/web/"
  echo "  H5 商城   http://<服务器IP>:${HTTP_PORT}/h5/"
  echo "  管理后台  http://<服务器IP>:${HTTP_PORT}/admin/"
  echo "  健康检查  http://<服务器IP>:${HTTP_PORT}/healthz"
  echo
  echo "  /api/front/**  →  crmeb-front:20012"
  echo "  /api/admin/**  →  crmeb-admin:20010"
  echo "  /crmebimage/** →  crmeb-front:20012"
  echo
  echo "首次启动 MySQL 要导入 SQL，接口容器可能重启几次，属正常现象。"
  echo "查看日志：./deploy.sh logs"
fi
