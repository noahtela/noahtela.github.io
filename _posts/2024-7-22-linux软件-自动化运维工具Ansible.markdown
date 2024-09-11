---
  layout:     post
title:      "linux软件-自动化运维工具Ansible"
subtitle:   " \"linux\""
date:       2024-7-22 16:41:12
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

# linux软件-自动化运维工具Ansible



## 特性

- 模块化:调用特定的模块，完成特定任务
- 有Paramiko，PyYAML，Jinja2(模板语言)三个关键模块
- 支持自定义模块
- 基于Python语言实现
- 部署简单，基于python和SSH(默认已安装)，agentless
- 安全，基于OpensSH
- 支持playbook编排任务
- 幂等性:一个任务执行1遍和执行n遍效果一样，不因重复执行带来意外情况
- 无需代理不依赖PKI(无需ssI)
- 可使用任何编程语言写模块
- YAML格式，编排任务，支持丰富的数据结构
- 较强大的多层解决方案

![image-20240722161553409](\img\springBoot\image-20240722161553409.png)



## 工作原理

ansible通过ssh实现配置管理、应用部署、任务执行等功能，建议配置ansible端能基于密钥认证的方式联系各被管理节点

## 主要组成部分

- Ansible-playbook(剧本)执行过程
  - 将已有编排好的任务集写入Ansible-Playbook
  - 通过ansible-playbook命令分拆任务集至逐条ansible命令，按预定规则逐条执行
- Ansible主要操作对象
  - HOSTS主机
  - NETWORKING网络设备
- 注意事项
  - 执行ansible的主机一般称为主控端，中控，master或堡垒机
  - 主控端Python版本需要2.6或以上
  - 被控端Python版本小于2.4需要安装python-simplejson
  - 被控端如开启SELinux需要安装libselinux-python
  - windows不能做为主控端

## 配置文件

- /etc/ansible/ansible.cfg主配置文件，配置ansible工作特性
- /etc/ansible/hosts 主机清单
- /etc/ansible/roles/存放角色的目录

## 程序

- /usr/bin/ansible 主程序，临时命令执行工具
- /usr/bin/ansible-doc 查看配置文档，模块功能查看工具
- /usr/bin/ansible-galaxy下载/上传优秀代码或Roles模块的官网平台
- /usr/bin/ansible-playbook定制自动化任务,编排剧本工具
- /usr/bin/ansible-pull 远程执行命令的工具
- /usr/bin/ansible-vault 文件加密工具
- /usr/bin/ansible-console 基于console界面与用户交互的执行工具

## Ansible使用和模块化

### Ansible主配置文件

![image-20240722170158421](\img\springBoot\image-20240722170158421.png)

```shell
[defaults]
#inventory = /etc/ansible/hosts #主机列表配置文件
#library = /usr/share/my_modules/ #库文件存放目录
#remote_tmp =$HOME/.ansible/tmp #临时py命令文件存放在远程主机目录
#local_tmp=$HOME/.ansible/tmp #本机的临时命令执行目录
#forks=5 #默认并发数
#sudo_user= root # 默认sudo 用户
#ask_sudo_pass = True #每次执行ansible命令是否询问ssh密码
#ask_pass =True
#remote_port =22
#host_key_checking = False # 检査对应服务器的host_key，建议取消注释
#log_path=/var/log/ansible.log #日志文件
```



### 基本用法

```shell
ansible <host-pattern> [-m module_name] [-a args]
	--version 显示版本
	-m module 指定模块，默认为command
	-v 详细过程 -vv -vv更详细
	--list-hosts 显示主机列表，可简写-list
	-k,--ask-pass 提示输入ssh连接密码，默认Key验证
	-K,--ask-become-pass 提示输入sudo时的口令
	-c,--check 检查，并不执行
	-T,--timeout=TIMEOUT 执行命令的超时时间，默认10s
	-u,--user=REMOTE USER 执行远程执行的用户
	-b，--become 代替日版的sudo 切换
```



## ansible的Host-pattern

逻辑与

```shell
ansible “websrvs:&dbsrvs" -m ping
```

