package cn.orchard.service;

import cn.orchard.common.ApiException;
import cn.orchard.entity.*;
import cn.orchard.mapper.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 管理员账号管理。停用保留业务数据；密码不返回前端。 */
@Service
public class UserManagementService {
  private final UserMapper users;
  private final OrchardMapper orchards;
  private final AuthService auth;

  public UserManagementService(UserMapper users, OrchardMapper orchards, AuthService auth) {
    this.users = users;
    this.orchards = orchards;
    this.auth = auth;
  }

  public Map<String, Object> list(
      User actor, String keyword, String role, Boolean enabled, int page, int size) {
    auth.admin(actor);
    if (page < 1 || page > 100000 || size < 1 || size > 100) throw new ApiException(400, "分页参数不合法");
    if (role != null && !role.isBlank() && !List.of("FARMER", "ADMIN").contains(role))
      throw new ApiException(400, "角色不合法");
    if (keyword != null && keyword.length() > 50) throw new ApiException(400, "查询内容不能超过50字");
    String key = keyword == null ? "" : keyword.trim();
    QueryWrapper<User> query = new QueryWrapper<>();
    if (!key.isEmpty()) query.and(q -> q.like("username", key).or().like("nickname", key));
    query.eq(role != null && !role.isBlank(), "role", role).eq(enabled != null, "enabled", enabled);
    long total = users.selectCount(query);
    List<Map<String, Object>> rows = new ArrayList<>();
    for (User user :
        users.selectList(
            query.orderByDesc("id").last("LIMIT " + size + " OFFSET " + ((page - 1) * size)))) {
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("id", user.id);
      row.put("username", user.username);
      row.put("nickname", user.nickname);
      row.put("role", user.role);
      row.put("enabled", user.enabled);
      row.put("createdAt", user.createdAt);
      row.put("lastLoginAt", user.lastLoginAt);
      row.put("loginCount", user.loginCount);
      row.put(
          "orchardCount",
          orchards.selectCount(new QueryWrapper<Orchard>().eq("owner_id", user.id)));
      row.put("activeSessions", auth.sessionCount(user.id));
      rows.add(row);
    }
    Map<String, Object> stats =
        Map.of(
            "total",
            users.selectCount(null),
            "enabled",
            users.selectCount(new QueryWrapper<User>().eq("enabled", true)),
            "disabled",
            users.selectCount(new QueryWrapper<User>().eq("enabled", false)),
            "admins",
            users.selectCount(new QueryWrapper<User>().eq("role", "ADMIN")));
    return Map.of("records", rows, "total", total, "page", page, "size", size, "stats", stats);
  }

  /** 按固定顺序锁住管理员行，串行校验管理权限和最后一个可用管理员，防止并发降权。 */
  private void lockAdministrators(User actor) {
    users.selectList(
        new QueryWrapper<User>().eq("role", "ADMIN").orderByAsc("id").last("FOR UPDATE"));
    User current = users.selectById(actor.id);
    if (current == null || !Boolean.TRUE.equals(current.enabled))
      throw new ApiException(403, "账号不可用");
    auth.admin(current);
  }

  private User target(Long id) {
    User user = users.selectById(id);
    if (user == null) throw new ApiException(404, "账号不存在");
    return user;
  }

  @Transactional
  public User create(
      User actor, String username, String password, String nickname, String role, Boolean enabled) {
    lockAdministrators(actor);
    return auth.createAccount(username, password, nickname, role, enabled);
  }

  @Transactional
  public void update(User actor, Long id, String nickname, String role, Boolean enabled) {
    lockAdministrators(actor);
    User user = target(id);
    if (nickname == null || nickname.isBlank() || nickname.length() > 30)
      throw new ApiException(400, "请填写30字以内的昵称");
    if (!List.of("ADMIN", "FARMER").contains(role == null ? "" : role) || enabled == null)
      throw new ApiException(400, "请填写有效角色和账号状态");
    if (actor.id.equals(id) && (!enabled || !role.equals(user.role)))
      throw new ApiException(400, "不能停用或更改自己的角色");
    boolean wasAdmin = "ADMIN".equals(user.role) && Boolean.TRUE.equals(user.enabled),
        willAdmin = "ADMIN".equals(role) && enabled;
    if (wasAdmin
        && !willAdmin
        && users.selectCount(new QueryWrapper<User>().eq("role", "ADMIN").eq("enabled", true)) <= 1)
      throw new ApiException(400, "至少保留一个可用管理员");
    boolean revoke = !Objects.equals(user.role, role) || !Objects.equals(user.enabled, enabled);
    user.password = null;
    user.nickname = nickname;
    user.role = role;
    user.enabled = enabled;
    users.updateById(user);
    if (revoke) auth.revokeSessions(id);
  }

  @Transactional
  public void resetPassword(User actor, Long id, String password) {
    lockAdministrators(actor);
    User user = target(id);
    String hash = auth.encode(password);
    User patch = new User();
    patch.id = user.id;
    patch.password = hash;
    users.updateById(patch);
    auth.revokeSessions(id);
  }
}
