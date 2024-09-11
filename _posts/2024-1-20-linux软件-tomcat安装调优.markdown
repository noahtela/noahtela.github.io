---

  layout:     post
title:      "linux软件-tomcat安装调优"
subtitle:   " \"linux\""
date:       2024-1-15 17:18:49
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

# linux软件-tomcat安装调优



部署环境：

 MySQL-Connector-Java：         mysql-connector-java-5.1.47

 Tomcat：                                    apache-tomcat-8.5.42

 JDK：                                         jdk-8u171-linux-x64

 MySQL：                                    mysql-5.7.26



## 一、安装JDK



### 1、卸载openjdk

 安装之前需要查看下系统是否安装了openjdk，如果安装了openjdk，请先卸载，否则安装不了oracle官方的jdk

```shell
yum remove java-* -y
```

### 2、下载（或上传）所需安装包



### 3、解压JDK

```shell
tar -zxvf jdk-8u171-linux-x64.tar.gz -C /usr/local/
```

```shell
ln -s /usr/local/jdk1.8.0_171 /usr/local/jdk
```



### 4、配置JDK环境变量



```shell
vim /etc/profile #在文件最后加入一下行

 JAVA_HOME=/usr/local/jdk

 PATH=$JAVA_HOME/bin:$PATH

 CLASSPATH=$JAVA_HOME/jre/lib/ext:$JAVA_HOME/lib/tools.jar

 export PATH JAVA_HOME CLASSPATH
```

```shell
source /etc/profile #重载环境变量
```

### 5、查看java环境变量

```shell
java -version
```





## 二、安装Tomcat

### 1、解压软件包

```shell
 tar -zxvf apache-tomcat-8.5.42.tar.gz -C /usr/local/
```



### 2、做连接

```shell
ln -s /usr/local/apache-tomcat-8.5.42 /usr/local/tomcat
```



### 3、Tomcat个目录文件用途

   |---bin：存放启动和关闭tomcat执行脚本；

   |---conf ：存放Tomcat服务器的各种全局配置文件，其中最重要的是server.xml和web.xml；

   |---lib： 存放Tomcat运行需要的库文件（jar），包含Tomcat使用的jar文件。unix平台此目录下的任何文件都被加到Tomcat的classpath中；

   |---logs：存放Tomcat执行时的LOG文件；

   |---webapps：Tomcat的主要Web发布目录，默认情况下把Web应用文件放于此目录，即供外界访问的web资源的存放目录；

   |--- webapps/ROOT：tomcat的家目录

   |--- webapps/ROOT/ index.jsp：Tomcat的默认首页文件

   |---work：存放jsp编译后产生的class文件或servlet文件存放

   |---temp：存放Tomcat运行时所产生的临时文件

### 4、Tomcat启动脚本

```shell
vim /etc/init.d/tomcat


#!/bin/bash
# Tomcat init script for Linux
# chkconfig: 2345 96 14
# discription: The Apache Tomcat Server/JSP container
JAVA_HOME=/usr/local/jdk/
CATALINA_HOME=/usr/local/tomcat
start_tomcat=$CATALINA_HOME/bin/startup.sh       #tomcat启动文件
stop_tomcat=$CATALINA_HOME/bin/shutdown.sh     #tomcat关闭文件

start() {
        echo -n "Starting tomcat: "
        ${start_tomcat}
        echo "tomcat start ok."
}
stop() {
        echo -n "Shutting down tomcat: "
        ${stop_tomcat}
        echo "tomcat stop ok."
}

# See how we were called

case "$1" in
  start)
        start
        ;;
  stop)
        stop
        ;;
  restart)
        stop
        sleep 5
        start
        ;;
  *)
        echo "Usage: $0 {start|stop|restart}"
esac
exit 0



chmod +x /etc/init.d/tomcat  #添加执行权限
```





### 5、建立系统服务文件

```shell
vim /lib/systemd/system/tomcat.service

[Unit]
Description=tomcat
After=network.target

[Service]
Type=forking
Environment=JAVA_HOME=/usr/local/jdk1.8.0_171/
Environment=CATALINA_HOME=/usr/local/tomcat
ExecStart=/etc/init.d/tomcat start
ExecStop=/etc/init.d/tomcat stop
ExecRestart=/etc/init.d/tomcat restart
PrivateTmp=true

[Install]
WantedBy=multi-user.target



systemctl daemon-reload  #重载service文件
```



### 6、启动并测试访问

```shell
systemctl start tomcat
```



浏览器访问  http://192.168.171.155:8080/







### 7、点击Server Status



 拒绝访问，因为这个页面需要我们配置一个账号密码，来访问。

 注：如果你需要查看 Tomcat 的运行状态可以配置tomcat管理员账户，然后登陆 Tomcat 后台进行查看。



#### 1）修改tomcat-users.xml配置文件

```
vim /usr/local/tomcat/conf/tomcat-users.xml
```

在 `<tomcat-users>  </tomcat-users>`标签内添加

```shell
<role rolename="admin-gui"/>
 <role rolename="admin-script"/>
 <role rolename="manager-gui"/>
 <role rolename="manager-script"/>
 <role rolename="manager-jmx"/>
 <role rolename="manager-status"/>
 <user username="admin" password="123456" roles="manager-gui,manager-script,manager-jmx,manager-status,admin-script,admin-gui"/>
```

 **角色说明：**

