<script setup>
import { ref, reactive, onMounted } from "vue";
import { api } from "../api";
import ImageUpload from "../components/ImageUpload.vue";
import { ElMessage, ElMessageBox } from "element-plus";
const list = ref([]),
  visible = ref(false),
  busy = ref(false),
  form = reactive({});
async function load() {
  list.value = await api.get("/orchards");
}
function edit(o = {}) {
  Object.keys(form).forEach((k) => delete form[k]);
  Object.assign(
    form,
    {
      name: "",
      location: "",
      area: 10,
      variety: "东魁",
      imageUrl: "",
      fruiting: true,
    },
    o,
  );
  visible.value = true;
}
async function save() {
  busy.value = true;
  try {
    await api[form.id ? "put" : "post"](
      "/orchards" + (form.id ? "/" + form.id : ""),
      form,
    );
    visible.value = false;
    ElMessage.success("果园已保存");
    await load();
  } finally {
    busy.value = false;
  }
}
async function remove(o) {
  try {
    await ElMessageBox.confirm(
      "删除果园将同时删除设备、环境数据、预警及观测记录，确定继续？",
      "删除 " + o.name,
      { type: "warning" },
    );
    await api.delete("/orchards/" + o.id);
    await load();
  } catch (e) {
    if (e !== "cancel" && e !== "close") console.error(e);
  }
}
onMounted(load);
</script>
<template>
  <div class="toolbar">
    <span>共 {{ list.length }} 个杨梅果园</span
    ><el-button type="primary" @click="edit()">＋ 新建果园</el-button>
  </div>
  <el-empty
    v-if="!list.length"
    description="还没有果园，创建第一个地块开始监测"
  />
  <div class="orchard-grid">
    <article v-for="o in list" :key="o.id" class="orchard-card">
      <div class="orchard-cover">
        <img v-if="o.imageUrl" :src="o.imageUrl" :alt="o.name + '果园图片'" />
        <div v-else class="cover-pattern">
          <span>杨梅</span><small>BAYBERRY GROVE</small>
        </div>
        <el-tag :type="o.fruiting ? 'success' : 'info'">{{
          o.fruiting ? "果实期" : "非果实期"
        }}</el-tag>
      </div>
      <div class="orchard-info">
        <h3>{{ o.name }}</h3>
        <p>{{ o.location }}</p>
        <div class="orchard-facts">
          <span
            ><small>种植面积</small><b>{{ o.area }} <em>亩</em></b></span
          ><span
            ><small>杨梅品种</small><b>{{ o.variety }}</b></span
          >
        </div>
        <div class="card-actions">
          <el-button @click="edit(o)">编辑果园</el-button
          ><el-button text type="danger" @click="remove(o)">删除</el-button>
        </div>
      </div>
    </article>
  </div>
  <el-dialog
    v-model="visible"
    :title="form.id ? '编辑果园' : '新建果园'"
    width="560px"
    ><el-form label-width="100px"
      ><el-form-item label="果园名称" required
        ><el-input v-model="form.name" maxlength="100" /></el-form-item
      ><el-form-item label="地理位置" required
        ><el-input v-model="form.location" maxlength="200" /></el-form-item
      ><el-form-item label="种植面积" required
        ><el-input-number v-model="form.area" :min="0.1" :precision="1" />
        亩</el-form-item
      ><el-form-item label="杨梅品种" required
        ><el-input v-model="form.variety" maxlength="100" /></el-form-item
      ><el-form-item label="果实期"
        ><el-switch v-model="form.fruiting" /><small class="hint"
          >影响果蝇风险规则</small
        ></el-form-item
      ><el-form-item label="果园图片"
        ><ImageUpload v-model="form.imageUrl" /></el-form-item></el-form
    ><template #footer
      ><el-button @click="visible = false">取消</el-button
      ><el-button type="primary" :loading="busy" @click="save"
        >保存果园</el-button
      ></template
    ></el-dialog
  >
</template>
