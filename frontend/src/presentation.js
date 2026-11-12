/** 页面文案转换；保留原始记录中的来源标记，不修改接口数据或判定规则。 */
export function businessText(value) {
  return String(value ?? "")
    .replace(/^\[(?:模拟示例|模拟模型)\]\s*/, "")
    .replace(/虚拟图像传感器\s+(?:CAM-DEMO-|IMG-SOURCE-)(\d+)/g, "样例图片来源 IMG-SOURCE-$1")
    .replace(/Bayberry-Demo-v\d+预设演示分类[:：]/g, "样例匹配结果：")
    .replace(/，模拟评分\d+(?:\.\d+)?%（不代表真实识别准确率）。/g, "。")
    .replace(/演示结果[:：]\s*/g, "")
    .replace(/模拟果实期/g, "果实期")
    .replace(/模拟|演示/g, "");
}
