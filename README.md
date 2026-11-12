# 梅境 · 杨梅果园物联网病虫害预警平台

前后端分离的本地可运行演示工程。所有硬件演示操作均在网页中完成，不需要 Postman、单片机或真实设备。

## ① 建表 SQL

`backend/src/main/resources/schema.sql` 是完整的 H2 / MySQL 8 通用建表脚本，使用 `CREATE TABLE IF NOT EXISTS`。默认 H2 MySQL 模式，启动自动执行，不需要导入 SQL。默认内存数据在后端退出后清空，上传图片保存在 `backend/uploads`，不随数据库清空。

表包括：`app_user`、`orchard`、`device`、`sensor_data`、`risk_warning`、`observation`、`threshold_config`、`control_log`、`announcement`。业务关系由服务层维护；删除果园会在事务中清理关联业务数据，删除设备保留历史记录。

## ② 后端 Entity / Mapper / Service / Controller

固定版本：Spring Boot **2.7.18**、MyBatis-Plus **3.5.5**，Maven 管理；密码 BCrypt 加密，不返回密码摘要。使用 JDK 17 或 21，Maven 3.6+。

- `backend/src/main/java/cn/orchard/entity`：9 个持久化实体。
- `mapper`：对应 MyBatis-Plus BaseMapper。
- `service/AuthService`：注册、BCrypt 校验、24 小时内存令牌、角色控制。注册固定为 FARMER。
- `service/OrchardService`：果园、设备、独立阈值与农户所有权校验。
- `service/MonitoringService`：采集与预警事务、历史曲线、控制日志。
- `service/RiskService`：风险规则和简易防治建议。
- `service/ContentService`：观测、预警处理、公告；公告写操作限定 ADMIN。
- `service/SimulationJob`：每 15 秒生成环境数据，共用上传服务，没有额外模拟硬件 API。
- `service/FileService`：5MB 内 JPG/PNG 本地图片上传。
- `controller`：MVC API 层；`config`：鉴权拦截器、本地图片映射、演示数据。

普通业务请求携带 `Authorization: Bearer <token>`。农户只能操作自己的果园，管理员能管理全部果园。令牌在后端重启时失效。Vite 开发代理转发 `/api` 与 `/uploads`，前后端保持分离。

### 保留的两个硬件原始接口

**POST `/api/hardware/sensor/upload`**

```json
{"deviceSn":"YM-2026-001","temperature":26,"humidity":92,"soilHumidity":90,"light":30000,"co2":460,"collectTime":"2026-11-11T10:30:00"}
```

`collectTime` 使用本地 `yyyy-MM-ddTHH:mm:ss`，不能超前超过 5 分钟。温度单位 ℃、湿度 %、光照 lx、CO₂ ppm。返回保存的数据以及本次生成的预警。网页自动填写当前采集时间。

注释：【此接口对外开放，未来果园物联网传感器直接POST请求此地址上报环境数据；当前演示由前端模拟面板调用该接口】

**POST `/api/hardware/device/control`**

```json
{"deviceSn":"YM-2026-001","controlCommand":"开启通风"}
```

指令为 `开启通风`、`开启喷淋降湿`、`关闭喷淋`。当前仅写控制日志，不修改环境值。

注释：【未来对接真实硬件时，后端在这里增加转发逻辑；现阶段仅保存控制操作日志】

两条接口为演示要求的开放入口，无令牌也可使用已注册设备编号调用；Web 模拟器携带登录令牌时仍校验果园归属。未来真实硬件接入应在这两个入口增加设备凭据及转发逻辑。工程没有其他 `/api/hardware/**` 接口。

### 默认判定规则

| 类型 | 条件 | 等级 |
|---|---|---|
| 褐斑病 | 温度 22～28℃（含端点）且空气湿度 >85% | 高风险 |
| 果蝇 | 果园设为果实期、温度 25～30℃（含端点）且空气湿度 >75% | 高风险 |
| 癌肿病 | 空气湿度 >85% 且土壤湿度 >80% | 高风险 |
| 环境异常 | 温度超出 10～35℃、空气湿度 >85%、土壤湿度 >80%、光照超出 0～90000 lx 或 CO₂ >1200 ppm，任意满足 | 中等风险 |
| 正常 | 以上均不命中 | 低风险 |

每个果园可独立修改阈值；一条采集可生成多条不同类型预警，最终等级取最高等级。每次命中独立留痕，不对重复上报去重。历史记录保留当时判定结果，修改阈值后从下一次采集生效。卷叶蛾通过实地观测管理，环境条件不作为自动诊断依据；癌肿病规则用高湿作为演示条件，未模拟降雨传感器。

## ③ Vue 全部页面代码

