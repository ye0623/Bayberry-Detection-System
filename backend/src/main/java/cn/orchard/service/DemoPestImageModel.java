package cn.orchard.service;

import cn.orchard.common.ApiException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/** 课程演示桩：以配套图片SHA-256或手动场景返回预设结果，不进行视觉推理。 */
@Service
public class DemoPestImageModel implements PestImageModel {
  private final Map<String, String> sampleTypes = new HashMap<>();
  private final Map<String, String> fingerprints = new HashMap<>();
  private final Path uploadDir;

  public static class Sample {
    public String path;
    public String sha256;
    public String scenario;
  }

  public DemoPestImageModel(ObjectMapper mapper, @Value("${app.upload-dir}") String uploadDir)
      throws IOException {
    this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    try (InputStream in = new ClassPathResource("demo-image-fingerprints.json").getInputStream()) {
      List<Sample> samples = mapper.readValue(in, new TypeReference<List<Sample>>() {});
      for (Sample sample : samples) {
        sampleTypes.put(sample.path, sample.scenario);
        fingerprints.put(sample.sha256, sample.scenario);
      }
    }
  }

  @Override
  public Map<String, Object> predict(String imageUrl, String demoScenario) {
    if (!List.of("AUTO", "健康", "褐斑病", "癌肿病", "卷叶蛾", "果蝇")
        .contains(demoScenario == null ? "" : demoScenario))
      throw new ApiException(400, "请选择自动匹配、健康或有效的病虫害演示场景");
    String knownType = sampleTypes.get(imageUrl), matchedBy = "MANUAL_SCENARIO";
    if (knownType == null) {
      if (imageUrl == null || !imageUrl.matches("/uploads/[0-9a-fA-F-]{36}\\.(png|jpg)"))
        throw new ApiException(400, "请先通过本系统上传图片或选择配套示例");
      Path file = uploadDir.resolve(imageUrl.substring("/uploads/".length())).normalize();
      if (!file.startsWith(uploadDir) || !Files.isRegularFile(file))
        throw new ApiException(400, "图片不存在，请重新上传");
      // 仅匹配原文件的内容指纹；改名不影响结果，压缩、裁剪等内容变更则不保证匹配。
      try {
        if (Files.size(file) > 5 * 1024 * 1024) throw new ApiException(400, "图片不能超过5MB");
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file));
        knownType = fingerprints.get(HexFormat.of().formatHex(digest));
      } catch (IOException | java.security.NoSuchAlgorithmException ex) {
        throw new ApiException(400, "无法读取演示图片，请重新上传");
      }
    } else if (!"AUTO".equals(demoScenario) && !knownType.equals(demoScenario)) {
      throw new ApiException(400, "内置示例与手动场景不一致，请选择自动匹配或对应场景");
    }
    String type = demoScenario;
    if ("AUTO".equals(demoScenario)) {
      type = knownType;
      matchedBy = knownType == null ? "UNMATCHED" : "DEMO_FINGERPRINT";
    }
    boolean unknown = type == null, healthy = "健康".equals(type);
    String label = unknown ? "未匹配到演示样本" : healthy ? "健康杨梅" : type;
    int score =
        unknown
            ? 0
            : healthy
                ? 98
                : switch (type) {
                  case "褐斑病" -> 96;
                  case "癌肿病" -> 93;
                  case "卷叶蛾" -> 95;
                  default -> 94;
                };
    String symptom =
        unknown
            ? "该图片不在配套样本中。请使用配套图片，或先选择健康/病害演示场景再上传。"
            : healthy
                ? "演示结果：未发现明显病虫害异常，果实外观完整。"
                : switch (type) {
                  case "褐斑病" -> "演示结果：叶片出现褐色斑点，建议实地复查。";
                  case "癌肿病" -> "演示结果：枝条局部瘤状隆起，建议记录位置并复查。";
                  case "卷叶蛾" -> "演示结果：叶片卷曲及虫苞，建议核查幼虫与取食痕迹。";
                  default -> "演示结果：成熟果受损及小型飞虫，建议结合诱捕监测确认。";
                };
    String advice =
        unknown
            ? "当前不判定健康与病害，不生成病虫害记录。"
            : healthy ? "继续常规巡园，保持通风排水，观察后续变化；无需生成病害记录。" : RiskService.advice(type);
    return Map.ofEntries(
        Map.entry("simulated", true),
        Map.entry("modelName", "Bayberry-Demo-v2"),
        Map.entry("healthStatus", unknown ? "UNKNOWN" : healthy ? "HEALTHY" : "ABNORMAL"),
        Map.entry("recordable", !unknown && !healthy),
        Map.entry("matchedBy", matchedBy),
        Map.entry("pestType", label),
        Map.entry("severity", unknown ? "待确认" : healthy ? "未见异常" : "中度"),
        Map.entry("demoScore", score),
        Map.entry("symptom", symptom),
        Map.entry("suggestion", advice),
        Map.entry("imageUrl", imageUrl),
        Map.entry("predictedAt", LocalDateTime.now()),
        Map.entry("notice", "演示模型仅匹配配套图片指纹或手动场景，不进行真实视觉推理；分数不代表识别准确率。"));
  }
}