在websrvs组并且在dbsrvs组中的主机

逻辑非

```shell
ansible 'websrvs:!dbsrvs' -m ping
```

在websrvs组，但不在dbsrvs组中的主机

注意:此处为单引号
综合逻辑

```shell
ansible 'websrvs:dbsrvs:&appsrvs:!ftpsrvs' -m ping
```

正则表达式

```shell
ansible "websrvs:&dbsrvs" -m ping
ansible "~(webdb).*.magedul.com" -m ping
```



## ansible命令执行过程

执行过程

1. 加载自己的配置文件 默认/etc/ansible/ansible.cfg
2. 加载自己对应的模块文件，如command
3. 通过ansible将模块或命令生成对应的临时py文件，并将该文件传输至远程服务器的对应执行用户$HOME/.ansible/tmp/ansible-tmp-数字/XXX.PY文件
4. 给文件+x执行
5. 执行并返回结果
6. 删除临时py文件，sheep o退出

执行状态

- 绿色:执行成功并且不需要做改变的操作
- 黄色:执行成功并且对目标主机做变更
- 红色:执行失败

## ansible常见模块

### Command：在远程主机执行命令，默认模块，可忽略-m选项

![image-20240724143743852](\img\springBoot\image-20240724143743852.png)

- ansible srvs -m command -a 'service vsftpd start
- ansible srvs -m command -a 'echo magedu |passwd --stdin wang’不成功
- 此命令不支持 SVARNAME< > | ; &等，用shell模块实现

### Shell:和command相似，用shell执行命令

- ansible srv -m shell -a 'echo magedu | passwd -stdin wang' 
- 调用bash执行命令 类似 cat /tmp/stanley.md | awk -F' | ' '{print $1,$2}'&>/tmp/example.txt 这些复杂命令，即使使用shell也可能会失败，解决办法:写到脚本时，copy到远程，执行，再把需要的结果拉回执行命令的机器

### Script:运行脚本

- -a"/PATH/TO/SCRIPT_FILE"
- snsible websrvs -m script -a f1.sh

### copy:拷贝文件

- src参数    ：用于指定需要copy的文件或目录

- dest参数  ：用于指定文件将被拷贝到远程主机的哪个目录中，dest为必须参数

- content参数  ：当不使用src指定拷贝的文件时，可以使用content直接指定文件内容，src与content两个参数必有其一，否则会报错。

- force参数  :  当远程主机的目标路径中已经存在同名文件，并且与ansible主机中的文件内容不同时，是否强制覆盖，可选值有yes和

  no，默认值为yes，表示覆盖，如果设置为no，则不会执行覆盖拷贝操作，远程主机中的文件保持不变。

- backup参数 :  当远程主机的目标路径中已经存在同名文件，并且与ansible主机中的文件内容不同时，是否对远程主机的文件进行备

  份，可选值有yes和no，当设置为yes时，会先备份远程主机中的文件，然后再将ansible主机中的文件拷贝到远程主机。

- owner参数 : 指定文件拷贝到远程主机后的属主，但是远程主机上必须有对应的用户，否则会报错。

- group参数 : 指定文件拷贝到远程主机后的属组，但是远程主机上必须有对应的组，否则会报错。

- mode参数 : 指定文件拷贝到远程主机后的权限，如果你想将权限设置为"rw-r--r--"，则可以使用mode=0644表示，如果你想要在user对应的权限位上添加执行权限，则可以使用mode=u+x表示。

例：

```shell
ansible test70 -m copy -a "src=/testdir/copytest dest=/opt/"
```



### Fetch:从客户端取文件至服务器端，copy相反，目录可先tar

例：

```shell
ansible srv -m fetch -a 'src=/root/a.sh dest=/data/scripts'
```



### file:文件管理

