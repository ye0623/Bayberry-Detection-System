package cn.orchard.config;

import cn.orchard.service.AuthService;
import java.nio.file.*;
import javax.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfig implements WebMvcConfigurer {
  private final AuthService auth;

  @Value("${app.upload-dir}")
  private String uploadDir;

  public WebConfig(AuthService auth) {
    this.auth = auth;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry
        .addInterceptor(
            new HandlerInterceptor() {
              @Override
              public boolean preHandle(
                  HttpServletRequest req, HttpServletResponse res, Object handler) {
                req.setAttribute("user", auth.authenticate(req.getHeader("Authorization")));
                return true;
              }
            })
        .addPathPatterns("/api/**")
        .excludePathPatterns(
            "/api/auth/login",
            "/api/auth/register",
            "/api/hardware/sensor/upload",
            "/api/hardware/device/control");
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry
        .addResourceHandler("/uploads/**")
        .addResourceLocations(Paths.get(uploadDir).toAbsolutePath().toUri().toString() + "/");
  }
}
