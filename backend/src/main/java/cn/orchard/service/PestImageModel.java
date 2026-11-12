package cn.orchard.service;

import java.util.Map;

/** 图像识别适配边界；当前注入明确标记的演示实现，将来可替换为真实模型。 */
public interface PestImageModel {
  Map<String, Object> predict(String imageUrl, String demoScenario);
}
