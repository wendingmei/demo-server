#k8s部署文档(胡一兰整理)

## 一、目录结构(SpringBoot应用, ```APP_NAME``` 为应用名称)
```text
[APP_NAME]/ ----------------------- 项目目录
├── deploy/ ----------------------- 部署文件目录
│   └── [env]/ -------------------- 环境目录
│       ├── Dockerfile ------------ Dockerfile文件
│       └── configmap/ ------------ 配置文件目录
│           └─ bootstrap.yml ------ 配置文件
├── src/ -------------------------- 源代码根目录
├── target/ ----------------------- maven打包 [APP_NAME].jar 目录
│   └── [APP_NAME].jar ------------ 应用 jar 包
```

## 二、oc安装、oc平台访问
### 1.安装 oc
* 下载链接:
    ```text
    windows 版本: https://downloads-openshift-console.caas.ccic-test.com.cn/amd64/windows/oc.zip
    linux 版本: https://downloads-openshift-console.caas.ccic-test.com.cn/amd64/linux/oc.zip
    mac 版本: https://downloads-openshift-console.caas.ccic-test.com.cn/amd64/mac/oc.zip
    ```

* 安装：
    ```text
    windows: 将oc添加到环境变量
    linux: 
        1.设置 oc 可执行的权限 chmod +x oc
        2.将oc文件放置 /use/local/bin 目录下
          或
          配置 .profile 文件
    ```

### 2.平台访问
*  VPN 访问测试环境需要配置 hosts 文件
    ```shell script
    # 测试环境
    10.2.9.11 oauth-openshift.caas.ccic-test.com.cn
    10.2.9.11 docs.caas.ccic-test.com.cn
    10.2.9.11 console-openshift-console.caas.ccic-test.com.cn
    10.2.9.11 api.caas-test.ccic-test.com.cn
    10.2.9.19 tregistry-0.caas-test.ccic-test.com.cn
    10.2.9.11 downloads-openshift-console.caas.ccic-test.com.cn
    ```

* 操作平台访问地址
    ```shell script
    # 测试环境(选择 CCIC-LDAP 登陆)
    https://console-openshift-console.caas.ccic-test.com.cn
  
    # 生产环境(选择 CCIC-LDAP 登陆)
    https://console-openshift-console.caas.ccic-net.com.cn
    ```

* 命令行登陆
    ```shell script
    # 测试环境
    oc login -u [用户名] -p [密码] --server=https://api.caas-test.ccic-test.com.cn:6443
    例如:
    oc login -u wangruibxdl -p wr@DD_2009 --server=https://api.caas-test.ccic-test.com.cn:6443

    # 生产环境
    oc login -u [用户名] -p [密码] --server=https://api.caas-host.ccic-net.com.cn:6443
    例如:
    oc login -u wangruibxdl -p XXXXX --server=https://api.caas-host.ccic-net.com.cn:6443
    ```

## 三、部署环境搭建步骤(环境如果已搭建就不要重复执行, 直接进行第四点)
* 登录后选择 ```name-space```
    ```shell script
    oc project [project_name]
    ``` 
    > ps: 不知道 ```project_name``` , 可通过 ```oc get projects``` 命令查看

* 打开命令行, 切换到 ```APP_NAME``` 目录
    ```shell script
    cd [APP_NAME]/deploy/[env]/
    ```

* 创建应用
    ```shell script
    oc new-app --name [APP_NAME] --binary --strategy=docker
    ```

* 配置文件挂载
    ```shell script
    # 将configmap文件夹下的配置文件创建成一个configmap
    oc create configmap [APP_NAME]-config --from-file ./configmap
    
    #　挂载配置到应用
    oc set volume dc/[APP_NAME] --add --mount-path=/deployments/config --configmap-name=[APP_NAME]-config
    ```

* 日志持久卷挂载
    ```shell script
    # 命令(或者直接从控制台创建)
    oc apply -f ./log-pvc.yaml
    
    # 内容如下：
    apiVersion: v1
    kind: PersistentVolumeClaim
    metadata:
      name: [pvc_name]
      namespace: [project_name]
    spec:
      accessModes:
        - ReadWriteOnce
      resources:
        requests:
          storage: [空间分配大小, 单位: Mi 或 Gi]
      storageClassName: [StorageClass名称]
      volumeMode: Filesystem
    
    # volume 挂载pvc
    oc set volume dc/[APP_NAME] --add --mount-path=/deployments/logs --claim-name=[pvc_name]
    ```

* 暴露 ```service```
    ```shell script
    # 命令:
    oc expose dc/[APP_NAME] --port=[server端口] --target-port=[容器服务端口]
    # 例如:
    oc expose dc/integral-server --port=80 --target-port=8080
    ```

* 暴露供k8s集群外访问的域名
    ```shell script
    # 命令:
    oc expose svc/[SVC_NAME]
    # 例如:
    oc expose svc/integral-server
    ```

