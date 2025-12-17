---
layout:     post
title:      "linux-搭建k8s单点master集群"
subtitle:   " \"linux\""
date:       2024-3-15 17:06:49
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

| 操作系统 | IP              | 主机名 |
| -------- | --------------- | ------ |
| centos7  | 192.168.171.151 | master |
| centos7  | 192.168.171.152 | node1  |
| centos7  | 192.168.171.154 | node2  |





# 搭建k8s单点master集群





## 一、环境准备

以下配置均在实验环境中进行，部分操作不适用于生产环境

### 1、所有主机配置禁用防火墙和selinux 

```
setenforce 0
iptables -F
systemctl stop firewalld
systemctl disable firewalld
systemctl stop NetworkManager
systemctl disable NetworkManager
sed -i '/^SELINUX=/s/enforcing/disabled/' /etc/selinux/config
```

### 2、配置主机名并绑定host



在192.168.171.151上执行

```
hostnamectl set-hostname master && bash 
```

在192.168.171.152上执行

```
hostnamectl set-hostname node1 && bash 
```

在192.168.171.154上执行

```
hostnamectl set-hostname node2 && bash 
```



```shell
[root@master ~]# cat << EOF >> /etc/hosts
> 192.168.171.151 master
> 192.168.171.152 node1
> 192.168.171.154 node2
> EOF
```



#### 命令解析

`cat << EOF：这是一个 here document 的语法，表示接下来的所有内容会被传递给 cat 命令，直到遇到 EOF 为止。`

`>> /etc/hosts：这是一个 追加重定向，表示将输出追加到 /etc/hosts 文件中，而不是覆盖现有内容。`

```shell
[root@k8s-master ~]# scp /etc/hosts 192.168.171.152:/etc/
[root@k8s-master ~]# scp /etc/hosts 192.168.171.154:/etc/
```



### 3、配置所有主机之间无密码登录（仅限实验环境中，非常危险）



```
ssh-keygen
```

把本地生成的密钥文件和私钥文件拷贝到远程主机

```
ssh-copy-id node1
ssh-copy-id node2
```

**所有主机**都要进行类似操作,即生成密钥,复制密钥到其他两台



### 4、配置时间同步

```
yum install ntpdate -y

ntpdate cn.pool.ntp.org

#把时间同步做成计划任务
[root@master ~]# crontab -e
* */1 * * * /usr/sbin/ntpdate   cn.pool.ntp.org
#重启crond服务
[root@master ~]#service crond restart

```

**每台主机**上都要进行

注意：

正常情况下生产环境中不会使用定时任务来同步时间，而是单独起一个NTP server，以提供更精确和持续的时间同步功能。

使用NTP服务的**优点**：

1. 持续时间同步

- 自动调整：NTP 服务会持续监控系统时间并自动进行微调，确保时间始终与 NTP 服务器保持一致，而不仅仅是在预定时间点进行一次性同步。

2. 精确度

- 高精度：NTP 能够以毫秒级别的精度同步时间，这对许多应用（如金融系统、分布式数据库等）至关重要。

3. 网络适应性

- 动态调整：NTP 可以根据网络延迟和变化自动调整时间同步策略，提供更稳健的时间同步解决方案。

4. 负载分配

- 多个服务器：NTP 客户端可以同时连接多个 NTP 服务器，采用最佳的时间源进行同步，增加了系统的可靠性和稳定性。

5. 健康监测

- 状态监控：NTP 服务可以持续监测时间同步状态，及时发现并报告同步问题，减少手动干预的需要。

6. 适合大规模部署

- 高效管理：在大规模服务器环境中，NTP 服务能够更高效地管理多个系统的时间同步，而手动管理定时任务则难以扩展。

7. 处理时间漂移

- 慢漂移修正：NTP 服务能够处理因温度变化、硬件故障等原因引起的时间漂移，通过慢慢调整系统时间来修正。

使用定时任务可能带来的**风险**

1. 时间延迟

- 任务未及时执行：如果系统时间不准确或出现时间漂移，可能导致定时任务未在预定时间执行。

2. 资源竞争

- 重叠执行：如果任务执行时间较长而下一个周期已到，可能导致任务重叠执行，增加系统负载，甚至引发资源冲突。

3. 依赖问题

- 依赖服务未启动：如果定时任务依赖的服务未启动或未就绪，可能导致任务执行失败。

4. 环境变化

- 环境变量未设置：定时任务运行时的环境变量可能与交互式 shell 中不同，导致任务无法正常执行。

5. 监控不足

