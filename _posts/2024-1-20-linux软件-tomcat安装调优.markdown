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



## 五、安装mysql

```mysql
mysql -uroot -p12345678
create database tomcat;    #创建tomcat数据库
use tomcat;
create table tt(id int,name varchar(128)); #创建tt测试表
insert into tt values (1,"come on boy"),(2,"come on girl");  #插入数据
grant all on tomcat.* to tomcat@'192.168.1.%' identified by 'tomcat'; #授权
flush privileges;  #刷新权限
```

### 建立测试页面

```shell
# vim /usr/local/tomcat/webapps/ROOT/mysql.jsp

<%@ page contentType="text/html;charset=utf-8"%>
<%@ page import="java.sql.*"%>
<html>
<body>
<%
Class.forName("org.gjt.mm.mysql.Driver").newInstance();
String url ="jdbc:mysql://192.168.1.12/tomcat?user=tomcat&password=tomcat&useUnicode=true&characterEncoding=utf-8";
Connection conn= DriverManager.getConnection(url);
Statement stmt=conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,ResultSet.CONCUR_UPDATABLE);
String sql="select * from tt";
ResultSet rs=stmt.executeQuery(sql);
while(rs.next()){%>
step:<%=rs.getString(1)%>
context:<%=rs.getString(2)%><br><br>
<%}%>
<%out.print("Congratulations!!! JSP connect MYSQL IS OK!!");%>
<%rs.close();
stmt.close();
conn.close();
%> 
</body>
</html>
```

### 测试

 http://192.168.171.153:8080/mysql.jsp

测试成功！！！！



## 六、Tomcat优化

### 1、隐藏版本信息

#### 1）隐藏HTTP头部的版本信息

```shell
 vim /usr/local/tomcat/conf/server.xml
 #为Connector 添加 server 属性
 <Connector port="8080" protocol="HTTP/1.1"
               connectionTimeout="20000"
               redirectPort="8443" server="APP Srv1.0"/>
```



重启tomcat服务



#### 2） 隐藏404页面出现的版本号

修改前



 针对该信息的显示是由一个jar包控制的，该jar包存放在 Tomcat 安装目录下的lib目录下，名称为 catalina.jar。

 我们可以通过 jar xf 或unzip命令解压这个 jar 包会得到两个目录 META-INF 和 org ,通过修改org/apache/catalina/util/ServerInfo.properties 文件中的 serverinfo 字段来实现来更改我们tomcat的版本信息。

```shell
cd /usr/local/tomcat/lib/

#解压catalina.jar包
unzip catalina.jar
#修改ServerInfo.properties文件
 #进入org/apache/catalina/util 编辑配置文件ServerInfo.properties
cd org/apache/catalina/util
vim ServerInfo.properties
```



```shell
# 将修改后的信息压缩回jar包
cd /usr/local/tomcat/lib/
jar uvf catalina.jar org/apache/catalina/util/ServerInfo.properties

#删除解压目录
rm -rf META-INF/  org/
#重启tomcat
systemctl restart tomcat

```

优化后：



### 2、开启NIO2



 NIO是Java 1.4 及后续版本提供的一种新的I/O操作方式，是一个基于缓冲区、并能提供非阻塞I/O操作的Java API，利用java异步IO技术使Tomcat运行性能有所提升，可以通过少量的线程处理大量的请求。它拥有比传统I/O操作(BIO)更好的并发运行性能。tomcat 8版本及以上默认就是在NIO模式下允许。

 Java NIO 可以让你非阻塞的使用IO，例如：当线程从通道读取数据到缓冲区时，线程还是可以进行其他事情。当数据被写入到缓冲区时，线程可以继续处理它。从缓冲区写入通道也类似。**Tomcat8在Linux系统中默认使用这种方式。**

修改配置文件

```shell
# vim /usr/local/tomcat/conf/server.xml 

改：
     <Connector port="8080" protocol="HTTP/1.1"
                connectionTimeout="20000"
                redirectPort="8443" />
为：
     <Connector port="8080" protocol="org.apache.coyote.http11.Http11Nio2Protocol"
                connectionTimeout="20000"
                redirectPort="8443" server="APP Srv1.0"/>

```



### 3、Tomcat 执行器（线程池）的优化

 Tomcat 默认是没有启用线程池的，在 Tomcat 中每一个用户请求都是一个线程，所以我们可以使用线程池来提高性能。

 使用线程池，用较少的线程处理较多的访问，可以提高tomcat处理请求的能力。

```shell
# 开启线程池
# vim /usr/local/tomcat/conf/server.xml

 <!--
   <Executor name="tomcatThreadPool" namePrefix="catalina-exec-"
       maxThreads="150" minSpareThreads="4"/>
    -->
# 给这两行去掉注释，并修改为
<Executor name="tomcatThreadPool" namePrefix="catalina-exec-"
        maxThreads="900" minSpareThreads="100" maxSpareThreads="500"
        prestartminSpareThreads="true" maxQueueSize="300" />

```

参数说明：

- name：共享线程池的名字。这是 Connector 为了共享线程池要引用的名字，该名字必须唯一。

- namePrefix:在 JVM 上，每个运行线程都可以有一个 name 字符串。这一属性为线程池中每个线程的 name 字符串设置了一个前缀，Tomcat 将把线程号追加到这一前缀的后面。

