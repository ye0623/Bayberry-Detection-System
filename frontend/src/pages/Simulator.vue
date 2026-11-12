<script setup>
// Web 模拟器只调用两条已有硬件原始接口，不创建额外的硬件入口。
import { ref, reactive, onMounted, onUnmounted, computed, watch } from "vue";
import { api, time, riskType } from "../api";
import { ElMessage } from "element-plus";
import {
  controlCommandGroups,
  allControlCommands,
  quickControlCommands,
} from "../data/controlCommands";
const devices = ref([]),
  logs = ref([]),
  selected = ref(""),
  command = ref("开启通风"),
  busy = ref(false),
  sending = ref(false),
  result = ref(null),
  latest = ref(null),
  readingLoading = ref(false),
  refreshing = ref(false),
  refreshedAt = ref("");
const admin = JSON.parse(localStorage.getItem("user") || "{}").role === "ADMIN";
const selectedDevice = computed(() =>
  devices.value.find((device) => device.deviceSn === selected.value),
);
const environment = reactive({
  temperature: 26,
  humidity: 90,
  soilHumidity: 88,
  light: 35000,
  co2: 450,
});
const fields = [
  ["temperature", "温度", "℃", -40, 80],
  ["humidity", "空气湿度", "%", 0, 100],
  ["soilHumidity", "土壤湿度", "%", 0, 100],
  ["light", "光照", "lx", 0, 200000],
  ["co2", "CO₂", "ppm", 0, 10000],
];
const selectedControl = computed(() =>
  allControlCommands.find((item) => item.value === command.value),
);
let timer,
  active = true,
  historyVersion = 0;