1：“manager-gui”：Allows access to the html interface（允许通过web的方式登录查看服务器信息）。

2：“manager-script”: Allows access to the plain text interface（允许以纯文本的方式访问）。

3：“manager-jmx”: Allows access to the JMX proxy interface（允许jmx的代理访问）。

4：“manager-status”: Allows access to the read-only status pages（允许以只读状态访问）。

5: admin-gui: 允许访问HTML GUI

6 : admin-script: 允许访问文本接口



#### 2)tomcat8以上还要增加以下配置

```shell
vim /usr/local/tomcat/conf/Catalina/localhost/manager.xml

<Context privileged="true" antiResourceLocking="false"
         docBase="${catalina.home}/webapps/manager">
    <Valve className="org.apache.catalina.valves.RemoteAddrValve" allow="^.*$" />
</Context>
```

```shell
vim /usr/local/tomcat/webapps/host-manager/META-INF/context.xml
#修改红框位置
```





#### 3）重启tomcat，测试连接



**我们注意到当前配置的jvm大小非常小，那么接下来，调大jvm**

```shell
vim /usr/local/tomcat/bin/catalina.sh
#在最前面（注释除外）添加
JAVA_OPTS='-Xms512m -Xmx1024m'
```

 -Xms：表示java虚拟机堆内存初始内存分配的大小，虚拟机在启动时向系统申请的内存的大小，-Xmx表示最大可分配给jvm的内存大小，根据自己需要修改。一般建议堆的最大值设置为可用内存的最大值的80%。



## 三、搭建基于域名的虚拟主机

```shell
 ls /usr/local/tomcat/conf/
```



server.xml是Tomcat的主配置文件（全局）,服务器设置的，例如端口设置，路径设置。



### 1、修改server.xml配置文件

```shell
 vim /usr/local/tomcat/conf/server.xml
```

在末尾`</Engine>`上面添加内容

```shell
 <Host name="www.test.com"  appBase="/www/html">
         <Context path="" docBase="/www/html/web1" />
      </Host>
      <Host name="map.test.com"  appBase="/www/html">
         <Context path="" docBase="/www/html/web2" />
      </Host>
```

(此处省略DNS解析过程)



参数说明：

-  name指定虚拟主机的名称，那么使用对应的ip将无法访问，如果需要使用 ip 来访问，需要把 host 的name属性改成ip即可。
- appBase指定应用程序(网站)的根目录,这里可以存放多个程序(网站),一般是相对路径,相对于tomcat的安装目录。
- Context path=""为虚拟目录，如果是空，表示直接就是/，如果是如path="aa",那么访问的时候就是site:8080/aa
- docBase="……" 为实际目录，可以是绝对路径，如果是相对路径就是基于appBase



### 2、创建测试网页

```shell
mkdir -p /www/html/{web1,web2}
echo "web1" > /www/html/web1/index.html
echo "web2" > /www/html/web2/index.html
```

再次提醒 此处省略DNS解析配置



### 3、重启tomcat测试连接

```shell
systemctl restart tomcat
```



测试成功！！！



## 四、安装tomcat-Native

Tomcat 可以使用 apr 来提供更好的伸缩性、性能和集成到本地服务器技术。用来提高 tomcat 的性能。 tomcat native 在具体的运行平台上，提供了一种优化技术，它本身是基于 ARP（Apache Portable（轻便） Runtime）技术，我们应用了 tomcat native 技术之后，tomcat 在跟操作系统级别的交互方面可以做得更好，并且它更像apache 一样，可以更好地作为一台 web server。 tomcat 可以利用 apache 的 apr 接口，使用操作系统的部分本地操作，从而提升性能APR 提升的是静态页面处理能力.

 **Tomcat8.5 在bin下已有tomcat-native.tar.gz，我们不需要去下载**



### 一、安装依赖

```shell
yum install -y apr apr-devel gcc gcc-c++ openssl-devel openssl
```

#### 1、解压压缩包

```shell
cd /usr/local/tomcat/bin/
tar zxf tomcat-native.tar.gz -C /usr/local/src/
```

#### 2、预编译

```shell
cd /usr/local/src/tomcat-native-1.2.21-src/native/

./configure --with-apr=/usr/bin/apr-1-config --with-java-home=/usr/local/jdk1.8.0_171/ --with-ssl
```

#### 3、编译安装

```shell
make && make install
```

#### 4、添加库文件

```shell
vim /etc/ld.so.conf
```

#### 5、使配置文件生效

```shell
 ldconfig
 
 echo "ldconfig" >>/etc/rc.local  #添加开机生效
 
 chmod +x /etc/rc.d/rc.local
 
  # 其实添加完ldconfig并无法立即引用类库变量，我们可以做软连接解决：
  ln -s /usr/local/apr/lib/*  /usr/lib/
```

#### 6、重启tomcat

```shell
systemctl restart tomcat
```



#### 7、看日志是否支持native



```shell
 cat /usr/local/tomcat/logs/catalina.out | grep Native
```
