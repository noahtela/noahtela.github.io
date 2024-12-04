---
  layout:     post
title:      "linux软件-SSH"
subtitle:   " \"linux\""
date:       2024-1-7 16:28:12
author:     "yangsir"
header-img: "img/bg-material.jpg"
catalog: true
tags:
    - 笔记
    - linux学习
    - 云原生


---

> “Yeah It's on. ”


<p id = "build"></p>

# linux软件-SSH

## 一、ssh简介

SSH (Secure Shell) 是一种安全通道协议，主要用来实现字符界面的远程登录、远程命令执行、远程复制等功能。SSH 协议对通信双方的数据传输进行了加密处理，其工包括用户登录时输入的用户口令，与早期的 TELNET (远程登录)、RSH (Remote Shell，远程执行命令)、RCP (Remote File Copy，远程文件复制)等应用相比，SSH 协议提供了更好的安全性。OpenSSH 是实现 SSH 协议的开源软件项目，适用于各种 UNIX、Linux 类操作系统。

- SSH 协议默认监听端口: TCP 协议  **22**
- SSH 协议版本:V1、V2

## 二、OpenSSH配置

OpenSSH 服务器由 **openssh-clients**、**openssh-server** 等软件包提供(默认已安装)，属于典型的 c/s 结构、并已将 sshd 添加为标准的系统服务。执行 systemctl start sshd 命令即可启动 sshd 服务，包括 root 在内的大部分用户(只要拥有合法的登录 shell) 都可以远程登录系统。

- 服务名称: sshd
- 服务端主程序: /usr/sbin/sshd
- 服务端配置文件: **/etc/ssh/sshd_config daemon**
- 客户端配置文件: /etc/ssh/ssh config

### 1、服务监听选项(/etc/ssh/sshd_config)

- ```shell
  Port 22  #监听端口，建议修改为其他端口以提高在网络中的隐蔽性,大概在17行
  ```

- ```shell
  ListenAddress 0.0.0.0   #监听IP 地址，默认监听到 0.0.0.0 任意地址,大概19行
  ```

- ```shell
  protocol 2      #ssh 协议的版本选用 V2 比 V1 的安全性更好
  ```

- ```shell
  UseDNS no       #禁用 DNS 反向解可以提高服务的响应速度 大概在116行
  ```

### 2、用户登录控制(/etc/ssh/sshd_config)

sshd 服务默认允许 root 用户登录，但在nternet 中使用时是非常不安全的。普遍的做法如下:先以普通用户远程登入，进入安全 shell 环境后，根据实际需要使用 su-命令切换为root用户。

- ```shell
  LoginGraceTime 10s   #登录验证时间为 10 秒,大概在38行,根据情况设置
  ```

- ```shell
  PermitRootLogin no   #禁止 root 用户登录,大概在39行
  ```

- ```shell
  MaxAuthTries 3       # 最大重试次数为3,大概在41行
  ```

- ```shell
  PermitEmptyPasswords no   #禁止空密码用户登录,大概在65行
  ```

### 3、OpenSSH 服务访问控制

AllowUsers      仅允许用户登录
DenyUsers      仅禁止用户登录

注意:
1)AllowUsers 不要与 DenyUsers 同时使用
2)当服务器在 Internet 时，控制包含的 IP 地址时应是公司公网地址

```shell
 vim /etc/ssh/sshd config
 AllowUsers zhangsan lisi ys@192.168.171.217
```

仅允许 zhangsan lisi ys 等用户登录，其中 ys 用户只能来源于 192.168.171.217

### 4、登录验证方式

SSH 服务支持两种验证方式:

- 密码验证
- 密钥对验证

可以设置只使用其中一种方式，也可以两种方式都启用

密码验证: 对服务器中本地系统用户的登录名称、密码进行验证。这种方式使用最为简便，但从客户端角度来看，正在连接的服务器有可能被假冒。从服务器角度来看，当遭遇穷举(暴力破解)攻击时防御能力比较弱。

密钥对验证:要求提供相匹配的密钥信息才能通过验证。通常先在客户端中创建一对密钥文件(公钥/私钥)，然后将公钥文件放到服务器中的指定位置。远程登录时，系统将使用公钥私钥进行加密/解密关联验证，大大增强了远程管理的安全性。该方式不易被假冒，且可以免交互登录，在 shell 中被广泛使用。

1. **在本地机器上生成密钥对**

   打开终端，使用`ssh-keygen`命令生成新的密钥对。

   ```
   ssh-keygen -t rsa
   ```

   这将生成一对RSA密钥，默认保存在`~/.ssh/`目录下，名为`id_rsa`（私钥）和`id_rsa.pub`（公钥）。在生成密钥对的过程中，你可以选择添加一个密码，但这也意味着每次使用密钥时都需要输入密码。

2. **将公钥复制到远程服务器**

   使用`ssh-copy-id`命令将公钥复制到远程服务器。你需要替换`username`和`your.server.com`为你的用户名和服务器地址。

   ```
   ssh-copy-id username@your.server.com
   ```

   这个命令将公钥复制到远程服务器的`~/.ssh/authorized_keys`文件中。如果这个文件不存在，命令会自动创建。

3. **测试密钥对登录**

   现在尝试SSH登录到远程服务器，你应该不需要输入密码。

   ```
   ssh username@your.server.com
   ```

```shell
vim /etc/ssh/sshd_config
PasswordAuthentication yes              #启用密码验证
PubkeyAuthentication yes                #启用密钥对验证
AuthorizedKeysFile .ssh/authorized_keys #指定公钥库文件(用于保存多个客户端上传的公文本)
```

### 5、常用命令

#### (1) ssh 命令 (远程安全登录)

