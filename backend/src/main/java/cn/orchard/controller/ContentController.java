package cn.orchard.controller;

import cn.orchard.entity.*;
import cn.orchard.service.*;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

/** 观测、预警处理及科普公告 API；公告写操作校验管理员权限。 */
@RestController
@RequestMapping("/api")
public class ContentController {
  private final ContentService service;
  private final AuthService auth;

  public ContentController(ContentService s, AuthService a) {
    service = s;
    auth = a;
  }

  @GetMapping("/observations")
  public Object records(
      @RequestParam(required = false) Long orchardId,
      @RequestParam(required = false) String pestType,
      HttpServletRequest r) {
    return service.observations(auth.current(r), orchardId, pestType);
  }

  @PostMapping("/observations")
  public Object observe(@RequestBody Observation o, HttpServletRequest r) {
    return service.observe(o, auth.current(r));
  }

  @GetMapping("/warnings")
  public Object warnings(HttpServletRequest r) {
    return service.warnings(auth.current(r));
  }

  @PutMapping("/warnings/{id}/handle")
  public Object handle(@PathVariable Long id, HttpServletRequest r) {
    service.handle(id, auth.current(r));
    return Map.of("message", "已标记处理");
  }

  @GetMapping("/announcements")
  public Object announcements() {
    return service.announcements();
  }

  @PostMapping("/announcements")
  public Object publish(@RequestBody Announcement n, HttpServletRequest r) {
    return service.publish(n, auth.current(r));
  }

  @DeleteMapping("/announcements/{id}")
  public Object delete(@PathVariable Long id, HttpServletRequest r) {
    service.deleteAnnouncement(id, auth.current(r));
    return Map.of("message", "已删除公告");
  }
}
