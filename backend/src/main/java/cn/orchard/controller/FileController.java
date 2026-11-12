package cn.orchard.controller;

import cn.orchard.service.FileService;
import java.io.IOException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** 受登录保护的图片上传入口，资源由本地 uploads 目录提供。 */
@RestController
@RequestMapping("/api/files")
public class FileController {
  private final FileService files;

  public FileController(FileService f) {
    files = f;
  }

  @PostMapping
  public Object upload(@RequestParam("file") MultipartFile file) throws IOException {
    return files.upload(file);
  }
}
