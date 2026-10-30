# 良物换物 · 生产部署说明（给运维）

> 全部操作在服务器上完成，**不需要本地构建**（H5 产物已编译好，随包提供）。  
> 请在服务器上执行下面 ①→⑤ 五步，中途不要跳步。

---

## 0. 包里是什么

```
fspa_deploy/
├── docker-compose.yml          # 编排：mysql + redis + 2 个后端 + 1 个 nginx
├── .env.example                # 环境变量模板（复制成 .env 再改）
├── deploy.sh                   # 一键脚本（检查环境 → 构建 → 启动）
├── .dockerignore
├── README-OPS.md               # 本文件
├── deploy/
│   ├── docker/api.Dockerfile   # 后端镜像（JDK8 + Maven 编译）
│   ├── docker/web.Dockerfile   # 前端镜像（Node 编译管理端 + nginx 托管三站）
│   ├── nginx/                  # nginx 单端口配置（按路径分流 + 反代接口）
│   ├── portal/                 # 入口导航页（访问根路径时看到的那三个卡片）
│   ├── mysql/initdb/           # 建库建表脚本，首次启动自动按序导入
│   ├── mysql/optional/         # 演示数据，**不要导入**
│   ├── h5/                     # H5 构建脚本（服务器上不用跑，仅供参考）
│   └── README.md               # 详细技术说明，排障时查
├── fspa_java/                  # 后端源码（front + admin，Maven 多模块）
├── fspa_vue/                   # 管理端源码（镜像内 npm build）
├── fspa_web/                   # 官网静态站（4 个页面）
└── fspa_uni/unpackage/dist/build/h5/   # H5 商城编译产物（已构建，直接用）
```

**包里没有的东西（故意的）**：`node_modules`、`target`、日志、`.git`、历史备份包、以及任何真实密码。

---

## 1. 端口约定（最容易被搞错的地方）

**对外只有 13999 一个端口**，三个站点靠路径区分：

| 路径         | 内容       | 后端              |
| ---------- | -------- | --------------- |
| `/`        | 入口导航页    | —               |
| `/web/`    | 品牌官网（静态） | —               |
| `/h5/`     | 用户端 H5   | `/api/front/**` |
| `/admin/`  | 运营管理后台   | `/api/admin/**` |
| `/healthz` | 健康检查     | —               |

容器内部端口（**不对外，不要改**）：

| 服务           | 容器内端口         | 接口前缀            | 谁在用        |
| ------------ | ------------- | --------------- | ---------- |
| `api-front`  | **20012**     | `/api/front/**` | 小程序、H5 商城  |
| `api-admin`  | **20010**     | `/api/admin/**` | 管理后台       |
| `web`(nginx) | 80 → 映射 13999 | —               | 入口页 + 三套前端 |

> 服务器 13999 被占用时改 `.env` 的 `HTTP_PORT` 即可，前端不用重新打包  
> （三套前端全部同源相对路径，nginx 只是换了监听端口）。



---

## 2. 服务器要求

| 项      | 要求                                                                                                                         |
| ------ | -------------------------------------------------------------------------------------------------------------------------- |
| 系统     | Linux x86_64（CentOS 7+ / Ubuntu 20.04+）                                                                                    |
| Docker | **20.10+**，需要 `docker compose` v2（`docker-compose` 1.x 不支持本编排里的 `name:` 字段）                                                |
| 内存     | **≥ 4G**（构建阶段 Maven + Node 峰值较高）；4G 以下把 `.env` 里 JVM 堆调到 768m                                                              |
| 磁盘     | ≥ 20G（Maven 依赖 + 镜像约 5G，加上数据卷）                                                                                             |
| 外网     | 需能访问 Docker Hub、`maven.aliyun.com`、`registry.npmmirror.com`；**纯离线环境要先在能上网的机器上 `docker compose build` 后 `docker save` 导镜像** |
| 端口     | **13999** 未被占用                                                                                                             |
| 字符集    | 无所谓，容器内统一 `C.UTF-8` + `Asia/Shanghai`                                                                                      |

检查命令：