async function loadReading() {
  const version = ++historyVersion;
  const device = selectedDevice.value;
  if (!device) {
    latest.value = null;
    readingLoading.value = false;
    return;
  }
  readingLoading.value = true;
  try {
    const rows = await api.get(`/orchards/${device.orchardId}/history`);
    if (!active || version !== historyVersion) return;
    // 原接口按果园返回历史。这里只显示选中设备的数据，不混用同果园其他设备的读数。
    latest.value =
      rows.filter((row) => row.deviceId === device.id).at(-1) || null;
    refreshedAt.value = new Date().toLocaleTimeString();
  } finally {
    if (active && version === historyVersion) readingLoading.value = false;
  }
}
async function load() {
  if (refreshing.value) return;
  refreshing.value = true;
  try {
    const [ds, ls] = await Promise.all([
      api.get("/devices"),
      api.get("/control-logs"),
    ]);
    if (!active) return;
    const previous = selected.value;
    devices.value = ds;
    logs.value = ls;
    if (!ds.some((d) => d.deviceSn === selected.value))
      selected.value = ds[0]?.deviceSn || "";
    if (previous === selected.value) await loadReading();
  } finally {
    if (active) refreshing.value = false;
  }
}
watch(selected, () => {
  latest.value = null;
  result.value = null;
  loadReading().catch(() => {});
});
function scenario(high) {
  Object.assign(
    environment,
    high
      ? {
          temperature: 26,
          humidity: 92,
          soilHumidity: 90,
          light: 30000,
          co2: 460,
        }
      : {
          temperature: 20,
          humidity: 65,
          soilHumidity: 55,
          light: 35000,
          co2: 430,
        },
  );
}
async function upload() {
  busy.value = true;
  try {
    // 采用本地时间字符串，避免 toISOString() 引入 UTC 时差。
    const now = new Date();
    const collectTime = new Date(
      now.getTime() - now.getTimezoneOffset() * 60000,
    )
      .toISOString()
      .slice(0, 19);
    result.value = await api.post("/hardware/sensor/upload", {
      deviceSn: selected.value,
      ...environment,
      collectTime,
    });
    ElMessage.success("环境数据上报成功，已完成风险判定");
    await loadReading();
  } finally {
    busy.value = false;
  }
}
async function control() {
  sending.value = true;
  try {
    await api.post("/hardware/device/control", {
      deviceSn: selected.value,
      controlCommand: command.value,
    });
    ElMessage.success("控制指令已记入日志");
    logs.value = await api.get("/control-logs");
  } finally {
    sending.value = false;
  }
}
onMounted(() => {
  load().catch(() => {});
  timer = setInterval(() => load().catch(() => {}), 15000);
});
onUnmounted(() => {
  active = false;
  historyVersion++;
  clearInterval(timer);
});
</script>
<template>
  <el-alert
    class="notice"
    title="采集数据自动入库并判断风险，本页查看最新读数和设备控制记录，无需填写环境数值。"
    type="info"
    show-icon
    :closable="false"
  />
  <div class="panel device-picker">
    <span class="section-index">01</span>
    <div>
      <b>选择监测设备</b>
      <p>使用已经注册并绑定果园的设备编号</p>
    </div>
    <el-select
      v-model="selected"
      placeholder="请先注册设备"
      style="width: 320px"
      ><el-option
        v-for="d in devices"
        :key="d.id"
        :label="d.deviceSn + ' · ' + d.location"
        :value="d.deviceSn" /></el-select
    ><el-button :loading="refreshing" @click="load">刷新</el-button>
  </div>
  <div class="two-columns">
    <div
      class="panel automatic-data"
      v-loading="readingLoading"
      :data-reading-id="latest?.id"
    >
      <div class="panel-heading">
        <div>
          <h3>自动采集数据</h3>
          <small class="muted"
            >每15秒刷新 ·
            {{ refreshedAt ? "页面更新 " + refreshedAt : "等待数据" }}</small
          >
        </div>
        <el-tag
          v-if="latest"
          :type="riskType(latest.riskLevel)"
          effect="dark"
          >{{ latest.riskLevel }}</el-tag
        >
      </div>
      <div class="collection-flow">
        <span>采集数据</span><i>→</i><span>自动入库</span><i>→</i
        ><span>风险判断</span>
      </div>
      <template v-if="latest"
        ><div class="automatic-readings">
          <div v-for="[key, label, unit] in fields" :key="key">
            <span>{{ label }}</span
            ><strong
              >{{ latest[key] }}<small>{{ unit }}</small></strong
            >
          </div>
        </div>
        <div class="collection-times">
          <p>采集时间：{{ time(latest.collectTime) }}</p>
          <p>接收时间：{{ time(latest.receivedTime) }}</p>
        </div>
        <p class="muted">
          当前等级由该设备最近一次采集数据计算；具体触发原因和历史曲线可在监测大盘查看。
        </p></template
      >
      <el-empty
        v-else
        :description="
          selected ? '等待该设备自动上报数据' : '请先注册并选择监测设备'
        "
        :image-size="80"
      />
    </div>
    <div class="panel">
      <div class="panel-heading">
        <h3>设备控制</h3>
        <span class="pill">DEVICE CONTROL</span>
      </div>
      <p class="muted">下发控制指令，查看平台保存的操作日志。</p>
      <div class="command-shortcuts">
        <span>快捷选择</span
        ><el-button
          v-for="item in quickControlCommands"
          :key="item.value"
          size="small"
          :disabled="sending"
          @click="command = item.value"
          >{{ item.label }}</el-button
        >
      </div>
      <el-form label-position="top"
        ><el-form-item label="设备控制指令"
          ><el-select v-model="command"
            ><el-option-group
              v-for="group in controlCommandGroups"
              :key="group.label"
              :label="group.label"
              ><el-option
                v-for="item in group.commands"
                :key="item.value"
                :label="item.label"
                :value="
                  item.value
                " /></el-option-group></el-select></el-form-item
      ></el-form>
      <p class="muted command-purpose">{{ selectedControl?.hint }}</p>
      <el-button
        type="primary"
        :disabled="!selected"
        :loading="sending"
        @click="control"
        >下发设备控制指令</el-button
      >
      <div class="endpoint">POST /api/hardware/device/control</div>
      <div class="control-note">
        <b>操作说明</b>
        <p>当前仅保存控制操作日志；不会驱动真实设备，也不会修改环境读数。</p>
        <p>
          “开启喷淋降湿”为系统约定指令文本，实际效果取决于未来硬件与现场工况。
        </p>
      </div>
    </div>
  </div>
  <div v-if="admin" class="panel debug-area">
    <el-collapse
      ><el-collapse-item title="设备接入调试（管理员）" name="sensor-upload">
        <div class="panel-heading">
          <h3>环境上报接口调试</h3>
          <span class="pill">SENSOR UPLOAD</span>
        </div>
        <p class="muted">
          此表单用于设备接入联调；日常环境监测由采集任务自动完成，无需人工填数。
        </p>
        <div class="scenario-buttons">
          <el-button size="small" @click="scenario(false)"
            >填入低风险场景</el-button
          ><el-button size="small" type="danger" plain @click="scenario(true)"
            >填入高风险场景</el-button
          >
        </div>
        <el-form label-position="top" class="sensor-form"
          ><el-form-item
            v-for="[key, label, unit, min, max] in fields"
            :key="key"
            :label="label + ' (' + unit + ')'"
            ><el-input-number
              v-model="environment[key]"
              :min="min"
              :max="max"
              :precision="1"
              controls-position="right" /></el-form-item></el-form
        ><el-button
          type="primary"
          :disabled="!selected"
          :loading="busy"
          @click="upload"
          >上报果园环境数据</el-button
        >
        <div class="endpoint">POST /api/hardware/sensor/upload</div>
        <el-alert
          v-if="result"
          :title="
            '上报成功 · ' +
            result.data.riskLevel +
            ' · 生成 ' +
            result.warnings.length +
            ' 条预警'
          "
          :type="result.data.riskLevel === '高风险' ? 'error' : 'success'"
          :closable="false"
        />
        <div v-if="result?.warnings.length" class="result-lines">
          <p v-for="w in result.warnings" :key="w.pestType">
            <b>{{ w.pestType }}</b> · {{ w.reason }}
          </p>
        </div>
      </el-collapse-item></el-collapse
    >
  </div>
  <div class="panel">
    <div class="panel-heading">
      <h3>硬件控制操作历史</h3>
      <span class="muted">最近 200 条</span>
    </div>
    <el-table :data="logs" empty-text="尚无控制操作"
      ><el-table-column prop="deviceSn" label="设备编号" /><el-table-column
        prop="controlCommand"
        label="控制指令"
      /><el-table-column prop="operatorName" label="操作来源" /><el-table-column
        label="下发时间"
        min-width="180"
        ><template #default="{ row }">{{
          time(row.createdAt)
        }}</template></el-table-column
      ><el-table-column label="结果"
        ><template #default
          ><el-tag type="success">日志已保存</el-tag></template
        ></el-table-column
      ></el-table
    >
  </div>