- maxThreads：最大并发数，默认设置 200，一般建议在 500 ~1000，根据硬件设施和业务来判断
-  minSpareThreads：最小空闲线程数，Tomcat初始化时创建的线程数，默认设置25
- maxSpareThreads：最大空闲线程数，一旦空闲线程超过这个值，Tomcat就会关闭不再需要的线程。
-  prestartminSpareThreads在Tomcat初始化的时候就初始化minSpareThreads 的参数值，如果不等于 true，
- minSpareThreads的值就没啥效果了
-  maxQueueSize：最大的等待队列数，超过则拒绝请求

```shell
# 开启并使用线程池
# 在connector中设置executor [ɪɡˈzekjətər]属性指向上面的执行器
<Connector executor="tomcatThreadPool" port="8080" protocol="org.apache.coyote.http11.Http11AprProtocol"
               connectionTimeout="20000"
               redirectPort="8443" server="APP Srv1.0"/>

```



### 4、连接器（Connector）优化



Connector是连接器，负责接收客户的请求，以及向客户端回送响应的消息。所以 Connector 的优化是重要部分。默认情况下Tomcat支持200线程访问，超过这个数量的连接将被等待甚至超时放弃，所以我们需要提高这方面的处理能力

```shell
<Connector executor="tomcatThreadPool" port="8080" protocol="org.apache.coyote.http11.Http11AprProtocol"
               connectionTimeout="20000"
               redirectPort="8443" server="APP Srv1.0"/>
```



拓展必要选项

```shell
<Connector executor="tomcatThreadPool" port="8080" 
               protocol="org.apache.coyote.http11.Http11Nio2Protocol"
               connectionTimeout="20000"
               redirectPort="8443" server="APP Srv1.0"
               maxThreads="1000"
               minSpareThreads="100"
               acceptCount="1000"
               maxConnections="1000"
               maxHttpHeaderSize="8192"
               tcpNoDelay="true"
               compression="on"
               disableUploadTimeout="true"
               enableLookups="false"
               URIEncoding="UTF-8"/>
```

参数说明：

- maxThreads:最大线程数。即最多同时处理的连接数，Tomcat使用线程来处理接收的每个请求。这个值表示Tomcat可创建的最大的线程数。如果没有指定，该属性被设置为200。如果使用了executor将忽略此连接器的该属性，连接器将使用executor。
- minSpareThreads:最小空闲线程数。
- acceptCount:接受最大队列长度，当队列满时收到的任何请求将被拒绝。
- maxConnections:在任何给定的时间服务器接受并处理的最大连接数。
- connectionTimeout：超时等待时间（毫秒）
- maxHttpHeaderSize:请求头最大值
- tcpNoDelay:如果为true，服务器socket会设置TCP_NO_DELAY选项，在大多数情况下可以提高性能。缺省情况下设为true
- compression：是否开启压缩GZIP 。可接受的参数的值是“off ”（禁用压缩），“on ”（允许压缩，这会导致文本数据被压缩），“force ”（强制在所有的情况下压缩）。提示：压缩会增加Tomcat负担，最好采用Nginx + Tomcat 或者 Apache + Tomcat 方式，压缩交由Nginx/Apache 去做。
- disableUploadTimeout：此标志允许servlet容器在数据上传时使用不同的连接超时，通常较长。如果没有指定，该属性被设置为true，禁用上传超时。
- enableLookups：关闭DNS反向查询，DNS反查很耗时间

### 5、禁用AJP连接器



 AJP端口用来与应用服务器交互时候用，比如apache连接tomcat等，由于 Tomcat 服务器相对于 Nginx 服务器在处理静态资源上效率较低。因此我们的网站服务器一般是 Nginx+Tomcat，Nginx 负责处理静态资源，因此 AJP 协议我们在使用 Nginx+Tomcat 架构时可以禁止掉。

```shell
# vim /usr/local/tomcat/conf/server.xml

修改：
   <Connector port="8009" protocol="AJP/1.3" redirectPort="8443" />
为：
   <!-- <Connector port="8009" protocol="AJP/1.3" redirectPort="8443" /> -->
```



### 6、禁用8005端口

 SHUTDOWN端口是写在server参数里面的，一般在安全设置时候建议把端口修改为其他端口，SHUTDOWN修改为其他复杂的字符串。

 实际上这个端口是可以直接屏蔽不监听的。设置时候将其port值修改为-1就可以。

```shell
# vim /usr/local/tomcat/conf/server.xml
修改：
<Server port="8005" shutdown="SHUTDOWN">
为：
<Server port="-1" shutdown="SHUTDOWN">
```



### 7、JVM 参数优化

```shell
# vim /usr/local/tomcat/bin/catalina.sh
# 在110行添加如下内容
export JAVA_OPTS="-server -Xms1024M -Xmx1024M -Xmn512M -Xss512k -XX:PermSize=256M -XX:MaxPermSize=512M -XX:NewRatio=2 -XX:SurvivorRatio=4 -XX:MaxTenuringThreshold=10 -XX:+DisableExplicitGC -XX:+UseParNewGC -XX:+UseConcMarkSweepGC -XX:ParallelGCThreads=20 -XX:+CMSParallelRemarkEnabled -XX:+UseCMSCompactAtFullCollection -XX:LargePageSizeInBytes=128m -XX:+UseFastAccessorMethods -XX:+UseCMSInitiatingOccupancyOnly -Djava.awt.headless=true"
```