- path参数 ：必须参数，用于指定要操作的文件或目录，在之前版本的ansible中，使用dest参数或者name参数指定要操作的文件或目录，为了兼容之前的版本，使用dest或name也可以。
- state参数 ：此参数非常灵活，此参数对应的值需要根据情况设定，比如，当我们需要在远程主机中创建一个目录的时候，我们需要使用path参数指定对应的目录路径，假设，我想要在远程主机上创建/testdir/a/b目录，那么我则需要设置path=/testdir/a/b，但是，我们无法从"/testdir/a/b"这个路径看出b是一个文件还是一个目录，ansible也同样无法单单从一个字符串就知道你要创建文件还是目录，所以，我们需要通过state参数进行说明，当我们想要创建的/testdir/a/b是一个目录时，需要将state的值设置directory，"directory"为目录之意，当它与path结合，ansible就能知道我们要操作的目标是一个目录，同理，当我们想要操作的/testdir/a/b是一个文件时，则需要将state的值设置为touch，当我们想要创建软链接文件时，需将state设置为link，想要创建硬链接文件时，需要将state设置为hard，当我们想要删除一个文件时（删除时不用区分目标是文件、目录、还是链接），则需要将state的值设置为absent，"absent"为缺席之意，当我们想让操作的目标"缺席"时，就表示我们想要删除目标。
- src参数 ：当state设置为link或者hard时，表示我们想要创建一个软链或者硬链，所以，我们必须指明软链或硬链链接的哪个文件，通过src参数即可指定链接源。
  force参数  :  当state=link的时候，可配合此参数强制创建链接文件，当force=yes时，表示强制创建链接文件，不过强制创建链接文件分为两种情况，情况一：当你要创建的链接文件指向的源文件并不存在时，使用此参数，可以先强制创建出链接文件。情况二：当你要创建链接文件的目录中已经存在与链接文件同名的文件时，将force设置为yes，回将同名文件覆盖为链接文件，相当于删除同名文件，创建链接文件。情况三：当你要创建链接文件的目录中已经存在与链接文件同名的文件，并且链接文件指向的源文件也不存在，这时会强制替换同名文件为链接文件。
- owner参数 ：用于指定被操作文件的属主，属主对应的用户必须在远程主机中存在，否则会报错。
- group参数 ：用于指定被操作文件的属组，属组对应的组必须在远程主机中存在，否则会报错。
- mode参数：用于指定被操作文件的权限，比如，如果想要将文件权限设置为"rw-r-x---"，则可以使用mode=650进行设置，或者使用mode=0650，效果也是相同的，如果你想要设置特殊权限，比如为二进制文件设置suid，则可以使用mode=4700，很方便吧。
- recurse参数：当要操作的文件为目录，将recurse设置为yes，可以递归的修改目录中文件的属性。
  

![image-20240723104246093](\img\springBoot\image-20240723104246093.png)



例：

- 在test70主机上创建一个名为testfile的文件，如果testfile文件已经存在，则会更新文件的时间戳，与touch命令的作用相同。

```shell
ansible test70 -m file -a "path=/testdir/testfile state=touch"
```

- 在test70主机上创建一个名为testdir的目录，如果testdir目录已经存在，则不进行任何操作。

```shell
ansible test70 -m file -a "path=/testdir/testdir state=directory"
```

- 在test70上为testfile文件创建软链接文件，软链接名为linkfile，执行下面命令的时候，testfile已经存在。

```shell
ansible test70 -m file -a "path=/testdir/linkfile state=link src=/testdir/testfile"
```

- 在test70上为testfile文件创建硬链接文件，硬链接名为hardfile，执行下面命令的时候，testfile已经存在。

```shell
ansible test70 -m file -a "path=/testdir/hardfile state=hard src=/testdir/testfile"
```

- 在创建链接文件时，如果源文件不存在，或者链接文件与其他文件同名时，强制覆盖同名文件或者创建链接文件，参考上述force参数的解释。

```shell
ansible test70 -m file -a "path=/testdir/linkfile state=link src=sourcefile force=yes"
```

- 删除远程机器上的指定文件或目录

```shell
ansible test70 -m file -a "path=/testdir/testdir state=absent"
```

- 在创建文件或目录的时候指定属主，或者修改远程主机上的文件或目录的属主。

```shell
ansible test70 -m file -a "path=/testdir/abc state=touch owner=zsy"
ansible test70 -m file -a "path=/testdir/abc owner=zsy"
ansible test70 -m file -a "path=/testdir/abc state=directory owner=zsy"
```

