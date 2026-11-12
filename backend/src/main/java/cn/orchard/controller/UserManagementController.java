package cn.orchard.controller;

import cn.orchard.service.*;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.*;
import org.springframework.web.bind.annotation.*;

/** 所有账号管理请求在服务层再次校验管理员权限。 */
@RestController
@RequestMapping("/api/admin/users")
public class UserManagementController {
  private final UserManagementService service;
  private final AuthService auth;

  public UserManagementController(UserManagementService service, AuthService auth) {
    this.service = service;
    this.auth = auth;
  }

  public static class CreateRequest {
    @NotBlank public String username;
    @NotBlank public String password;
    @NotBlank public String nickname;
    @NotBlank public String role;
    @NotNull public Boolean enabled;
  }

  public static class UpdateRequest {
    @NotBlank public String nickname;
    @NotBlank public String role;
    @NotNull public Boolean enabled;
  }

  public static class PasswordRequest {
    @NotBlank public String password;
  }

  @GetMapping
  public Object list(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String role,
      @RequestParam(required = false) Boolean enabled,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "10") int size,
      HttpServletRequest http) {
    return service.list(auth.current(http), keyword, role, enabled, page, size);
  }

  @PostMapping
  public Object create(@Valid @RequestBody CreateRequest r, HttpServletRequest http) {
    return service.create(
        auth.current(http), r.username, r.password, r.nickname, r.role, r.enabled);
  }

  @PutMapping("/{id}")
  public Object update(
      @PathVariable Long id, @Valid @RequestBody UpdateRequest r, HttpServletRequest http) {
    service.update(auth.current(http), id, r.nickname, r.role, r.enabled);
    return Map.of("message", "账号信息已更新");
  }

  @PutMapping("/{id}/password")
  public Object reset(
      @PathVariable Long id, @Valid @RequestBody PasswordRequest r, HttpServletRequest http) {
    service.resetPassword(auth.current(http), id, r.password);
    return Map.of("message", "密码已重置，该账号需重新登录");
  }
}
