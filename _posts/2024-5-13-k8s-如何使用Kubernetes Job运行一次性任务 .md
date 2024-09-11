---
layout:     post
title:      "K8S-如何使用Kubernetes Job运行一次性任务"
subtitle:   " \"linux\""
date:       2024-5-13 22:49:49
author:     "yangsir"
header-img: "img/bg-material.jpg"
catalog: true
tags:
    - 笔记
    - k8s
    - 云原生




---

> “k8s”


<p id = "build"></p>

# 如何使用Kubernetes Job运行一次性任务



## 什么是Job?

在 kubernetes 中，Deployment、DaemonSet会持续运行任务，这些 pod 中的进程在崩溃退出时会重新启动，永远达不到完成态。你也许会遇到这样的场景，当需要运行一个一次性的可完成的任务，其进程终止后，不应该再重新启动，那么 Job 资源类型完全符合你。 Kubernetes 中通过 Job 资源提供了对此的支持，它允许你运行一种 pod，该 pod 在内部进程成功结束时，不重启容器。一旦任务完成，pod 就被认为处于完成状态。 在发生节点故障时，该节点上由 Job 管理的 pod 将按照 ReplicaSet 的 pod 的方式， 重新安排到其他节点，以确保任务能够成功完成，所以 Job 通常用于执行一次性任务或批处理作业。 Job 还可以控制 Pod 的数量，确保一定数量的 Pod 成功完成任务。 Job 的一些常用使用场景：

- 批处理作业：Job可以被用来运行需要大量计算资源的作业，例如对大量数据的处理，机器学习模型训练等。
- 数据处理：Job也可以用来处理大量数据，例如数据的清洗、归档和备份等。
- 定时任务：Job可以被用来定期执行一些任务，例如定期生成报表、定期清理数据等。
- 资源分配：通过Job控制器，我们可以为特定任务分配所需的计算资源，例如CPU和内存等，以保证任务能够顺利执行。



## Job定义

下面是一个 Job 配置示例。它负责计算 π 到小数点后 2000 位，并将结果打印出来。 此计算大约需要 10 秒钟完成。

job.yaml：

```yml
apiVersion: batch/v1
kind: Job
metadata:
  name: pi
spec:
  template:
    spec:
      containers:
      - name: pi
        image: perl:5.34.0
        command: ["perl",  "-Mbignum=bpi", "-wle", "print bpi(2000)"]
      restartPolicy: Never
```





创建它，查看Job 、Pods 的状态： Pod 状态为 Running，说明已经在执行，Job 的 COMPLETIONS 为 0/1，表示按照预期启动了一个 Pod，还未完成。

```shell
#kubectl apply -f  job.yaml

job.batch/pi created


#kubectl get jobs,pods


NAME           COMPLETIONS   DURATION   AGE
job.batch/pi   0/1           39s        39s

NAME           READY   STATUS    RESTARTS   AGE
pod/pi-d5f6q   1/1     Running   0          39s
```

等待大概10s左右，发现状态已经变为 Completed 了， kubectl logs 可以查看 Pod 的标准输出：

```shell
# kubectl get jobs,pods
NAME           COMPLETIONS   DURATION   AGE
job.batch/pi   1/1           43s        47s
 
NAME           READY   STATUS      RESTARTS   AGE
pod/pi-d5f6q   0/1     Completed   0          47s
 
 
# 查看日志
# kubectl logs -f pi-d5f6q
3.1415926535897932384626433832795028841971693993751058209749445923078164062862089986280348253421170679821480865132823066470938446095505822317253594081284811174502841027019385211055596446229489549303819644288109756659334461284756482337867831652712019091456485669234603486104543266482133936072602491412737245870066063155881748815209209628292540917153643678925903600113305305488204665213841469519415116094330572703657595919530921861173819326117931051185480744623799627495673518857527248912279381830119491298336733624406566430860213949463952247371907021798609437027705392171762931767523846748184676694051320005681271452635608277857713427577896091736371787214684409012249534301465495853710507922796892589235420199561121290219608640344181598136297747713099605187072113499999983729780499510597317328160963185950244594553469083026425223082533446850352619311881710100031378387528865875332083814206171776691473035982534904287554687311595628638823537875937519577818577805321712268066130019278766111959092164201989380952572010654858632788659361533818279682303019520353018529689957736225994138912497217752834791315155748572424541506959508295331168617278558890750983817546374649393192550604009277016711390098488240128583616035637076601047101819429555961989467678374494482553797747268471040475346462080466842590694912933136770289891521047521620569660240580381501935112533824300355876402474964732639141992726042699227967823547816360093417216412199245863150302861829745557067498385054945885869269956909272107975093029553211653449872027559602364806654991198818347977535663698074265425278625518184175746728909777727938000816470600161452491921732172147723501414419735685481613611573525521334757418494684385233239073941433345477624168625189835694855620992192221842725502542568876717904946016534668049886272327917860857843838279679766814541009538837863609506800642251252051173929848960841284886269456042419652850222106611863067442786220391949450471237137869609563643719172874677646575739624138908658326459958133904780275901
Job 失败处理
```