格式: ssh user@host (若客户机登录用户与主机用户名相同，可省去 user@)

格式: ssh user@host command

端口选项: -p 22

#### (2) scp 命令(远程安全复制)

通过 scp 命令可以利用 SSH 安全连接与远程主机相互复制文件。使用 scp命令时，除了必须指定复制源、目标以外还应指定目标主机地址登录用户、执行后根据提示输入验证密码即可。

格式 1: scp [-r] user@host:file1 file2

格式 2: scp [-r] file1 user@host;file2

注意:用户名这个部分影响了文件的权限，**第一个是复制源**

## 三、构建密钥对验证的SSH体系

密钥对验证整过细节包含四步:

### 1、首先要在 SSH 客户端以 root 用户身份创建密钥对

在 Linux 客户端中通过 ssh-keygen 命令工具为当前登录用户创建密钥对文件，可用的加密算法: ECDSA、RSA、DSA，通过-t 选项指定。

```
ssh-keygen -t ECDSA
```

密钥放在/root/.ssh中，新生成的密钥对文件中，id_ecdsa 是私钥文件，默认权限为 600，对于私钥文件必须妥善保管，不能泄露给他人。id_ecdsa.pub 是公钥文件，用来提供给 SSH 服务器。

### 2、客户端将创建的公钥文件上传至 SSH 服务端临时位置

### 3、服务端将公钥信息导入用户 root 的公钥数据库文件

在服务器中目标用户的公钥数据库位于~/.ssh 目录默认的文件名为 authorized keys，如果文
件不存在用户需要自己创建。

```shell
cat /tmp/id_ecdsa.pub >> .ssh/authorized_keys
```



### 4、客户端以 root 用户身份连接服务器端 root 用户测试



## 四、TCP Wrappers

在Linux系统中,许多网络服务针对客户端提供了访问控制机制,如Samba、BIND、HTTPD、OpenSSH 等。TCP Wrappers (TCP 封套)防护机制，是作为应用服务于网络之间的一道特殊防线，提供额外的安全保障。

### 1、TCP Wrappers 保护原理

TCP Wrappers 将 TCP 服务程序“包裹”起来，代为监听 TCP 服务程序的端口，增加了一个安全监测的过程，外来的连接请求必须先通过这层安全检测，获得许可后才能访问真正的服务程序。

![image-20240112204124094](\img\springBoot\image-20240112204124094.png)

### 2、保护机制的实现方式

方式 1: 通过 tcpd 主程序对其他服务程序进行包装

**方式 2: 由其他服务程序调用 libwrap.so.*链接库**



### 3、TCP Wrappers 保护的条件

(1) 程序必须是采用 TCP 协议的服务
(2) 服务的函数库中必须包含 libwrap.so.0(共享连接库)、大多数服务通过这种方式
可用 ldd 命令查看

```shell
ldd /usr/sbin/sshd | grep "libwrap"
```

### 4、访问控制策略的配置文件

/etc/hosts.allow #允许

/etc/hosts.deny #拒绝

![image-20240112205422534](\img\springBoot\image-20240112205422534.png)



注意：/etc/hosts.allow 文件的优先级更高，若同一 IP 地址即出现在 hosts.allow 中,也存在与 hosts.deny 中，则该 IP 地址的访问请求将被接受。

使用建议:
1、使用 hosts.allow 和 hosts.deny 两个文件 (仅允许策略)

2、使用 hosts.deny 文件和默认策略 (仅拒绝策略)



### 5、配置项及格式

`hosts.allow`文件的基本格式如下：

```
service_list : host_list [: option_list]
```

- `service_list`：指定允许访问的服务名列表。可以使用服务名（如`sshd`）、端口号（如`22`）或者通配符`ALL`来表示所有服务。
- `host_list`：指定允许访问这些服务的主机列表。可以使用IP地址、主机名、域名甚至通配符等。
- `option_list`：可选部分，用来设置额外的选项，比如日志记录级别、环境变量等。

### 示例

1. 允许来自特定IP的所有服务访问：

   ```
   ALL: 192.168.1.100
   ```

2. 只允许某些主机通过SSH连接：

   ```
   sshd: 192.168.1.100, 192.168.1.101
   ```

3. 对于特定服务，允许某个网段内的所有主机访问，并设置日志记录：

   ```
   vsftpd: 192.168.1. : LOG = connect:info
   ```

4. 使用否定模式，除了列出的主机外都允许访问（虽然通常在`hosts.deny`中使用否定更常见）：

   ```
   ALL EXCEPT 192.168.1.100
   ```

5. 设置环境变量，例如限制最大并发连接数：

   ```
   sshd: .example.com MAX-CONNECTIONS 10
   ```

   每条规则占据单独一行。如果需要多行配置，可以使用反斜杠`\`作为续行符。记得在修改了`/etc/hosts.allow`和`/etc/hosts.deny`之后检查你的更改是否按预期工作，并确保没有意外地封锁了合法用户的访问。

## 五、生产服务器配置

ssh防爆破

```shell
#!/bin/bash
# Host.deny Shell Script
# yangsir
cat /var/log/secure | awk '/Failed/{print $(NF-3)}' | sort | uniq -c | awk '{print $2 "=" $1;}' >/tmp/black_ip.txt
DEFINE=10
for i in `cat /tmp/black_ip.txt`
do
IP=`echo $i | awk -F= '{print $1}'`
NUM=`echo $i | awk -F= '{print $2}'`
if [ $NUM -gt $DEFINE ]
then
grep $IP /etc/hosts.deny > /dev/null
if [ $? -gt 0 ]
then
echo "sshd:$IP" >> /etc/hosts.deny
fi
fi
done
```

