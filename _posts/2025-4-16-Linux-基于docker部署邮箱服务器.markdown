---
layout:     post
title:      "Linux-基于Docker部署邮件服务器"
subtitle:   " \"Linux\""
date:       2025-4-16 11:28:12
author:     "yangsir"
header-img: "img/bg-material.jpg"
catalog: true
tags:
    - 笔记
    - Linux


---

> “邮箱服务器”


<p id = "build"></p>

# 基于Docker部署邮件服务器

本教程中使用的镜像：`iredmail/mariadb:stable`

镜像网盘地址：`https://www.123865.com/s/gIBcVv-8uvO3?提取码:bkJ1`



## 一、部署Docker

部署docker请移步我之前的笔记或自行百度，这里不做赘述



## 二、Docker 部署 iRedMail 准备

由于目前社区中有使用 Docker 部署 iRedMail 的尝试（例如 GitHub 上的一些项目），这里采用常见的基于 Docker Compose 的方式进行部署。

### 1.创建工作目录及数据卷目录

建议在宿主机上为 iRedMail 相关数据单独建立目录，以便持久化存储数据、配置和日志：

```shell
mkdir -p /data/iredmail/{conf,data,logs}
chown -R $(whoami):$(whoami) /data/iredmail
```

### 2.编辑配置文件

```shell
# 进入工作目录（例如 /data/iredmail/conf）
cd /data/iredmail/conf

# 如果文件不存在则创建文件
vi iredmail-docker.conf

# 服务器的主机名，建议设置为 mail.yourhost.com
HOSTNAME=mail.yourhost.com

# 邮件服务所管理的首个邮件域名，此处必须设置！
FIRST_MAIL_DOMAIN=yourhost.com

# 为 postmaster@xvaier.top 邮箱设置一个初始密码（请使用足够复杂的密码）
FIRST_MAIL_DOMAIN_ADMIN_PASSWORD=YourStrongPasswordHere

# 用于 mlmmjadmin API 的令牌（可以使用 OpenSSL 随机生成）
MLMMJADMIN_API_TOKEN=$(openssl rand -base64 32)

# 用于 Roundcube 会话加密的密钥（同样建议使用 OpenSSL 生成）
ROUNDCUBE_DES_KEY=$(openssl rand -base64 24)
```

### 3.编写 docker-compose.yml 文件

在工作目录中创建 `docker-compose.yml` 文件，内容示例如下：

```yaml
version: "3"
services:
  iredmail:
    # 此处请使用社区镜像；注意确认镜像版本是否更新且安全
    image: iredmail/docker-iredmail:stable
    container_name: iredmail
    hostname: mail.yourhost.com #换成你自己的域名
    env_file:
      - /data/iredmail/conf/iredmail-docker.conf
    environment:
      - HOSTNAME=mail.yourhost.com  #换成你自己的域名
      - DOMAIN=yourhost.com #换成你自己的域名
      - TZ=Asia/Shanghai
      # 根据实际需要，可增加其它环境变量参数，如管理员邮箱密码等
    ports:
      - "80:80"       # Web管理界面、登录页面
      - "443:443"     # HTTPS 接口（SSL）
      - "25:25"       # SMTP 普通端口
      - "587:587"     # SMTP 提交（STARTTLS）
      - "993:993"     # IMAP SSL
      - "995:995"     # POP3 SSL（如需要）
    volumes:
      - /data/iredmail/conf:/etc/iredmail
      - /data/iredmail/data:/var/lib/iredmail
      - /data/iredmail/logs:/var/log/iredmail
    restart: always
```

**说明：**

- **hostname**：推荐设置为 `mail.yourhost.com`，这样容器内部程序能获取正确的邮件服务器标识；
- **environment**：根据镜像要求设置必要的参数，如时区（TZ）、域名等；同时可添加管理员密码配置，具体参数可参考该 Docker 镜像的说明文档；
- **ports**：映射了 Web（80/443）、SMTP（25/587）以及 IMAP/POP3 等常用端口，根据实际需求调整；
- **volumes**：将容器内的配置、数据和日志目录映射到宿主机，实现数据持久化。

