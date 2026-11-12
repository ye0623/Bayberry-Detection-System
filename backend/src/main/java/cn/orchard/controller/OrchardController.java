package cn.orchard.controller;

import cn.orchard.entity.*;
import cn.orchard.service.*;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

/** 果园与设备管理入口；所有写操作及读取均由服务层校验果园归属。 */
@RestController
@RequestMapping("/api")
public class OrchardController {
  private final OrchardService service;
  private final AuthService auth;

  public OrchardController(OrchardService s, AuthService a) {
    service = s;
    auth = a;
  }

  @GetMapping("/orchards")
  public Object list(HttpServletRequest r) {
    return service.list(auth.current(r));
  }

  @PostMapping("/orchards")
  public Object create(@RequestBody Orchard o, HttpServletRequest r) {
    o.id = null;
    return service.save(o, auth.current(r));
  }

  @PutMapping("/orchards/{id}")
  public Object update(@PathVariable Long id, @RequestBody Orchard o, HttpServletRequest r) {
    o.id = id;
    return service.save(o, auth.current(r));
  }

  @DeleteMapping("/orchards/{id}")
  public Object delete(@PathVariable Long id, HttpServletRequest r) {
    service.delete(id, auth.current(r));
    return Map.of("message", "已删除果园及其业务数据");
  }

  @GetMapping("/devices")
  public Object devices(HttpServletRequest r) {
    return service.devices(auth.current(r));
  }

  @PostMapping("/devices")
  public Object device(@RequestBody Device d, HttpServletRequest r) {
    d.id = null;
    return service.saveDevice(d, auth.current(r));
  }

  @PutMapping("/devices/{id}")
  public Object deviceEdit(@PathVariable Long id, @RequestBody Device d, HttpServletRequest r) {
    d.id = id;
    return service.saveDevice(d, auth.current(r));
  }

  @DeleteMapping("/devices/{id}")
  public Object deviceDelete(@PathVariable Long id, HttpServletRequest r) {
    service.deleteDevice(id, auth.current(r));
    return Map.of("message", "已删除设备，历史数据保留");
  }

  @GetMapping("/orchards/{id}/threshold")
  public Object threshold(@PathVariable Long id, HttpServletRequest r) {
    service.access(id, auth.current(r));
    return service.threshold(id);
  }

  @PutMapping("/orchards/{id}/threshold")
  public Object thresholdSave(
      @PathVariable Long id, @RequestBody Threshold t, HttpServletRequest r) {
    service.saveThreshold(id, t, auth.current(r));
    return t;
  }
}
