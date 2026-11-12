package cn.orchard.controller;

import cn.orchard.service.*;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

/** 受登录保护的监测查询入口，用于大盘、时序曲线和控制历史。 */
@RestController
@RequestMapping("/api")
public class MonitorController {
  private final MonitoringService service;
  private final AuthService auth;

  public MonitorController(MonitoringService s, AuthService a) {
    service = s;
    auth = a;
  }

  @GetMapping("/dashboard")
  public Object dashboard(HttpServletRequest r) {
    return service.dashboard(auth.current(r));
  }

  @GetMapping("/orchards/{id}/history")
  public Object history(@PathVariable Long id, HttpServletRequest r) {
    return service.history(id, auth.current(r));
  }

  @GetMapping("/control-logs")
  public Object logs(HttpServletRequest r) {
    return service.logs(auth.current(r));
  }
}
