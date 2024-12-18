/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package app.metrodelay.server;

import app.metrodelay.server.registry.ServiceRegistry;

/**
 *
 * @author mvolejnik
 */
public class Registry {

  private static ServiceRegistry registry;

  static void serviceRegistry(ServiceRegistry registry) {
    synchronized (Registry.class) {
      if (Registry.registry == null) {
        Registry.registry = registry;
      }
    }
  }

  public static ServiceRegistry serviceRegistry() {
    return registry;
  }

}
