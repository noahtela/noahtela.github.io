---
layout:     post
title:      "iptables进阶使用"
subtitle:   " \"iptables\""
date:       2025-4-16 10:28:12
author:     "yangsir"
header-img: "img/bg-material.jpg"
catalog: true
tags:
    - 笔记
    - iptables
    - Linux软件


---

> “iptables在生产中有什么用？”


<p id = "build"></p>

# 一篇文章理清的 iptables 核心



## 引言

在日常运维中，你是否遇到过 Kubernetes 节点突然无法转发流量，日志中频繁出现 `iptables rules conflict` 的报错？这类问题看似是简单的规则冲突，实则触及 Linux 网络的核心机制——**iptables**。

作为 Linux 内核的“流量指挥官”，iptables 通过 常用的**五张表**(全部七张) 与 **五条链** 的精密配合，掌控着数据包的“生杀大权”。然而，其复杂的规则体系常令人望而生畏。本文将从 **底层原理**、**核心架构** 到 **实战命令**，为你揭开 iptables 的神秘面纱。无论你是初学 Linux 的网络小白，还是需要排查生产环境问题的资深工程师，都能在这里找到答案。



**阅读本文，你将掌握：**

- 数据包在 Linux 内核中的完整“旅程”
- 五张表如何分工协作，实现流量精细化控制
- 五条链如何层层把关，决定数据包命运
- 生产环境中 iptables 的经典应用与避坑指南

*若文中有任何疏漏，欢迎指正。*





## **一、先导知识：数据包的“奇幻漂流”**

想象你是一个数据包，从进入 Linux 系统到离开，会经历一场严格的“闯关游戏”。每一关都有不同的规则决定你的去向，整个过程分为 **五大关卡**：

1. **PREROUTING（入境检查站）**
   - **任务**：检查是否需要“改头换面”（NAT），比如将公网 IP 转换为内网 IP。
   - **类比**：国际快递入境时，海关决定是否要拆箱检查或重新贴标签。
2. **INPUT（本地签收台）**
   - **任务**：判断数据包是否交给本机的应用程序（如 Nginx、MySQL）。
   - **类比**：快递到达收件人地址后，由前台签收并转交本人。
3. **FORWARD（跨境中转站）**
   - **任务**：如果数据包的目标是其他机器（如 Kubernetes 的 Pod 通信），在此中转。
   - **类比**：快递站的中转仓，负责将包裹分拣到其他城市。
4. **OUTPUT（出境安检口）**
   - **任务**：处理本机应用程序主动发出的数据包（如 MySQL 客户端连接远程数据库）。
   - **类比**：你寄出的快递在发出前，需要经过安全检查。
5. **POSTROUTING（出境登记处）**
   - **任务**：数据包离开网卡前的最后一道关卡，通常用于修改源地址（SNAT）。
   - **类比**：快递发出前，统一贴上发货仓库的地址标签。



## 二、五张表：流量控制的“五大部门”



**注意**：传统教材常提到“七张表”，但现代 Linux 内核（4.x+）实际常用 **五张表**：`raw`、`filter`、`nat`、`mangle`、`security`。它们各司其职，形成决策流水线。

#### **1. raw表：数据包的“免检通道”**

- **职能**：在连接跟踪（conntrack）前处理数据包，可跳过状态跟踪以**提升性能**。
- **经典场景**：
  - 高并发场景下，禁用对特定流量的连接跟踪（如 HTTP 短连接）。
  - 标记无需 NAT 的流量。

```shell
# 示例：对 80 端口的流量禁用连接跟踪  
iptables -t raw -A PREROUTING -p tcp --dport 80 -j NOTRACK
```



#### **2. filter表：流量的“安检门”**

- **职能**：决定数据包的“生死”（ACCEPT/DROP/REJECT），是使用最频繁的表。

- **核心链**：INPUT、FORWARD、OUTPUT。

