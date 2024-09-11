---
  layout:     post
title:      "linux软件-Ansible实现数据库自动化部署"
subtitle:   " \"linux\""
date:       2024-7-26 14:22:12
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

# linux软件-Ansible实现数据库自动化部署



实验环境：

| IP              | 主机名        | 备注   |
| --------------- | ------------- | ------ |
| 192.168.171.121 | ansibleMaster | 服务端 |
| 192.168.171.122 | ansibleOne    | 客户端 |
| 192.168.171.123 | ansibleTwo    | 客户端 |

```
├── ansible.cfg
├── hosts
├── mysql_role.yml
└── roles
    └── mysql
        ├── files
        │   ├── init_mysql.sh
        │   ├── my.cnf
        │   └── mysql-8.0.26-linux-glibc2.12-x86_64.tar.xz
        └── tasks
            ├── copyFile.yml
            ├── createGroup.yml
            ├── createUser.yml
            ├── fetchFile.yml
            ├── initMysql.yml
            ├── linkfile.yml
            ├── main.yml
            └── mkdirDataFile.yml
```

mysql_role.yml

```yml
- hosts: mysql
  remote_user: root
  roles:
    - role: mysql
```

init_mysql.sh

```shell
#!/bin/bash

# 运行初始化命令并将输出重定向到临时文件
/usr/local/mysql/bin/mysqld --user=mysql --basedir=/usr/local/mysql --datadir=/usr/local/mysql/data --initialize > /tmp/mysql_init.log 2>&1

# 从临时文件中提取临时密码
TEMP_PASSWORD=$(grep -oP 'temporary password is generated for root@localhost: \K[^ ]+' /tmp/mysql_init.log)
cp -a /usr/local/mysql/support-files/mysql.server /etc/init.d/mysql
chmod +x /etc/init.d/mysql
chkconfig --add mysql
service mysql start
ln -s /usr/local/mysql/bin/mysql /usr/bin
echo "数据库初始化完成"
#生成随机密码
NEW_PASSWORD=$(openssl rand -base64 12)
#获取本机ip
LOCAL_IP=$(hostname -I | awk '{print $1}')
# 登录MySQL并修改密码
mysql -uroot -p"$TEMP_PASSWORD" -e "ALTER USER 'root'@'localhost' IDENTIFIED BY '$NEW_PASSWORD';" --connect-expired-password
# 检查命令是否成功
if [ $? -eq 0 ]; then
    # 将新密码和本机IP地址保存到文件中
    echo "IP地址: $LOCAL_IP" > /tmp/$LOCAL_IP.txt
    echo "临时密码: $TEMP_PASSWORD" >> /tmp/$LOCAL_IP.txt
    echo "新密码: $NEW_PASSWORD" >> /tmp/$LOCAL_IP.txt
else
    echo "密码修改失败"
fi
```



my.cnf

```
[mysqld]
basedir = /usr/local/mysql
datadir = /usr/local/mysql/data
socket = /usr/local/mysql/mysql.sock
character-set-server=utf8mb4
port = 3306
sql_mode=NO_ENGINE_SUBSTITUTION,STRICT_TRANS_TABLES
[client]
socket = /usr/local/mysql/mysql.sock
default-character-set=utf8mb4
```

copyFile.yml

```yml
- name: 复制tar包
  copy:
    src: mysql-8.0.26-linux-glibc2.12-x86_64.tar.xz
    dest: /opt/
- name: 解压
  shell: tar xJf /opt/mysql-8.0.26-linux-glibc2.12-x86_64.tar.xz -C /usr/local/
- name: 复制配置文件
  copy: 
    src: /etc/ansible/roles/mysql/files/my.cnf
    dest: /etc/
- name: 复制初始化文件
  copy:
    src: /etc/ansible/roles/mysql/files/init_mysql.sh
    dest: /opt/
```

createGroup.yml

```yml
- name: create group
  group: name=mysql gid=306
```

createUser.yml

```yaml
- name: create user
  user: name=mysql uid=306  group=mysql system=yes shell=/sbin/nologin

- name: 授权文件
  file:
    path: /usr/local/mysql-8.0.26-linux-glibc2.12-x86_64/
    state: directory
    recurse: yes
    owner: mysql
    group: mysql
```

fetchFile.yml

```yaml
- name: 复制密码文件到服务器端
  fetch:
    src: /tmp/{{ ansible_default_ipv4.address }}.txt
    dest: /opt/mysqlpasswd/
    flat: yes
- name: 删除变量
  shell: unset NEW_PASSWORD
- name: 删除密码文件
  file: 
    path: /tmp/{{ ansible_default_ipv4.address }}.txt
    state: absent
```

initMysql.yml

```yaml
- name: 添加执行权限1
  file: 
    path: /opt/init_mysql.sh
    mode: '0755'

- name: 添加执行权限2
  file:    
    path: /etc/my.cnf
    mode: '0755'

- name: 运行初始化脚本
  shell: sh /opt/init_mysql.sh
```

linkfile.yml

```yaml
- name: 创建符号链接
  file: 
    src: /usr/local/mysql-8.0.26-linux-glibc2.12-x86_64
    dest: /usr/local/mysql
    state: link
```

main.yml

```yaml
- include: copyFile.yml
- include: linkfile.yml
- include: mkdirDataFile.yml
- include: createGroup.yml
- include: createUser.yml
- include: initMysql.yml
- include: fetchFile.yml
```

mkdirDataFile.yml

```yaml
- name: 创建data文件夹存储文件
  file: 
    path: /usr/local/mysql/data
    state: directory
    mode: '0755'
```



实现功能：

传输mysql安装包到客户端，初始化数据库，生成随机密码修改数据库密码，擦除客户端记录。

![3507146c76be7aa2d7a11fafccabeb4](\img\springBoot\11fafccabeb4.png)