- 缺乏日志记录：如果没有适当的日志记录，可能难以追踪任务的执行情况和故障原因。

6. 配置错误

- 语法错误：在 `crontab` 中配置的语法错误可能导致任务不执行或执行不如预期。

7. 权限问题

- 权限不足：定时任务可能需要特定权限，权限不足可能导致执行失败。

8. 系统负载

- 高负载情况下执行：在系统负载较高时执行的定时任务可能会进一步加重负载，导致性能下降。

9. 网络问题

- 依赖网络的任务：如果任务依赖网络操作，网络故障可能导致任务执行失败。

10. 安全隐患

- 未及时更新：定时任务可能执行一些未及时更新的软件或脚本，带来安全隐患。

### 5、修改机器内核参数 



```shell
swapoff -a
#关闭所有的swap空间,Kubernetes建议禁用swap，以确保Kubernetes集群中的资源调度和管理更为稳定和一致。
modprobe br_netfilter
#加载 br_netfilter 内核模块,这个模块允许iptables在桥接网络时过滤网络流量，对于Kubernetes网络组件（比如Flannel、Calico等）是必要的
cat > /etc/sysctl.d/k8s.conf <<EOF
net.bridge.bridge-nf-call-ip6tables = 1
net.bridge.bridge-nf-call-iptables = 1
#这些参数确保了通过桥接的IPv6和IPv4流量可以被iptables规则处理。
net.ipv4.ip_forward = 1
#这个参数启用IPv4的转发功能，允许系统转发传入的数据包到其他网络接口，这是设置Kubernetes集群网络的必要条件。
EOF
sysctl -p /etc/sysctl.d/k8s.conf
#这个命令加载并应用 /etc/sysctl.d/k8s.conf 中的系统配置参数。sysctl命令用于动态地修改内核运行时参数。
```

补充：

`swapoff -a`为暂时禁用交换空间，重启主机后仍会开启swap，SWAP是让你在明确的OOM和没有报错但是莫名其妙服务就不能用了之间二选一，对于集群化的服务（比如etcd）来说是挂掉一个实例和整个集群都出毛病之间二选一，一般正常人都知道该选哪个

解决方法：

```
vi /etc/fstab

把带swap的一行注释掉

最后，我们需要通过运行以下命令来禁用系统上的交换空间：

swapoff -a
```



6、开启ipvs

```
#把ipvs.modules上传到master机器的/etc/sysconfig/modules/目录下
[root@master]# chmod 755 /etc/sysconfig/modules/ipvs.modules && bash /etc/sysconfig/modules/ipvs.modules && lsmod | grep ip_vs
ip_vs_ftp              13079  0 
nf_nat                 26583  1 ip_vs_ftp
ip_vs_sed              12519  0 
ip_vs_nq               12516  0 
ip_vs_sh               12688  0 
ip_vs_dh               12688  0 


#把ipvs.modules拷贝到node1的/etc/sysconfig/modules/目录下
[root@master ~]# scp /etc/sysconfig/modules/ipvs.modules node1:/etc/sysconfig/modules/

#把ipvs.modules拷贝到node2的/etc/sysconfig/modules/目录下
[root@master ~]# scp /etc/sysconfig/modules/ipvs.modules node2:/etc/sysconfig/modules/

[root@node1]# chmod 755 /etc/sysconfig/modules/ipvs.modules && bash /etc/sysconfig/modules/ipvs.modules && lsmod | grep ip_vs
ip_vs_ftp              13079  0 
nf_nat                 26583  1 ip_vs_ftp
ip_vs_sed              12519  0 
ip_vs_nq               12516  0 
ip_vs_sh               12688  0 
ip_vs_dh               12688  0 

[root@node2]# chmod 755 /etc/sysconfig/modules/ipvs.modules && bash /etc/sysconfig/modules/ipvs.modules && lsmod | grep ip_vs
ip_vs_ftp              13079  0 
nf_nat                 26583  1 ip_vs_ftp
ip_vs_sed              12519  0 
ip_vs_nq               12516  0 
ip_vs_sh               12688  0 
ip_vs_dh               12688  0
```

附：(ipvs.modules脚本信息)

```shell
#!/bin/bash
ipvs_modules="ip_vs ip_vs_lc ip_vs_wlc ip_vs_rr ip_vs_wrr ip_vs_lblc ip_vs_lblcr ip_vs_dh ip_vs_sh ip_vs_nq ip_vs_sed ip_vs_ftp nf_conntrack"
for kernel_module in ${ipvs_modules}; do
  /sbin/modinfo -F filename ${kernel_module} > /dev/null 2>&1
  if [ $? -eq 0 ]; then  # 修正了检查是否成功的条件
    /sbin/modprobe ${kernel_module}
  fi
done
```