- **实战规则**：

  ```shell
  # 允许已建立的连接（防止误杀正常流量）  
  iptables -A INPUT -m conntrack --ctstate ESTABLISHED,RELATED -j ACCEPT  
  
  # 防御 SSH 暴力破解：60 秒内 3 次尝试则封禁  
  iptables -A INPUT -p tcp --dport 22 -m recent --name SSH --set  
  iptables -A INPUT -p tcp --dport 22 -m recent --name SSH --update --seconds 60 --hitcount 3 -j DROP  
  ```

#### **3. nat表：地址的“翻译官”**

- **职能**：修改数据包的源地址（SNAT）或目标地址（DNAT），实现网络地址转换。

- **核心链**：PREROUTING（DNAT）、POSTROUTING（SNAT）。

- **典型配置**：

  ```shell
  # DNAT：将访问 1.1.1.1:80 的流量转发到内网 10.0.0.2:80  
  iptables -t nat -A PREROUTING -d 1.1.1.1 -p tcp --dport 80 -j DNAT --to 10.0.0.2:80  
  
  # SNAT：内网 10.0.0.0/24 的机器通过网关 2.2.2.2 上网  
  iptables -t nat -A POSTROUTING -s 10.0.0.0/24 -j SNAT --to-source 2.2.2.2 
  ```



#### **4. mangle表：数据的“整形师”**

- **职能**：修改数据包头部信息（如 TTL、MARK），常用于 QoS 或策略路由。

- **实战案例**：

  ```shell
  # 标记 HTTP 流量为优先级 1（需配合 iproute2 实现流量调度）  
  iptables -t mangle -A PREROUTING -p tcp --dport 80 -j MARK --set-mark 1 
  ```



#### **5. security表：强制的“安检室”**

- **职能**：与 SELinux 等安全模块配合，实现强制访问控制。
- **使用场景**：企业级安全策略（一般场景较少直接操作）。



## 三、五条链：数据包的“通关路线”

五条链定义了数据包必经的关卡，结合五张表形成完整的控制流程：



#### **1. PREROUTING链：入境第一站**

- **触发时机**：数据包进入网卡后，**路由决策前**。
- **常用表**：raw（免检）、nat（改目标地址）、mangle（打标记）。

#### **2. INPUT链：本机流量的守门员**

- **触发时机**：数据包目标是本机进程（如 Nginx 接收请求）。
- **常用表**：filter（决定是否放行）、mangle（修改包头）。

#### **3. FORWARD链：中转流量的交警**

- **触发时机**：数据包需要转发到其他机器（如 Kubernetes Pod 通信）。
- **常用表**：filter（过滤非法中转）、mangle（流量标记）。

#### **4. OUTPUT链：出境流量的质检员**

- **触发时机**：本机进程主动发送数据包（如 curl 访问外部 API）。
- **常用表**：raw（免检）、filter（权限控制）、nat（源地址转换）。

#### **5. POSTROUTING链：出境前的最后一步**

- **触发时机**：数据包离开网卡前，**路由决策后**。
- **常用表**：nat（改源地址）、mangle（最终调整）。



## 四、优先级与规则执行顺序：谁先谁后？

iptables 的规则执行遵循严格的优先级体系，就像快递分拣中心的流水线，每个环节必须按顺序处理。

#### **1. 表的处理顺序：从“粗筛”到“精检”**

数据包会依次经过五张表，顺序为：
**raw → mangle → nat → filter → security**

- **为什么这个顺序重要？**
  - `raw` 表最早处理，适合快速决策（如免检流量）。
  - `nat` 表在路由前修改地址，确保后续表处理正确的目标。
  - `filter` 表最后执行，负责最终放行或拦截。

**类比**：快递先过X光机（raw表），再贴标签（mangle表），改地址（nat表），最后安检（filter表）。

#### **2. 链的执行流程：数据包的“人生地图”**

