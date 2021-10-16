# CRM-11.0

## CRM 介绍

CRM 采用全新的前后端分离模式，本仓库代码中已集成前端 vue 打包后文件， **可免去打包操作，无需运行前端** 。如需调整前端代码，请单独下载前端代码

## CRM 目录结构

```python
crm
├── admin         -- 系统管理模块和用户管理模块
├── authorization -- 鉴权模块，目前仅用于登录鉴权，后期可能有更改
├── bi            -- 商业智能模块
├── core          -- 通用的代码和工具类
├── crm           -- 客户管理模块
├── examine       -- 审批模块
├── gateway       -- 网关模块
├── job           -- 定时任务模块
├── oa            -- OA模块
└── work          -- 项目管理模块
```

## CRM 使用的主要技术栈

| 名称                 | 版本                      | 说明                       |
| -------------------- | ------------------------- | -------------------------- |
| spring-cloud-alibaba | 2.2.1.RELEASE(Hoxton.SR3) | 核心框架                   |
| swagger              | 2.9.2                     | 接口文档                   |
| mybatis-plus         | 3.3.0                     | ORM 框架                   |
| sentinel             | 2.2.1.RELEASE             | 断路器以及限流             |
| nacos                | 1.2.1.RELEASE             | 注册中心以及分布式配置管理 |
| seata                | 1.2.0                     | 分布式事务                 |
| elasticsearch        | 2.2.5.RELEASE(6.8.6)      | 搜索引擎中间件             |
| jetcache             | 2.6.0                     | 分布式缓存框架             |
| xxl-job              | 2.1.2                     | 分布式定时任务框架         |
| gateway              | 2.2.2.RELEASE             | 微服务网关                 |
| feign                | 2.2.2.RELEASE             | 服务调用                   |

## CRM 项目架构图

<img src="https://images.gitee.com/uploads/images/2020/0910/094237_e7cb3bca_1096736.jpeg" width="650">

## 使用说明

### 一、前置环境

- Jdk：1.8
- Maven：3.5.0+
- Mysql：8.x
- Redis(版本不限)
- Elasticsearch 6.8.19
- Seata：1.4.2
- Sentinel：1.7.2（项目中 sentinel 使用 8079 端口）
- Nacos：2.0.3

### 安装

### 手动安装说明

#### 1. 导入初始化 sql,目前项目下 gateway 模块使用的独立数据库，其他模块使用同一数据库

- 安装 nacos，新建数据库 `nacos` 在`nacos`数据库中运行` DB/nacos.sql`<br/>
  修改 nacos 安装目录/conf/application.properties 文件，修改数据持久化类型为 mysql，添加 mysql 数据源的 url、用户名和密码,配置如下。<br/>

```
   spring.datasource.platform=mysql
   db.num=1
   db.url.0=jdbc:mysql://127.0.0.1:3306/nacos?characterEncoding=utf8&connectTimeout=1000&socketTimeout=3000&autoReconnect=true
   db.user=root
   db.password=password
```

- 修改 gateway 模块数据库配置为`nacos`数据库
- 初始化其余模块数据库：新建数据库 `wk_crm_single` 在`wk_crm_single`数据库中运行 `DB/wk_crm_single.sql`
- 初始化定时任务模块数据库：新建数据库 `xxl_job` 在`xxl_job`数据库中运行 `DB/xxl_job.sql`
- 初始化 seata 数据库：新建数据库 `seata` 在`seata`数据库中运行 `DB/seata.sql`

#### 2.在项目根目录执行`mvn install`

#### 3.在各个模块下 resource 目录配置数据库帐号信息以及 redis 帐号信息`（默认使用的是 application-dev.yml 配置文件, 打包后启动脚本默认使用的 application-test.yml 配置文件）`

#### 4.在`crm\src\main\resources\application-dev.yml`里面修改 elasticsearch 配置

```
spring.elasticsearch.rest.uris = elasticsearch地址 例：127.0.0.1:9200
spring.elasticsearch.rest.username = elasticsearch用户名 例：elastic 无密码可留空
spring.elasticsearch.rest.password = elasticsearch密码 例: password 无密码可留空

```

注意：elasticsearch [配置注意点](https://gitee.com/wukongcrm/crm_pro/wikis/elasticsearch配置说明?sort_id=2927431)

#### 5.（可选）修改系统中文件上传地址，默认为本地配置，本地上传还需配置公网地址，指向服务器网关

```
crm.upload.config:1                文件上传配置 1:本地 2:阿里云OSS
crm.upload.oss                     oss上传文件所需配置内容
crm.upload.oss.bucketName        需要配置两个bucket，0为登录才可访问文件上传地址，1为完全公开文件上传地址
crm.upload.local                   本地上传文件所需配置内容
crm.upload.local.uploadPath      需要配置两个地址0为登录才可访问文件上传地址，1为完全公开文件上传地址
```

#### 6.（可选）修改 jetcache 缓存配置详见 <a href="https://github.com/alibaba/jetcache/wiki" target="_blank">官方文档</a>

#### 7.（可选）项目日志文件在`core\src\main\resources\logback-spring.xml`修改

#### 8. 项目打包部署

```
·在项目根目录下执行 mvn clean -Dmaven.test.skip=true package
·然后把对应模块下target文件夹下
·${name}-${version}-SNAPSHOT.zip/tar.gz上传到服务器,例：admin-0.0.1-SNAPSHOT.zip 并将压缩文件解压，检查对应配置文件。

```

#### 9. 项目启动 <br/>

```
先启动 nacos, seata, sentinel, elasticsearch, mysql, redis 等基础服务
在第八步解压的文件模块下通过执行`sh crm.sh start` 启动各个模块服务。
其中项目基础模块：gateway，authorization，admin必须启动，其他模块可按需启动。
启动完成后，在浏览器中访问：http://localhost:8443/即可登录系统

```

#### 10. 初始化用户信息<br/>

```
访问 http://localhost:8443/
按照提示初始化超级管理员账号和密码信息，成功之后使用初始化后的管理员账户登录系统添加其他员工，分配权限等

```

#### 11. 升级说明以及注意事项<br/>

```
1、后端代码更新直接下载全量代码替换即可
2、数据库更新请下载 DB/update/V11.x.x.sql，增量执行即可（比如当前版本为V11.0.1，升级为V11.1.0需执行 V11.0.2.sql,V11.1.0.sql）
3、docker更新请使用在线更新或手动将数据库数据备份后执行增量升级SQL，再备份至本地，然后下载新版docker镜像，将增量升级后SQL恢复至新版数据库

```

### 三、其他说明

#### 1.代码生成器及接口文档<br/>

```
代码生成器地址：core\src\test\com\kakarote\generator\Generator.java
接口文档地址`http://localhost:8443/doc.html`
```

#### 2.模块依赖关系 <br/>

```
- 除网关外，其余项目均依赖于admin模块，用于获取当前登录人的信息
- oa模块的任务依赖于work模块，其余一些关联业务功能依赖于crm模块，examine模块
- 商业智能依赖crm,oa模块

```
