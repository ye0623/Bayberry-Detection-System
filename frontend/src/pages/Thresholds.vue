<script setup>
import { ref, reactive, onMounted, watch } from "vue";
import { api } from "../api";
import { ElMessage } from "element-plus";
const orchards = ref([]),
  selected = ref(null),
  form = reactive({}),
  busy = ref(false),
  loading = ref(false);
const groups = [
  {
    title: "环境告警范围",
    note: "任意指标越界 → 中等风险",
    fields: [
      ["temperatureMin", "温度下限 (℃)", -40, 80],
      ["temperatureMax", "温度上限 (℃)", -40, 80],
      ["humidityMax", "空气湿度上限 (%)", 0, 100],
      ["soilHumidityMax", "土壤湿度上限 (%)", 0, 100],
      ["lightMin", "光照下限 (lx)", 0, 200000],
      ["lightMax", "光照上限 (lx)", 0, 200000],
      ["co2Max", "CO₂ 上限 (ppm)", 0, 10000],
    ],
  },
  {
    title: "褐斑病诱发阈值",
    note: "温度在闭区间内，且空气湿度严格大于阈值 → 高风险",
    fields: [
      ["brownTempMin", "温度下限 (℃)", -40, 80],
      ["brownTempMax", "温度上限 (℃)", -40, 80],
      ["brownHumidity", "空气湿度阈值 (%)", 0, 100],
    ],
  },
  {
    title: "果蝇诱发阈值",
    note: "果实期 + 温度在闭区间内 + 空气湿度严格大于阈值 → 高风险",
    fields: [
      ["flyTempMin", "温度下限 (℃)", -40, 80],
      ["flyTempMax", "温度上限 (℃)", -40, 80],
      ["flyHumidity", "空气湿度阈值 (%)", 0, 100],
    ],
  },
  {
    title: "癌肿病诱发阈值",
    note: "空气湿度与土壤湿度同时严格大于阈值 → 高风险",
    fields: [
      ["cancerHumidity", "空气湿度阈值 (%)", 0, 100],
      ["cancerSoil", "土壤湿度阈值 (%)", 0, 100],
    ],
  },
];
let request = 0;
async function load() {
  const n = ++request;
  if (!selected.value) return;
  loading.value = true;
  try {
    const r = await api.get(`/orchards/${selected.value}/threshold`);
    if (n === request) Object.assign(form, r);
  } finally {
    if (n === request) loading.value = false;
  }
}
async function save() {
  busy.value = true;
  try {
    await api.put(`/orchards/${selected.value}/threshold`, form);
    ElMessage.success("阈值已保存，将在下一次上报时生效");
  } finally {
    busy.value = false;
  }
}
watch(selected, load);
onMounted(async () => {
  orchards.value = await api.get("/orchards");
  selected.value = orchards.value[0]?.id;
});
</script>
<template>
  <div class="toolbar">
    <el-select
      v-model="selected"
      placeholder="选择杨梅果园"
      style="width: 300px"
      ><el-option
        v-for="o in orchards"
        :key="o.id"
        :label="o.name"
        :value="o.id" /></el-select
    ><el-button
      type="primary"
      :disabled="!selected || loading"
      :loading="busy"
      @click="save"
      >保存本果园阈值</el-button
    >
  </div>
  <el-empty v-if="!selected" description="请先创建杨梅果园" /><template v-else
    ><el-alert
      class="notice"
      type="info"
      title="规则说明：所有病虫害条件均未命中且环境正常时为低风险；命中任意病虫害条件为高风险。保存阈值不会重算历史记录。"
      :closable="false"
    />
    <div v-loading="loading" class="threshold-grid">
      <div v-for="g in groups" :key="g.title" class="panel">
        <h3>{{ g.title }}</h3>
        <p class="muted">{{ g.note }}</p>
        <el-form label-position="top" class="sensor-form"
          ><el-form-item
            v-for="[key, label, min, max] in g.fields"
            :key="key"
            :label="label"
            ><el-input-number
              v-model="form[key]"
              :min="min"
              :max="max"
              :precision="1"
              controls-position="right" /></el-form-item
        ></el-form>
      </div>
    </div>
    <p class="muted">
      癌肿病风险由空气与土壤湿度辅助判断，当前未采集降雨数据。卷叶蛾暂不按环境阈值自动判断，请通过实地观测记录管理。
    </p></template
  >
</template>
