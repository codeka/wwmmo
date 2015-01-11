package au.com.codeka.warworlds.api;

import com.google.protobuf.Message;

import javax.annotation.Nullable;

/** Encapsulates a request to the server. */
public class ApiRequest {
  private final String url;
  @Nullable private final Message requestBody;
  @Nullable private Class<? extends Message> responseBodyClass;
  @Nullable private Message responseBody;

  protected ApiRequest(String url, Message requestBody,
      Class<? extends Message> responseBodyClass) {
    this.url = url;
    this.requestBody = requestBody;
    this.responseBodyClass = responseBodyClass;
  }
}
