package cn.orchard.config;

import cn.orchard.entity.*;
import cn.orchard.mapper.*;
import cn.orchard.service.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class SeedData implements CommandLineRunner {
  private final AuthService auth;
  private final UserMapper users;
  private final OrchardService orchards;
  private final SimulationJob simulation;
  private final AnnouncementMapper announcements;
  private final ObservationMapper observations;

  @Value("${app.seed-enabled}")
  private boolean enabled;

  public SeedData(
      AuthService a,
      UserMapper u,
      OrchardService o,
      SimulationJob s,
      AnnouncementMapper n,
      ObservationMapper b) {
    auth = a;
    users = u;
    orchards = o;
    simulation = s;
    announcements = n;
    observations = b;
  }

  @Override
  public void run(String... args) {
    if (!enabled) return;
    auth.seed("admin", "ADMIN");
    auth.seed("farmer", "FARMER");
    User farmer = users.selectOne(new QueryWrapper<User>().eq("username", "farmer"));
    if (orchards.orchardMapper().selectCount(null) == 0) {
      String[] names = {"东魁 · 山麓一号园", "荸荠种 · 溪畔二号园"};
      for (int i = 0; i < 2; i++) {
        Orchard o = new Orchard();
        o.name = names[i];
        o.location = i == 0 ? "浙江台州 · 仙居县田市镇" : "浙江宁波 · 余姚市丈亭镇";
        o.area = i == 0 ? 36.5 : 24.;
        o.variety = i == 0 ? "东魁" : "荸荠种";
        o.imageUrl = i == 0 ? "/demo-orchards/foothill-orchard.png" : "/demo-orchards/riverside-orchard.png";
        o.fruiting = true;
        orchards.save(o, farmer);
        Device d = new Device();
        d.orchardId = o.id;
        d.deviceSn = "YM-2026-00" + (i + 1);
        d.location = "果园中心 · 距地面1.5米";
        orchards.saveDevice(d, farmer);
      }
      for (int i = 19; i >= 0; i--) simulation.generate(LocalDateTime.now().minusSeconds(i * 15L));
    }
    seedObservations(farmer);
    if (announcements.selectCount(null) == 0) {
      Announcement n = new Announcement();
      n.title = "杨梅果实期巡园与高湿环境管理";
      n.content =
          "关注连续高湿天气，及时疏通排水沟、清理病叶与落果。果实期结合果蝇诱捕监测、套袋和卫生管理。环境预警仅提示诱发风险，需实地确认后开展防治。卷叶蛾请结合虫苞、卷叶等观测记录判断。";
      n.createdAt = LocalDateTime.now();
      announcements.insert(n);
    }
  }

  /** 仅在演示初始化启用时写入示例；按果园和描述检查重复，不影响真实观测记录。 */
  private void seedObservations(User farmer) {
    var plots = orchards.list(farmer);
    if (plots.isEmpty()) return;
    String[][] examples = {
      {"褐斑病", "轻度", "brown-spot", "[模拟示例]一号园东侧巡查20片叶，3片出现零星褐色斑点，暂未见明显落叶；记录位置并安排复查。"},
      {"褐斑病", "中度", "brown-spot", "[模拟示例]雨后抽查30片叶，9片可见多处褐色病斑，部分斑点连片；建议加强通风、清理病叶，并结合实地检查确认。"},
      {"癌肿病", "轻度", "canker", "[模拟示例]巡查发现1株杨梅侧枝出现局部粗糙瘤状隆起，主干暂未见明显异常；标记枝位，后续由农技人员确认。"},
      {"癌肿病", "中度", "canker", "[模拟示例]溪畔低洼区域2株树枝条出现瘤状膨大，土壤偏湿；先排查积水和机械伤口，做好独立观测记录。"},
      {"卷叶蛾", "轻度", "leafroller", "[模拟示例]抽查10株树梢，发现2处卷叶和少量丝状物，其中1处可见绿色幼虫；记录虫苞位置并复查。"},
      {"卷叶蛾", "中度", "leafroller", "[模拟示例]树冠外围多片嫩叶卷曲，局部叶缘有咀嚼缺刻，抽查发现6个虫苞；建议清除虫叶并持续观察。"},
      {"果蝇", "中度", "fruit-fly", "[模拟示例]模拟果实期巡园，成熟果附近出现小型飞虫，少量落果变软；记录诱捕和落果情况，及时清理腐烂果。"},
      {"果蝇", "重度", "fruit-fly", "[模拟示例]模拟采收前巡查，多个点位成熟果受损、落果增多，个别软果内见浅色幼虫；需结合诱捕监测与专业鉴定确认虫种。"}
    };
    for (int i = 0; i < examples.length; i++) {
      String[] e = examples[i];
      Long orchardId = plots.get(i % plots.size()).id;
      if (observations.selectCount(
              new QueryWrapper<Observation>().eq("orchard_id", orchardId).eq("description", e[3]))
          > 0) continue;
      Observation record = new Observation();
      record.orchardId = orchardId;
      record.pestType = e[0];
      record.severity = e[1];
      record.imageUrl = "/demo-pests/" + e[2] + ".png";
      record.description = e[3];
      record.suggestion = RiskService.advice(e[0]);
      record.createdAt = LocalDateTime.now().minusDays(7 - i).minusHours(i);
      observations.insert(record);
    }
  }
}
