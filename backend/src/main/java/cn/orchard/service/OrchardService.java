package cn.orchard.service;

import cn.orchard.common.ApiException;
import cn.orchard.entity.*;
import cn.orchard.mapper.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 果园所有权隔离与设备、阈值管理；删除果园时事务清理关联记录。 */
@Service
public class OrchardService {
  private final OrchardMapper orchards;
  private final DeviceMapper devices;
  private final ThresholdMapper thresholds;

  public OrchardMapper orchardMapper() {
    return orchards;
  }

  public DeviceMapper deviceMapper() {
    return devices;
  }

  private final SensorDataMapper sensors;
  private final RiskWarningMapper warnings;
  private final ObservationMapper observations;
  private final ControlLogMapper logs;

  public OrchardService(
      OrchardMapper o,
      DeviceMapper d,
      ThresholdMapper t,
      SensorDataMapper s,
      RiskWarningMapper w,
      ObservationMapper b,
      ControlLogMapper l) {
    orchards = o;
    devices = d;
    thresholds = t;
    sensors = s;
    warnings = w;
    observations = b;
    logs = l;
  }

  public List<Orchard> list(User u) {
    return orchards.selectList(
        new QueryWrapper<Orchard>()
            .eq(!"ADMIN".equals(u.role), "owner_id", u.id)
            .orderByDesc("id"));
  }

  public List<Long> ids(User u) {
    return list(u).stream().map(o -> o.id).toList();
  }

  public Orchard access(Long id, User u) {
    Orchard o = orchards.selectById(id);
    if (o == null) throw new ApiException(404, "果园不存在");
    if (!"ADMIN".equals(u.role) && !u.id.equals(o.ownerId)) throw new ApiException(403, "无权访问此果园");
    return o;
  }

  public Device bySn(String sn) {
    Device d = devices.selectOne(new QueryWrapper<Device>().eq("device_sn", sn));
    if (d == null) throw new ApiException(404, "设备不存在，请先注册设备");
    return d;
  }

  @Transactional
  public Orchard save(Orchard o, User u) {
    if (o.name == null
        || o.name.isBlank()
        || o.location == null
        || o.location.isBlank()
        || o.variety == null
        || o.variety.isBlank()
        || o.area == null
        || !Double.isFinite(o.area)
        || o.area <= 0) throw new ApiException(400, "请填写果园名称、位置、品种及有效面积");
    if (o.id == null) {
      o.ownerId = u.id;
      if (o.fruiting == null) o.fruiting = true;
      orchards.insert(o);
      Threshold t = defaults(o.id);
      thresholds.insert(t);
    } else {
      Orchard old = access(o.id, u);
      o.ownerId = old.ownerId;
      orchards.updateById(o);
    }
    return o;
  }

  @Transactional
  public void delete(Long id, User u) {
    access(id, u);
    devices.delete(new QueryWrapper<Device>().eq("orchard_id", id));
    thresholds.delete(new QueryWrapper<Threshold>().eq("orchard_id", id));
    sensors.delete(new QueryWrapper<SensorData>().eq("orchard_id", id));
    warnings.delete(new QueryWrapper<RiskWarning>().eq("orchard_id", id));
    observations.delete(new QueryWrapper<Observation>().eq("orchard_id", id));
    logs.delete(new QueryWrapper<ControlLog>().eq("orchard_id", id));
    orchards.deleteById(id);
  }

  public List<Device> devices(User u) {
    List<Long> ids = ids(u);
    return ids.isEmpty()
        ? List.of()
        : devices.selectList(new QueryWrapper<Device>().in("orchard_id", ids).orderByDesc("id"));
  }

  public Device saveDevice(Device d, User u) {
    access(d.orchardId, u);
    if (d.deviceSn == null
        || !d.deviceSn.matches("[A-Za-z0-9_-]{3,40}")
        || d.location == null
        || d.location.isBlank()) throw new ApiException(400, "设备编号需3～40位字母数字下划线或短横线，请填写安装位置");
    if (d.id == null) devices.insert(d);
    else {
      Device old = devices.selectById(d.id);
      if (old == null) throw new ApiException(404, "设备不存在");
      access(old.orchardId, u);
      if (!old.orchardId.equals(d.orchardId) || !old.deviceSn.equals(d.deviceSn))
        throw new ApiException(400, "已注册设备不允许更改编号及所属果园，请删除后重新注册");
      devices.updateById(d);
    }
    return d;
  }

  public void deleteDevice(Long id, User u) {
    Device d = devices.selectById(id);
    if (d == null) throw new ApiException(404, "设备不存在");
    access(d.orchardId, u);
    devices.deleteById(id);
  }

  public Threshold threshold(Long id) {
    return thresholds.selectOne(new QueryWrapper<Threshold>().eq("orchard_id", id));
  }

  public static Threshold defaults(Long id) {
    Threshold t = new Threshold();
    t.orchardId = id;
    t.temperatureMin = 10.;
    t.temperatureMax = 35.;
    t.humidityMax = 85.;
    t.soilHumidityMax = 80.;
    t.lightMin = 0.;
    t.lightMax = 90000.;
    t.co2Max = 1200.;
    t.brownTempMin = 22.;
    t.brownTempMax = 28.;
    t.brownHumidity = 85.;
    t.flyTempMin = 25.;
    t.flyTempMax = 30.;
    t.flyHumidity = 75.;
    t.cancerHumidity = 85.;
    t.cancerSoil = 80.;
    return t;
  }

  public void saveThreshold(Long id, Threshold t, User u) {
    access(id, u);
    try {
      for (var f : Threshold.class.getFields()) {
        if (f.getType() == Double.class) {
          Double n = (Double) f.get(t);
          if (n == null || !Double.isFinite(n)) throw new ApiException(400, "阈值必须完整且为有效数字");
          if (f.getName().toLowerCase().contains("humidity") || f.getName().contains("Soil")) {
            if (n < 0 || n > 100) throw new ApiException(400, "湿度阈值应在0～100之间");
          }
        }
      }
    } catch (IllegalAccessException e) {
      throw new IllegalStateException(e);
    }
    if (t.temperatureMin >= t.temperatureMax
        || t.brownTempMin > t.brownTempMax
        || t.flyTempMin > t.flyTempMax
        || t.lightMin > t.lightMax
        || t.lightMin < 0
        || t.co2Max < 0) throw new ApiException(400, "阈值上下限不合法");
    t.id = threshold(id).id;
    t.orchardId = id;
    thresholds.updateById(t);
  }
}
