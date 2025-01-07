---
layout:     post
title:      "k8s-kubeadm快速搭建k8s(1.30版本)"
subtitle:   " \"kubeadm\""
date:       2025-1-7 11:37:12
author:     "yangsir"
header-img: "img/bg-material.jpg"
catalog: true
tags:
    - 笔记
    - Linux
    - Prometheus


---

> “Yeah It's on. ”


<p id = "build"></p>



# kubeadm快速搭建k8s(1.30版本)



实验环境

| 操作系统 | 配置 | IP             | 主机名 |
| -------- | ---- | -------------- | ------ |
| centos7  | 2H2G | 192.168.13.132 | master |
| centos7  | 2H2G | 192.168.13.133 | node1  |
| centos7  | 2H2G | 192.168.13.134 | node2  |



本实验涉及的所有配置文件、软件包、镜像：



## 一、环境准备

以下配置均在实验环境中进行，部分操作不适用于生产环境

### 1、所有主机配置禁用防火墙和selinux

```shell
setenforce 0
iptables -F
systemctl stop firewalld
systemctl disable firewalld
systemctl stop NetworkManager
systemctl disable NetworkManager
sed -i '/^SELINUX=/s/enforcing/disabled/' /etc/selinux/config
```

### 2、配置主机名并绑定host

在192.168.13.132上执行

```
hostnamectl set-hostname master && bash 
```

在192.168.13.133上执行

```
hostnamectl set-hostname node1 && bash 
```

在192.168.13.134上执行

```
hostnamectl set-hostname node2 && bash
```

三台上执行

```
cat << EOF >> /etc/hosts
192.168.13.132 master
192.168.13.133 node1
192.168.13.134 node2
EOF
```





### 3、配置时间同步(生产环境建议单独设置时间服务器)

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



### 4、修改机器内核参数(三台都要)

```shell
vi /etc/fstab
把带swap的一行注释掉
#关闭所有的swap空间,Kubernetes建议禁用swap，以确保Kubernetes集群中的资源调度和管理更为稳定和一致。

#回到命令行
modprobe br_netfilter
#加载 br_netfilter 内核模块,这个模块允许iptables在桥接网络时过滤网络流量，对于Kubernetes网络组件（比如Flannel、Calico等）是必要的
cat > /etc/sysctl.d/k8s.conf <<EOF
net.bridge.bridge-nf-call-ip6tables = 1
#不使用ipv6可以不设置
net.bridge.bridge-nf-call-iptables = 1
#这些参数确保了通过桥接的IPv6和IPv4流量可以被iptables规则处理。
net.ipv4.ip_forward = 1
#这个参数启用IPv4的转发功能，允许系统转发传入的数据包到其他网络接口，这是设置Kubernetes集群网络的必要条件。
EOF
sysctl -p /etc/sysctl.d/k8s.conf
#这个命令加载并应用 /etc/sysctl.d/k8s.conf 中的系统配置参数。sysctl命令用于动态地修改内核运行时参数。
```





### 5、开启ipvs(三台都要)

```shell
#vi /etc/sysconfig/modules/ipvs.modules

ipvs_modules="ip_vs ip_vs_lc ip_vs_wlc ip_vs_rr ip_vs_wrr ip_vs_lblc ip_vs_lblcr ip_vs_dh ip_vs_sh ip_vs_nq ip_vs_sed ip_vs_ftp nf_conntrack"
for kernel_module in ${ipvs_modules}; do
  /sbin/modinfo -F filename ${kernel_module} > /dev/null 2>&1
  if [ $? -eq 0 ]; then  
    /sbin/modprobe ${kernel_module}
  fi
done
#保存退出

chmod 755 /etc/sysconfig/modules/ipvs.modules && bash /etc/sysconfig/modules/ipvs.modules && lsmod | grep ip_vs
```

加载这些模块能够确保 Kubernetes 集群中的网络流量被正确处理和负载均衡，特别是在大规模集群中，使用 IPVS 相较于 iptables 模式，可以提供更高效的网络管理性能和负载分发能力。



## 二、安装容器进行时(containerd)



### 1、安装 containerd

将`containerd-1.6.35-linux-amd64.tar`上传至虚拟机，并将其解压到`/usr/local`

```shell
tar Cxzvf /usr/local containerd-1.6.35-linux-amd64.tar
bin/
bin/containerd-shim-runc-v2
bin/containerd-shim
bin/ctr
bin/containerd-shim-runc-v1
bin/containerd
bin/containerd-stress
```

```shell
systemctl daemon-reload
systemctl enable --now containerd
```



### 2、安装 runc

将`runc.amd64`上传至`/opt`下，并将其安装为`/usr/local/sbin/runc`

```shell
install -m 755 /opt/runc.amd64 /usr/local/sbin/runc
```

### 3、安装 CNI 插件

将`cni-plugins-linux-amd64-v1.6.1.tgz`上传至`/opt`下，然后将其解压到`/opt/cni/bin`：