加载这些模块能够确保 Kubernetes 集群中的网络流量被正确处理和负载均衡，特别是在大规模集群中，使用 IPVS 相较于 iptables 模式，可以提供更高效的网络管理性能和负载分发能力。



## 二、部署docker和containerd



所有主机上分别部署 Docker 和containerd环境，因为 Kubernetes 对容器的编排需要 Docker 的支持。

配置阿里云的repo源（每台主机上都要）

```shell
#配置国内安装docker和containerd的阿里云的repo源

yum install yum-utils -y
yum-config-manager --add-repo http://mirrors.aliyun.com/docker-ce/linux/centos/docker-ce.repo
#安装依赖
yum install -y device-mapper-persistent-data lvm2 wget net-tools nfs-utils lrzsz gcc gcc-c++ make cmake libxml2-devel openssl-devel curl curl-devel unzip sudo ntp libaio-devel wget vim ncurses-devel autoconf automake zlib-devel  python-devel epel-release openssh-server socat  ipvsadm conntrack telnet ipvsadm

#安装containerd服务
yum install containerd.io-1.6.6 -y
mkdir -p /etc/containerd
containerd config default > /etc/containerd/config.toml


#修改配置文件
vim /etc/containerd/config.toml
125把SystemdCgroup = false修改成SystemdCgroup = true
61把sandbox_image = "k8s.gcr.io/pause:3.6"修改成sandbox_image="registry.aliyuncs.com/google_containers/pause:3.7"

配置 containerd 开机启动，并启动 containerd
systemctl enable containerd  --now

cat > /etc/crictl.yaml <<EOF
runtime-endpoint: unix:///run/containerd/containerd.sock
image-endpoint: unix:///run/containerd/containerd.sock
timeout: 10
debug: false
EOF

systemctl restart  containerd


#安装docker-ce服务
yum -y install docker-ce
systemctl start docker
systemctl enable docker

#配置镜像加速器
vim /etc/containerd/config.toml	
145找到config_path = ""，修改成如下目录：
config_path = "/etc/containerd/certs.d"
mkdir /etc/containerd/certs.d/docker.io/ -p
vim /etc/containerd/certs.d/docker.io/hosts.toml
#写入如下内容：
[host."https://vh3bm52y.mirror.aliyuncs.com",host."https://registry.docker-cn.com"]
  capabilities = ["pull"]
重启containerd：
systemctl restart containerd

配置docker镜像加速器，k8s所有节点均按照以下配置
vim /etc/docker/daemon.json
写入如下内容：
{
"registry-mirrors":["https://vh3bm52y.mirror.aliyuncs.com","https://registry.docker-cn.com","https://docker.mirrors.ustc.edu.cn","https://dockerhub.azk8s.cn","http://hub-mirror.c.163.com"]
} 

重启docker：
systemctl daemon-reload
systemctl restart docker
```



## 三、部署kubernetes集群



### 1、组件介绍

所有节点都需要安装下面三个组件

kubeadm：安装工具，使所有的组件都会以容器的方式运行

kubectl：客户端连接K8S API工具

kubelet：运行在node节点，用来启动容器的工具



### 2、配置yum源（各个节点）

```
cat >  /etc/yum.repos.d/kubernetes.repo <<EOF
[kubernetes]
name=Kubernetes
baseurl=https://mirrors.aliyun.com/kubernetes/yum/repos/kubernetes-el7-x86_64/
enabled=1
gpgcheck=0
EOF

```



### 3、安装kubelet kubeadm kubectl（各个节点）

```
yum install -y kubelet-1.25.0 kubeadm-1.25.0 kubectl-1.25.0
systemctl enable kubelet
```

注意：kubelet 刚安装完成后，通过 systemctl start kubelet 方式是无法启动的，需要加入节点或初始化为 master 后才可启动成功。

如果在命令执行过程中出现索引 gpg 检查失败的情况, 请使用 yum install -y --nogpgcheck kubelet kubeadm kubectl 来安装。



## 四、初始化kubeadm初始化k8s集群



### 1、设置容器运行时（各个节点）

```
crictl config runtime-endpoint unix:///run/containerd/containerd.sock
```



### 2、使用kubeadm初始化k8s集群（只在master节点）

```
kubeadm config print init-defaults > kubeadm.yaml
```

修改配置文件

