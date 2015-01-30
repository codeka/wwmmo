package au.com.codeka.warworlds.api;

import com.google.protobuf.Message;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

/** Encapsulates a request to the server. */
public class ApiRequest {
  private final String url;
  @Nullable private final Message requestBody;
  @Nullable private Class<? extends Message> responseBodyClass;
  @Nullable private Message responseBody;
  @Nullable Map<String, List<String>> extraHeaders;

  protected ApiRequest(String url, Message requestBody,
      Class<? extends Message> responseBodyClass) {
    this.url = url;
    this.requestBody = requestBody;
    this.responseBodyClass = responseBodyClass;
  }
}