## 四、应用部署
### 1.测试
* 打开命令行
    ```shell script
    cd [APP_NAME]/deploy/[env]/
    ```

* build上传应用(上传 -> build成镜像 -> 部署 -> 启动)
    ```shell script
    # 将打包后的.jar文件复制到[APP_NAME]/deploy/[env]/目录
    cp ../../target/[APP_NAME].jar ./
  
    # 上传、build镜像、初始化容器、部署启动一体化操作
    oc start-build [APP_NAME] --from-dir ./
    ```

* 查看 ```build```
    ```shell script
    # 查看 build 日志
    oc logs -f bc/[APP_NAME]
  
    # 查看 build 列表
    oc get build 或者 oc get build -w
  
    # 列表结果如下(Running: 正在运行; Complete: build成功):
    NAME            TYPE     FROM     STATUS     STARTED       DURATION
    app-name        Docker   Binary   Complete   5 hours ago   3m10s
    ```
## 2.UAT、生产
* 创建应用(仅第一次需要)
    ```shell script
    # 命令:
    oc new-app --name=[APP_NAME] --image=imagestream
  
    # imagestream 可以通过 oc get is 查看
    oc get is
    ```

* 拉取```image```(从镜像仓库拉取稳定的测试或者UAT环境镜像到生产)
    ```shell script
    # 命令:
    oc import-image [APP_NAME:TAG] --from=[IMAGE REPOSITORY:latest] --confirm --reference-policy=local
  
    # 例如:
    oc import-image integral-server:1.0.0 --from=default-route-openshift-image-registry.caas.ccic-test.com.cn/baodai-bxmxapp-test/integral-server:latest --confirm --reference-policy=local
    ```
    >  "reference-policy" 必须是"local"(这样才会把测试的镜像拉到生产. 否则只是打了个标记, 还是会从test去拉镜像, 但是测试一不稳定, 二会定期清理镜像)

* 将拉取到生产的```tag```打成```latest```
    ```shell script
    # 命令
    oc tag serverImageStream:tagVersion serverImageStream:latest --reference-policy=local
  
    # 例如:
    oc tag integral-server:1.0.0 integral-server:latest --reference-policy=local
    ```

* 发布应用, ```triggers```为手动模式(manual)下, 生产镜像变更不会自动生效, 需要手动发布
    ```
    # 命令：
    oc rollout latest dc/<dc_name>
  
    # 例如：
    oc rollout latest dc/integral-server 
    oc rollout latest dc/nginx 
    ```
    
* 查看 ```pod```
    ```shell script
    # 查看部署进度
    oc logs -f dc/[APP_NAME]
    
    # 查看 pod 列表
    oc get pod
    oc get pod -w
    oc get pod -o wide
  
    # 列表结果如下(NAME=[APP_NAME]-[version]-[随机字符串]):
    NAME                   READY   STATUS      RESTARTS   AGE
    [APP_NAME]-1-gcnjk     1/1     Running     0          4h33m(pod容器 node1)
    [APP_NAME]-1-jkdh9     1/1     Running     0          120m(pod容器 node2)
    ```
 

## 五、其他 ```oc``` 命令(具体可以参考 ```kubectl``` 命令, 并结合 ```oc --help``` )
* triggers 策略(configmap、imagestream 发生改变时自动更新部署pod)
    ```shell script
    # 手动模式
    oc set triggers dc/[APP_NAME] --manual
    # 自动
    oc set triggers dc/[APP_NAME] --auto
    ```

* pod 扩缩容
    ```shell script
    # DeploymentConfig 扩缩容
    oc scale --replicas=集群数 dc/[APP_NAME]
    # 举例
    oc scale --replicas=2 dc/gateway-server
  
    # StatefulSet 扩缩容
    oc scale --replicas=集群数 sts/[APP_NAME]
    # 举例
    oc scale --replicas=3 sts/nacos
    ```

* 查看信息( ```Pod``` 、 ```DeploymentConfig``` 、 ```Service``` 、 ```Route``` 、 ```Build``` 、 ```BuildConfig``` 、```PersistentVolumeClaims``` 、 ```ConfigMap``` 、 ```ImageStream``` )
    ```shell script
    oc get pod | dc | svc 或者 service | route | build | bc | pvc | cm | is 或者 imagestream
    举例: oc get pod 或者 oc get pod -o wide
    ```
* 查看组件 yaml 文件信息
    ```shell script
    # 例子1(查看dc):
    oc get dc/gateway-server -o yaml
    
    # 例子2(查看svc):
    oc get svc/gateway-server -o yaml
    
    # 例子3(查看route:
    oc get route/gateway-server -o yaml
    ```

*  查看详细信息
    ```shell script
    oc describe pod | dc | svc 或者 service | route | build | bc | pvc | cm | is 或者 imagestream [name]
    举例: oc describe pod [pod_name]
    ```

* 进入 ```pod```
    ```shell script
    oc exec -it [pod_name] /bin/bash
    oc rsh <project_name> #到容器执行命令
    ```

