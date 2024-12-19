package app.metrodelay.server.storage;

import app.metrodelay.server.management.RegistryInit;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.glassfish.jersey.servlet.ServletContainer;

public class App {

  private static final String ARG_PORT = "port";
  private static final String DEFAULT_PORT = "8001";

  public static org.eclipse.jetty.server.Server server(int port) {
    org.eclipse.jetty.server.Server server = new org.eclipse.jetty.server.Server(port);
    return server;
  }

  private static Options options() {
    var options = new Options();
    options.addOption("p", ARG_PORT, true, "server port");
    return options;
  }

  private static ContextHandler restHandler() {
    ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
    context.setContextPath("/");
    ServletHandler handler = new ServletHandler();
    context.setHandler(handler);
    context.setWelcomeFiles(new String[]{"index.jsp"});
    ServletHolder servletHolder = handler.addServletWithMapping(ServletContainer.class, "/api/*");
    servletHolder.setInitOrder(1);
    servletHolder.setInitParameter("jersey.config.server.provider.packages", "app.metrodelay.server.storage.rs");
    servletHolder.setInitParameter("jersey.config.server.tracing.type", "ALL");
    servletHolder.setInitParameter("jersey.config.server.tracing.threshold", "TRACE");
    servletHolder.setInitParameter("jersey.config.server.logging.logger.level", "DEBUG");
    context.setInitParameter(RegistryInit.REGISTRY_MULTICAST_IP, "233.146.53.48");
    context.setInitParameter(RegistryInit.REGISTRY_MULTICAST_PORT, "6839");
    context.setInitParameter(RegistryInit.REGISTRY_STATUS_UPDATE_SERVICE_URI, "urn:metrodelay.app:service:statusupdate:1.0");
    context.setInitParameter(RegistryInit.CONTEXT_PARAM_INTERVAL, "PT1M");
    context.addEventListener(new RegistryInit());
    return context;
  }

  public static void main(String[] args) throws InterruptedException, Exception {
        CommandLine line = new DefaultParser().parse( options(), args );
        org.eclipse.jetty.server.Server server = server(Integer.parseInt(line.getOptionValue(ARG_PORT, DEFAULT_PORT)));
        server.setHandler(restHandler());
        server.start();
        server.join();
  }
}
