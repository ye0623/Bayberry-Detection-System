import { businessText } from "./presentation";
import axios from "axios";
import { ElMessage } from "element-plus";
export const api = axios.create({ baseURL: "/api", timeout: 12000 });
api.interceptors.request.use((config) => {
  const t = localStorage.getItem("token");
  if (t) config.headers.Authorization = `Bearer ${t}`;
  return config;
});
api.interceptors.response.use(
  (r) => r.data,
  (e) => {
    ElMessage.error(
      businessText(e.response?.data?.message) || "请求失败，请检查后端是否启动",
    );
    if (e.response?.status === 401) {
      localStorage.removeItem("token");
      localStorage.removeItem("user");
      if (!location.pathname.startsWith("/login")) location.assign("/login");
    }
    return Promise.reject(e);
  },
);
export const time = (v) => (v ? v.replace("T", " ").slice(0, 19) : "—");
export const riskType = (v) =>
  v === "高风险" ? "danger" : v === "中等风险" ? "warning" : "success";
export const orchardName = (list, id) =>
  list.find((o) => o.id === id)?.name || "已删除果园";
export const pests = ["褐斑病", "癌肿病", "卷叶蛾", "果蝇"];
export async function uploadFile(file) {
  const f = new FormData();
  f.append("file", file);
  return api.post("/files", f);
}
