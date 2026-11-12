package cn.orchard.common;

/** 携带 HTTP 状态的业务异常。 */
public class ApiException extends RuntimeException {
  public final int status;

  public ApiException(int status, String message) {
    super(message);
    this.status = status;
  }
}