```bash
docker version --format '{{.Server.Version}}'
docker compose version
free -g | head -2
df -h /var/lib/docker
ss -lntp | grep ':13999'
```

---

## 3. 执行顺序（五步）

### ① 上传并解压

```bash
mkdir -p /opt && cd /opt
# 上传 fspa_deploy_20261029.zip 到 /opt（scp / sftp 均可）
unzip fspa_deploy_20261029.zip
cd /opt/fspa_deploy
```

> 解压后 `deploy.sh` 可能没有执行权限，**统一用 `bash deploy.sh` 调用**，不依赖权限位。

### ② 配置 .env（唯一需要手改的地方）

```bash
cp .env.example .env
vi .env
```

必须改的两项：

```
MYSQL_ROOT_PASSWORD=改成强密码
REDIS_PASSWORD=改成强密码
```

脚本会拦住默认值（以 `ChangeMe` 开头就报错退出）。这三项按实际情况看：

```
HTTP_PORT=13999    # 对外唯一端口；被占用时改这里即可，前端不用重新打包
```

### ③ 构建镜像（约 8~15 分钟，取决于网络）

```bash
bash deploy.sh build
```

会依次编译：后端两个 jar（Maven，JDK8）→ 管理端（Node16 + webpack）→ nginx 镜像。  
先单独跑 build 是为了让失败停在构建阶段，不污染正在运行的环境。

### ④ 启动

```bash
bash deploy.sh up
```

首次启动 MySQL 会导入 `deploy/mysql/initdb/` 里的 10 个 SQL（按文件名顺序，顺序有依赖不能调换）。

### ⑤ 验证

```bash
bash deploy.sh ps                       # 5 个容器都应 Up，mysql/redis 为 healthy
docker compose logs --tail=100 api-front api-admin
```

接口连通性（在服务器本机执行）：

```bash
P=13999   # 改成你 .env 里的 HTTP_PORT

curl -s -o /dev/null -w 'health   %{http_code}\n' http://127.0.0.1:$P/healthz
curl -s -o /dev/null -w 'portal   %{http_code}\n' http://127.0.0.1:$P/
curl -s -o /dev/null -w 'web      %{http_code}\n' http://127.0.0.1:$P/web/
curl -s -o /dev/null -w 'h5       %{http_code}\n' http://127.0.0.1:$P/h5/
curl -s -o /dev/null -w 'h5-route %{http_code}\n' http://127.0.0.1:$P/h5/pages/index/index
curl -s -o /dev/null -w 'admin    %{http_code}\n' http://127.0.0.1:$P/admin/
curl -s -o /dev/null -w 'front    %{http_code}\n' http://127.0.0.1:$P/api/front/index
curl -s -o /dev/null -w 'adminAPI %{http_code}\n' http://127.0.0.1:$P/api/admin/getLoginPic
```

判定标准：

- `health` 返回 `ok`（200）→ nginx 活着
- `portal` / `web` / `h5` / `admin` 都是 200 → 四套前端都挂对了
- `h5-route` 也是 200 → H5 的 history 路由回落正常（返回的是 index.html，不是 404）
- `front` 返回 200 → 小程序/H5 接口通了
- `adminAPI` 返回 200 且是 JSON → 管理端接口通了
- 前 5 分钟出现 502/504 属正常：后端正在建表/连库，等 1~2 分钟再试
- 若 `h5` 是 200 但页面白屏，看浏览器控制台有没有 `/static/...` 的 404 —— 说明 H5 包不是按 `/h5/` 子路径构建的

---

## 4. 上线后必须做的三件事

