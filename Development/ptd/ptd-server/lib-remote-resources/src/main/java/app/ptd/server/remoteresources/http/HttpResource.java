package app.ptd.server.remoteresources.http;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import app.ptd.server.remoteresources.RemoteResourceException;
import app.ptd.server.remoteresources.Resource;
import app.ptd.server.remoteresources.ResourceImpl;
import java.util.Objects;

public class HttpResource implements AutoCloseable {

  private static final Logger l = LogManager.getLogger(HttpResource.class);

  private static final String ETAG_HEADER = "ETag";
  private static final String ETAG_IF_NONE_MATCH = "If-None-Match";
  private static final String ETAG_IF_MODIFIED_SINCE = "If-Modified-Since";

  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.RFC_1123_DATE_TIME;
  CloseableHttpClient httpclient;

  public HttpResource() {
    httpclient = HttpClients.createDefault();
  }

  /**
   * Fetches remote resource.
   *
   * @param resourceUrl resource UrL
   * @return resource content or empty Optional instance
   * @throws RemoteResourceException if resource cannot be downloaded
   */
  public Optional<Resource> content(URL resourceUrl) throws RemoteResourceException {
    return content(resourceUrl, Optional.empty(), Optional.empty());
  }

  /**
   * Fetches remote resource.
   *
   * @param resourceUrl resource URL
   * @param etag ETag of the already downloaded resource
   * @param ifModifiedSince timestamp of already downloaded resource
   * @return resource or empty Optional instance if resource was not modified since last fetch or has no content
   * @throws RemoteResourceException if resource cannot be downloaded
   */
  public Optional<Resource> content(URL resourceUrl, Optional<String> etag, Optional<ZonedDateTime> ifModifiedSince) throws RemoteResourceException {
    l.debug("content::");
    Objects.nonNull(resourceUrl);
    Objects.nonNull(etag);
    Objects.nonNull(ifModifiedSince);
    if (ifModifiedSince.isPresent() && ZonedDateTime.now().isBefore(ifModifiedSince.get())) {
      throw new IllegalArgumentException("ifModifiedSince cannot be from future.");
    }
    try {
      Optional<Resource> resource;
      HttpGet httpGet = new HttpGet(resourceUrl.toURI());
      etag.ifPresent(etagValue -> httpGet.addHeader(ETAG_IF_NONE_MATCH, etagValue));
      if (ifModifiedSince.isPresent()) {
        httpGet.addHeader(ETAG_IF_MODIFIED_SINCE, DATE_TIME_FORMATTER.format(ifModifiedSince.get()));
      }
      l.debug("content:: downloading url '{}'", resourceUrl);
      var response = httpclient.execute(httpGet);
      var statusCode = new ResponseStatusCode(response.getStatusLine().getStatusCode());
      l.debug("content:: response status code '{}'", statusCode);
      if (statusCode.isOk()) {
        var entity = response.getEntity();
        if (entity == null) {
          l.error("content:: Server response does not contain any data.");
          throw new RemoteResourceException("Missing server data.");
        }
        var responseEtagHeader = response.getFirstHeader(ETAG_HEADER);
        l.debug("content:: etag header '{}'", responseEtagHeader);
        resource = Optional.of(new ResourceImpl(entity.getContent(), responseEtagHeader == null ? null : responseEtagHeader.getValue()));
      } else if (statusCode.isNotUpdated()) {
        resource = Optional.empty();
      } else if (statusCode.isClientError()) {
        l.error("content:: Unable to get resource '{}' due to client error: '{}' ({}).", resourceUrl, statusCode.statusCode,
                response.getStatusLine().getReasonPhrase());
        throw new RemoteResourceException(String.format("Unable to get resource '%s' due to client error: '%s' (%s).",
                resourceUrl, statusCode.statusCode, response.getStatusLine().getReasonPhrase()));
      } else {
        l.warn("content:: Resource '{}' unexpected response '{}' ({}).", resourceUrl, statusCode.statusCode,
                response.getStatusLine().getReasonPhrase());
        HttpEntity entity = response.getEntity();
        resource = entity == null ? Optional.empty() : Optional.of(new ResourceImpl(entity.getContent()));
      }

      return resource;
    } catch (URISyntaxException | IOException e) {
      l.error("content::", e);
      throw new RemoteResourceException(e);
    }

  }

  @Override
  public void close() throws IOException {
    httpclient.close();
  }

  private static class ResponseStatusCode {

    int statusCode;

    ResponseStatusCode(int statusCode) {
      if (statusCode < 100 || statusCode > 599) {
        throw new IllegalArgumentException("Illegal HTTP Status code, expected range: 100 - 599.");
      }
      this.statusCode = statusCode;
    }

    public boolean isClientError() {
      return statusCode >= 400 && statusCode <= 499;
    }

    public boolean isOk() {
      return statusCode == 200;
    }

    public boolean isNotUpdated() {
      return statusCode == 304;
    }

    @Override
    public String toString() {
      return "ResponseStatusCode{" + "statusCode=" + statusCode + '}';
    }

  }

}
