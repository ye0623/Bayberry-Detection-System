package cn.orchard.controller;

import cn.orchard.entity.User;
import cn.orchard.service.*;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.*;

/** 硬件域严格保留以下两个 POST 接口，没有额外的模拟硬件接口。 */
@RestController
@RequestMapping("/api/hardware")
public class HardwareController {
  private final MonitoringService monitoring;
  private final AuthService auth;

  public HardwareController(MonitoringService m, AuthService a) {
    monitoring = m;
    auth = a;
  }

  private User actor(HttpServletRequest r) {
    String h = r.getHeader("Authorization");
    return h == null ? null : auth.authenticate(h);
  }

  /** 【此接口对外开放，未来果园物联网传感器直接POST请求此地址上报环境数据；当前演示由前端模拟面板调用该接口】 */
  @PostMapping("/sensor/upload")
  public Object upload(@Valid @RequestBody HardwareRequests.Upload r, HttpServletRequest req) {
    return monitoring.upload(r, actor(req));
  }

  /** 【未来对接真实硬件时，后端在这里增加转发逻辑；现阶段仅保存控制操作日志】 */
  @PostMapping("/device/control")
  public Object control(@Valid @RequestBody HardwareRequests.Control r, HttpServletRequest req) {
    return monitoring.control(r, actor(req));
  }
}
