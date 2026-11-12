package cn.orchard.entity;

import com.baomidou.mybatisplus.annotation.*;

/** device 持久化实体，字段采用数据库下划线自动映射。 */
@TableName("device")
public class Device {
  @TableId(type = IdType.AUTO)
  public Long id;

  public Long orchardId;
  public String deviceSn;
  public String location;
}
