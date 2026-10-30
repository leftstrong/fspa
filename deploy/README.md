# 生产部署说明

一套 `docker compose` 起完整环境：MySQL、Redis、前台接口、管理端接口、三套前端。

## 一、端口对照（先看这个）

**对外只开一个端口 `13999`**，四个站点按路径分流，全部由同一个 nginx 处理：

| 路径 | 内容 | 构建方式 |
|---|---|---|
| `/` | 入口导航页（三个卡片） | `deploy/portal/`，纯静态 |
| `/web/` | 品牌官网 | `fspa_web`，纯静态相对路径 |
| `/h5/` | 用户端 H5 | `bash deploy/h5/build-h5.sh`，`base=/h5/` |
| `/admin/` | 运营管理后台 | 镜像内 `npm run build:prod`，`PUBLIC_PATH=/admin/` |

容器内部端口（都不对外）：

| 服务 | 容器内端口 | 说明 |
|---|---|---|
| **crmeb-front** | **20012** | 小程序 / H5 调用的接口，路径 `/api/front/**` |
| **crmeb-admin** | **20010** | `fspa_vue` 管理后台调用的接口，路径 `/api/admin/**` |
| mysql | 3306 | 仅容器网络内可访问 |
| redis | 6379 | 仅容器网络内可访问 |
| nginx | 80 | 映射到宿主机 `${HTTP_PORT:-13999}` |

两个后端端口容易搞混，记住一条：**前台 20012、后台 20010**，差 2。
`application.yml` 里 front 写的是 20011，但 dev/prod profile 和启动参数都会覆盖成 20012，
容器启动时已经显式传了 `--server.port`，不会被外部环境变量带偏。

前端不直连后端端口，全部走 nginx 同源反代：

```
/api/front/**   → api-front:20012
/api/admin/**   → api-admin:20010
/crmebimage/**  → api-front:20012   （用户上传的图片）
```

### 子路径部署的硬约束（改代码时别踩）

三套前端挂在不同路径下，**它们各自的资源基址必须跟着路径走**，否则页面能打开但 JS/CSS 全 404（表现为白屏）：

| 站点 | 基址来源 | 改哪里 |
|---|---|---|
| 官网 | 全站相对路径（`assets/...`、`features.html`） | 不用改，放任何子目录都能用 |
| H5 | `manifest.json` → `h5.router.base = "/h5/"` | 生产构建时 uni-app 会自动把它同时用作 `publicPath` |
| 管理端 | `vue.config.js` 的 `publicPath` + router 的 `base`，都读 `PUBLIC_PATH` | 镜像构建参数 `PUBLIC_PATH`，默认 `/admin/` |

`deploy.sh` 与 `deploy/h5/build-h5.sh` 都会校验 H5 产物的 `index.html` 里是否有 `/h5/static/`，
不满足直接中断，避免把白屏包发上线。

要让管理端换路径（比如 `/console/`），改两处：`docker-compose.yml` 的 `PUBLIC_PATH` 和
`deploy/nginx/conf.d/10-site.conf` 的 location，**必须同名**。

## 二、首次部署

先把代码传到服务器（**必须包含 `fspa_uni/unpackage/dist/build/h5`**）：

```bash
# 本地执行，排除依赖和备份包，几百 MB 变几十 MB
rsync -av --exclude node_modules --exclude target --exclude '*.bak*' \
      ./fspa_pro/ user@your-server:/opt/fspa/
```

服务器上：

```bash
cd /opt/fspa
cp .env.example .env
vi .env                    # 必改 MYSQL_ROOT_PASSWORD、REDIS_PASSWORD

chmod +x deploy.sh
./deploy.sh                # 构建 + 启动（首次 8~15 分钟，Maven 要下依赖）
./deploy.sh logs           # 跟踪日志
```

> 也可以直接 `bash deploy.sh`，不依赖执行权限。

首次启动时 MySQL 会按文件名顺序自动导入 `deploy/mysql/initdb/` 下的 SQL：

```
01-schema.sql                  CRMEB 基础库（1.3.4）
02-barter-tables.sql           eb_barter_* 换物业务表
03-barter-chain.sql            换物单加环号字段
04-barter-first-hop.sql        路径费字段（依赖上一步的 chain_size）
05-barter-want-address.sql     想要 / 收货地址 / 用户城市
06-barter-message-express.sql  站内消息 + 快递字段
07-barter-reject-reason.sql    驳回原因
08-barter-menu.sql             后台「良物换物」菜单 + 系统配置
09-barter-rule-config.sql      规则配置初始值
10-barter-hide-old.sql         隐藏旧电商模块菜单
```

