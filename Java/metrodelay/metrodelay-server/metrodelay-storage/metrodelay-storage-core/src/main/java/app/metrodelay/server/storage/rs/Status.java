/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package app.metrodelay.server.storage.rs;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author mvolejnik
 */
@Path("/status")
public class Status {
  
  private static final Logger l = LogManager.getLogger(Status.class);
	
	@POST
  @Consumes(MediaType.APPLICATION_JSON)
	@Path("/countries/{country}/cities/{city}/operators/{operator}")
	public void status(){
		l.debug("status::");
	}
  
  
}
