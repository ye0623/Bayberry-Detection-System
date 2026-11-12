package cn.orchard.service;

import cn.orchard.common.ApiException;
import cn.orchard.common.ControlCommands;
import cn.orchard.controller.HardwareRequests;
import cn.orchard.entity.*;
import cn.orchard.mapper.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MonitoringService {
  private final OrchardService orchards;
  private final SensorDataMapper sensors;
  private final RiskService risk;
  private final ControlLogMapper logs;

  public MonitoringService(
      OrchardService o, SensorDataMapper s, RiskService r, ControlLogMapper l) {
    orchards = o;
    sensors = s;
    risk = r;
    logs = l;
  }

  /** 所有采集入口统一入库并判定风险，保证原始数据与预警原子提交。 */
  @Transactional
  public Map<String, Object> upload(HardwareRequests.Upload r, User actor) {
    Device d = orchards.bySn(r.deviceSn);
    if (actor != null) orchards.access(d.orchardId, actor);
    if (r.collectTime.isAfter(LocalDateTime.now().plusMinutes(5)))
      throw new ApiException(400, "采集时间不能超前超过5分钟");
    for (Double n : List.of(r.temperature, r.humidity, r.soilHumidity, r.light, r.co2))
      if (!Double.isFinite(n)) throw new ApiException(400, "环境数值必须为有限数字");
    SensorData s = new SensorData();
    s.deviceId = d.id;
    s.orchardId = d.orchardId;
    s.temperature = r.temperature;
    s.humidity = r.humidity;
    s.soilHumidity = r.soilHumidity;
    s.light = r.light;
    s.co2 = r.co2;
    s.collectTime = r.collectTime;
    s.receivedTime = LocalDateTime.now();
    List<RiskWarning> ws =
        risk.evaluate(
            s,
            orchards.threshold(d.orchardId),
            Boolean.TRUE.equals(orchards.orchardMapper().selectById(d.orchardId).fruiting));
    s.riskLevel =
        ws.stream().anyMatch(w -> w.riskLevel.equals("高风险"))
            ? "高风险"
            : ws.isEmpty() ? "低风险" : "中等风险";
    sensors.insert(s);
    risk.persist(ws, s.id);
    return Map.of("data", s, "warnings", ws);
  }

  @Transactional
  public ControlLog control(HardwareRequests.Control r, User actor) {
    Device d = orchards.bySn(r.deviceSn);
    if (actor != null) orchards.access(d.orchardId, actor);
    if (!ControlCommands.SUPPORTED.contains(r.controlCommand))
      throw new ApiException(400, "不支持的控制指令");
    ControlLog l = new ControlLog();
    l.deviceId = d.id;
    l.deviceSn = d.deviceSn;
    l.orchardId = d.orchardId;
    l.controlCommand = r.controlCommand;
    l.operatorName = actor == null ? "外部硬件API" : actor.username;
    l.createdAt = LocalDateTime.now();
    logs.insert(l);
    return l;
  }

  public List<SensorData> history(Long id, User u) {
    orchards.access(id, u);
    List<SensorData> rows =
        sensors.selectList(
            new QueryWrapper<SensorData>()
                .eq("orchard_id", id)
                .orderByDesc("collect_time", "id")
                .last("LIMIT 120"));
    Collections.reverse(rows);
    return rows;
  }

  public List<Map<String, Object>> dashboard(User u) {
    List<Map<String, Object>> result = new ArrayList<>();
    for (Orchard o : orchards.list(u)) {
      Map<String, Object> row = new HashMap<>();
      row.put("orchard", o);
      row.put(
          "latest",
          sensors.selectOne(
              new QueryWrapper<SensorData>()
                  .eq("orchard_id", o.id)
                  .orderByDesc("collect_time", "id")
                  .last("LIMIT 1")));
      row.put(
          "deviceCount",
          orchards.deviceMapper().selectCount(new QueryWrapper<Device>().eq("orchard_id", o.id)));
      result.add(row);
    }
    return result;
  }

  public List<ControlLog> logs(User u) {
    List<Long> ids = orchards.ids(u);
    return ids.isEmpty()
        ? List.of()
        : logs.selectList(
            new QueryWrapper<ControlLog>()
                .in("orchard_id", ids)
                .orderByDesc("id")
                .last("LIMIT 200"));
  }
}
