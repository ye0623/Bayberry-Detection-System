package cn.orchard.common;

import java.util.Set;

/** 设备控制指令白名单。原指令保持兼容，新增光照、CO2与灌溉控制；当前仅保存日志。 */
public final class ControlCommands {
  private ControlCommands() {}

  public static final Set<String> SUPPORTED =
      Set.of(
          "开启通风", "关闭通风", "开启喷淋降湿", "关闭喷淋", "开启补光灯", "关闭补光灯", "展开遮阳网", "收起遮阳网", "开启CO₂补充",
          "停止CO₂补充", "开启灌溉", "停止灌溉");
}
