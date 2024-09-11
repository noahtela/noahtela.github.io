---
layout:     post
title:      "Python-GUI编程基础"
subtitle:   " \"linux\""
date:       2024-8-2 9:22:12
author:     "yangsir"
header-img: "img/bg-material.jpg"
catalog: true
tags:
    - 笔记
    - Python
    - GUI编程


---

> “Yeah It's on. ”


<p id = "build"></p>

# Python GUI编程基础



## 基于 tkinter模块创建GUI程序包含如下4个核心步骤

1、创建应用程序主窗口对象（也称根窗口）

（1）通过类Tk的无参构造函数

```python
from tkinter import *

root = Tk()
```

注意：此时窗口可以加载出来，但是不能维持住



2、在主窗口中，添加各种可视化组件，比如:按钮(Button)文本框(Label)等。

```python
from tkinter import *

root = Tk()

btn01 = Button(root)

btn01["text"] = "点我一下"
```



3、通过几何布局管理器，管理组件的大小和位置

```python
from tkinter import *

root = Tk()

btn01 = Button(root)

btn01["text"] = "点我一下"


btn01.pack()
```

4、事件处理

通过绑定事件处理程序，响应用户操作所触发的事件（比如:单击、双击等）