Vue 3 + Element Plus + Axios + Vue Router + ECharts，Vite 构建，依赖版本锁定，包含 package-lock.json。

| 页面 | 文件 |
|---|---|
| 注册登录 | `frontend/src/pages/Login.vue` |
| 果园实时数据、红色告警、三条 ECharts 时序曲线、预警处理 | `frontend/src/pages/Dashboard.vue` |
| 果园增删改查、图片、果实期设置 | `frontend/src/pages/Orchards.vue` |
| 监测设备注册、编辑、删除 | `frontend/src/pages/Devices.vue` |
| **完整硬件模拟控制台单文件组件** | **`frontend/src/pages/Simulator.vue`** |
| 实地病虫害记录、图片、筛选与防治建议 | `frontend/src/pages/Observations.vue` |
| 每果园独立环境和病虫害阈值 | `frontend/src/pages/Thresholds.vue` |
| 管理员发布公告、农户阅读 | `frontend/src/pages/Announcements.vue` |

`App.vue` 提供响应式导航，`api.js` 集中处理 Axios、令牌和错误，`components/ImageUpload.vue` 负责真实文件上传。无外部图片或 CDN 依赖。

## ④ 项目启动与现场演示

### Windows 一键启动

安装 JDK 17/21、Maven、Node.js 20.19+（含 npm），确保命令加入 PATH。在项目根目录运行：

```powershell
powershell -ExecutionPolicy Bypass -File .\start-demo.ps1
```

启动脚本会自动查找 PATH、`NODE_HOME`、`NVM_SYMLINK` 以及 Node.js 常见安装目录。即使当前终端的 PATH 尚未更新，也可使用 `C:\Program Files\nodejs` 中的 Node.js，无需修改系统环境变量。自定义目录可在启动前设置 `$env:NODE_HOME='D:\你的Node安装目录'`。

首次运行会下载前后端依赖，后续使用本地缓存。脚本编译后端，启动两个隐藏进程并打开浏览器，日志和 PID 保存在 `.runtime`。

- 网页：<http://127.0.0.1:5173>
- 后端：<http://127.0.0.1:8080>
- 农户：`farmer` / `123456`
- 管理员：`admin` / `123456`

停止：

```powershell
powershell -ExecutionPolicy Bypass -File .\stop-demo.ps1
```

### 手动启动（也适用于 macOS / Linux）

在项目根目录先编译：

```shell
mvn -s maven-settings.xml -f backend/pom.xml package -DskipTests
```

终端一：

```shell
cd backend
java -jar target/bayberry-iot-1.0.0.jar
```

终端二：

```shell
cd frontend
npm install
npm run dev
```

前端生产构建：在 `frontend` 执行 `npm run build`，产物为 `dist`。本地预览可用 `npm run preview`（4173 端口，同样代理到后端）。生产部署将 `/api`、`/uploads` 反向代理到 Java 服务，并为 Vue Router 配置 history 回退到 `index.html`。

### 建议现场演示顺序

1. 用 `farmer / 123456` 登录，大盘已有 2 个果园、2 台设备和 20 组历史采集，等待 15 秒可观察曲线更新。
2. 进入「杨梅果园」，创建地块、上传本地实拍图、设置面积与品种，保留果实期开关。
3. 「监测设备」注册唯一编号并绑定刚创建的果园，填写安装位置。
4. 「硬件模拟控制台」选择该设备，点「填入低风险场景」再点「模拟果园传感器上报环境数据」，查看低风险回执。
5. 点「填入高风险场景」，再次上报。默认规则下命中褐斑病、果蝇、癌肿病以及环境异常；回大盘选此果园，查看红色高风险、最新读数、曲线与具体建议。自动任务仍每 15 秒运行，最新卡片可能随后变化，手动上报的预警保留在列表中。
6. 回模拟台选择「开启通风」「开启喷淋降湿」「关闭喷淋」，分别下发，操作历史立即显示。当前不会驱动硬件或修改传感器值。
7. 「病虫害观测」新增褐斑病、癌肿病、卷叶蛾或果蝇记录，填写程度、描述并上传病害实拍，列表展示对应建议；可按果园和病虫害筛选。
8. 「阈值配置」选择果园，修改空气湿度阈值并保存，再从模拟台上报验证；仅影响该果园后续数据。
9. 退出并用管理员登录，在「果园公告」发布一条科普公告；再用农户登录查看。也可注册一个新农户，确认其只能看到自己的地块。

### 切换 MySQL 8.0

先创建空库（仅创建库，不导入建表脚本）：`CREATE DATABASE bayberry CHARACTER SET utf8mb4;`。使用有该库建表权限的账号，在 `backend` 运行：

