<script setup>
import { ref, reactive, onMounted } from "vue";
import { useRouter } from "vue-router";
import { api, time } from "../api";
import { ElMessage, ElMessageBox } from "element-plus";
const router = useRouter(),
  current = JSON.parse(localStorage.getItem("user") || "{}");
const records = ref([]),
  total = ref(0),
  stats = ref({}),
  loading = ref(false),
  saving = ref(false),
  visible = ref(false),
  passwordVisible = ref(false),
  passwordTarget = ref(null),
  changing = ref(null);
const filter = reactive({
    keyword: "",
    role: "",
    enabled: null,
    page: 1,
    size: 10,
  }),
  form = reactive({}),
  password = reactive({ value: "", repeat: "" });
async function load() {
  loading.value = true;
  try {
    const r = await api.get("/admin/users", { params: filter });
    records.value = r.records;
    total.value = r.total;
    stats.value = r.stats;
  } finally {
    loading.value = false;
  }
}
function search() {
  filter.page = 1;
  load();
}
function edit(row = null) {
  Object.keys(form).forEach((k) => delete form[k]);
  Object.assign(
    form,
    row
      ? {
          id: row.id,
          username: row.username,
          nickname: row.nickname,
          role: row.role,
          enabled: row.enabled,
        }
      : {
          username: "",
          nickname: "",
          password: "",
          role: "FARMER",
          enabled: true,
        },
  );
  visible.value = true;
}
async function save() {
  if (!form.username || !form.nickname) {
    ElMessage.warning("请填写用户名和昵称");
    return;
  }
  if (!form.id && (!form.password || form.password.length < 6)) {
    ElMessage.warning("初始密码至少6位");
    return;
  }
  saving.value = true;
  try {
    const editing = !!form.id;
    await api[editing ? "put" : "post"](
      "/admin/users" + (editing ? "/" + form.id : ""),
      form,
    );
    if (form.id === current.id) {
      current.nickname = form.nickname;
      localStorage.setItem("user", JSON.stringify(current));
      window.dispatchEvent(new Event("account-updated"));
    }
    visible.value = false;
    form.password = "";
    ElMessage.success(editing ? "账号信息已更新" : "账号已创建");
    if (!editing) filter.page = 1;
    await load();
  } finally {
    saving.value = false;
  }
}
async function toggle(row) {
  try {
    await ElMessageBox.confirm(
      row.enabled
        ? "停用后该账号将立即退出，且不能再次登录；所属果园和历史记录会保留。"
        : "启用后该账号可正常登录。",
      `${row.enabled ? "停用" : "启用"}账号 ${row.username}`,
      {
        type: row.enabled ? "warning" : "info",
        confirmButtonText: "确认",
        cancelButtonText: "取消",
      },
    );
  } catch {
    return;
  }
  changing.value = row.id;
  try {
    await api.put("/admin/users/" + row.id, {
      nickname: row.nickname,
      role: row.role,
      enabled: !row.enabled,
    });
    ElMessage.success(row.enabled ? "账号已停用" : "账号已启用");
    await load();
  } finally {
    changing.value = null;
  }
}
function reset(row) {
  passwordTarget.value = row;
  password.value = "";
  password.repeat = "";
  passwordVisible.value = true;
}
async function resetPassword() {
  if (password.value.length < 6) {
    ElMessage.warning("新密码至少6位");
    return;
  }
  if (password.value !== password.repeat) {
    ElMessage.warning("两次密码不一致");
    return;
  }
  saving.value = true;
  try {
    await api.put("/admin/users/" + passwordTarget.value.id + "/password", {
      password: password.value,
    });
    passwordVisible.value = false;
    password.value = "";
    password.repeat = "";
    ElMessage.success("密码已重置，原登录会话已失效");
    if (passwordTarget.value.id === current.id) {
      localStorage.removeItem("token");
      localStorage.removeItem("user");
      router.replace("/login");
      return;
    }
    await load();
  } finally {
    saving.value = false;
  }
}
onMounted(load);
</script>
<template>
  <div class="account-stats">
    <div
      v-for="[key, label] in [
        ['total', '全部账号'],
        ['enabled', '启用账号'],
        ['disabled', '停用账号'],
        ['admins', '管理员账号'],
      ]"
      :key="key"
    >
      <span>{{ label }}</span
      ><b>{{ stats[key] ?? "—" }}</b>
    </div>
  </div>
  <div class="toolbar">
    <div class="user-filters">
      <el-input
        v-model="filter.keyword"
        clearable
        maxlength="50"
        placeholder="搜索用户名或昵称"
        @keyup.enter="search"
        @clear="search"
      /><el-select
        v-model="filter.role"
        clearable
        placeholder="全部角色"
        @change="search"
        ><el-option label="果园农户" value="FARMER" /><el-option
          label="管理员"
          value="ADMIN" /></el-select
      ><el-select
        v-model="filter.enabled"
        clearable
        placeholder="全部状态"
        @change="search"
        ><el-option label="启用" :value="true" /><el-option
          label="停用"
          :value="false" /></el-select
      ><el-button @click="search">查询</el-button>
    </div>
    <el-button type="primary" @click="edit()">＋ 新增账号</el-button>
  </div>
  <div class="panel">
    <el-table v-loading="loading" :data="records" empty-text="暂无匹配账号"
      ><el-table-column label="账号" min-width="170"
        ><template #default="{ row }"
          ><div class="user-name">
            <b>{{ row.username }}</b
            ><el-tag v-if="row.id === current.id" size="small" type="info"
              >当前账号</el-tag
            >
          </div>
          <small class="muted">{{ row.nickname }}</small></template
        ></el-table-column
      ><el-table-column label="角色" width="105"
        ><template #default="{ row }"
          ><el-tag
            :type="row.role === 'ADMIN' ? 'warning' : 'success'"
            effect="plain"
            >{{ row.role === "ADMIN" ? "管理员" : "果园农户" }}</el-tag
          ></template
        ></el-table-column
      ><el-table-column label="状态" width="90"
        ><template #default="{ row }"
          ><el-tag :type="row.enabled ? 'success' : 'danger'">{{
            row.enabled ? "启用" : "停用"
          }}</el-tag></template
        ></el-table-column
      ><el-table-column
        prop="orchardCount"
        label="果园数"
        width="80"
      /><el-table-column
        prop="loginCount"
        label="登录次数"
        width="90"
      /><el-table-column
        prop="activeSessions"
        label="有效会话"
        width="95"
      /><el-table-column label="注册时间" min-width="170"
        ><template #default="{ row }">{{
          time(row.createdAt)
        }}</template></el-table-column
      ><el-table-column label="最近登录" min-width="170"
        ><template #default="{ row }">{{
          row.lastLoginAt ? time(row.lastLoginAt) : "尚未登录"
        }}</template></el-table-column
      ><el-table-column label="操作" width="225" fixed="right"
        ><template #default="{ row }"
          ><el-button text type="primary" @click="edit(row)">编辑</el-button
          ><el-button text type="primary" @click="reset(row)"
            >重置密码</el-button
          ><el-button
            text
            :type="row.enabled ? 'danger' : 'success'"
            :disabled="row.id === current.id"
            :loading="changing === row.id"
            @click="toggle(row)"
            >{{ row.enabled ? "停用" : "启用" }}</el-button
          ></template
        ></el-table-column
      ></el-table
    >
    <div class="users-pagination">
      <el-pagination
        v-model:current-page="filter.page"
        :page-size="filter.size"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="load"
      />
    </div>
    <p class="muted account-help">
      停用账号会保留所属果园和历史记录。重置密码、修改角色或停用账号后，原登录会话立即失效。“有效会话”表示尚未过期的登录凭据，不等同于实时在线人数。
    </p>
  </div>
  <el-dialog
    v-model="visible"
    :title="form.id ? '编辑账号' : '新增账号'"
    width="500px"
    ><el-form label-position="top"
      ><el-form-item label="用户名" required
        ><el-input
          v-model="form.username"
          :disabled="!!form.id"
          maxlength="30"
          placeholder="3～30位字母、数字或下划线" /></el-form-item
      ><el-form-item label="昵称" required
        ><el-input v-model="form.nickname" maxlength="30" /></el-form-item
      ><el-form-item v-if="!form.id" label="初始密码" required
        ><el-input
          v-model="form.password"
          type="password"
          show-password
          maxlength="72"
          autocomplete="new-password"
          placeholder="至少6位" /></el-form-item
      ><el-form-item label="账号角色" required
        ><el-select v-model="form.role" :disabled="form.id === current.id"
          ><el-option label="果园农户" value="FARMER" /><el-option
            label="系统管理员"
            value="ADMIN" /></el-select></el-form-item
      ><el-form-item label="账号状态"
        ><el-switch
          v-model="form.enabled"
          :disabled="form.id === current.id"
          active-text="启用"
          inactive-text="停用"
      /></el-form-item>
      <p v-if="form.id === current.id" class="muted">
        不能停用或更改自己的角色。
      </p></el-form
    ><template #footer
      ><el-button @click="visible = false">取消</el-button
      ><el-button type="primary" :loading="saving" @click="save"
        >保存账号</el-button
      ></template
    ></el-dialog
  >
  <el-dialog
    v-model="passwordVisible"
    :title="'重置密码 · ' + (passwordTarget?.username || '')"
    width="460px"
    @closed="
      password.value = '';
      password.repeat = '';
    "
    ><el-alert
      title="重置后原密码和登录会话均失效，该用户需要使用新密码重新登录。"
      type="warning"
      :closable="false"
    /><el-form label-position="top" style="margin-top: 20px"
      ><el-form-item label="新密码" required
        ><el-input
          v-model="password.value"
          type="password"
          show-password
          maxlength="72"
          autocomplete="new-password" /></el-form-item
      ><el-form-item label="再次输入新密码" required
        ><el-input
          v-model="password.repeat"
          type="password"
          show-password
          maxlength="72"
          autocomplete="new-password" /></el-form-item></el-form
    ><template #footer
      ><el-button @click="passwordVisible = false">取消</el-button
      ><el-button type="primary" :loading="saving" @click="resetPassword"
        >确认重置</el-button
      ></template
    ></el-dialog
  >
