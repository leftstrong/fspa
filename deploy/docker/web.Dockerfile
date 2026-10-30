# =============================================================================
# 良物换物 · Web 镜像（一个 nginx 托管入口页 + 三套前端，只对外开一个端口）
#
#   /        入口导航页       deploy/portal/
#   /web/    官网            fspa_web（纯静态，相对路径，放子目录天然可用）
#   /h5/     H5 商城         fspa_uni/unpackage/dist/build/h5（bash deploy/h5/build-h5.sh 构建）
#   /admin/  管理后台        fspa_vue（镜像内 npm build，publicPath=/admin/）
#
# 站点根目录统一是 /usr/share/nginx，四个子目录平级，互不覆盖。
# 三套站点同源反代 /api/front、/api/admin、/crmebimage，前端不需要跨域配置。
# =============================================================================

# ------------------------------ 管理端构建 ------------------------------
FROM node:16-bullseye-slim AS admin-build

WORKDIR /src

# 留空 = 走同源（推荐，换域名不用重新打包）；
# 要指向独立接口域名时：docker compose build --build-arg VUE_APP_BASE_API=https://api.xxx.com
ARG VUE_APP_BASE_API=
# 部署子路径，必须和 nginx 的 location 一致，否则资源与路由都会 404
ARG PUBLIC_PATH=/admin/

COPY fspa_vue/package.json fspa_vue/package-lock.json ./
RUN npm ci --no-audit --no-fund || npm install --no-audit --no-fund

COPY fspa_vue/ ./

# 管理端源码里 apiBaseURL = VUE_APP_BASE_API + '/api'，接口路径是 'admin/xxx'，
# 所以同源时这一项必须是空值（空值会回落到 location.origin）
# PUBLIC_PATH 会被 vue.config.js 的 publicPath 和 router 的 base 同时读取
RUN if [ -n "${VUE_APP_BASE_API}" ]; then \
        sed -i "s#^VUE_APP_BASE_API.*#VUE_APP_BASE_API = '${VUE_APP_BASE_API}'#" .env.production; \
    else \
        sed -i "/^VUE_APP_BASE_API/d" .env.production; \
    fi \
    && NODE_OPTIONS=--max-old-space-size=4096 \
       PUBLIC_PATH="${PUBLIC_PATH}" npm run build:prod

# ------------------------------ 运行时 ------------------------------
FROM nginx:1.25-alpine

RUN rm -f /etc/nginx/conf.d/default.conf

COPY deploy/nginx/nginx.conf          /etc/nginx/nginx.conf
COPY deploy/nginx/conf.d/             /etc/nginx/conf.d/
COPY deploy/nginx/snippets/           /etc/nginx/snippets/

# 入口导航页
COPY deploy/portal/                     /usr/share/nginx/portal/
# 官网
COPY fspa_web/                          /usr/share/nginx/web/
# 管理后台
COPY --from=admin-build /src/dist/       /usr/share/nginx/admin/
# H5（先跑 bash deploy/h5/build-h5.sh，产物落到 unpackage/dist/build/h5）
COPY fspa_uni/unpackage/dist/build/h5/   /usr/share/nginx/h5/

EXPOSE 80
