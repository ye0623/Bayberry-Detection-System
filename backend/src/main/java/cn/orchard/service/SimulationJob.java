package cn.orchard.service;

import cn.orchard.controller.HardwareRequests;
import cn.orchard.entity.Device;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 仅开发演示使用。自动任务复用上传业务，无额外硬件接口。 */
@Component
public class SimulationJob {
  private final OrchardService orchards;
  private final MonitoringService monitoring;

  @Value("${app.simulation-enabled}")
  private boolean enabled;

  public SimulationJob(OrchardService o, MonitoringService m) {
    orchards = o;
    monitoring = m;
  }

  @Scheduled(initialDelay = 15000, fixedDelay = 15000)
  public void tick() {
    if (enabled) generate(LocalDateTime.now());
  }

  public void generate(LocalDateTime time) {
    for (Device d : orchards.deviceMapper().selectList(null)) {
      try {
        HardwareRequests.Upload r = new HardwareRequests.Upload();
        r.deviceSn = d.deviceSn;
        var random = ThreadLocalRandom.current();
        r.temperature = Math.round(random.nextDouble(20, 32) * 10) / 10.;
        r.humidity = Math.round(random.nextDouble(65, 96) * 10) / 10.;
        r.soilHumidity = Math.round(random.nextDouble(50, 91) * 10) / 10.;
        r.light = (double) random.nextInt(10000, 65000);
        r.co2 = (double) random.nextInt(380, 850);
        r.collectTime = time;
        monitoring.upload(r, null);
      } catch (Exception e) {
        org.slf4j.LoggerFactory.getLogger(getClass())
            .warn("模拟设备 {} 采集失败：{}", d.deviceSn, e.getMessage());
      }
    }
  }
}
