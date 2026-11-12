<script setup>
import { computed, ref, onMounted, onUnmounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { api } from "./api";
const route = useRoute(),
  router = useRouter(),
  collapsed = ref(false);
const accountVersion = ref(0);
const accountUpdated = () => accountVersion.value++;
onMounted(() => window.addEventListener("account-updated", accountUpdated));
onUnmounted(() =>
  window.removeEventListener("account-updated", accountUpdated),
);
const user = computed(() => {
  accountVersion.value;
  route.path;
  try {
    return JSON.parse(localStorage.getItem("user") || "{}");
  } catch {
    return {};
  }
});
const nav = [
  ["dashboard", "◫", "监测大盘"],
  ["orchards", "▤", "杨梅果园"],
  ["devices", "⌁", "监测设备"],
  ["simulator", "⏣", "设备控制台"],
  ["observations", "◎", "病虫害观测"],
  ["thresholds", "≋", "阈值配置"],
  ["announcements", "◷", "果园公告"],
];
const visibleNav = computed(() =>
  user.value.role === "ADMIN" ? [...nav, ["users", "♙", "用户管理"]] : nav,
);
async function logout() {
  try {
    await api.post("/auth/logout");
  } finally {
    localStorage.clear();
    router.push("/login");
  }
}
</script>
<template>
  <router-view v-if="route.path === '/login'" />
  <div v-else class="shell">
    <aside :class="{ open: collapsed }">
      <div class="brand">
        <span class="brand-mark">梅</span>
        <div>梅境<span>BAYBERRY ORCHARD</span></div>
      </div>
      <div class="nav-caption">
        {{ user.role === "ADMIN" ? "平台管理工作台" : "果园工作台" }}
      </div>
      <nav>
        <router-link
          v-for="[path, icon, label] in visibleNav"
          :key="path"
          :to="'/' + path"
          @click="collapsed = false"
          ><span class="nav-icon">{{ icon }}</span
          >{{ label }}</router-link
        >
      </nav>
      <div class="sidebar-bottom">
        <i class="live-dot" /> 果园运行监测<br /><small
          >采集任务每 15 秒更新</small
        >
      </div>
    </aside>
    <main>
      <header>
        <div>
          <el-button class="mobile-nav" @click="collapsed = !collapsed"
            >菜单</el-button
          ><span class="breadcrumb">果园工作台 / {{ route.meta.title }}</span>
        </div>
        <div class="account">
          <el-tag type="success" effect="plain">{{
            user.role === "ADMIN" ? "管理员" : "果园农户"
          }}</el-tag
          ><span>{{ user.nickname || user.username }}</span
          ><el-button text @click="logout">退出</el-button>
        </div>
      </header>
      <section class="workspace">
        <div class="page-heading">
          <div>
            <span class="eyebrow">BAYBERRY INTELLIGENCE</span>
            <h1>{{ route.meta.title }}</h1>
            <p>{{ route.meta.subtitle }}</p>
          </div>
          <span class="date-label">杨梅果园 · IoT</span>
        </div>
        <router-view />
      </section>
      <footer>
        梅境 · 杨梅果园物联网病虫害预警平台
        <span>环境风险提示需结合实地巡园确认</span>
      </footer>
    </main>
  </div>
</template>