</template>

<style scoped>
.automatic-readings {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 22px 16px;
  margin: 25px 0;
}
.automatic-readings > div {
  background: #f5f8f0;
  border-radius: 8px;
  padding: 17px;
}
.automatic-readings span {
  display: block;
  color: #84967c;
  font-size: 12px;
  margin-bottom: 12px;
}
.automatic-readings strong {
  font-weight: 500;
  font-size: 28px;
  color: #2b5942;
}
.automatic-readings small {
  font-size: 11px;
  margin-left: 5px;
  color: #8f9b86;
}
.collection-flow {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  background: #edf3e6;
  border-radius: 7px;
  padding: 13px;
  font-size: 11px;
  color: #6e8860;
  margin-top: 20px;
}
.collection-flow i {
  font-style: normal;
  color: #a4b598;
}
.collection-times {
  font-size: 11px;
  color: #9aa58f;
  border-top: 1px solid #eaf0e2;
  padding-top: 14px;
  line-height: 1.8;
}
.collection-times p {
  margin-bottom: 7px;
}
.debug-area :deep(.el-collapse) {
  border: none;
}
.debug-area :deep(.el-collapse-item__header) {
  border: none;
  font-weight: 500;
  color: #57734a;
}
.debug-area :deep(.el-collapse-item__wrap) {
  border: none;
}
.debug-area .sensor-form {
  max-width: 650px;
}
.command-shortcuts {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin: 18px 0;
}
.command-shortcuts > span {
  font-size: 11px;
  color: #87957e;
  margin-right: 3px;
}
.command-shortcuts .el-button + .el-button {
  margin-left: 0;
}
.command-purpose {
  min-height: 42px;
  margin: 0 0 16px;
}
</style>
