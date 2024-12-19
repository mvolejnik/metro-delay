package app.metrodelay.server.storage.rs;

import app.metrodelay.server.status.StatusUpdate;
import app.metrodelay.server.status.StatusUpdateImpl;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;
import java.io.InputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author mvolejnik
 */
@Path("/")
public class Status {
  
  private static final Logger l = LogManager.getLogger(Status.class);
	
	@PUT
  @Consumes(MediaType.APPLICATION_JSON)
	@Path("/countries/{country}/cities/{city}/operators/{operator}/updates/{guid}")
	public void status( @PathParam("guid") String guid, InputStream statusUpdate){
		l.debug("status:: {}", guid);
    Jsonb jsonb = JsonbBuilder.create();
    var update = jsonb.fromJson(statusUpdate, StatusUpdateImpl.class);
    l.debug("status:: {}", update);
	}
  
  
}
