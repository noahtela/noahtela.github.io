---

layout:     post
title:      "linux练习-keepalived高可用集群(二)"
subtitle:   " \"linux\""
date:       2024-1-25 17:25:49
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

# keepalived高可用集群(二)



前面（一）只是一个简单的配置，只能在主机宕掉之后，keepalived才会切换，而不能对应到某个服务或某些服务，这节将优化配置文件，达到更好的可用性。

​	

## 一、keepalived单播

在阿里云中配置keepalived时，各服务器之间不允许广播或者组播，这里就用到了单播

```shell
# vim /etc/keepalived/keepalived.conf 


global_defs {
       router_id haweb_1
       }
vrrp_sync_group VGM {
       group {
       VI_HA
       }
}
vrrp_instance VI_HA {
      state MASTER
      interface ens33
      lvs_sync_daemon_inteface ens33
      virtual_router_id 51
      priority 100
      advert_int 5
      authentication {
          auth_type PASS
          auth_pass zhangbin
          }
      unicast_src_ip 192.168.206.132 #源地址
      unicast_peer {
      192.168.206.135  #目标地址 
      }
      virtual_ipaddress {
          192.168.206.138/24 dev ens33 label ens33:0 #vip虚拟地址
          }
}

```



## 二、针对某一服务的keepalived(防脑裂)



这里以nginx为例

```shell
# vim /etc/keepalived/nginxstatus.sh 


#!/bin/bash
counter=$(ps -C nginx --no-heading|wc -l)
if [ "${counter}" = "0" ]; then
/usr/local/nginx/sbin/nginx
#/usr/bin/systemctl restart nginx.service
sleep 2
counter=$(ps -C nginx --no-heading|wc -l)
if [ "${counter}" = "0" ]; then
ps -ef |grep keepalived | grep  -v grep | awk '{print $2}' | xargs kill -9
#/usr/bin/systemctl stop keepalived.service
fi
fi
```

```shell
chmod +x /etc/keepalived/nginxstatus.sh
```

然后修改keepalived配置文件



```shell
vi /etc/keepalived/keepalived.conf 


! Configuration File for keepalived

global_defs {
   router_id LVS_001B #router_id 这要个唯一
}


vrrp_instance VI_1 {
    state MASTER
    interface ens33
    virtual_router_id 51 #这个virtual_router_id 在两台机器上要相同
    priority 100
    unicast_src_ip  192.168.3.55  #本地IP地址  
    unicast_peer {  
                  192.168.3.56    #对端IP地址,此地址一定不能忘记  
                       }
    advert_int 1
    authentication {
        auth_type PASS
        auth_pass zhangbin
    }
    virtual_ipaddress {
        192.168.3.58/24 dev ens33 label ens33:0
    }
}

virtual_server 192.168.3.58 80 {
    delay_loop 2  #每隔2秒 检测real_server状态
    lb_algo rr    #定义lvs调度算法
    lb_kind DR    #定义lvs工作模式
    persistence_timeout 60  #定义持久链接时长
    protocol TCP   #定义集群的协议
    real_server 192.168.3.55 80 {
        weight 1
        notify_down /etc/keepalived/nginxstatus.sh  #检查服务器失败后要执行的脚本，与notify_up相对
        TCP_CHECK {
            connect_port 80    #监控检查的端口
            connect_timeout 3  #连接超时时间
            nb_get_retry 2     #重连次数
            delay_before_retry 1    #重连间隔
        }
    }
}
```

当主服务器上的nginx出现问题死掉后，脚本会杀死keepalived进程，然后服务转向从服务器
