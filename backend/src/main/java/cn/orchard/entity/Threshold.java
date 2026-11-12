package cn.orchard.entity;

import com.baomidou.mybatisplus.annotation.*;

/** threshold_config 持久化实体，字段采用数据库下划线自动映射。 */
@TableName("threshold_config")
public class Threshold {
  @TableId(type = IdType.AUTO)
  public Long id;

  public Long orchardId;
  public Double temperatureMin;
  public Double temperatureMax;
  public Double humidityMax;
  public Double soilHumidityMax;
  public Double lightMin;
  public Double lightMax;
  public Double co2Max;
  public Double brownTempMin;
  public Double brownTempMax;
  public Double brownHumidity;
  public Double flyTempMin;
  public Double flyTempMax;
  public Double flyHumidity;
  public Double cancerHumidity;
  public Double cancerSoil;
}