```shell
vim kubeadm.yaml

apiVersion: kubeadm.k8s.io/v1beta3
bootstrapTokens:
- groups:
  - system:bootstrappers:kubeadm:default-node-token
  token: abcdef.0123456789abcdef
  ttl: 24h0m0s
  usages:
  - signing
  - authentication
kind: InitConfiguration
#localAPIEndpoint  #前面加注释
#advertiseAddress   #前面加注释
#bindPort         #前面加注释
nodeRegistration:
  criSocket: unix:///run/containerd/containerd.sock  #指定containerd容器运行时
  imagePullPolicy: IfNotPresent
  #name: node  #前面加注释
apiServer:
  timeoutForControlPlane: 4m0s
apiVersion: kubeadm.k8s.io/v1beta3
certificatesDir: /etc/kubernetes/pki
clusterName: kubernetes
controllerManager: {}
dns: {}
etcd:
  local:
    dataDir: /var/lib/etcd
imageRepository: registry.cn-hangzhou.aliyuncs.com/google_containers 
#指定阿里云镜像仓库
kind: ClusterConfiguration
kubernetesVersion: 1.25.0
#新增加如下内容：
controlPlaneEndpoint: 192.168.200.199:16443
networking:
  dnsDomain: cluster.local
  podSubnet: 10.244.0.0/16 #指定pod网段
  serviceSubnet: 10.96.0.0/12
scheduler: {}
#追加如下内容
---
apiVersion: kubeproxy.config.k8s.io/v1alpha1
kind: KubeProxyConfiguration
mode: ipvs
---
apiVersion: kubelet.config.k8s.io/v1beta1
kind: KubeletConfiguration
cgroupDriver: systemd
```



基于kubeadm.yaml初始化k8s集群

各个节点上传`k8s_1.25.0.tar.gz`并导入镜像

```
ctr -n=k8s.io images import k8s_1.25.0.tar.gz
```

![image-20240315162845569](\img\springBoot\image-20240315162845569.png)

开始初始化

```
kubeadm init --config=kubeadm.yaml --ignore-preflight-errors=SystemVerification
```

显示如下，说明安装完成：

![image-20240315163158352](\img\springBoot\image-20240315163158352.png)

在master上执行

```
mkdir -p $HOME/.kube
sudo cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
sudo chown $(id -u):$(id -g) $HOME/.kube/config
```

![image-20240315163342684](\img\springBoot\image-20240315163342684.png)



Kubeadm 通过初始化安装是不包括网络插件的，也就是说初始化之后是不具备相关网络功能的，比如 k8s-master 节点上查看节点信息都是“Not Ready”状态、Pod 的 CoreDNS无法提供服务等



### 3、扩容k8s集群-添加工作节点

在master节点上查看加入节点的token

```
kubeadm token create --print-join-command
```



![image-20240315163631136](\img\springBoot\image-20240315163631136.png)



把node1加入k8s集群(在node1和node2上运行，token和密码都换成自己的)

```

kubeadm join 192.168.171.151:6443 --token 18qd6k.z0tdbambn1alldkp \
--discovery-token-ca-cert-hash \
sha256:368af44a5e55161bd72ab3dc82d59b95fd3f7e5b8fb6ce5c61d347420ee19009  --ignore-preflight-errors=SystemVerification 
```

![image-20240315164744304](\img\springBoot\image-20240315164744304.png)

![image-20240315164828607](\img\springBoot\image-20240315164828607.png)

加入成功！

### 4、为工作节点打上标签



```
[root@master ~]# kubectl label nodes node1 node-role.kubernetes.io/work=work
[root@master ~]# kubectl label nodes node2 node-role.kubernetes.io/work=work
```

![image-20240315165032293](\img\springBoot\image-20240315165032293.png)





### 5、安装kubernetes网络组件-Calico解决网络问题



把安装calico需要的镜像calico.tar.gz传到master和node1和node2节点，手动解压：



```
ctr -n=k8s.io images import calico.tar.gz
```

![image-20240315165354255](\img\springBoot\image-20240315165354255.png)

上传calico.yaml到master上，使用yaml文件安装calico 网络插件

```
kubectl apply -f calico.yaml
```

**注：在线下载配置文件地址是： https://docs.projectcalico.org/manifests/calico.yaml**



查看创建进度

```
kubectl get pods -n kube-system
```

![image-20240315165821859](\img\springBoot\image-20240315165821859.png)

calico的STATUS状态是Ready，说明k8s集群正常运行了



![image-20240315170009587](\img\springBoot\image-20240315170009587.png)
