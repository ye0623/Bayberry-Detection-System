package cn.orchard.entity;

import com.baomidou.mybatisplus.annotation.*;

/** risk_warning 持久化实体，字段采用数据库下划线自动映射。 */
@TableName("risk_warning")
public class RiskWarning {
  @TableId(type = IdType.AUTO)
  public Long id;

  public Long orchardId;
  public Long sensorDataId;
  public String pestType;
  public String riskLevel;
  public String reason;
  public String suggestion;
  public java.time.LocalDateTime createdAt;
  public Boolean handled;
}
