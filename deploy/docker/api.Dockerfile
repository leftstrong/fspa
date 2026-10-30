# =============================================================================
# 良物换物 · 后端镜像（crmeb-front / crmeb-admin 共用，靠 ARG 区分）
#
#   crmeb-front → 小程序 / H5 接口，容器内 20012
#   crmeb-admin → 管理后台接口，  容器内 20010
#
# 关键点：
#   1) 必须 JDK8（Spring Boot 2.2.6）
#   2) 必须 -Pprod 打包：application.yml 里的 spring.profiles.active 是
#      @profiles.active@，靠 maven 资源过滤替换，而默认 profile 是 dev
#   3) prod 配置里数据库地址写死在 10.0.4.6:33306，容器里全部由环境变量覆盖
#
# 构建上下文是仓库根目录，所以源码都在 fspa_java/ 前缀下
# =============================================================================

# ARG 必须声明在第一个 FROM 之前，运行阶段才能拿到同一个值；
# 否则 admin 会 COPY 到 front 的 jar，两个端口就对调了
ARG MODULE=crmeb-front
ARG JAR_NAME=Crmeb-front.jar

# ------------------------------ 构建阶段 ------------------------------
FROM maven:3.8.8-eclipse-temurin-8 AS build

ARG MODULE
ARG JAR_NAME

ENV MAVEN_OPTS="-Xmx1024m"

# 国内构建走阿里云镜像；海外服务器可删掉这行用官方源
RUN mkdir -p /root/.m2 && printf '%s' \
    '<settings><mirrors><mirror><id>aliyun</id><mirrorOf>central</mirrorOf><name>aliyun</name><url>https://maven.aliyun.com/repository/central</url></mirror></mirrors></settings>' \
    > /root/.m2/settings.xml

WORKDIR /build

# 先只拷 pom，让依赖层能被缓存复用
COPY fspa_java/pom.xml ./
COPY fspa_java/crmeb-common/pom.xml  crmeb-common/
COPY fspa_java/crmeb-service/pom.xml crmeb-service/
COPY fspa_java/crmeb-admin/pom.xml   crmeb-admin/
COPY fspa_java/crmeb-front/pom.xml   crmeb-front/
RUN mvn -B -q -Pprod dependency:go-offline || true

COPY fspa_java/ ./
RUN mvn -B -Pprod -DskipTests -pl "${MODULE}" -am clean package \
    && ls -l "${MODULE}/target/${JAR_NAME}"

# ------------------------------ 运行阶段 ------------------------------
FROM eclipse-temurin:8-jre

ARG MODULE
ARG JAR_NAME

ENV TZ=Asia/Shanghai \
    LANG=C.UTF-8 \
    JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError"

RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone \
    && mkdir -p /app/images /app/crmeb_log

WORKDIR /app

COPY --from=build /build/${MODULE}/target/${JAR_NAME} /app/app.jar

# 20012 = front，20010 = admin
EXPOSE 20012 20010

# SERVER_PORT / SPRING_PROFILES_ACTIVE 由 compose 注入：
#   显式传 --server.port 是为了绕开外部环境变量被 relaxed-binding 当成 server.port 的坑
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai -jar /app/app.jar --spring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod} --server.port=${SERVER_PORT:-20012}"]
