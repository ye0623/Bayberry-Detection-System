package cn.orchard.service;

import cn.orchard.common.ApiException;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** 本地图片存储，仅接收有真实图片签名的 JPG/PNG，随机文件名防止路径覆盖。 */
@Service
public class FileService {
  @Value("${app.upload-dir}")
  private String dir;

  public Map<String, String> upload(MultipartFile file) throws IOException {
    if (file.isEmpty() || file.getSize() > 5 * 1024 * 1024)
      throw new ApiException(400, "图片应为1字节至5MB");
    byte[] b = file.getBytes();
    String ext;
    if (b.length > 8 && (b[0] & 255) == 137 && b[1] == 80 && b[2] == 78 && b[3] == 71) ext = "png";
    else if (b.length > 3 && (b[0] & 255) == 255 && (b[1] & 255) == 216 && (b[2] & 255) == 255)
      ext = "jpg";
    else throw new ApiException(400, "仅支持真实JPG/PNG图片");
    if (ImageIO.read(new ByteArrayInputStream(b)) == null) throw new ApiException(400, "图片内容无效");
    Path base = Paths.get(dir).toAbsolutePath().normalize();
    Files.createDirectories(base);
    String name = UUID.randomUUID() + "." + ext;
    Files.write(base.resolve(name), b, StandardOpenOption.CREATE_NEW);
    return Map.of("url", "/uploads/" + name);
  }
}
