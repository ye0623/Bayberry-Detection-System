package cn.orchard.service;

import cn.orchard.common.ApiException;
import cn.orchard.entity.*;
import cn.orchard.mapper.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Service;

/** 实地观测、防治建议、预警处理与管理员公告业务。 */
@Service
public class ContentService {
  private final OrchardService orchards;
  private final ObservationMapper observations;
  private final RiskWarningMapper warnings;
  private final AnnouncementMapper announcements;
  private final AuthService auth;

  public ContentService(
      OrchardService o,
      ObservationMapper b,
      RiskWarningMapper w,
      AnnouncementMapper n,
      AuthService a) {
    orchards = o;
    observations = b;
    warnings = w;
    announcements = n;
    auth = a;
  }

  public Object observations(User u, Long orchardId, String pest) {
    List<Long> ids = orchards.ids(u);
    if (ids.isEmpty()) return List.of();
    if (orchardId != null) orchards.access(orchardId, u);
    return observations.selectList(
        new QueryWrapper<Observation>()
            .in("orchard_id", ids)
            .eq(orchardId != null, "orchard_id", orchardId)
            .eq(pest != null && !pest.isBlank(), "pest_type", pest)
            .orderByDesc("id")
            .last("LIMIT 300"));
  }

  public Object observe(Observation o, User u) {
    orchards.access(o.orchardId, u);
    if (!List.of("褐斑病", "癌肿病", "卷叶蛾", "果蝇").contains(o.pestType == null ? "" : o.pestType)
        || !List.of("轻度", "中度", "重度").contains(o.severity == null ? "" : o.severity)
        || o.description == null
        || o.description.isBlank()
        || o.description.length() > 2000) throw new ApiException(400, "请填写病虫害类型、程度及2000字内描述");
    o.id = null;
    o.createdAt = LocalDateTime.now();
    o.suggestion = RiskService.advice(o.pestType);
    observations.insert(o);
    return o;
  }

  public Object warnings(User u) {
    List<Long> ids = orchards.ids(u);
    return ids.isEmpty()
        ? List.of()
        : warnings.selectList(
            new QueryWrapper<RiskWarning>()
                .in("orchard_id", ids)
                .orderByDesc("id")
                .last("LIMIT 300"));
  }

  public void handle(Long id, User u) {
    RiskWarning w = warnings.selectById(id);
    if (w == null) throw new ApiException(404, "预警不存在");
    orchards.access(w.orchardId, u);
    w.handled = true;
    warnings.updateById(w);
  }

  public Object announcements() {
    return announcements.selectList(new QueryWrapper<Announcement>().orderByDesc("id"));
  }

  public Object publish(Announcement n, User u) {
    auth.admin(u);
    if (n.title == null
        || n.title.isBlank()
        || n.title.length() > 150
        || n.content == null
        || n.content.isBlank()
        || n.content.length() > 3500) throw new ApiException(400, "请填写150字内标题和3500字内正文");
    n.id = null;
    n.createdAt = LocalDateTime.now();
    announcements.insert(n);
    return n;
  }

  public void deleteAnnouncement(Long id, User u) {
    auth.admin(u);
    announcements.deleteById(id);
  }
}