```powershell
$env:SPRING_PROFILES_ACTIVE='prod'
$env:MYSQL_URL='jdbc:mysql://localhost:3306/bayberry?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai'
$env:MYSQL_USER='root'
$env:MYSQL_PASSWORD='你的数据库密码'
java -jar target/bayberry-iot-1.0.0.jar
```

生产配置自动建表，但默认不生成演示账号、不运行模拟任务。首次管理员可先在网页注册农户，再由数据库维护者将对应 `app_user.role` 改为 `ADMIN`，重新登录即可。只在演示时需要 MySQL 也带演示数据，可显式设置 `SEED_ENABLED=true`、`SIMULATION_ENABLED=true`。

如需固定低/高风险画面，可以启动后端前设置 `SIMULATION_ENABLED=false`，仍保留模拟台按钮与启动历史样例。端口占用时先停止对应项目进程；依赖下载失败检查网络。项目自带 `maven-settings.xml` 使用官方 HTTPS 仓库及项目内缓存，不依赖本机的旧 Maven 镜像配置。

## 病虫害图片与示例记录

「病虫害观测」页面内置4类模拟示意图：褐斑病、癌肿病、卷叶蛾、果蝇，支持放大预览、按类型筛选、查看记录详情。在新增观测表单中选择「快速填入模拟示例」，可自动填入图片、程度及描述，再选择果园保存。

默认演示初始化自动写入8条记录，每类2条，覆盖轻度、中度和重度，并包含描述、图片及对应防治建议。示例归属演示农户farmer的果园；新注册用户仍只查看自己的果园记录，可通过表单自行添加模拟示例。生产配置关闭演示初始化时不写入这些样例。

图片由内置 image_gen 工具生成，保存在 `frontend/public/demo-pests/`，完整生成提示词在同目录 `generation.json`。图片为课程演示插图，不是果园实拍，也不是图片识别结果；记录中的「模拟示例」标记用于区分演示与真实观测。所有图片随源码打包，无需外网图片服务。

## 虚拟图像传感器与模拟模型

在「病虫害观测」点击「模拟摄像头识别」：选择果园与演示场景→点击「模拟摄像头采图并识别」→查看预设分类、模拟评分及建议→点击「确认结果并填入观测表单」→检查内容并保存。也可以上传JPG/PNG图片进行同样的演示。采图与识别不会自动创建记录，只有最后确认保存才入库。

- 虚拟图像传感器编号为 `CAM-DEMO-果园ID`，从内置示例图片取帧，记录采集时间，不调用电脑摄像头、不需要相机权限。最近采图只保留在当前页面内存中，最多4张；切换果园或场景会清除当前结果，防止错配。
- 后端 `POST /api/observations/model/predict` 为受登录保护的观测模块接口，参数 `orchardId`、`imageUrl`、`demoScenario`，校验果园归属，返回 `simulated=true`、`modelName=Bayberry-Demo-v2`、预设病虫害类型、程度、模拟评分、描述及防治建议。原来的两条 `/api/hardware/**` 接口完全保留，没有增加硬件接口。
- `service/PestImageModel.java` 定义模型适配边界，`DemoPestImageModel.java` 是演示实现；将来接入真实模型时替换实现。当前不会加载权重、不会进行视觉推理、不会调用外部AI服务。默认AUTO模式读取上传文件的SHA-256，与配套示例指纹匹配；其他图片可先选择手动演示场景再上传。分数不代表模型准确率或真实置信度。
- 保存的模拟识别记录描述带 `[模拟模型]` 前缀，并包含图片来源、采集时间、模拟模型名称和预设结果，便于与实地观测区分。图像采集、模型评分与环境阈值预警互不冒充。
- 页面组件为 `frontend/src/components/ImageDetectionDemo.vue`，由原观测页弹窗调用，不额外增加复杂导航或实验页面。

### 先上传好的杨梅，再上传坏的杨梅

项目根目录的 `演示上传图片` 文件夹已经备好两张配套模拟图片：

1. `01_健康杨梅.png`：默认自动模式上传后，显示绿色「健康杨梅：未发现明显异常」，仅展示日常管理建议，不允许登记为病害。
2. `02_异常杨梅.png`：接着上传，显示红色异常提示、果蝇演示分类和防治建议，可确认后保存为观测记录，并显示上次健康与本次异常的对比。

打开「模拟摄像头识别」，保持「自动匹配配套演示图片（推荐）」，通过「上传图片模拟识别」依次选择这两张图片。弹窗内也提供两张图片的下载链接。模拟摄像头自动模式依次取健康、果蝇等配套图片，上传过程不需要真实硬件。

