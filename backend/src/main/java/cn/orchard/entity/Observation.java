package cn.orchard.entity;

import com.baomidou.mybatisplus.annotation.*;

/** observation 持久化实体，字段采用数据库下划线自动映射。 */
@TableName("observation")
public class Observation {
  @TableId(type = IdType.AUTO)
  public Long id;

  public Long orchardId;
  public String pestType;
  public String severity;
  public String description;
  public String imageUrl;
  public String suggestion;
  public java.time.LocalDateTime createdAt;
}
