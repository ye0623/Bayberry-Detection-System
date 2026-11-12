import { createRouter, createWebHistory } from "vue-router";
const Login = () => import("./pages/Login.vue");
const Dashboard = () => import("./pages/Dashboard.vue");
const Orchards = () => import("./pages/Orchards.vue");
const Devices = () => import("./pages/Devices.vue");
const Simulator = () => import("./pages/Simulator.vue");
const Observations = () => import("./pages/Observations.vue");
const Thresholds = () => import("./pages/Thresholds.vue");
const Announcements = () => import("./pages/Announcements.vue");
const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: "/users",
      component: () => import("./pages/UserManagement.vue"),
      meta: {
        title: "用户管理",
        subtitle: "管理平台账号、角色权限、登录状态与密码。",
        requiresAdmin: true,
      },
    },
    { path: "/login", component: Login },
    { path: "/", redirect: "/dashboard" },
    {
      path: "/dashboard",
      component: Dashboard,
      meta: {
        title: "果园监测大盘",
        subtitle: "实时感知环境，及时识别杨梅病虫害诱发风险。",
      },
    },
    {
      path: "/orchards",
      component: Orchards,
      meta: { title: "杨梅果园", subtitle: "管理地块、种植信息与果实期状态。" },
    },
    {
      path: "/devices",
      component: Devices,
      meta: {
        title: "监测设备",
        subtitle: "连接果园与环境数据，管理设备档案。",
      },
    },
    {
      path: "/simulator",
      component: Simulator,
      meta: {
        title: "设备控制台",
        subtitle: "查看设备自动采集数据，管理设备控制操作。",
      },
    },
    {
      path: "/observations",
      component: Observations,
      meta: {
        title: "病虫害观测",
        subtitle: "记录实地巡园情况，让环境预警与田间观测相互印证。",
      },
    },
    {
      path: "/thresholds",
      component: Thresholds,
      meta: {
        title: "果园阈值配置",
        subtitle: "按地块配置环境范围与病虫害诱发条件。",
      },
    },
    {
      path: "/announcements",
      component: Announcements,
      meta: { title: "果园公告", subtitle: "杨梅种植与病虫害防治知识。" },
    },
    { path: "/:pathMatch(.*)*", redirect: "/dashboard" },
  ],
});
router.beforeEach((to) => {
  if (to.path != "/login" && !localStorage.getItem("token")) return "/login";
  if (to.meta.requiresAdmin) {
    try {
      if (JSON.parse(localStorage.getItem("user") || "{}").role !== "ADMIN")
        return "/dashboard";
    } catch {
      return "/login";
    }
  }
});
export default router;