匹配基于文件内容而非文件名，因此改名不影响配套图片的自动结果；重新压缩、裁剪或截图会改变指纹，不保证自动匹配。非配套图片默认显示「未匹配到演示样本」，不会一律判为异常；可手动指定健康或病虫害场景进行演示。这只是可复现的演示模型，不是能判断任意图片好坏的真实模型。指纹清单位于 `backend/src/main/resources/demo-image-fingerprints.json`。

## 扩展设备控制指令

「设备控制台」按光照、CO₂、通风、喷淋、灌溉分组提供12条指令。快捷按钮「加强光照」「降低光照」「增加CO₂」只选中对应指令，再点击「下发设备控制指令」提交；每次操作均保存设备编号、指令、来源账号和时间。

支持：开启/关闭补光灯、展开/收起遮阳网、开启/停止CO₂补充、开启/关闭通风、开启喷淋降湿/关闭喷淋、开启/停止灌溉。喷淋指令保留旧名称以兼容原接口，页面显示「开启喷淋」。前端目录在 `frontend/src/data/controlCommands.js`，后端白名单在 `common/ControlCommands.java`。

全部操作仍共用 `POST /api/hardware/device/control`，参数保持 `deviceSn` 与 `controlCommand`，不增加硬件API。当前仅保存操作日志，不自动改变环境读数，也不表示真实设备已执行；实际调节效果需结合硬件和后续采集反馈验证。

## 日常监测与设备接入调试的区分

设备控制台默认显示「自动采集数据」，按所选设备读取最新采集记录，每15秒刷新，并显示风险等级、采集时间和服务器接收时间。农户不需要输入温度、湿度等读数；后台任务每15秒产生环境数据并自动触发判断。真实传感器接入后，通过原有上传接口提供数据即可，网页仍只读展示。

旧的手动上报表单已移动到管理员可见、默认折叠的「设备接入调试」区域，仅用于联调。该区域不是日常监测的前置步骤，也没有更改两条原始硬件API的开放方式。当前尚未连接真实硬件，自动数据源仍是开发环境的后台任务；`SIMULATION_ENABLED=false` 时该任务停止。

现场演示以本版本的 `现场演示稿.md` 为准，普通农户流程为「查看自动采集→查看风险与曲线→人工确认观测→选择设备控制指令」。

### 图像页面操作简化

图像采集与识别窗口固定使用配套图片自动匹配，界面不再显示识别方式或指定结果的下拉框。直接选择果园，依次上传「演示上传图片」中的健康图与异常图即可。非配套图片显示未匹配提示，不会自动归为病害。接口兼容参数仍保留，网页固定传 `demoScenario=AUTO`。

### 示例果园封面

溪畔二号园与山麓一号园分别使用溪流果园和山麓果园封面，资源位于 `frontend/public/demo-orchards/`。图片由内置 image_gen 生成，属于虚构果园配图，不是对应真实地块的实拍；完整提示词见同目录 `generation.json`。首次初始化时自动关联封面，图片随前端打包，不依赖外部图床。

## 管理员用户管理

使用 `admin / 123456` 登录后，左侧出现独立的「用户管理」入口；农户菜单中没有该入口，直接访问其页面会返回监测大盘，账号管理接口也会校验管理员身份。

- 按用户名/昵称搜索、按角色与启停状态筛选、分页查看账号。
- 新增农户或管理员账号，设置初始密码、昵称、角色和启停状态。
- 编辑昵称、调整其他账号角色、启用或停用账号；不能停用或更改自己的角色，至少保留一个可用管理员。
- 重置密码，原密码及该账号旧登录会话失效。密码使用BCrypt保存，页面和接口都不返回密码或密码摘要。
- 查看注册时间、最近成功登录时间、累计成功登录次数、所属果园数量和有效登录会话数。有效会话不是实时在线人数，关闭浏览器不会自动注销登录凭据。
- 停用账号会立即阻止登录和受保护业务接口访问，但保留所属果园、设备及历史业务数据。角色变化也会使该账号已有会话失效，需要重新登录。开放的两条硬件原始接口保持原约定，不由用户账号状态替代未来的硬件认证。

管理API为 `GET/POST /api/admin/users`、`PUT /api/admin/users/{id}`、`PUT /api/admin/users/{id}/password`。不提供批量删除用户或连带清空果园的操作，账号退出使用停用状态管理。

已有旧版MySQL数据库升级前应备份，并执行 `backend/src/main/resources/db/migrate-user-management.sql` 一次。已有用户的原注册/登录时间无法还原，迁移后保留为空，后续登录开始更新统计；全新库及默认H2启动时自动创建字段，无需手动执行迁移。默认H2为内存数据库，重启后重新初始化演示账号和数据。
