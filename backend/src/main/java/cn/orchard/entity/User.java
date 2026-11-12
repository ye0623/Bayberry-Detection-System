package cn.orchard.entity;

import com.baomidou.mybatisplus.annotation.*;

/** app_user 持久化实体，字段采用数据库下划线自动映射。 */
@TableName("app_user")
public class User {
  @TableId(type = IdType.AUTO)
  public Long id;

  public String username;
  @com.fasterxml.jackson.annotation.JsonIgnore public String password;
  public String role;
  public String nickname;
  public Boolean enabled;
  public java.time.LocalDateTime createdAt;
  public java.time.LocalDateTime lastLoginAt;
  public Long loginCount;
}
