<script setup>
import { ref, reactive, onMounted } from "vue";
import { api, time } from "../api";
import { ElMessage, ElMessageBox } from "element-plus";
const list = ref([]),
  visible = ref(false),
  busy = ref(false),
  form = reactive({ title: "", content: "" });
const admin = JSON.parse(localStorage.getItem("user") || "{}").role === "ADMIN";
async function load() {
  list.value = await api.get("/announcements");
}
async function save() {
  busy.value = true;
  try {
    await api.post("/announcements", form);
    visible.value = false;
    form.title = "";
    form.content = "";
    ElMessage.success("公告已发布");
    await load();
  } finally {
    busy.value = false;
  }
}
async function remove(n) {
  try {
    await ElMessageBox.confirm("确定删除这条公告？", "删除公告", {
      type: "warning",
    });
    await api.delete("/announcements/" + n.id);
    await load();
  } catch (e) {
    if (e !== "cancel" && e !== "close") console.error(e);
  }
}
onMounted(load);
</script>
<template>
  <div class="toolbar">
    <span>种植知识与防治科普 · {{ list.length }} 篇公告</span
    ><el-button v-if="admin" type="primary" @click="visible = true"
      >＋ 发布公告</el-button
    >
  </div>
  <el-empty v-if="!list.length" description="暂无公告" />
  <article v-for="n in list" :key="n.id" class="panel announcement">
    <div class="panel-heading">
      <el-tag type="success" effect="plain">种植科普</el-tag
      ><span class="muted">{{ time(n.createdAt) }}</span>
    </div>
    <h2>{{ n.title }}</h2>
    <p>{{ n.content }}</p>
    <el-button v-if="admin" text type="danger" @click="remove(n)"
      >删除公告</el-button
    >
  </article>
  <el-dialog v-model="visible" title="发布果园公告" width="620px"
    ><el-form label-position="top"
      ><el-form-item label="公告标题" required
        ><el-input v-model="form.title" maxlength="150" /></el-form-item
      ><el-form-item label="公告正文" required
        ><el-input
          v-model="form.content"
          type="textarea"
          :rows="8"
          maxlength="3500"
          show-word-limit /></el-form-item></el-form
    ><template #footer
      ><el-button @click="visible = false">取消</el-button
      ><el-button type="primary" :loading="busy" @click="save"
        >发布公告</el-button
      ></template
    ></el-dialog
  >
</template>