- 在创建文件或目录的时候指定属组，或者修改远程主机上的文件或目录的属组。

```shell
ansible test70 -m file -a "path=/testdir/abb state=touch group=zsy"
ansible test70 -m file -a "path=/testdir/abb group=zsy"
ansible test70 -m file -a "path=/testdir/abb state=directory group=zsy"
```

- 在创建文件或目录的时候指定权限，或者修改远程主机上的文件或目录的权限。

```shell
ansible test70 -m file -a "path=/testdir/abb state=touch mode=0644"
ansible test70 -m file -a "path=/testdir/abb mode=0644"
ansible test70 -m file -a "path=/testdir/binfile mode=4700"ansible test70 -m file -a "path=/testdir/abb state=directory mode=0644"
```

- 当操作远程主机中的目录时，同时递归的将目录中的文件的属主属组都设置为zsy。

```shell
ansible test70 -m file -a "path=/testdir/abd state=directory owner=zsy group=zsy recurse=yes"
```



### Hostname:管理主机名

例：

```shell
ansible node1 -m hostname -a "name=websry"
```



### cron:计划任务

支持时间:minute，hour，day，month，weekday

分时日月周

```shell
ansible srv -m cron -a "minute=*/5 job='/usr/sbin/ntpdate 172.16.0.1 &> /dev/null' name=Synctime" #创建任务
ansible srv -m cron -a 'state=absent name=Synctime' #删除任务
```



### service:管理服务

```shell
ansible srv -m service -a 'name=httpd state=stopped
ansible srv -m service -a 'name=httpd state=started
ansible srv -m service -a 'name=httpd state=reloaded
ansible srv -m service -a 'name=httpd state=restarted
```



### user:管理用户

```shell
ansible srv -m user -a 'name=user1 comment="test user" uid=2048 home=/app/user1 group=root'
ansible srv -m user -a 'name=sysuser1 system=yes home=/app/sysuser1'
ansible srv -m user -a 'name=user1 state=absent remove=yes' #删除用户及家目录等数据
```

## ansible-vault

- 功能:管理加密解密yml文件
- ansible-vault [create decrypt edit encrypt|rekey|view]>
- ansible-vault encrypt hello.yml         加密
- ansible-vault decrypt hello.yml         解密
- ansible-vault view hello.yml              查看
- ansible-vault edit hello.yml               编辑加密文件
- ansible-vault rekey hello.yml            修改囗令
- ansible-vault create new.yml            创建新文件



## ansible-galaxy

- 作用：连接 https://galaxy.ansible.com下载相应的roles

- 列出所有已安装的galaxy

  ```shell
  ansible-galaxy list
  ```

- 安装galaxy

  ```
  ansible-galaxy install geerlingguy.redis
  ```

- 删除galaxy

  ```shell
  ansible-galaxy remove geerlingguy.redis
  ```

## ansible-playbook

- playbook是由一个或多个"play"组成的列表
- play的主要功能在于将事先归并为一组的主机装扮成事先通过ansible中的task定义好的角色。从根本上来讲，所谓task无非是调用ansible的一个module。将多个play组织在一个playbook中，即可以让它们联同起来按事先编排的机制同唱一台大戏
- Playbook采用YAML语言编写

### 核心元素

- Hosts 执行的远程主机列表

- Tasks 任务集

- Varniables 内置变量或自定义变量在playbook中调用

- Templates 模板，可替换模板文件中的变量并实现一些简单逻辑的文件

- Handlers 和notity结合使用，由特定条件触发的操作，满足条件方才执行，否则不执行

- taqs 标签 指定某条任务执行，用于选择运行playbook中的部分代码。ansible具有幂等性，因此会自动跳过没有变化的部分，即便如此，有些代码为测试其确实没有发生变化的时间依然会非常地长。此时，如果确信其没有变化，就可以通过tags跳过此些代码片断

  ```shell
  ansible-playbook-t tagsname useradd.yml
  ```

### handlers和notify结合使用触发条件

