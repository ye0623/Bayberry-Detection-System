<script setup>
import { ref, reactive, onMounted, computed } from "vue";
import { api, time, pests, orchardName } from "../api";
import ImageUpload from "../components/ImageUpload.vue";
import ImageDetectionDemo from "../components/ImageDetectionDemo.vue";
import { pestExamples, isDemo } from "../data/pestExamples";
import { ElMessage } from "element-plus";
import { businessText } from "../presentation";
const records = ref([]),
  orchards = ref([]),
  visible = ref(false),
  busy = ref(false),
  detail = ref(null),
  demoVisible = ref(false),
  filter = reactive({ orchardId: null, pestType: "" }),
  form = reactive({}),
  selectedExample = ref("");
const descriptionInput = computed({
  get: () => businessText(form.description),
  set: (value) => {
    const marker =
      form.description?.match(/^\[(?:模拟示例|模拟模型)\]/)?.[0] || "";
    form.description = marker + value;
  },
});
async function load() {
  records.value = await api.get("/observations", { params: filter });
}
function create() {
  Object.assign(form, {
    orchardId: orchards.value[0]?.id,
    pestType: "褐斑病",
    severity: "轻度",
    description: "",
    imageUrl: "",
  });
  selectedExample.value = "";
  visible.value = true;
}
function acceptDetection(record) {
  create();
  Object.assign(form, record);
  demoVisible.value = false;
}
function fillExample(type) {
  const example = pestExamples.find((e) => e.type === type);
  if (!example) return;
  Object.assign(form, {
    pestType: example.type,
    severity: example.severity,
    description: example.description,
    imageUrl: example.image,
  });
}
function showType(type) {
  filter.pestType = filter.pestType === type ? "" : type;
  load();
}
async function save() {
  busy.value = true;
  try {
    await api.post("/observations", form);
    ElMessage.success("观测记录已保存，系统已生成防治建议");
    visible.value = false;
    await load();
  } finally {
    busy.value = false;
  }
}
onMounted(async () => {
  orchards.value = await api.get("/orchards");
  await load();
});
</script>
<template>
  <section class="sample-section">
    <div class="sample-heading">
      <h3>病虫害参考图例</h3>
      <span>点击图片放大，点击「查看记录」筛选观测记录</span>
    </div>
    <div class="pest-samples">
      <article
        v-for="example in pestExamples"
        :key="example.type"
        class="pest-sample"
        :class="{ chosen: filter.pestType === example.type }"
      >
        <div class="sample-photo">
          <el-image
            :src="example.image"
            :preview-src-list="[example.image]"
            fit="cover"
            preview-teleported
            :alt="example.type + '参考图片'"
          /><span class="sample-label">参考图片</span>
        </div>
        <div class="sample-caption">
          <div>
            <b>{{ example.type }}</b>
            <p>{{ example.hint }}</p>
          </div>
          <el-button text type="primary" @click="showType(example.type)">{{
            filter.pestType === example.type ? "取消筛选" : "查看记录"
          }}</el-button>
        </div>
      </article>
    </div>
    <p class="sample-notice">
      参考图片用于症状对照，病虫害判断需结合实地检查确认。
    </p>
  </section>
  <div class="toolbar">
    <div class="filters">
      <el-select
        v-model="filter.orchardId"
        clearable
        placeholder="全部果园"
        @change="load"
        ><el-option
          v-for="o in orchards"
          :key="o.id"
          :label="o.name"
          :value="o.id" /></el-select
      ><el-select
        v-model="filter.pestType"
        clearable
        placeholder="全部病虫害"
        @change="load"
        ><el-option v-for="p in pests" :key="p" :value="p"
      /></el-select>
    </div>
    <div class="observation-actions">
      <el-button
        type="primary"
        :disabled="!orchards.length"
        @click="demoVisible = true"
        >图像采集与识别</el-button
      ><el-button :disabled="!orchards.length" @click="create"
        >＋ 新增实地观测</el-button
      >
    </div>
  </div>
  <div class="panel">
    <div class="panel-heading">
      <h3>
        观测记录 <small class="muted">{{ records.length }} 条</small>
      </h3>
      <span class="muted">图片可放大 · 详情查看完整描述和建议</span>
    </div>
    <el-table
      :data="records"
      empty-text="暂无匹配记录，可新增实地观测或填入样例"
      ><el-table-column label="图片" width="120"
        ><template #default="{ row }"
          ><div class="record-image">
            <el-image
              v-if="row.imageUrl"
              :src="row.imageUrl"
              :preview-src-list="[row.imageUrl]"
              preview-teleported
              fit="cover"
              style="width: 88px; height: 66px; border-radius: 7px"
            /><span v-else class="muted">暂无图片</span
            ><small v-if="isDemo(row)">样例图片</small>
          </div></template
        ></el-table-column
      ><el-table-column label="所属果园" min-width="160"
        ><template #default="{ row }">{{
          orchardName(orchards, row.orchardId)
        }}</template></el-table-column
      ><el-table-column
        prop="pestType"
        label="病虫害"
        width="95"
      /><el-table-column label="程度" width="80"
        ><template #default="{ row }"
          ><el-tag
            :type="
              row.severity === '重度'
                ? 'danger'
                : row.severity === '中度'
                  ? 'warning'
                  : 'success'
            "
            >{{ row.severity }}</el-tag
          ></template
        ></el-table-column
      ><el-table-column
        prop="description"
        :formatter="(row, column, value) => businessText(value)"
        label="现场描述"
        min-width="220"
        show-overflow-tooltip
      /><el-table-column
        prop="suggestion"
        :formatter="(row, column, value) => businessText(value)"
        label="简易防治建议"
        min-width="230"
        show-overflow-tooltip
      /><el-table-column label="记录时间" min-width="170"
        ><template #default="{ row }">{{
          time(row.createdAt)
        }}</template></el-table-column
      ><el-table-column label="操作" fixed="right" width="75"
        ><template #default="{ row }"
          ><el-button text type="primary" @click="detail = row"
            >详情</el-button
          ></template
        ></el-table-column
      ></el-table
    >
  </div>
  <ImageDetectionDemo
    v-model="demoVisible"
    :orchards="orchards"
    @confirm="acceptDetection"
  />
  <el-dialog v-model="visible" title="新增实地病虫害观测" width="600px"
    ><el-form label-position="top"
      ><el-form-item label="快速填入样例（可选）"
        ><el-select
          v-model="selectedExample"
          placeholder="选择类型，自动填入示意图与示例描述"
          clearable
          @change="fillExample"
          ><el-option
            v-for="e in pestExamples"
            :key="e.type"
            :value="e.type"
            :label="e.type + ' · 参考样例'" /></el-select></el-form-item
      ><el-form-item label="杨梅果园" required
        ><el-select v-model="form.orchardId"
          ><el-option
            v-for="o in orchards"
            :key="o.id"
            :label="o.name"
            :value="o.id" /></el-select
      ></el-form-item>
      <div class="two-columns">
        <el-form-item label="病虫害类型" required
          ><el-select v-model="form.pestType"
            ><el-option
              v-for="p in pests"
              :key="p"
              :value="p" /></el-select></el-form-item
        ><el-form-item label="发病程度" required
          ><el-select v-model="form.severity"
            ><el-option
              v-for="s in ['轻度', '中度', '重度']"
              :key="s"
              :value="s" /></el-select
        ></el-form-item>
      </div>
      <el-form-item label="发病描述" required
        ><el-input
          v-model="descriptionInput"
          type="textarea"
          :rows="4"
          maxlength="2000"
          show-word-limit
          placeholder="记录叶片、枝干或果实症状，以及发现的位置…" /></el-form-item
      ><el-form-item label="病害图片"
        ><ImageUpload v-model="form.imageUrl" /><small
          v-if="isDemo(form)"
          class="demo-form-note"
          >请核对图片、类型和观测描述后保存。</small
        ></el-form-item
      ></el-form
    ><template #footer
      ><el-button @click="visible = false">取消</el-button
      ><el-button type="primary" :loading="busy" @click="save"
        >保存观测记录</el-button
      ></template
    ></el-dialog
  >
  <el-dialog
    :model-value="!!detail"
    title="病虫害观测详情"
    width="820px"
    @update:model-value="
      (v) => {
        if (!v) detail = null;
      }
    "
    ><template v-if="detail"
      ><div class="observation-detail">
        <div class="detail-picture">
          <el-image
            v-if="detail.imageUrl"
            :src="detail.imageUrl"
            :preview-src-list="[detail.imageUrl]"
            preview-teleported
            fit="contain"
          /><el-empty v-else description="未上传图片" />
          <p v-if="isDemo(detail)">
            {{
              detail.imageUrl?.startsWith("/demo-pests/")
                ? "参考图片"
                : "样例匹配记录"
            }}
          </p>
        </div>
        <div class="detail-text">
          <el-tag v-if="isDemo(detail)" type="warning" effect="plain"
            >参考记录</el-tag
          >
          <h2>
            {{ detail.pestType }}
            <el-tag
              :type="
                detail.severity === '重度'
                  ? 'danger'
                  : detail.severity === '中度'
                    ? 'warning'
                    : 'success'
              "
              >{{ detail.severity }}</el-tag
            >
          </h2>
          <p class="muted">
            {{ orchardName(orchards, detail.orchardId) }}<br />{{
              time(detail.createdAt)
            }}
          </p>
          <h4>观测描述</h4>
          <p>{{ businessText(detail.description) }}</p>
          <div class="detail-advice">
            <h4>简易防治建议</h4>
            <p>{{ businessText(detail.suggestion) }}</p>
          </div>
        </div>
      </div></template
    ></el-dialog
  >