## 三、启动容器

在 `docker-compose.yml` 文件所在目录下执行：

```shell
docker-compose up -d
```



这会后台启动 iRedMail 容器。首次运行时，容器内将自动执行安装脚本（包括数据库初始化、邮件服务配置等），请注意容器日志中可能的提示信息，根据提示设置管理员账号等密码信息。

可通过下面命令查看容器日志：

```shell
docker logs -f iredmail
```

**注意:第一次启动非常慢**



## 四、邮件服务后续配置

### 1、访问 Web 管理界面

过浏览器访问 `http://mail.yourhost.com` 或 `https://mail.yourhost.com`，根据镜像内预置的管理页面，进一步完成邮件系统的初始化配置与账号管理。

### 2、SSL/TLS 证书配置

如果需要支持生产环境的 HTTPS 和加密传输，建议使用 Let’s Encrypt 申请免费证书。方法有两种：

- 在宿主机上使用工具（如 certbot），将证书挂载到容器中；
- 部署反向代理（如 Nginx 或 Traefik）在前端处理 HTTPS，再将流量代理到 iRedMail 容器。

### 3、防火墙及安全加固

开放必要的防火墙端口，例如：

```shell
firewall-cmd --permanent --add-port=25/tcp
firewall-cmd --permanent --add-port=80/tcp
firewall-cmd --permanent --add-port=443/tcp
firewall-cmd --permanent --add-port=587/tcp
firewall-cmd --permanent --add-port=993/tcp
firewall-cmd --permanent --add-port=995/tcp
firewall-cmd --reload
```



## 五、代码验证

```python
import smtplib
import ssl
from email.mime.text import MIMEText
from email.header import Header

def send_test_email(smtp_host, smtp_port, sender_email, sender_password, recipient_email):
    try:
        # 创建邮件内容
        message = MIMEText('这是一封测试邮件，用于验证iRedMail服务器的发送功能。', 'plain', 'utf-8')
        message['From'] = Header(sender_email)
        message['To'] = Header(recipient_email)
        message['Subject'] = Header('iRedMail测试邮件')

        # 创建SSL上下文，禁用证书验证
        context = ssl.create_default_context()
        context.check_hostname = False
        context.verify_mode = ssl.CERT_NONE

        # 连接到SMTP服务器
        smtp = smtplib.SMTP(smtp_host, smtp_port)
        smtp.starttls(context=context)  # 启用TLS加密，使用自定义SSL上下文
        smtp.login(sender_email, sender_password)

        # 发送邮件
        smtp.sendmail(sender_email, recipient_email, message.as_string())
        print('邮件发送成功！')

        # 关闭连接
        smtp.quit()

    except Exception as e:
        print(f'发送邮件时出错: {str(e)}')

if __name__ == '__main__':
    # 配置邮件服务器信息
    SMTP_HOST = 'mail.yourhost.com'  # 替换为你的iRedMail服务器域名
    SMTP_PORT = 587  # 默认TLS端口
    SENDER_EMAIL = 'postmaster@yourhost.com'  # 替换为你的发件人邮箱
    SENDER_PASSWORD = 'yourpasswd'  # 替换为你的邮箱密码
    RECIPIENT_EMAIL = '12345@qq.com'  # 替换为收件人邮箱

    # 发送测试邮件
    send_test_email(SMTP_HOST, SMTP_PORT, SENDER_EMAIL, SENDER_PASSWORD, RECIPIENT_EMAIL)
```

![image-20250416115305396](\img\linux\image-20250416115305396.png)

![image-20250416115356114](\img\linux\image-20250416115356114.png)

![image-20250416115519126](\img\linux\image-20250416115519126.png)

![image-20250416115645870](\img\linux\image-20250416115645870.png)