* 查看 ```pod``` 日志
    ```shell script
    oc logs -f [pod_name]
    ```
  
* 文件导入/导出
    ```shell script
    # 命令
    oc cp [pod]:{容积内文件路径} {本地电脑路径}
  
    # 例如:
    ## 导出容器内某个目录的所有文件到本地当前目录
    oc cp integral-server-uh49f:/deployments/logs/ ./

    ## 导出容器内某个指定文件到本地当前目录
    oc cp integral-server-uh49f:/deployments/logs/integral-server-uh49f/info.log ./

    ## 从本地将目录copy到容器
    oc cp ./bdproplatview nginx-1-8rm49:/opt/app-root/src

    ## 从本地将文件copy到容器
    oc cp ./index.html nginx-1-8rm49:/opt/app-root/src/bdproplatview/
    ```

* ```apply``` 命令
    ```shell script
    # apply 除了能添加 svc、route 之外还可以添加其他的k8s组件, *.yaml 文件中 kind 区分组件的类型
    oc apply -f [name].yaml
    ```

## 六、参数设置
* 参数设置方式如下
    ```text
    # 方式1: Dockerfile 中添加 ENV key=value
    # 方式2: 通过命令 oc set env dc/[APP_NAME] key=value .... (可设置多个)
    # 方式3: 进入平台 DeploymentConfig 添加 Environment -> env
    ```
    > 如果都有配置则优先级为: ```方式1 < 方式2 = 方式3```
  
* ```JVM``` 启动参数设置
	```shell script
	# JVM参数对应表如下
	JVM OPTSION                     容器平台自定义
	MaxMetaspaceSize                GC_MAX_METASPACE_SIZE
	MinHeapFreeRatio                GC_MIN_HEAP_FREE_RATIO
	MaxHeapFreeRatio                GC_MAX_HEAP_FREE_RATIO
	GCTimeRatio                     GC_TIME_RATIO
	AdaptiveSizePolicyWeight        GC_ADAPTIVE_SIZE_POLICY_WEIGHT
	[Other Options]                 GC_CONTAINER_OPTIONS(优先级低于上面的配置)
	
	# 方式1:
		ENV GC_MAX_METASPACE_SIZE="256"
		ENV GC_CONTAINER_OPTIONS="JVM 参数"
	
	# 方式2:
        oc set env dc/[APP_NAME] GC_MAX_METASPACE_SIZE=512 GC_CONTAINER_OPTIONS="-Xms2048m -Xmx2048m"
	```

* 系统参数设置
    ```shell script
    # 1.容器时区
        方式1: ENV TZ="Asia/Shanghai"
        方式2: oc set env dc/[APP_NAME] TZ=Asia/Shanghai
    # 2.语言设置
        方式1: ENV LANG="en_US.UTF-8"
        方式2: oc set env dc/[APP_NAME] LANG="en_US.UTF-8"
    ```
  
## 七、其他
* 测试环境镜像仓库地址
    ```
    https://quay.caas.ccic-test.com.cn/organization/openshift_baodai-bxmxapp-test
    ```
  
* dashboard 监控平台地址
    ```
    http://grafana.caas.ccic-net.com.cn/
    ```


* 容器平台相关组件账号密码
    ```text
    # 1.nacos配置中心 mysql 数据库地址
        a.测试环境:
            host: bxmxapp-mysql1.db.ccic-test.com.cn
            port: 3307
            database: bdmxappdb
            username: bdnacosbusi
            password: Ccic_1234
        b.生产:
            host: bxmxapp-mysql1.db.ccic-net.com.cn
            port: 3306
            database: bxmxappdb
            username: bdnacosbusi
            password: Qhwux_9530
    
    # 2.nacos控制台
        a.本地开发环境:
            url: http://10.1.12.170:8848/nacos
        a.测试环境:
            url: http://nacos-baodai-bxmxapp-test.caas.ccic-test.com.cn/nacos
            username: nacos
            password: nacos
        b.生产环境:
            url: http://nacos-baodai-bxmxapp.caas.ccic-net.com.cn/nacos
            username: nacos
            password: nacos
    
    # 3.redis
        a.测试环境
            cluster:
                容器外访问地址: 10.2.9.25:32235
                容器内访问地址: redis-cluster-redis:6379
            sentinel: 
                容器外访问地址: 10.2.9.25:30811
                容器内访问地址: redis-sentinel-redis:26379
            password: 5ae84ff727
        b.生产环境
            cluster:
                容器内访问地址: redis-cluster-redis:6379
            sentinel: 
                容器内访问地址: redis-sentinel-redis:26379
            password: ace50410ca
    ```

* 监控地址
    ```text
    Metrics: java和redis监控地址: http://grafana.caas.ccic-net.com.cn/
    Tracing: pinpoint java监控地址: http://oa.ccic-net.com.cn:26127/ 搜索 baodai 关键词
    ```