</template>
<style scoped>
.observation-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.observation-actions .el-button + .el-button {
  margin-left: 0;
}
.sample-section {
  margin-bottom: 25px;
}
.sample-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 15px;
  margin-bottom: 15px;
}
.sample-heading h3 {
  margin: 0;
}
.sample-heading > span {
  font-size: 11px;
  color: #8b9785;
}
.pest-samples {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}
.pest-sample {
  background: #fff;
  border: 1px solid #e1e8de;
  border-radius: 10px;
  overflow: hidden;
  transition: border-color 0.15s;
}
.pest-sample.chosen {
  border-color: #398368;
  box-shadow: 0 0 0 1px #398368;
}
.sample-photo {
  height: 155px;
  position: relative;
  background: #e7ecdf;
}
.sample-photo .el-image {
  width: 100%;
  height: 100%;
  display: block;
}
.sample-label {
  position: absolute;
  top: 10px;
  left: 10px;
  font-size: 10px;
  padding: 5px 8px;
  background: #173f35c9;
  color: #fff;
  border-radius: 4px;
  pointer-events: none;
}
.sample-caption {
  padding: 15px 13px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 4px;
}
.sample-caption b {
  font-size: 15px;
}
.sample-caption p {
  margin: 8px 0 0;
  font-size: 10px;
  line-height: 1.6;
  color: #8b9682;
}
.sample-caption .el-button {
  font-size: 11px;
  padding: 5px;
  flex-shrink: 0;
}
.sample-notice {
  font-size: 11px;
  line-height: 1.8;
  color: #99a08e;
  margin: 12px 0 0;
}
.record-image {
  position: relative;
  display: inline-flex;
  flex-direction: column;
  gap: 3px;
}
.record-image small {
  font-size: 9px;
  color: #a18552;
}
.demo-form-note {
  display: block;
  color: #a18552;
  margin-top: 7px;
  font-size: 11px;
}
.observation-detail {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 25px;
}
.detail-picture {
  background: #f3f5ef;
  border-radius: 9px;
  padding: 12px;
  align-self: start;
}
.detail-picture .el-image {
  width: 100%;
  min-height: 230px;
  max-height: 370px;
  display: block;
}
.detail-picture > p {
  font-size: 11px;
  color: #9a936e;
  text-align: center;
  margin: 12px 0 0;
}
.detail-text h2 {
  font-size: 23px;
  margin: 15px 0;
}
.detail-text h4 {
  font-size: 13px;
  margin: 20px 0 10px;
}
.detail-text > p,
.detail-advice p {
  font-size: 13px;
  line-height: 2;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
.detail-advice {
  background: #eff4e7;
  padding: 1px 16px 12px;
  border-radius: 8px;
  color: #58714a;
}
.detail-advice h4 {
  margin-top: 16px;
}
.detail-advice p {
  margin: 0;
}
@media (max-width: 1250px) {
  .pest-samples {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .sample-photo {
    height: 175px;
  }
}
@media (max-width: 550px) {
  .pest-samples {
    gap: 10px;
  }
  .sample-photo {
    height: 125px;
  }
  .sample-heading {
    align-items: flex-start;
    flex-direction: column;
    gap: 7px;
  }
  .sample-caption {
    align-items: flex-start;
    flex-direction: column;
    gap: 8px;
    padding: 12px;
  }
  .observation-detail {
    grid-template-columns: 1fr;
  }
  .detail-picture .el-image {
    max-height: 260px;
  }
}
</style>
