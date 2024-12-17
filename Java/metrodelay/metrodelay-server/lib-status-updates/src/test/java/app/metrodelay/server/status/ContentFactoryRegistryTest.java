/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit5TestClass.java to edit this template
 */
package app.metrodelay.server.status;

import app.metrodelay.server.status.cz.prg.dpp.DppFactory;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author mvolejnik
 */
public class ContentFactoryRegistryTest {
  
  public ContentFactoryRegistryTest() {
  }

  @Test
  public void get() {
    var factory = ContentFactoryRegistry.get("cz.prg.dpp");
    assertNotNull(factory);
    assertInstanceOf(DppFactory.class, factory);
  }
  
  @Test
  public void getUndefined() {
    var ex = assertThrows(NoSuchElementException.class, () -> ContentFactoryRegistry.get("un.defined.operator"));
    assertEquals("No value present", ex.getMessage());
  }
  
}
