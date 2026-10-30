# 基础镜像
FROM  openjdk:8-jre
# author
MAINTAINER Chennong
# 改变容器的时区
RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && echo 'Asia/Shanghai' >/etc/timezone
# 复制jar文件到路径
COPY /*.jar /app/Crmeb-front.jar
# 复制配置文件到路径
ADD /config/application.yml /crmeb-front/config/
ADD /config/application-prod.yml /crmeb-front/config/
ADD /config /crmeb-front/config/
# 启动服务命令
ENTRYPOINT ["java","-jar","/app/Crmeb-front.jar"]