```
[数据包进入网卡]  
        │  
        ▼  
   PREROUTING链（raw/mangle/nat表）  
        │  
        ▼  
    路由决策（判断去向）  
        ├─目标为本机 → INPUT链 → 本地应用  
        │  
        └─需要转发 → FORWARD链 → POSTROUTING链  
        │  
[本机发出数据包]  
        │  
        ▼  
    OUTPUT链（raw/mangle/nat表）  
        │  
        ▼  
   POSTROUTING链（nat/mangle表）  
        │  
        ▼  
[数据包离开网卡]  
```



**关键点**：

- **PREROUTING** 和 **POSTROUTING** 是全局关卡，无论数据包去向如何都会经过。
- **OUTPUT** 链仅处理本机主动发出的流量。



## 五、生产环境实战案例：从报错到解决

#### **案例1：Kubernetes节点流量转发异常**

- **现象**：Pod跨节点通信失败，日志提示 `iptables rules conflict`。

- **根因**：`nf_conntrack` 表记录已满，导致新连接无法跟踪。

- **解决方案**：

  ```shell
  # 扩大连接跟踪表容量  
  echo 524288 > /proc/sys/net/netfilter/nf_conntrack_max  
  
  # 对HTTP流量禁用连接跟踪（需评估安全风险）  
  iptables -t raw -A PREROUTING -p tcp --dport 80 -j NOTRACK  
  ```

  **验证命令**：

  ```shell
  # 查看当前连接跟踪表大小  
  sysctl net.netfilter.nf_conntrack_max  
  # 监控连接跟踪数量  
  conntrack -L | wc -l  
  ```

#### **案例2：防御SSH暴力破解**

- **需求**：动态封禁1小时内尝试3次失败的IP。

- **配置**：

  ```shell
  # 创建名为ssh_attack的计数器  
  iptables -A INPUT -p tcp --dport 22 -m recent --name ssh_attack --set  
  
  # 若1小时内同一IP尝试3次，则封禁  
  iptables -A INPUT -p tcp --dport 22 -m recent --name ssh_attack --update --seconds 3600 --hitcount 3 -j DROP  
  
  # 允许正常SSH连接（此规则需放在封禁规则之后）  
  iptables -A INPUT -p tcp --dport 22 -j ACCEPT  
  ```

  **注意**：规则顺序不可颠倒，否则封禁失效！



## 六、性能优化技巧：让iptables飞起来

#### **1. 减少规则数量：用ipset代替重复匹配**

- **传统做法**：为每个黑名单IP写一条规则。
- **高效做法**：使用ipset集合管理IP列表。

```shell
# 创建名为blacklist的IP集合  
ipset create blacklist hash:ip  

# 添加恶意IP到集合  
ipset add blacklist 192.168.1.100  
ipset add blacklist 192.168.1.101  

# 用一条规则封禁整个集合  
iptables -A INPUT -m set --match-set blacklist src -j DROP  
```



#### **2. 调整表优先级：让高频规则先执行**

- **优化原理**：在raw表尽早丢弃无效流量，减少后续处理开销。

```shell
# 丢弃无效TCP标志位的数据包（如NULL扫描）  
iptables -t raw -A PREROUTING -p tcp --tcp-flags ALL NONE -j DROP  
```



#### **3. 关闭无用功能：卸载冗余内核模块**

- **示例**：停用FTP连接跟踪模块（若无FTP服务）。

```shell
# 禁用nf_conntrack_ftp  
echo 0 > /proc/sys/net/netfilter/nf_conntrack_helper  
```



## 七、常见误区与排坑指南：少走弯路！

#### **误区1：“规则顺序无所谓”**

**反例**：

```shell
iptables -A INPUT -j ACCEPT    # 放行所有流量  
iptables -A INPUT -p tcp --dport 22 -j DROP   # 此规则永远不生效！  
```

**正解**：严格规则在前，通用规则在后。



#### **误区2：“DROP比REJECT更安全”**



- **陷阱**：DROP静默丢弃数据包，客户端会等待超时（如SSH卡住30秒）。
- **建议**：

