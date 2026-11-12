package cn.orchard.controller;

import java.time.LocalDateTime;
import javax.validation.constraints.*;

/** 原始硬件协议 DTO，Web 模拟器与未来硬件共用。 */
public class HardwareRequests {
  public static class Upload {
    @NotBlank(message = "设备编号不能为空")
    public String deviceSn;

    @NotNull
    @DecimalMin("-40")
    @DecimalMax("80")
    public Double temperature;

    @NotNull
    @DecimalMin("0")
    @DecimalMax("100")
    public Double humidity;

    @NotNull
    @DecimalMin("0")
    @DecimalMax("100")
    public Double soilHumidity;

    @NotNull
    @DecimalMin("0")
    @DecimalMax("200000")
    public Double light;

    @NotNull
    @DecimalMin("0")
    @DecimalMax("10000")
    public Double co2;

    @NotNull(message = "采集时间不能为空")
    public LocalDateTime collectTime;
  }

  public static class Control {
    @NotBlank public String deviceSn;
    @NotBlank public String controlCommand;
  }
}
