<script setup>
import { ref } from "vue";
import { uploadFile } from "../api";
import { ElMessage } from "element-plus";
defineProps({ modelValue: String });
const emit = defineEmits(["update:modelValue"]),
  busy = ref(false);
async function change(event) {
  const file = event.target.files[0];
  if (!file) return;
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning("请选择5MB内的图片");
    return;
  }
  busy.value = true;
  try {
    const r = await uploadFile(file);
    emit("update:modelValue", r.url);
  } finally {
    busy.value = false;
    event.target.value = "";
  }
}
</script>
<template>
  <div class="image-upload">
    <el-image
      v-if="modelValue"
      :src="modelValue"
      :preview-src-list="[modelValue]"
      fit="cover"
      style="width: 110px; height: 80px; border-radius: 8px"
    /><label class="file-button"
      >{{ busy ? "正在上传…" : "＋ 上传实拍图片"
      }}<input
        :disabled="busy"
        type="file"
        accept="image/png,image/jpeg"
        @change="change" /></label
    ><small>JPG / PNG，最大 5MB</small>
  </div>
</template>
