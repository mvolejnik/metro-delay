package app.metrodelay.server.management;

import app.metrodelay.server.registry.ServiceRegistryClient;
import app.metrodelay.server.registry.ServiceRegistryClientImpl;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author mvolejnik
 */
@WebListener
public class RegistryInit implements ServletContextListener {

    public static final String CONTEXT_PARAM_INTERVAL = "schedulerjobinterval";
    public static final String REGISTRY_MULTICAST_IP = "registryMulticastIp";
    public static final String REGISTRY_MULTICAST_PORT = "registryMulticastPort";
    public static final String REGISTRY_STATUS_UPDATE_SERVICE_URI = "serviceStatusUpdate";
    private final ScheduledExecutorService scheduler
            = Executors.newScheduledThreadPool(1);
    private static final Logger l = LogManager.getLogger(RegistryInit.class);

    @Override
    public void contextInitialized(ServletContextEvent contextEvent) {
        l.info("contextInitialized::");
        try {
            ServletContext context = contextEvent.getServletContext();
            Duration interval = Duration.parse(context.getInitParameter(CONTEXT_PARAM_INTERVAL));
            ServiceRegistryClient registryClient = new ServiceRegistryClientImpl(
                    new InetSocketAddress(context.getInitParameter(REGISTRY_MULTICAST_IP).trim(), Integer.parseInt(context.getInitParameter(REGISTRY_MULTICAST_PORT).trim())),
                    new URI(context.getInitParameter(REGISTRY_STATUS_UPDATE_SERVICE_URI).trim()),
                    URI.create("http://localhost:8001" + context.getContextPath()).toURL());
            scheduler.scheduleWithFixedDelay(() -> {
                l.info("Register service {}", registryClient.getServiceUri());
                registryClient.register();
            }, 1, interval.toSeconds(), TimeUnit.SECONDS);
        } catch (MalformedURLException | URISyntaxException ex) {
            l.error("Unable to register service!", ex);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent arg0) {
    }
    
}
