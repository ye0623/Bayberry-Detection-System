package cn.orchard;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 杨梅果园物联网演示入口，启动自动建表、初始化演示数据。 */
@SpringBootApplication
@EnableScheduling
@MapperScan("cn.orchard.mapper")
public class OrchardApplication {
  public static void main(String[] args) {
    SpringApplication.run(OrchardApplication.class, args);
  }
}
