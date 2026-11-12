package cn.orchard.entity;

import com.baomidou.mybatisplus.annotation.*;

/** announcement 持久化实体，字段采用数据库下划线自动映射。 */
@TableName("announcement")
public class Announcement {
  @TableId(type = IdType.AUTO)
  public Long id;

  public String title;
  public String content;
  public java.time.LocalDateTime createdAt;
}