</template>
<style scoped>
.account-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px;
  margin-bottom: 24px;
}
.account-stats > div {
  background: #fff;
  border: 1px solid #e2e8df;
  border-radius: 10px;
  padding: 23px;
}
.account-stats span {
  display: block;
  font-size: 12px;
  color: #8a9882;
}
.account-stats b {
  display: block;
  font-size: 31px;
  font-weight: 500;
  margin-top: 14px;
  color: #345c40;
}
.user-filters {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}
.user-filters > .el-input {
  width: 210px;
}
.user-filters > .el-select {
  width: 140px;
}
.user-name {
  display: flex;
  align-items: center;
  gap: 8px;
}
.user-name b {
  font-weight: 500;
  color: #36533e;
}
.users-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 22px;
}
.panel :deep(.el-table .el-button) { padding:5px 6px; }
.panel :deep(.el-table .el-button + .el-button) { margin-left:2px; }
.account-help {
  margin: 22px 0 0;
  padding-top: 16px;
  border-top: 1px solid #edf1e8;
}
@media (max-width: 900px) {
  .account-stats {
    grid-template-columns: 1fr 1fr;
    gap: 12px;
  }
  .user-filters > .el-input {
    width: 100%;
  }
  .user-filters > .el-select {
    flex: 1;
    min-width: 110px;
  }
  .user-filters {
    width: 100%;
  }
}
@media (max-width: 550px) {
  .account-stats > div {
    padding: 17px;
  }
  .users-pagination {
    justify-content: flex-start;
  }
}
</style>
