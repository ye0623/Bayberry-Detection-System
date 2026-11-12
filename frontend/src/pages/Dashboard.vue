<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from "vue";
import * as echarts from "echarts";
import { api, time, riskType, orchardName } from "../api";
const rows = ref([]),
  warnings = ref([]),
  selected = ref(null),
  chartEl = ref(null),
  updated = ref(""),
  busy = ref(false);
const orchards = computed(() => rows.value.map((r) => r.orchard)),
  high = computed(
    () => rows.value.filter((r) => r.latest?.riskLevel === "高风险").length,
  );
let chart,
  timer,
  active = true,
  chartRequest = 0;
async function history() {
  const id = selected.value,
    request = ++chartRequest;
  if (!id) {
    chart?.clear();
    return;
  }
  const data = await api.get(`/orchards/${id}/history`);
  if (!active || request !== chartRequest) return;
  await nextTick();
  if (!chart) chart = echarts.init(chartEl.value);
  chart.setOption(
    {
      color: ["#2d826b", "#779dcc", "#c9a25b"],
      tooltip: { trigger: "axis" },
      legend: { bottom: 0, data: ["温度", "空气湿度", "土壤湿度"] },
      grid: { left: 50, right: 50, top: 30, bottom: 60 },
      xAxis: {
        type: "category",
        data: data.map((d) => time(d.collectTime).slice(11)),
        boundaryGap: false,
        axisLine: { lineStyle: { color: "#dce5df" } },
      },
      yAxis: [
        {
          type: "value",
          name: "温度 ℃",
          splitLine: { lineStyle: { color: "#eef2ee" } },
        },
        {
          type: "value",
          name: "湿度 %",
          min: 0,
          max: 100,
          splitLine: { show: false },
        },
      ],
      series: [
        {
          name: "温度",
          type: "line",
          smooth: true,
          showSymbol: false,
          data: data.map((d) => d.temperature),
          areaStyle: { opacity: 0.06 },
        },
        {
          name: "空气湿度",
          type: "line",
          smooth: true,
          showSymbol: false,
          yAxisIndex: 1,
          data: data.map((d) => d.humidity),
        },
        {
          name: "土壤湿度",
          type: "line",
          smooth: true,
          showSymbol: false,
          yAxisIndex: 1,
          data: data.map((d) => d.soilHumidity),
        },
      ],
    },
    true,
  );
}
async function load() {
  if (busy.value) return;
  busy.value = true;
  try {
    const [rs, ws] = await Promise.all([
      api.get("/dashboard"),
      api.get("/warnings"),
    ]);
    if (!active) return;
    rows.value = rs;
    warnings.value = ws;
    updated.value = new Date().toLocaleTimeString();
    if (!rs.some((r) => r.orchard.id === selected.value))
      selected.value = rs[0]?.orchard.id || null;
    await history();
  } finally {
    busy.value = false;
  }
}
async function handle(w) {
  await api.put("/warnings/" + w.id + "/handle");
  await load();
}
function resize() {
  chart?.resize();
}
watch(selected, () => history().catch(() => {}));
onMounted(() => {
  load();
  timer = setInterval(() => load().catch(() => {}), 15000);
  window.addEventListener("resize", resize);
});
onUnmounted(() => {
  active = false;
  clearInterval(timer);
  window.removeEventListener("resize", resize);
  chart?.dispose();
});
</script>
<template>
  <div class="overview-banner">
    <div>
      <i class="live-dot" /><b>果园环境持续监测中</b>
      <p>每 15 秒刷新 · 最后更新 {{ updated || "加载中" }}</p>
    </div>
    <el-button :loading="busy" @click="load">刷新数据 ↻</el-button>
  </div>
  <div class="stats">
    <div>
      <span>管理果园</span><strong>{{ rows.length }}<small>个</small></strong>
      <p>独立地块与风险规则</p>
    </div>
    <div>
      <span>注册设备</span
      ><strong
        >{{ rows.reduce((s, r) => s + r.deviceCount, 0)
        }}<small>台</small></strong
      >
      <p>数据采集任务已启用</p>
    </div>
    <div :class="{ danger: high }">
      <span>当前高风险果园</span><strong>{{ high }}<small>个</small></strong>
      <p>依据各果园最新采集数据</p>
    </div>
    <div>
      <span>待处理预警</span
      ><strong
        >{{ warnings.filter((w) => !w.handled).length
        }}<small>条</small></strong
      >
      <p>最近 300 条预警中的待处理项</p>
    </div>
  </div>
  <div class="section-title">
    <h3>果园实时环境</h3>
    <span>最新采集 · 按果园展示</span>
  </div>
  <el-empty
    v-if="!rows.length"
    description="暂无果园，请先创建果园并注册设备"
  />
  <div class="environment-grid">
    <div
      v-for="r in rows"
      :key="r.orchard.id"
      class="panel environment-card"
      :class="{ 'high-risk': r.latest?.riskLevel === '高风险' }"
    >
      <div class="panel-heading">
        <div>
          <h3>{{ r.orchard.name }}</h3>
          <small class="muted">{{ r.orchard.location }}</small>
        </div>
        <el-tag
          v-if="r.latest"
          :type="riskType(r.latest.riskLevel)"
          effect="dark"
          >{{ r.latest.riskLevel }}</el-tag
        ><el-tag v-else type="info">待采集</el-tag>
      </div>
      <div v-if="r.latest" class="readings">
        <div
          v-for="[key, label, unit] in [
            ['temperature', '温度', '℃'],
            ['humidity', '空气湿度', '%'],
            ['soilHumidity', '土壤湿度', '%'],
            ['light', '光照', 'lx'],
            ['co2', 'CO₂', 'ppm'],
          ]"
          :key="key"
        >
          <small>{{ label }}</small
          ><b
            >{{ r.latest[key] }}<em>{{ unit }}</em></b
          >
        </div>
      </div>
      <p class="data-time">采集时间 {{ time(r.latest?.collectTime) }}</p>
    </div>
  </div>
  <div class="panel">
    <div class="panel-heading">
      <div>
        <h3>环境变化趋势</h3>
        <small class="muted">最近 120 次采集 · 双轴时序曲线</small>
      </div>
      <el-select v-model="selected" placeholder="选择果园" style="width: 240px"
        ><el-option
          v-for="o in orchards"
          :key="o.id"
          :label="o.name"
          :value="o.id"
      /></el-select>
    </div>
    <div ref="chartEl" class="chart" />
    <p class="muted" v-if="!selected">添加果园和设备后将在此显示环境曲线。</p>
  </div>
  <div class="panel">
    <div class="panel-heading">
      <h3>病虫害风险预警</h3>
      <span class="muted">最近 300 条 · 每次命中独立留痕</span>
    </div>
    <el-table :data="warnings" max-height="480" empty-text="暂无风险预警"
      ><el-table-column label="风险" width="105"
        ><template #default="{ row }"
          ><el-tag
            :type="riskType(row.riskLevel)"
            :effect="row.riskLevel === '高风险' ? 'dark' : 'light'"
            >{{ row.riskLevel }}</el-tag
          ></template
        ></el-table-column
      ><el-table-column label="果园" min-width="160"
        ><template #default="{ row }">{{
          orchardName(orchards, row.orchardId)
        }}</template></el-table-column
      ><el-table-column
        prop="pestType"
        label="预警类型"
        width="110"
      /><el-table-column
        prop="reason"
        label="触发原因"
        min-width="230"
      /><el-table-column
        prop="suggestion"
        label="防治建议"
        min-width="250"
      /><el-table-column label="时间" min-width="170"
        ><template #default="{ row }">{{
          time(row.createdAt)
        }}</template></el-table-column
      ><el-table-column label="处理" width="110"
        ><template #default="{ row }"
          ><el-tag v-if="row.handled" type="info">已处理</el-tag
          ><el-button v-else text type="primary" @click="handle(row)"
            >标记处理</el-button
          ></template
        ></el-table-column
      ></el-table
    >
  </div>
</template>
