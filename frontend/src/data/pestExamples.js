// AI生成的课程演示示意图；这些样例不代表真实果园诊断结果。
export const pestExamples = [
  {
    type: "褐斑病",
    image: "/demo-pests/brown-spot.png",
    hint: "叶片褐色斑点、局部连片",
    severity: "轻度",
    description:
      "[模拟示例]巡查20片杨梅叶，3片出现零星褐色斑点，暂未见明显落叶；标记位置并安排复查。",
  },
  {
    type: "癌肿病",
    image: "/demo-pests/canker.png",
    hint: "枝条局部瘤状膨大",
    severity: "轻度",
    description:
      "[模拟示例]1株杨梅侧枝出现局部粗糙瘤状隆起，主干暂未见明显异常；记录枝位并请农技人员确认。",
  },
  {
    type: "卷叶蛾",
    image: "/demo-pests/leafroller.png",
    hint: "卷叶、虫苞与咀嚼痕迹",
    severity: "中度",
    description:
      "[模拟示例]抽查10株树梢，发现卷叶、丝状物和少量绿色幼虫，部分叶缘有咀嚼缺刻；记录虫苞位置。",
  },
  {
    type: "果蝇",
    image: "/demo-pests/fruit-fly.png",
    hint: "成熟果受损、软果内幼虫",
    severity: "中度",
    description:
      "[模拟示例]模拟果实期巡查，成熟果附近有小型飞虫，少量软果内见浅色幼虫；结合诱捕监测与专业鉴定确认。",
  },
];
export const isDemo = (record) =>
  record.imageUrl?.startsWith("/demo-pests/") ||
  record.description?.startsWith("[模拟示例]") ||
  record.description?.startsWith("[模拟模型]");