> 顺序不能改，`04` 依赖 `03` 加的字段。
> 演示数据没有导入，需要时手动执行 `deploy/mysql/optional/99-demo-seed.sql`。

默认账号（基础库注释里给的）：

- 管理后台：`admin` / `123456`
- 商城端：`18292417675` / `crmeb@123456`

**上线第一件事就是把这两个密码改掉。**

## 三、构建 H5 产物

`fspa_uni` 是 HBuilderX 工程，镜像里只负责托管产物，所以**部署前必须重新构建**，
产物输出到 `fspa_uni/unpackage/dist/build/h5`。

命令行一键构建（推荐，走的是 HBuilderX 自带的那套 uni-app 编译器，效果等同 GUI）：

```bash
bash deploy/h5/build-h5.sh          # 首次约 5~10 分钟（Maven 之外最慢的一步）
HBUILDERX_HOME=/d/HBuilderX/HBuilderX bash deploy/h5/build-h5.sh   # 自定义安装位置
```

也可以用 GUI：HBuilderX → 打开 `fspa_uni` → 发行 → 网站-H5。

脚本做的事：清空旧产物（避免累积上一轮的 hash 分块）→ 用 `NODE_ENV=production`
编译 → 校验没有残留内网地址 → 打印文件数与体积。构建完成后进程会自动退出
（官方入口编译完不会退出，脚本里挂了 webpack 的 done 钩子）。

`config/app.js` 已经改成 **H5 生产构建自动走同源**：

```js
HTTP_REQUEST_URL: process.env.NODE_ENV === 'production' ? window.location.origin : domain
```

也就是说 H5 打包时不用手改域名，部署到哪个域名/端口都能直接请求同源 `/api`。
`./deploy.sh` 会再检查一遍产物里是否还有 `192.168.x.x` / 占位域名，发现就拦住。

> 构建踩过的两个坑，改代码时注意：
> 1. `utils/date.js` 原来用了 `??`（空值合并），webpack 4 解析不了，已改成三元表达式。
> 2. 如果本地开着 H5 预览服务占着产物目录，清理旧产物会失败（脚本只告警不中断），
>    产物里可能残留旧分块，重构前先关掉预览。

## 四、数据持久化

| 卷 | 挂载点 | 内容 |
|---|---|---|
| `mysql-data` | /var/lib/mysql | 数据库 |
| `redis-data` | /data | Redis AOF |
| `image-data` | /app/images | 用户上传的图片（两个后端共用） |
| `app-logs` | /app/crmeb_log | 后端日志 |

备份：

```bash
docker exec fspa-mysql sh -c 'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" crmeb' > backup-$(date +%F).sql
docker run --rm -v fspa_image-data:/data -v "$PWD":/out alpine tar czf /out/images-$(date +%F).tgz -C /data .
```

## 五、常见问题

**接口容器一直重启？**
首次启动 MySQL 要导入 1.5MB 的 SQL，大约 30–90 秒，这段时间后端连不上库会退出重启，
`restart: unless-stopped` 会自动拉起，等 `docker compose ps` 里 mysql 变成 healthy 就好。

**管理后台能打开但所有接口 404？**
说明 `/api/admin/**` 没反代到 20010。检查 `deploy/nginx/snippets/api.conf`，
`proxy_pass http://api_admin;` 不能写成 `http://api_admin/`（带斜杠会截掉前缀）。

**上传图片报 500 / NPE？**
`crmeb.imagePath` 在 front 的 prod 配置里是缺的，compose 里已经用
`CRMEB_IMAGEPATH=/app/images/` 补上，不要删这行。

**改了 SQL 想重新初始化？**
初始化脚本只在数据卷为空时执行，重新初始化要 `./deploy.sh reset`（会清库）。

**服务器 13999 端口被占用？**
`.env` 里把 `HTTP_PORT` 改成别的值即可 —— 前端都是同源相对路径，换端口不用重新打包。

**要上 HTTPS？**
在宿主机或上层负载均衡终止 TLS 后转发到 13999 即可。
证书不要塞进这个 nginx 容器，方便轮换。

**管理端别裸奔公网**
三站共用一个端口后，`/admin/` 没法靠端口隔离了，建议在 `deploy/nginx/conf.d/10-site.conf`
的 `location ^~ /admin/` 上加 IP 白名单（文件里有示例注释），或只在内网/VPN 访问。

## 六、小程序端

小程序不能走同源，必须在微信后台配置已备案的 HTTPS 域名，
并把 `fspa_uni/config/app.js` 里的 `PROD_DOMAIN` 指向该域名
（nginx 已经能接收 `/api/front/**`，域名解析到这台机器即可，再套一层 HTTPS）。
