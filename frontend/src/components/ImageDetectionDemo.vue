<script setup>
import { ref, computed, watch, onUnmounted } from "vue";
import { api, uploadFile, time } from "../api";
import { pestExamples } from "../data/pestExamples";
import { ElMessage } from "element-plus";
import { businessText } from "../presentation";
const props = defineProps({
  modelValue: Boolean,
  orchards: { type: Array, default: () => [] },
});
const emit = defineEmits(["update:modelValue", "confirm"]);
const orchardId = ref(null),
  frame = ref(null),
  result = ref(null),
  busy = ref(false),
  recent = ref([]),
  uploadInput = ref(null),
  previousResult = ref(null);
const healthySample = { type: "健康", image: "/demo-pests/healthy.png" };
const captureSamples = [
  healthySample,
  pestExamples.find((e) => e.type === "果蝇"),
  ...pestExamples.filter((e) => e.type !== "果蝇"),
];
let captureIndex = 0;
const resultKind = computed(() => result.value?.healthStatus);
const resultColor = computed(() =>
  resultKind.value === "HEALTHY"
    ? "#398368"
    : resultKind.value === "ABNORMAL"
      ? "#c14c52"
      : "#8c9685",
);
const cameraSn = computed(() =>
  orchardId.value
    ? "IMG-SOURCE-" + String(orchardId.value).padStart(3, "0")
    : "请选择果园",
);
let requestVersion = 0;
watch(
  () => props.modelValue,
  (open) => {
    requestVersion++;
    busy.value = false;
    if (open) {
      orchardId.value = props.orchards[0]?.id || null;
      frame.value = null;
      result.value = null;
      previousResult.value = null;
    }
  },
);
watch(orchardId, () => {
  frame.value = null;
  result.value = null;
});
onUnmounted(() => {
  requestVersion++;
});
function localTime() {
  const d = new Date();
  return new Date(d.getTime() - d.getTimezoneOffset() * 60000)
    .toISOString()
    .slice(0, 19);
}
// 模拟传感器使用内置图片作为采集帧。它不调用电脑摄像头，不增加硬件API。
async function acquire(file = null) {
  if (busy.value || !orchardId.value) return;
  const version = ++requestVersion,
    plot = orchardId.value,
    scene = "AUTO",
    sn = cameraSn.value;
  busy.value = true;
  if (result.value) previousResult.value = result.value;
  result.value = null;
  try {
    const cameraSample = file ? null : captureSamples[captureIndex++ % captureSamples.length];
    const imageUrl = file ? (await uploadFile(file)).url : cameraSample.image;
    if (version !== requestVersion) return;
    const captured = {
      orchardId: plot,
      imageUrl,
      capturedAt: localTime(),
      source: file ? "upload" : "camera",
      cameraSn: sn,
      scenario: scene,
    };
    frame.value = captured;
    recent.value = [captured, ...recent.value].slice(0, 4);
    // 后端返回的是演示适配器的预设分类；动画等待只用于展示处理过程。
    const [prediction] = await Promise.all([
      api.post("/observations/model/predict", {
        orchardId: plot,
        imageUrl,
        demoScenario: scene,
      }),
      new Promise((resolve) => setTimeout(resolve, 650)),
    ]);
    if (version === requestVersion) {
      result.value = prediction;
      frame.value.scenario = prediction.pestType;
      recent.value[0].scenario = prediction.pestType;
    }
  } catch {
    if (version === requestVersion) result.value = null;
  } finally {
    if (version === requestVersion) busy.value = false;
  }
}
async function upload(event) {
  const file = event.target.files?.[0];
  event.target.value = "";
  if (!file) return;
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning("请选择5MB以内的JPG或PNG图片");
    return;
  }
  await acquire(file);
}
function confirm() {
  if (!result.value?.recordable || !frame.value || busy.value) return;
  const r = result.value,
    f = frame.value;
  emit("confirm", {
    orchardId: f.orchardId,
    pestType: r.pestType,
    severity: r.severity,
    imageUrl: f.imageUrl,
    description: `[模拟模型] 来源：${f.source === "camera" ? "虚拟图像传感器 " + f.cameraSn : "手动上传图片"}；采集时间：${time(f.capturedAt)}。${r.modelName}预设演示分类：${r.pestType}，模拟评分${r.demoScore}%（不代表真实识别准确率）。${r.symptom}`,
  });
  emit("update:modelValue", false);
}
</script>
<template>
  <el-dialog
    :model-value="modelValue"
    title="图像采集与识别"
    width="940px"
    :close-on-click-modal="false"
    @update:model-value="(v) => emit('update:modelValue', v)"
  >
    <p class="demo-explanation">
      先上传健康图，再上传异常图，展示绿色健康结果与红色病害结果。
    </p>
    <div class="demo-downloads">
      <span>参考图片下载：</span
      ><a href="/demo-pests/healthy.png" download="01_健康杨梅.png"
        >下载健康图</a
      ><a href="/demo-pests/fruit-fly.png" download="02_异常杨梅.png"
        >下载异常图</a
      ><small>改名不影响匹配，请使用配套原始图片。</small>
    </div>
    <el-form label-position="top" class="demo-settings"
      ><el-form-item label="所属杨梅果园"
        ><el-select v-model="orchardId" :disabled="busy" placeholder="选择果园"
          ><el-option
            v-for="o in orchards"
            :key="o.id"
            :value="o.id"
            :label="o.name" /></el-select></el-form-item
    ></el-form>
    <div class="detection-grid">
      <section class="capture-card">
        <div class="capture-heading">
          <div>
            <b>图像采集来源</b><small>{{ cameraSn }}</small>
          </div>
          <el-tag
            :type="busy ? 'warning' : frame ? 'success' : 'info'"
            effect="plain"
            >{{ busy ? "处理中" : frame ? "已采集" : "待采集" }}</el-tag
          >
        </div>
        <div
          class="camera-preview"
          v-loading="busy"
          element-loading-text="正在分析图片…"
        >
          <el-image
            v-if="frame"
            :src="frame.imageUrl"
            :preview-src-list="[frame.imageUrl]"
            preview-teleported
            fit="contain"
          />
          <div v-else class="camera-placeholder">
            <span>▣</span><b>等待图像采集</b>
            <p>采集参考图片，或上传本地杨梅图片</p>
          </div>
          <span v-if="frame?.source === 'camera'" class="camera-overlay"
            >IMAGE · 参考图片</span
          >
        </div>
        <p class="capture-time">
          {{
            frame
              ? "采集时间：" + time(frame.capturedAt)
              : "图片来源：参考图片集 / 本地上传"
          }}
        </p>
        <div class="capture-buttons">
          <el-button
            type="primary"
            :disabled="!orchardId"
            :loading="busy"
            @click="acquire()"
            >采集图片并分析</el-button
          ><el-button
            :disabled="!orchardId || busy"
            @click="uploadInput.click()"
            >上传图片并分析</el-button
          ><input
            ref="uploadInput"
            type="file"
            accept="image/png,image/jpeg"
            hidden
            @change="upload"
          />
        </div>
      </section>
      <section class="model-card">
        <div class="capture-heading">
          <div><b>病虫害图像分析</b><small>样例匹配服务</small></div>
          <el-tag type="warning" effect="plain">样例匹配</el-tag>
        </div>
        <div
          v-if="result"
          class="model-result"
          :class="{
            'healthy-result': resultKind === 'HEALTHY',
            'abnormal-result': resultKind === 'ABNORMAL',
          }"
        >
          <el-alert
            class="health-verdict"
            :title="
              resultKind === 'HEALTHY'
                ? '健康杨梅：未发现明显异常'
                : resultKind === 'ABNORMAL'
                  ? '异常杨梅：发现异常特征'
                  : '尚未匹配到参考图片'
            "
            :type="
              resultKind === 'HEALTHY'
                ? 'success'
                : resultKind === 'ABNORMAL'
                  ? 'error'
                  : 'info'
            "
            :closable="false"
            show-icon
          />
          <p v-if="previousResult" class="comparison-line">
            上次：{{ businessText(previousResult.pestType) }} → 本次：{{
              businessText(result.pestType)
            }}
          </p>
          <span class="result-caption">{{
            result.matchedBy === "DEMO_FINGERPRINT"
              ? "配套样图匹配结果"
              : "指定分类"
          }}</span>
          <h2>
            {{ businessText(result.pestType) }}
            <el-tag
              :type="
                resultKind === 'HEALTHY'
                  ? 'success'
                  : resultKind === 'ABNORMAL'
                    ? 'danger'
                    : 'info'
              "
              >{{ result.severity }}</el-tag
            >
          </h2>
          <div class="result-symptom">
            <b>结果描述</b>
            <p>{{ resultKind === 'UNKNOWN' ? '未匹配到参考图片，请上传配套样例的原始图片。' : businessText(result.symptom) }}</p>
          </div>
          <div class="result-advice">
            <b>{{
              resultKind === "HEALTHY" ? "日常管理建议" : "简易防治建议"
            }}</b>
            <p>{{ businessText(result.suggestion) }}</p>
          </div>
        </div>
        <el-empty
          v-else
          :description="busy ? '正在获取分析结果' : '采集后在此显示分析结果'"
          :image-size="85"
        />
        <p class="model-note">
          当前采用配套图片指纹匹配，健康结果不登记为病害。
        </p>
      </section>
    </div>
    <div v-if="recent.length" class="recent-captures">
      <span>最近采图 <small>仅本页保留，确认后才保存观测记录</small></span>
      <div class="recent-items">
        <div v-for="(f, i) in recent" :key="i">
          <el-image :src="f.imageUrl" fit="cover" /><span
            >{{ businessText(f.scenario)
            }}<small
              >{{ time(f.capturedAt).slice(11) }} ·
              {{ f.source === "camera" ? "参考图片集" : "手动上传" }}</small
            ></span
          >
        </div>
      </div>
    </div>
    <template #footer
      ><el-button @click="emit('update:modelValue', false)">关闭</el-button
      ><el-button
        type="primary"
        :disabled="!result?.recordable || busy"
        @click="confirm"
        >{{
          resultKind === "HEALTHY"
            ? "健康样本，无需登记病害"
            : resultKind === "UNKNOWN"
              ? "请先匹配参考图片"
              : "确认结果并填入观测表单"
        }}</el-button
      ></template
    >
  </el-dialog>