## Job 失败处理



Job 的 restart 策略只有如下两种（没有pod的策略Always：

1. Never：只要任务没有完成，则新创建pod运行，直到job完成，会产生多个pod。（默认）
2. OnFailure：只要pod没有完成，就会重启pod，重新执行任务。

如果失败了会怎么样呢？ 我们故意引入一个错误，修改 job.yaml：将执行命令修改为错误的

```bash
...
        command: ["per",  "", "-", ""]
```

创建它，查看Job 、Pods 的状态， 当 restart 策略为 Never 时，会看到只要任务没有完成，就会新创建pod运行，直到job完成，会产生多个pod：

```bash
# kubectl apply -f  job.yaml
job.batch/pi created
 
# kubectl get jobs,pods
NAME           COMPLETIONS   DURATION   AGE
job.batch/pi   0/1           3m14s      3m14s
 
NAME           READY   STATUS       RESTARTS   AGE
pod/pi-9shvk   0/1     StartError   0          3m10s
pod/pi-gjwp7   0/1     StartError   0          2m
pod/pi-mp96m   0/1     StartError   0          2m40s
pod/pi-nrb64   0/1     StartError   0          3m14s
pod/pi-nznrc   0/1     StartError   0          3m
```



当 restart 策略为 OnFailure 时，只要pod没有完成，就会重启pod，重新执行任务：

```bash
# kubectl apply -f  job.yaml
job.batch/pi created
 
 
# kubectl get jobs,pods
NAME           COMPLETIONS   DURATION   AGE
job.batch/pi   0/1           103s       103s
 
NAME           READY   STATUS              RESTARTS     AGE
pod/pi-drrft   0/1     RunContainerError   4 (8s ago)   103s
```

## 定时执行 Job

Linux 中有 cron 程序定时执行任务，Kubernetes 的 CronJob 也提供了类似的功能，可以定时执行 Job。CronJob 配置文件示例如下： cronjob.yaml：

```bash
apiVersion: batch/v1
kind: CronJob
metadata:
  name: pi
spec:
  schedule: "* * * * *"
  jobTemplate:
    spec:
      template:
        spec:
          containers:
          - name: pi
            image: perl:5.34.0
            command: ["perl",  "-Mbignum=bpi", "-wle", "print bpi(2000)"]
          restartPolicy: OnFailure
```

创建它，查看 cronjobs 、Pods 的状态， 可以看到每隔一分钟就会启动一个 Job：

```bash
# kubectl apply -f cronjob.yaml
cronjob.batch/pi created
 
 
# kubectl get cronjobs,pods
NAME               SCHEDULE    SUSPEND   ACTIVE   LAST SCHEDULE   AGE
cronjob.batch/pi   * * * * *   False     1        7s              2m36s
 
NAME                    READY   STATUS      RESTARTS   AGE
pod/pi-28255870-cd4mx   0/1     Completed   0          2m7s
pod/pi-28255871-9tv6x   0/1     Completed   0          67s
pod/pi-28255872-nl99x   0/1     Completed   0          7s
```



## 使用 Job 的注意事项

在使用 Kubernetes Job 时，需要注意以下几点：

1. Job 对象适用于一次性任务或批处理作业，不适用于长时间运行的服务。
2. 需要确保 Job Spec 中定义的容器可以正常运行，并有足够的资源和权限执行指定的操作。
3. 在设计 Job 时，应考虑 Pod 失败和重试的情况，并设置合适的重试次数和间隔时间。
4. 如果 Job 执行时间过长，需要设置合适的 Pod 生命周期以避免过度消耗资源。
5. 在使用 Job 控制器时，应确保控制器的版本和 Kubernetes 版本兼容。在不同版本之间可能存在语法变更和行为差异。
