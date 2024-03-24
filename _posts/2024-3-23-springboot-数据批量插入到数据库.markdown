---
layout:     post
title:      "springBoot-数据批量插入到数据库"
subtitle:   " \"linux\""
date:       2024-3-23 17:48:49
author:     "yangsir"
header-img: "img/bg-material.jpg"
catalog: true
tags:
    - 笔记
    - k8s
    - 云原生



---

> “Yeah It's on. ”


<p id = "build"></p>

# 数据批量插入到数据库解决方案





解决多条数据批量插入到数据库



数据表

![image-20240323174956850](\杨sir\AppData\Roaming\Typora\typora-user-images\image-20240323174956850.png)



编写`ProMappingGroup`，`ProMappingGroup`是一个假设的Java类，代表要插入数据库的数据结构

```java
public class ProMappingGroup {
    /** 项目id */
    private Long proId;
    /** 小组id */
    private Long groupId;
    //省略get、set方法
    //省略tostring方法
    }
```



编写`GroupDetailsMapper.java`

```java
    public int insertProMappingGroup(ProMappingGroup proMappingGroup);
    public void insertProMappingGroupBatch(List<ProMappingGroup> proMappingGroups);
```



编写映射文件`GroupDetailsMapper.xml`

```java
 <insert id="insertProMappingGroup" parameterType="ProMappingGroup">
        INSERT INTO pro_mapping_group (group_id, pro_id) VALUES (#{groupId}, #{proId})
    </insert>
    <insert id="insertProMappingGroupBatch" parameterType="List">
        INSERT INTO pro_mapping_group (group_id, pro_id)
        VALUES
        <foreach collection="list" item="item" index="index" separator=",">
            (#{item.groupId}, #{item.proId})
        </foreach>
    </insert>
```



编写`GroupDetailsServiceImpl.java`

```java
    public int insertProMappingGroup(ProMappingGroup proMappingGroup){
        return groupDetailsMapper.insertProMappingGroup(proMappingGroup);
    }
    /*批量插入*/
    @Resource
    public SqlSessionFactory sqlSessionFactory;
    public void insertProMappingGroupBatch(List<ProMappingGroup> proMappingGroups) {
          SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH,false);
          GroupDetailsMapper mapper =sqlSession.getMapper(GroupDetailsMapper.class);
          for (ProMappingGroup proMappingGroup : proMappingGroups){
              mapper.insertProMappingGroup(proMappingGroup);
          }
          sqlSession.commit();
          sqlSession.close();
    }
```



编写service接口`IGroupDetailsService.java`

```java
    public int insertProMappingGroup(ProMappingGroup proMappingGroup);

    public void insertProMappingGroupBatch(List<ProMappingGroup> proMappingGroups);
```



编写controller

```java
public void allocation(){
        Long parentDeptId = SecurityUtils.getDeptId();
        List<Long> projectIds  = proAllocationService.selectNotAllocationByParentDeptId(parentDeptId);
        List<Long> GroupIds = groupDetailsService.selectGroupIdByDeptId(parentDeptId);
        List<ProMappingGroup> proMappingGroups = new ArrayList<>();
        Collections.shuffle(projectIds);
        int groupSize =GroupIds.size();
        for (int i = 0;i < projectIds.size();i++){
            Long projectId = projectIds.get(i);
            Long groupId = GroupIds.get(i % groupSize);
            ProMappingGroup proMappingGroup = new ProMappingGroup();
            proMappingGroup.setProId(projectId);
            proMappingGroup.setGroupId(groupId);
            proMappingGroups.add(proMappingGroup);
        }
        groupDetailsService.insertProMappingGroupBatch(proMappingGroups);

    }
```