- handlers

  是task列表，这些task与前述的task并没有本质上的不同,用于当关注的资源发生变化时，才会采取一定的操作

- Notify此action可用于在每个play的最后被触发，这样可避免多次有改变发生时每次都执行指定的操作，仅在所有的变化发生完成后一次性地执行指定操作。在notify中列出的操作称为handler，也即notify中调用handler中定义的操作

```yaml
- hosts: websrvs
  remote_user: root
  tasks:
    - name: Install httpd
      yum: name=httpd state=present
    - name: Install configure file
      copy: src=files/httpd.conf dest=/etc/httpd/conf/
      notify: restart httpd
    - name: ensure apache is running
      service:name=httpd state=started enabled=yes
  handlers:
    - name:restart httpd
     service:name=httpd status=restarted
```

## playbook中变量使用

- 变量名:仅能由字母、数字和下划线组成，且**只能以字母开头**

- 变量来源:

  - ansible setup facts 远程主机的所有变量都可直接调用

  - 在/etc/ansible/hosts中定义

    普通变量：主机组中主机单独定义，优先级高于公共变量

    公共(组)变量：针对主机组中所有主机定义统一变量

  - 通过命令行指定变量，优先级最高

    ```
    ansible-plavbook-e varname=value
    ```

  - 在playbook中定义

    ```yaml
    vars:
      -var1: value1
      -var2: value2
    ```

  - 在role中定义

## 模板templates

- 文本文件，嵌套有脚本(使用模板编程语言编写)
- Jinja2语言，使用字面量，有下面形式
- 字符串:使用单引号或双引号
- 数字:整数，浮点数
- 列表:[item1, item2,...]
- 元组 :(item1, item2,...)
- 字典:{key1:value1, key2:value2,...}
- 布尔型 :true/false
- 算术运算 :+,-,*,/,//,%,**
- 比较操作 :==，!=，>，>=，<,<=
- 逻辑运算:and,or, not
- 流表达式:For If When

## 选代:with items

迭代: 当有需要重复性执行的任务时，可以使用迭代机制

- 对迭代项的引用，固定变量名为 'item'
- 要在task中使用with_items给定要迭代的元素列表
- 列表格式:
  - 字符串
  - 字典

示例:迭代嵌套子变量

```yaml
- hosts : websrvs
  remote_user: root
  tasks:
    - name: add some groups
      group:name={{ item }} state=present
      with_items:
       - group1
       - group2
       - group3
    - name: add some users
      user: name={{ item.name }} group={{ item.group }} state=present
      with_items:
        - { name: 'userl', group: 'groupl'}
        - { name: 'user2', group: 'group2'}
        - { name: 'user3', group: 'group3'}
```





## roles

- roles

  ansilbe自1.2版本引入的新特性，用于层次性、结构化地组织playbook。roles能够根据层次型结构自动装载变量文件、tasks以及handlers等。要使用roles只需要在playbook中使用include指令即可。简单来讲，roles就是通过分别将变量文件、任务、模板及处理器放置于单独的目录中，并可以便捷地include它们的一种机制。角色一般用于基于主机构建服务的场景中，但也可以是用于构建守护进程等场景中

- 复杂场景:建议使用roles，代码复用度高

  - 变更指定主机或主机组
  - 如命名不规范维护和传承成本大
  - 某些功能需多个Playbook，通过Includes即可实现

目录结构

```
playbook.yml
roles/
	project/ 
		tasks/       定义task,role的基本元素，至少应该包含一个名为main.yml的文件;其它的文件需要在此文件中通过include进行包含
		files/       存放由copy或script模块等调用的文件
		vars/   	 定义变量，至少应该包含一个名为main:ym!的文件;其它的文件需要在此文件中通过include进行包含，不常用
		default/ 	 设定默认变量时使用此目录中的main.yml文件，不常用
		templates/    template模块查找所需要模板文件的目录
		handlers/    至少应该包含一个名为main.yml的文件;其它的文件需要在此文件中通过include进行包含
		meta/		定义当前角色的特殊设定及其依赖关系,至少应该包含一个名为main.yml的文件，其它文件需在此文件中通过include进行包含，不常用
```

