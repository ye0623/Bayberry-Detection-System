package cn.orchard.service;

import cn.orchard.common.ApiException;
import cn.orchard.entity.User;
import cn.orchard.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.http.HttpServletRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
  private final UserMapper users;
  private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
  private final Map<String, Session> sessions = new ConcurrentHashMap<>();

  private static class Session {
    Long id;
    long expires;
    String credentialHash;
    String role;

    Session(Long id, String credentialHash, String role) {
      this.id = id;
      this.credentialHash = credentialHash;
      this.role = role;
      expires = System.currentTimeMillis() + 86400000;
    }
  }

  public AuthService(UserMapper users) {
    this.users = users;
  }

  public User register(String username, String password, String nickname) {
    return createAccount(username, password, nickname, "FARMER", true);
  }

  /** 账号创建共用BCrypt流程；公开注册固定农户角色，管理入口另行鉴权。 */
  public User createAccount(
      String username, String password, String nickname, String role, Boolean enabled) {
    if (username == null || !username.matches("[a-zA-Z0-9_]{3,30}"))
      throw new ApiException(400, "用户名需3～30位字母、数字或下划线");
    validatePassword(password);
    if (nickname != null && nickname.length() > 30) throw new ApiException(400, "昵称不能超过30字");
    if (!List.of("FARMER", "ADMIN").contains(role == null ? "" : role))
      throw new ApiException(400, "角色不合法");
    User user = new User();
    user.username = username;
    user.password = encode(password);
    user.nickname = nickname == null || nickname.isBlank() ? username : nickname;
    user.role = role;
    user.enabled = enabled == null || enabled;
    user.createdAt = LocalDateTime.now();
    user.loginCount = 0L;
    users.insert(user);
    return safe(user);
  }

  public void validatePassword(String password) {
    if (password == null
        || password.length() < 6
        || password.getBytes(StandardCharsets.UTF_8).length > 72)
      throw new ApiException(400, "密码至少6位，长度过长时请缩短");
  }

  public String encode(String password) {
    validatePassword(password);
    return encoder.encode(password);
  }

  public void seed(String name, String role) {
    if (users.selectCount(new QueryWrapper<User>().eq("username", name)) == 0)
      createAccount(name, "123456", role.equals("ADMIN") ? "系统管理员" : "示范果农", role, true);
  }

  public void revokeSessions(Long id) {
    sessions.entrySet().removeIf(entry -> entry.getValue().id.equals(id));
  }

  public long sessionCount(Long id) {
    return sessions.values().stream()
        .filter(session -> session.id.equals(id) && session.expires > System.currentTimeMillis())
        .count();
  }

  public Map<String, Object> login(String name, String password) {
    User u = users.selectOne(new QueryWrapper<User>().eq("username", name));
    if (u == null || password == null || !encoder.matches(password, u.password))
      throw new ApiException(401, "用户名或密码错误");
    if (!Boolean.TRUE.equals(u.enabled)) throw new ApiException(403, "账号已停用，请联系管理员");
    LocalDateTime now = LocalDateTime.now();
    if (users.update(
            null,
            new UpdateWrapper<User>()
                .eq("id", u.id)
                .eq("enabled", true)
                .eq("password", u.password)
                .set("last_login_at", now)
                .setSql("login_count = login_count + 1"))
        == 0) throw new ApiException(401, "账号状态已改变，请重新登录");
    u.lastLoginAt = now;
    u.loginCount = (u.loginCount == null ? 0 : u.loginCount) + 1;
    sessions.entrySet().removeIf(e -> e.getValue().expires < System.currentTimeMillis());
    String token = UUID.randomUUID().toString();
    sessions.put(token, new Session(u.id, u.password, u.role));
    return Map.of("token", token, "user", safe(u));
  }

  public User authenticate(String header) {
    String token = header != null && header.startsWith("Bearer ") ? header.substring(7) : "";
    Session s = sessions.get(token);
    if (s == null || s.expires < System.currentTimeMillis())
      throw new ApiException(401, "请先登录或重新登录");
    User u = users.selectById(s.id);
    if (u == null) throw new ApiException(401, "用户不存在");
    if (!Boolean.TRUE.equals(u.enabled)) {
      revokeSessions(u.id);
      throw new ApiException(401, "账号已停用，请联系管理员");
    }
    if (!Objects.equals(s.credentialHash, u.password) || !Objects.equals(s.role, u.role)) {
      revokeSessions(u.id);
      throw new ApiException(401, "账号权限或密码已改变，请重新登录");
    }
    return safe(u);
  }

  public void logout(String header) {
    if (header != null && header.startsWith("Bearer ")) sessions.remove(header.substring(7));
  }

  public User current(HttpServletRequest r) {
    return (User) r.getAttribute("user");
  }

  public void admin(User u) {
    if (!"ADMIN".equals(u.role)) throw new ApiException(403, "仅管理员可操作");
  }

  private User safe(User u) {
    u.password = null;
    return u;
  }
}
