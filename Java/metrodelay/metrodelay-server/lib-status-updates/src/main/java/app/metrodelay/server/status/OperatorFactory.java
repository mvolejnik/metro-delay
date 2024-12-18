/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package app.metrodelay.server.status;

import app.metrodelay.StatusUpdateException;
import app.metrodelay.StatusUpdate;
import java.io.InputStream;
import java.util.List;

/**
 *
 * @author mvolejnik
 */
public interface OperatorFactory {
  List<StatusUpdate> statusUpdates(InputStream contentInputStream) throws StatusUpdateException;
}
