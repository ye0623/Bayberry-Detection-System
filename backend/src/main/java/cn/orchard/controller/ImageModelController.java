package cn.orchard.controller;

import cn.orchard.service.*;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.*;
import org.springframework.web.bind.annotation.*;

/** 观测模块的模拟模型入口；没有增加 /api/hardware 下的摄像头或采集接口。 */
@RestController
@RequestMapping("/api/observations/model")
public class ImageModelController {
  private final PestImageModel model;
  private final OrchardService orchards;
  private final AuthService auth;

  public ImageModelController(PestImageModel model, OrchardService orchards, AuthService auth) {
    this.model = model;
    this.orchards = orchards;
    this.auth = auth;
  }

  public static class PredictRequest {
    @NotNull(message = "请选择所属果园")
    public Long orchardId;

    @NotBlank(message = "请先采集或上传图片")
    public String imageUrl;

    @NotBlank(message = "请选择演示场景")
    public String demoScenario;
  }

  @PostMapping("/predict")
  public Object predict(@Valid @RequestBody PredictRequest request, HttpServletRequest http) {
    orchards.access(request.orchardId, auth.current(http));
    return model.predict(request.imageUrl, request.demoScenario);
  }
}
