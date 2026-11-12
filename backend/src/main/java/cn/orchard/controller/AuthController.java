package cn.orchard.controller;

import cn.orchard.service.AuthService;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

/** 用户注册登录入口；新注册用户固定为农户角色。 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final AuthService auth;

  public AuthController(AuthService a) {
    auth = a;
  }

  @PostMapping("/register")
  public Object register(@RequestBody Map<String, String> r) {
    return auth.register(r.get("username"), r.get("password"), r.get("nickname"));
  }

  @PostMapping("/login")
  public Object login(@RequestBody Map<String, String> r) {
    return auth.login(r.get("username"), r.get("password"));
  }

  @GetMapping("/me")
  public Object me(HttpServletRequest r) {
    return auth.current(r);
  }

  @PostMapping("/logout")
  public Object logout(HttpServletRequest r) {
    auth.logout(r.getHeader("Authorization"));
    return Map.of("message", "已退出");
  }
}
