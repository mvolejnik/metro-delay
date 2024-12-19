package app.metrodelay.server.scheduler;

import app.metrodelay.server.status.StatusUpdateException;
import app.metrodelay.server.Registry;
import app.metrodelay.server.notification.impl.HttpClientNotifier;
import app.metrodelay.server.notification.impl.StatusUpdateNotificationImpl;
import app.metrodelay.server.remoteresources.http.HttpResource;
import app.metrodelay.server.remoteresources.RemoteResourceException;
import app.metrodelay.server.status.ContentFactoryRegistry;
import app.metrodelay.server.status.ResourceCache;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class GetUrlResourceJob implements Job {

    private static final ResourceCache RESOURCE_CACHE = new ResourceCache();
    private static final int NOTIFICATION_POOL_SIZE = 5; 
    private static final ExecutorService NOTIFICATION_EXECUTOR = Executors.newFixedThreadPool(NOTIFICATION_POOL_SIZE);
    static final String DATA_OPERATOR = "opr";
    static final String DATA_URL = "url";
    private static final Logger l = LogManager.getLogger(GetUrlResourceJob.class);
    private static final String STORAGE_PATH = "/countries/%s/cities/%s/operators/%s";

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    l.info("execute:: job [{}] started", context.getJobDetail().getKey());
    var operatorParam = context.getJobDetail().getJobDataMap().getString(DATA_OPERATOR);
    var urlParam = context.getJobDetail().getJobDataMap().getString(DATA_URL);
    l.debug("execute:: resource url '{}'", urlParam);
    try {
      var url = new URL(urlParam);
      var cached = RESOURCE_CACHE.resource(url);
      l.debug("execute:: resource '{}' already in cache '{}' with fingerprint '{}'", url, cached.isPresent(), cached.isPresent() ? cached.get().digest() : "");
      l.debug("execute:: getting resource '{}'", url.toExternalForm());
      var resource = new HttpResource().content(url, cached.isPresent() ? cached.get().fingerprint().orElse(null) : null, null);
      l.debug("execute:: resource has content '{}'", resource.isPresent());
      resource.ifPresent(r -> {
        RESOURCE_CACHE.resource(url, r);
        notifyServices(operatorParam, r.content().get());
      });
    } catch (MalformedURLException | RemoteResourceException e) {
      l.error("Incorrect URL to download resource '{}'", urlParam);
      l.info("execute:: job [{}] finished ✗", context.getJobDetail().getKey());
      throw new JobExecutionException(String.format("Unable to download resource '%s'", urlParam), e);
    } catch (Exception e){
      l.info("execute:: job [{}] finished ✗", context.getJobDetail().getKey());
      l.error("Exception has occured", e);
      throw e;
    }
    l.info("execute:: job [{}] finished ✓", context.getJobDetail().getKey());
  }
  
  protected void notifyServices(String operatorId, InputStream content){
      var baseUrl = Registry.serviceRegistry().get(URI.create("urn:metrodelay.app:service:statusupdate:1.0"));
      var contentFactory = ContentFactoryRegistry.get(operatorId);
      if (baseUrl.isPresent()) {
        try {
          contentFactory.statusUpdates(content);
          var operatorPathParts = operatorId.split("\\.", 3);
          var serviceUrl = new URI(baseUrl.get().toString() + STORAGE_PATH.formatted(operatorPathParts[0], operatorPathParts[1], operatorPathParts[2])).toURL();
          NOTIFICATION_EXECUTOR.submit(
                  () -> new HttpClientNotifier().send(serviceUrl, new StatusUpdateNotificationImpl(content)));
        } catch (MalformedURLException|URISyntaxException ex) {
          l.error("unable to send {} notification to {}", operatorId, baseUrl, ex);
        } catch (StatusUpdateException ex) {
            l.error("unable to parse notification", ex);
          }
      }
  }

}
