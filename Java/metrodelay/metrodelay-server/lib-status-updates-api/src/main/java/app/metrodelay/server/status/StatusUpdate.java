package app.metrodelay.server.status;

import java.net.URL;
import java.util.Collection;
import java.util.UUID;

public interface StatusUpdate {

  public UUID getUuid();

  public String getTitle();

  public String getDescription();

  public String getType();

  public Collection<String> getLines();

  public URL getInfoReference();

}
