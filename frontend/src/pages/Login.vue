<script setup>
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { api } from "../api";
import { ElMessage } from "element-plus";
const router = useRouter(),
  register = ref(false),
  busy = ref(false),
  form = reactive({ username: "farmer", password: "123456", nickname: "" });
async function submit() {
  busy.value = true;
  try {
    if (register.value) {
      await api.post("/auth/register", form);
      ElMessage.success("注册成功，请登录");
      register.value = false;
    } else {
      const r = await api.post("/auth/login", form);
      localStorage.setItem("token", r.token);
      localStorage.setItem("user", JSON.stringify(r.user));
      router.push("/dashboard");
    }
  } finally {
    busy.value = false;
  }
}
</script>
<template>
  <div class="login">
    <section class="login-story">
      <div class="brand">
        <span class="brand-mark">梅</span>
        <div>梅境<span>BAYBERRY ORCHARD</span></div>
      </div>
      <div class="story-copy">
        <span class="eyebrow">GROW WITH INSIGHT</span>
        <h1>感知果园每一刻<br />守护杨梅每一季</h1>
        <p>从环境监测到病虫害风险预警，<br />让每一次巡园，都有数据可循。</p>
        <div class="story-chips">
          <span>环境感知</span><span>风险预警</span><span>科学管理</span>
        </div>
      </div>
      <div class="orchard-art"><i /><i /><i /><i /><i /></div>
      <small>杨梅果园物联网病虫害预警平台</small>
    </section>
    <section class="login-form">
      <div class="login-box">
        <span class="eyebrow">WELCOME TO MEIJING</span>
        <h2>{{ register ? "创建农户账号" : "欢迎回到果园" }}</h2>
        <p>
          {{
            register
              ? "注册后即可创建自己的果园。"
              : "登录工作台，查看果园环境与风险动态。"
          }}
        </p>
        <el-form label-position="top" @submit.prevent="submit"
          ><el-form-item label="用户名"
            ><el-input
              v-model="form.username"
              placeholder="3～30位字母、数字或下划线"
              maxlength="30" /></el-form-item
          ><el-form-item v-if="register" label="昵称"
            ><el-input v-model="form.nickname" maxlength="30" /></el-form-item
          ><el-form-item label="密码"
            ><el-input
              v-model="form.password"
              type="password"
              show-password
              maxlength="72"
              placeholder="6～72位密码" /></el-form-item
          ><el-button
            type="primary"
            native-type="submit"
            :loading="busy"
            class="login-submit"
            >{{ register ? "注册农户账号" : "登录工作台 →" }}</el-button
          ></el-form
        ><el-button text @click="register = !register">{{
          register ? "已有账号？返回登录" : "新用户？注册农户账号"
        }}</el-button>
        <div class="demo-accounts">
          <b>快捷登录账号</b>
          <p>农户 farmer / 123456<br />管理员 admin / 123456</p>
          <small>注册账号固定为农户；管理员可查看全部果园并发布公告。</small>
        </div>
      </div>
    </section>
  </div>
</template>
