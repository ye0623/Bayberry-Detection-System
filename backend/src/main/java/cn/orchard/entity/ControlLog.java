package cn.orchard.entity;

import com.baomidou.mybatisplus.annotation.*;

/** control_log 持久化实体，字段采用数据库下划线自动映射。 */
@TableName("control_log")
public class ControlLog {
  @TableId(type = IdType.AUTO)
  public Long id;

  public Long deviceId;
  public Long orchardId;
  public String deviceSn;
  public String controlCommand;
  public String operatorName;
  public java.time.LocalDateTime createdAt;
}