1. **改初始密码**：管理后台 `admin / 123456`，商城端 `18292417675 / crmeb@123456`。
2. **配域名 + HTTPS**：在宿主机（或上层 SLB）做域名转发，证书别放进容器，方便续期。
3. **收敛 /admin/ 与 /api/admin/**：现在共用一个端口，无法靠端口隔离，建议在 nginx 加 IP 白名单（`deploy/nginx/conf.d/10-site.conf` 的 `location ^~ /admin/` 处有示例注释），或在上层代理/网关按路径限制。

另外站点 logo / 登录页背景图在后台「系统设置」里配；素材文件已在后端 jar 的 `static/images/` 下（`logo.png`、`logo-login.png`、`login-bg.jpg`、`login-banner.png`），填相对路径 `/static/images/xxx.png` 即可。

---

## 5. 日常运维命令

```bash
bash deploy.sh ps                 # 状态
bash deploy.sh logs               # 跟踪全部日志
docker compose logs -f api-front  # 只看某个服务
bash deploy.sh down               # 停止（保留数据卷）
bash deploy.sh up                 # 启动（改完 .env 端口后也用它）
bash deploy.sh reset              # 危险：down -v，连数据库一起删
```

更新版本时：替换代码目录 → `bash deploy.sh build && bash deploy.sh up`（管理端/H5 是打进 nginx 镜像的，必须重建 web 镜像才生效）。

备份：

```bash
# 数据库
docker exec fspa-mysql sh -c 'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" crmeb' \
  | gzip > /opt/backup/crmeb-$(date +%F).sql.gz
# 用户上传的图片（独立数据卷）
docker run --rm -v fspa_image-data:/data -v /opt/backup:/out alpine \
  tar czf /out/images-$(date +%F).tar.gz -C /data .
```

回滚：保留上一版目录和 `docker compose` 镜像 tag（`.env` 里 `TAG=v20261029`），出问题切回旧 tag `bash deploy.sh up` 即可，数据卷不动。

---

## 6. 排障对照表

| 现象                                    | 原因 / 处理                                                                                                                   |
| ------------------------------------- | ------------------------------------------------------------------------------------------------------------------------- |
| `api-front` 反复 Restarting             | 看 `docker compose logs api-front`。多为 MySQL 还没 ready 或密码不符；`depends_on` 已等 healthy，若仍失败检查 `.env` 密码是否含 `$`、`#` 等特殊字符（需加引号） |
| 页面能开但接口全 404                          | 反代把前缀截掉了。检查 nginx 里 `proxy_pass http://api_front;`（**结尾不能带 `/`**）                                                         |
| H5 白屏，控制台请求 `192.168.x.x`             | 说明 H5 包不是最新的。本包内产物已校验过，无内网地址；若被覆盖过，需本地重跑 `deploy/h5/build-h5.sh`                                                          |
| 后台登录 404 / 请求打到 `/api/api/...`        | 管理端接口基址拼错。镜像构建参数 `VUE_APP_BASE_API` 必须为空（本包已处理），不要手工传值                                                                    |
| 上传图片 500                              | 缺 `CRMEB_IMAGEPATH`。compose 已注入 `/app/images/`，不要把 `x-api-env` 里这行删掉                                                      |
| 上传的图片打不开（404）                         | nginx 里 `/crmebimage/`、`/image/` 必须写成 `location ^~`。写成普通前缀会被静态资源正则抢走，图片一律 404                                             |
| 访问 `/admin/` 白屏、控制台 404 `/static/...` | 管理端不是按 `/admin/` 构建的。镜像构建参数 `PUBLIC_PATH` 必须为 `/admin/`，改了要 `bash deploy.sh build web` 重建                                 |
| 访问 `/h5/` 白屏、控制台 404 `/static/...`    | 同上，H5 包必须按 `base=/h5/` 构建（`fspa_uni/manifest.json` 的 `h5.router.base`），本包产物已处理                                            |
| 刷新 `/h5/` 或 `/admin/` 内部页面 404        | history 路由回落没配。检查 `10-site.conf` 里两个 `try_files ... index.html` 是否被改过                                                     |
| 端口改了但访问不了                             | 只改 `.env` 后要 `bash deploy.sh up` 重建容器（`docker compose up -d` 才会重读端口映射）                                                    |
| 磁盘被日志占满                               | `app-logs` 卷里的 Spring 日志；`docker run --rm -v fspa_app-logs:/l alpine du -sh /l/*`                                         |
| 首次启动 mysql 报 `initdb` 相关错误            | 数据卷里已有旧库时不会重复导入。要重来必须 `bash deploy.sh reset`（会删数据）                                                                        |

需要更细的技术细节（Dockerfile 每个参数为什么这么写、SQL 导入顺序的依赖关系）看 `deploy/README.md`。
