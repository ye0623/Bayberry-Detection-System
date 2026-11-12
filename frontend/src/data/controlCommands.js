/** 分组控制指令；value与后端白名单一致，label描述人工操作意图。 */
export const controlCommandGroups = [
  {
    label: "光照调节",
    commands: [
      {
        value: "开启补光灯",
        label: "人工加强光照 · 开启补光灯",
        hint: "使用补光灯增加照明，后续以光照读数核对变化。",
      },
      { value: "关闭补光灯", label: "关闭补光灯", hint: "停止人工补光。" },
      {
        value: "展开遮阳网",
        label: "人工降低光照 · 展开遮阳网",
        hint: "展开遮阳网，减少直射光照。",
      },
      {
        value: "收起遮阳网",
        label: "收起遮阳网 · 恢复自然光照",
        hint: "收起遮阳网，恢复自然受光条件。",
      },
    ],
  },
  {
    label: "CO₂调节",
    commands: [
      {
        value: "开启CO₂补充",
        label: "人工增加CO₂ · 开启补充",
        hint: "启用CO₂补充设备；浓度变化需以后续采集结果确认。",
      },
      { value: "停止CO₂补充", label: "停止CO₂补充", hint: "停止CO₂补充设备。" },
    ],
  },
  {
    label: "通风换气",
    commands: [
      {
        value: "开启通风",
        label: "开启通风",
        hint: "启用通风设备；温湿度和CO₂变化以后续采集结果为准。",
      },
      { value: "关闭通风", label: "关闭通风", hint: "停止通风设备。" },
    ],
  },
  {
    label: "喷淋控制",
    commands: [
      {
        value: "开启喷淋降湿",
        label: "开启喷淋",
        hint: "启用喷淋设备。原接口指令名保留为“开启喷淋降湿”，实际温湿度变化取决于现场条件。",
      },
      { value: "关闭喷淋", label: "关闭喷淋", hint: "停止喷淋设备。" },
    ],
  },
  {
    label: "土壤灌溉",
    commands: [
      {
        value: "开启灌溉",
        label: "开启灌溉 · 补充土壤水分",
        hint: "启用灌溉设备，后续查看土壤湿度变化。",
      },
      { value: "停止灌溉", label: "停止灌溉", hint: "停止灌溉设备。" },
    ],
  },
];
export const allControlCommands = controlCommandGroups.flatMap(
  (group) => group.commands,
);
export const quickControlCommands = [
  { label: "加强光照", value: "开启补光灯" },
  { label: "降低光照", value: "展开遮阳网" },
  { label: "增加CO₂", value: "开启CO₂补充" },
];
