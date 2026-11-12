package cn.orchard.entity;

import com.baomidou.mybatisplus.annotation.*;

/** sensor_data 持久化实体，字段采用数据库下划线自动映射。 */
@TableName("sensor_data")
public class SensorData {
  @TableId(type = IdType.AUTO)
  public Long id;

  public Long deviceId;
  public Long orchardId;
  public Double temperature;
  public Double humidity;
  public Double soilHumidity;
  public Double light;
  public Double co2;
  public java.time.LocalDateTime collectTime;
  public java.time.LocalDateTime receivedTime;
  public String riskLevel;
}
