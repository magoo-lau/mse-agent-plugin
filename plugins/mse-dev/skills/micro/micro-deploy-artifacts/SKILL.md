---
name: micro-deploy-artifacts
description: >
  为 mse 业务工程生成/调整 Dockerfile 与 k8s 部署清单（多阶段构建，context 为仓库根，
  改 MODULE/PORT 即可）。当用户要容器化业务服务、构建镜像、部署到 k8s、调整副本/资源/探针时使用。
---

# micro-deploy-artifacts —— Dockerfile 与 k8s 清单

模板位置：`<插件根>/templates/micro/{{biz}}-boot/Dockerfile` 与 `k8s/{{biz}}.yaml`（渲染自 micro-create-project 时已随工程生成）；单体版见 `templates/mono/`。

## Dockerfile 要点（保持模板体，只改参数）

- `ARG MODULE=<工程相对仓库根路径>`（如 `mse-modules/mse-crm/crm-boot`）——构建命令 context 必须是 **仓库根**（能同时看到业务工程与基座模块的聚合根目录）：
  `docker build -f <工程>/crm-boot/Dockerfile -t mse/crm-boot:1.0.0 .`
- `ARG PORT=<端口>` 与 Nacos 留档 yaml 的 server.port 一致。
- 本地已 `mvn package` 快速出镜像：改用 `mse-framework/mse-boot/Dockerfile.prebuilt`（通用）。
- 已内置：BuildKit .m2 缓存、jarmode 分层、非 root 运行、exec java 优雅停机、MaxRAMPercentage。

## k8s 清单要点

- Deployment + Service（ClusterIP），name/image 按工程改：`mse/<biz>-boot:<版本>`。
- `envFrom configMapRef mse-config`（基座共享配置，必用 MSE_NACOS_ADDR）。
- containerPort/port 与 ARG PORT 一致；探针 exec+curl 访问 /actuator/health 并携带 X-Actuator-Token: ${MSE_ACTUATOR_TOKEN} 头（模板已内置该环境变量；readiness failureThreshold=20 给冷启动窗口）。
- 部署前置：业务 SQL 已执行、Nacos 基线已导入（micro-nacos-config）、网关路由已登记（micro-gateway-route）。

## 常见调整

| 需求 | 改法 |
| --- | --- |
| 副本数/资源 | spec.replicas、resources.requests/limits（内存 limit 与 MaxRAMPercentage 联动） |
| 预发/生产镜像 | image 字段按实际仓库改（如 registry.example.com/mse/crm-boot:1.2.0） |
| JVM 参数 | 容器 env JAVA_OPTS 覆盖 |
| 滚动更新卡住 | 检查 readinessProbe 与 Nacos 配置导入是否就绪 |

## 验证

```bash
docker build -f <工程>/<biz>-boot/Dockerfile -t mse/<biz>-boot:1.0.0 .   # context=仓库根
docker run --rm -e MSE_NACOS_ADDR=<nacos> -p <port>:<port> mse/<biz>-boot:1.0.0
kubectl apply -f <工程>/<biz>-boot/k8s/<biz>.yaml && kubectl get pod -l app=mse-<biz>
```
