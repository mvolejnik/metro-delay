/*
 * HTTP Client Status Update Notifier base on java HttpClient implementation.
 */
package app.metrodelay.server.notification.impl;

import app.metrodelay.server.Registry;
import app.metrodelay.server.notification.StatusUpdateNotification;
import app.metrodelay.server.notification.StatusUpdateNotifier;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author mvolejnik
 */
public class HttpClientNotifier implements StatusUpdateNotifier {

  private static final Logger l = LogManager.getLogger(HttpClientNotifier.class);

  @Override
  public void send(URL url, StatusUpdateNotification notification) {
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request;
    try {
      request = HttpRequest.newBuilder()
              .uri(url.toURI())
              .header("Content-Type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofInputStream(() -> notification.content()))
              .build();
      client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
              .thenApply(HttpResponse::statusCode)
              .thenAccept(s -> {
                l.info("send:: Status update notification status code [{}]", s);
              });
    } catch (URISyntaxException ex) {
      l.error("notification sending failed", ex);
    }
  }
}
