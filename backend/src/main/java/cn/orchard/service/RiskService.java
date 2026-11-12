package cn.orchard.service;

import cn.orchard.entity.*;
import cn.orchard.mapper.RiskWarningMapper;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Service;

/** 规则型演示预警，不替代实地病害诊断；卷叶蛾不虚构环境诊断规则。 */
@Service
public class RiskService {
  private final RiskWarningMapper warnings;

  public RiskService(RiskWarningMapper w) {
    warnings = w;
  }

  public static String advice(String pest) {
    return switch (pest) {
      case "褐斑病" -> "加强通风、清除病叶并减少叶面长时间积水，结合实地检查及时咨询当地农技人员。";
      case "果蝇" -> "果实期及时清理落果、监测诱捕量，结合套袋与卫生管理。";
      case "癌肿病" -> "改善排水、控制土壤过湿，检查枝干伤口，修剪工具消毒。";
      case "卷叶蛾" -> "巡查卷叶与虫苞，及时清除虫叶，开展诱捕监测。";
      default -> "核查传感器读数，结合通风、排水与实地巡园调整管理。";
    };
  }

  public List<RiskWarning> evaluate(SensorData s, Threshold t, boolean fruiting) {
    List<RiskWarning> result = new ArrayList<>();
    if (s.humidity > t.brownHumidity
        && s.temperature >= t.brownTempMin
        && s.temperature <= t.brownTempMax) add(result, s, "褐斑病", "高风险", "温度位于褐斑病诱发区间且空气湿度超过阈值");
    if (fruiting
        && s.temperature >= t.flyTempMin
        && s.temperature <= t.flyTempMax
        && s.humidity > t.flyHumidity) add(result, s, "果蝇", "高风险", "处于果实期，温度处于果蝇高发区间且空气湿度偏高");
    if (s.humidity > t.cancerHumidity && s.soilHumidity > t.cancerSoil)
      add(result, s, "癌肿病", "高风险", "空气及土壤湿度同时超过阈值（高湿环境代理条件，未测量降雨）");
    if (s.temperature < t.temperatureMin
        || s.temperature > t.temperatureMax
        || s.humidity > t.humidityMax
        || s.soilHumidity > t.soilHumidityMax
        || s.light < t.lightMin
        || s.light > t.lightMax
        || s.co2 > t.co2Max) add(result, s, "环境异常", "中等风险", "环境指标超出果园独立配置的告警范围");
    return result;
  }

  private void add(List<RiskWarning> list, SensorData s, String type, String level, String reason) {
    RiskWarning w = new RiskWarning();
    w.orchardId = s.orchardId;
    w.pestType = type;
    w.riskLevel = level;
    w.reason = reason;
    w.suggestion = advice(type);
    w.createdAt = LocalDateTime.now();
    w.handled = false;
    list.add(w);
  }

  public void persist(List<RiskWarning> list, Long sensorId) {
    for (RiskWarning w : list) {
      w.sensorDataId = sensorId;
      warnings.insert(w);
    }
  }
}
