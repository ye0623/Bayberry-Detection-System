<script setup>
import { ref, reactive, onMounted } from "vue";
import { api, orchardName } from "../api";
import { ElMessage, ElMessageBox } from "element-plus";
const list = ref([]),
  orchards = ref([]),
  visible = ref(false),
  busy = ref(false),
  form = reactive({});
async function load() {
  [list.value, orchards.value] = await Promise.all([
    api.get("/devices"),
    api.get("/orchards"),
  ]);
}
function edit(d = {}) {
  Object.keys(form).forEach((k) => delete form[k]);
  Object.assign(
    form,
    { deviceSn: "", orchardId: orchards.value[0]?.id, location: "" },
    d,
  );
  visible.value = true;
}
async function save() {
  busy.value = true;
  try {
    await api[form.id ? "put" : "post"](
      "/devices" + (form.id ? "/" + form.id : ""),
      form,
    );
    visible.value = false;
    ElMessage.success("设备已保存");
    await load();
  } finally {
    busy.value = false;
  }
}
async function remove(d) {
  try {
    await ElMessageBox.confirm(
      "删除设备档案后将停止此设备自动采集，保留历史记录。",
      "删除设备",
      { type: "warning" },
    );
    await api.delete("/devices/" + d.id);
    await load();
  } catch (e) {
    if (e !== "cancel" && e !== "close") console.error(e);
  }
}
onMounted(load);
</script>
<template>
  <div class="toolbar">
    <span>已注册 {{ list.length }} 台设备</span
    ><el-button type="primary" :disabled="!orchards.length" @click="edit()"
      >＋ 注册设备</el-button
    >
  </div>
  <el-alert
    v-if="!orchards.length"
    title="请先在杨梅果园页面创建果园，再注册设备。"
    type="info"
    :closable="false"
  />
  <div class="panel">
    <el-table :data="list" empty-text="暂无设备，请注册果园监测设备"
      ><el-table-column prop="deviceSn" label="设备编号" /><el-table-column
        label="所属杨梅果园"
        min-width="180"
        ><template #default="{ row }">{{
          orchardName(orchards, row.orchardId)
        }}</template></el-table-column
      ><el-table-column
        prop="location"
        label="安装位置"
        min-width="180"
      /><el-table-column label="操作" width="160"
        ><template #default="{ row }"
          ><el-button text type="primary" @click="edit(row)">编辑</el-button
          ><el-button text type="danger" @click="remove(row)"
            >删除</el-button
          ></template
        ></el-table-column
      ></el-table
    >
  </div>
  <el-dialog
    v-model="visible"
    :title="form.id ? '编辑设备' : '注册设备'"
    width="500px"
    ><el-form label-position="top"
      ><el-form-item label="设备编号 deviceSn" required
        ><el-input
          v-model="form.deviceSn"
          :disabled="!!form.id"
          maxlength="40"
          placeholder="例如 YM-2026-003" /></el-form-item
      ><el-form-item label="所属杨梅果园" required
        ><el-select v-model="form.orchardId" :disabled="!!form.id"
          ><el-option
            v-for="o in orchards"
            :key="o.id"
            :label="o.name"
            :value="o.id" /></el-select></el-form-item
      ><el-form-item label="设备安装位置" required
        ><el-input
          v-model="form.location"
          maxlength="200" /></el-form-item></el-form
    ><template #footer
      ><el-button @click="visible = false">取消</el-button
      ><el-button type="primary" :loading="busy" @click="save"
        >保存设备</el-button
      ></template
    ></el-dialog
  >
</template>