```shell
mkdir -p /opt/cni/bin
tar Cxzvf /opt/cni/bin cni-plugins-linux-amd64-v1.6.1.tgz
./
./macvlan
./static
./vlan
./portmap
./host-local
./vrf
./bridge
./tuning
./firewall
./host-device
./sbr
./loopback
./dhcp
./ptp
./ipvlan
./bandwidth
```

### 4、配置containerd

```shell
containerd config default > /etc/containerd/config.toml

vi /etc/containerd/config.toml

#修改沙箱镜像地址
63行把sandbox_image = "k8s.gcr.io/pause:3.6"修改成sandbox_image="registry.aliyuncs.com/google_containers/pause:3.7"

127行把SystemdCgroup = false修改成SystemdCgroup = true


147行找到config_path = ""，修改成如下目录：
config_path = "/etc/containerd/certs.d"

mkdir /etc/containerd/certs.d/docker.io/ -p
vim /etc/containerd/certs.d/docker.io/hosts.toml
#写入如下内容：
#链接是阿里云的镜像加速服务地址，可以替换成自己的
[host."https://vh3bm52y.mirror.aliyuncs.com"]
  capabilities = ["pull"]

重启containerd：
systemctl restart containerd
```





## 三、安装kubelet kubeadm kubectl

### 1、组件介绍

所有节点都需要安装下面三个组件

kubeadm：安装工具，使所有的组件都会以容器的方式运行

kubectl：客户端连接K8S API工具

kubelet：运行在node节点，用来启动容器的工具

### 2、配置repo

```shell
# 此操作会覆盖 /etc/yum.repos.d/kubernetes.repo 中现存的所有配置

cat <<EOF | sudo tee /etc/yum.repos.d/kubernetes.repo
[kubernetes]
name=Kubernetes
baseurl=https://pkgs.k8s.io/core:/stable:/v1.32/rpm/
enabled=1
gpgcheck=1
gpgkey=https://pkgs.k8s.io/core:/stable:/v1.32/rpm/repodata/repomd.xml.key
exclude=kubelet kubeadm kubectl cri-tools kubernetes-cni
EOF
```



### 3、安装 kubelet、kubeadm 和 kubectl，并启用 kubelet 以确保它在启动时自动启动

```
yum install -y kubelet kubeadm kubectl --disableexcludes=kubernetes
systemctl enable --now kubelet
```

kubelet 现在每隔几秒就会重启，因为它陷入了一个等待 kubeadm 指令的死循环。





## 四、使用kubeadm创建集群

### 1、加载默认配置文件

```shell
kubeadm config print init-defaults > kubeadm.yaml
```

### 2、修改配置文件

```yaml
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
#注释掉这三行
#localAPIEndpoint:
#  advertiseAddress: 1.2.3.4
#  bindPort: 6443
nodeRegistration:
  criSocket: unix:///var/run/containerd/containerd.sock
  imagePullPolicy: IfNotPresent
#注释掉这一行
#  name: node
  taints: null
---
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
#修改镜像地址为阿里云的镜像库
imageRepository: registry.cn-hangzhou.aliyuncs.com/google_containers
kind: ClusterConfiguration
kubernetesVersion: 1.30.0
networking:
  dnsDomain: cluster.local
  ##指定pod网段
  serviceSubnet: 10.96.0.0/16
  podSubnet: 10.244.0.0/24
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





### 3、初始化集群

```shell
kubeadm init --config=kubeadm.yaml --ignore-preflight-errors=SystemVerification
```

注：刚开始的时候，因为会下载k8s的镜像，所以会卡住一段时间，只要不报错，就等一会

显示如下，说明安装完成：

![image-20250107125701507](\img\linux\image-20250107125701507.png)

### 4、在master上执行

```shell
mkdir -p $HOME/.kube
sudo cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
sudo chown $(id -u):$(id -g) $HOME/.kube/config
```

Kubeadm 通过初始化安装是不包括网络插件的，也就是说初始化之后是不具备相关网络功能的，比如 k8s-master 节点上查看节点信息都是“Not Ready”状态、Pod 的 CoreDNS无法提供服务等





## 五、安装Calico

### 1、将以下镜像上传至/opt/images目录下

![image-20250107130156415](\img\linux\image-20250107130156415.png)



### 2、载入镜像

```shell
ctr -n=k8s.io images import <tar包名>
```



### 3、将ymal文件上传至/opt目录下

### 4、执行apply命令

```shell
kubectl apply -f calico.yaml
```



### 5、验证

```shell
kubectl get pods -n kube-system
```

![image-20250107130619704](\img\linux\image-20250107130619704.png)

```shell
kubectl get node
```

![image-20250107130708894](\img\linux\image-20250107130708894.png)



## 六、加入工作节点



### 1、在master节点上查看加入节点的token

```
kubeadm token create --print-join-command
```

![image-20250107130856549](\img\linux\image-20250107130856549.png)

### 2、在已经安装了kubeadm、kubectl、kubelet机器上面执行上图中的命令









## 至此，使用kubeadm快速搭建k8s已完成！
