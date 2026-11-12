package cn.orchard.entity;

import com.baomidou.mybatisplus.annotation.*;

/** orchard 持久化实体，字段采用数据库下划线自动映射。 */
@TableName("orchard")
public class Orchard {
  @TableId(type = IdType.AUTO)
  public Long id;

  public Long ownerId;
  public String name;
  public String location;
  public Double area;
  public String variety;
  public String imageUrl;
  public Boolean fruiting;
}