</template>
<style scoped>
.demo-downloads {
  display: flex;
  align-items: center;
  gap: 15px;
  flex-wrap: wrap;
  font-size: 12px;
  margin: -8px 0 22px;
  color: #6c8260;
}
.demo-downloads a {
  color: #28765d;
  text-decoration: underline;
}
.demo-downloads small {
  font-size: 10px;
  color: #99a58b;
}
.health-verdict {
  margin-bottom: 16px;
}
.comparison-line {
  font-size: 11px;
  color: #7a906c;
  background: #f3f7ed;
  padding: 9px;
  border-radius: 5px;
  line-height: 1.8;
}
.abnormal-result h2,
.abnormal-result .demo-score > b {
  color: #c14c52;
}
.abnormal-result .result-advice {
  background: #fff2ef;
}
.healthy-result h2 {
  color: #398368;
}
.demo-explanation {
  font-size: 12px;
  color: #84947b;
  background: #f2f6ec;
  padding: 12px 16px;
  border-radius: 7px;
  line-height: 1.8;
  margin-bottom: 22px;
}
.demo-settings {
  display: grid;
  grid-template-columns: 1fr;
  gap: 20px;
}
.detection-grid {
  display: grid;
  grid-template-columns: 1.1fr 1fr;
  gap: 20px;
}
.capture-card,
.model-card {
  border: 1px solid #e1e8db;
  border-radius: 10px;
  padding: 18px;
  min-width: 0;
}
.capture-heading {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}
.capture-heading b {
  font-size: 14px;
  color: #3c5e47;
}
.capture-heading small {
  display: block;
  font-size: 10px;
  color: #98a38d;
  margin-top: 7px;
  letter-spacing: 0.5px;
}
.camera-preview {
  height: 245px;
  background: #edf2e7;
  border-radius: 7px;
  position: relative;
  overflow: hidden;
}
.camera-preview .el-image {
  width: 100%;
  height: 100%;
  display: block;
}
.camera-placeholder {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #8ca07d;
}
.camera-placeholder > span {
  font-size: 42px;
  font-weight: 300;
}
.camera-placeholder b {
  font-size: 14px;
  font-weight: 400;
}
.camera-placeholder p {
  font-size: 10px;
  margin: 0 15px;
  text-align: center;
  line-height: 1.8;
}
.camera-overlay {
  position: absolute;
  bottom: 10px;
  left: 10px;
  border-radius: 4px;
  padding: 5px 8px;
  background: #173f35bb;
  color: #fff;
  font-size: 10px;
  pointer-events: none;
}
.capture-time {
  font-size: 10px;
  color: #8f9e83;
  margin: 13px 0 18px;
  line-height: 1.8;
}
.capture-buttons {
  display: flex;
  gap: 9px;
  flex-wrap: wrap;
}
.capture-buttons .el-button {
  font-size: 11px;
  margin-left: 0;
  padding: 9px;
}
.result-caption {
  font-size: 11px;
  color: #8d9b83;
}
.model-result h2 {
  font-size: 25px;
  margin: 12px 0 20px;
  color: #234b35;
  display: flex;
  align-items: center;
  gap: 14px;
}
.demo-score {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.demo-score > span {
  font-size: 12px;
  color: #8b9a7c;
}
.demo-score > b {
  font-size: 25px;
  font-weight: 500;
  color: #398368;
}
.demo-score small {
  font-size: 12px;
  margin-left: 3px;
}
.score-explanation {
  font-size: 10px;
  color: #a1aa93;
  line-height: 1.8;
  margin: 10px 0 20px;
}
.result-symptom,
.result-advice {
  font-size: 12px;
  line-height: 1.9;
}
.result-symptom b,
.result-advice b {
  font-size: 12px;
  color: #557047;
}
.result-symptom p {
  color: #7d8e70;
  margin: 7px 0 15px;
}
.result-advice {
  background: #f0f5e8;
  padding: 12px;
  border-radius: 7px;
}
.result-advice p {
  margin: 7px 0 0;
  color: #7b8f6c;
}
.model-note {
  font-size: 10px;
  line-height: 1.9;
  color: #a08c65;
  margin: 18px 0 0;
  border-top: 1px solid #edf0e6;
  padding-top: 12px;
}
.recent-captures {
  margin-top: 20px;
}
.recent-captures > span {
  font-size: 12px;
  color: #6d8560;
}
.recent-captures > span small {
  margin-left: 12px;
  font-size: 10px;
  color: #9ca78f;
}
.recent-items {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-top: 12px;
}
.recent-items > div {
  display: flex;
  align-items: center;
  gap: 9px;
  background: #f7f9f3;
  border-radius: 6px;
  padding: 8px;
}
.recent-items .el-image {
  width: 46px;
  height: 40px;
  border-radius: 4px;
  flex-shrink: 0;
}
.recent-items span {
  font-size: 11px;
  line-height: 1.8;
}
.recent-items small {
  display: block;
  font-size: 9px;
  color: #99a48e;
}
@media (max-width: 700px) {
  .demo-settings,
  .detection-grid {
    grid-template-columns: 1fr;
    gap: 12px;
  }
  .recent-items {
    grid-template-columns: 1fr 1fr;
  }
  .camera-preview {
    height: 210px;
  }
  .demo-explanation {
    font-size: 11px;
  }
  .recent-captures > span small {
    display: block;
    margin: 7px 0 0;
  }
}
</style>