```shell
# 对已知攻击者用DROP  
iptables -A INPUT -s 10.0.0.100 -j DROP  

# 对普通用户返回REJECT（快速反馈）  
iptables -A INPUT -p tcp --dport 22 -j REJECT --reject-with icmp-port-unreachable  
```



#### **误区3：“nat表能过滤流量”**

- **真相**：nat表仅修改地址，过滤必须依赖filter表。
- **错误示例**：

```shell
iptables -t nat -A POSTROUTING -s 10.0.0.5 -j DROP   # 无效！  
```

- **正确做法**：

```shell
# 在filter表拦截  
iptables -A FORWARD -s 10.0.0.5 -j DROP  
```



## 八、附常用命令

### 1.查看规则

```shell
# 查看所有表的规则（显示详细链、计数器）
iptables -L -n -v --line-numbers

# 查看指定表的规则（如nat表）
iptables -t nat -L -n -v

# 以原始格式导出规则（适合备份）
iptables-save > iptables-backup.txt
```

### 2.规则管理

(1)允许/拒绝流量

```shell
# 允许所有本地回环流量（必需）
iptables -A INPUT -i lo -j ACCEPT

# 允许已建立的连接（防止断连）
iptables -A INPUT -m conntrack --ctstate ESTABLISHED,RELATED -j ACCEPT

# 允许SSH（22端口）
iptables -A INPUT -p tcp --dport 22 -j ACCEPT

# 拒绝所有其他入站流量（最后一条规则）
iptables -A INPUT -j DROP
```

(2)端口转发

```shell
# 将外部80端口转发到内网10.0.0.2:80
iptables -t nat -A PREROUTING -p tcp --dport 80 -j DNAT --to 10.0.0.2:80
iptables -t nat -A POSTROUTING -p tcp -d 10.0.0.2 --dport 80 -j SNAT --to-source [网关IP]
```



(3)封禁IP

```shell
# 封禁单个IP
iptables -A INPUT -s 192.168.1.100 -j DROP

# 封禁IP段
iptables -A INPUT -s 192.168.1.0/24 -j DROP
```



### 3.规则维护

```shell
# 删除INPUT链的第3条规则
iptables -D INPUT 3

# 清空所有规则（谨慎操作！）
iptables -F

# 重置默认策略（放行所有流量，避免被锁）
iptables -P INPUT ACCEPT
iptables -P FORWARD ACCEPT
iptables -P OUTPUT ACCEPT
```



### 4.高级技巧

(1)使用IPSet管理IP集合

```shell
# 创建IP集合
ipset create blacklist hash:ip

# 添加IP到集合
ipset add blacklist 192.168.1.100

# 引用集合封禁IP
iptables -A INPUT -m set --match-set blacklist src -j DROP
```

(2)限制连接速率

```shell
# 限制SSH每分钟最多3次新连接
iptables -A INPUT -p tcp --dport 22 -m state --state NEW -m recent --set --name SSH
iptables -A INPUT -p tcp --dport 22 -m state --state NEW -m recent --update --seconds 60 --hitcount 4 --name SSH -j DROP
```



(3)日志记录

```shell
# 记录被DROP的数据包（日志位于/var/log/syslog）
iptables -A INPUT -j LOG --log-prefix "IPTABLES-DROP: " --log-level 4
iptables -A INPUT -j DROP
```

### 5.常用参数解析

| 参数      | 说明                                |
| :-------- | :---------------------------------- |
| `-A`      | 追加规则到链末尾                    |
| `-I`      | 插入规则到链开头（如 `-I INPUT 1`） |
| `-D`      | 删除规则                            |
| `-p`      | 协议（tcp/udp/icmp）                |
| `--dport` | 目标端口                            |
| `--sport` | 源端口                              |
| `-s`      | 源IP                                |
| `-d`      | 目标IP                              |
| `-j`      | 跳转动作（ACCEPT/DROP/LOG等）       |

**注意**：操作前建议备份规则（`iptables-save > backup.txt`），避免误操作导致服务不可用。
