package app.metrodelay.server.status;

import app.metrodelay.server.status.cz.prg.dpp.DppFactory;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/// Operators content factory registry
public class ContentFactoryRegistry {
  
  private static Map<String, Class<? extends OperatorFactory>> REGISTRY = Map.of(
          "cz.prg.dpp", DppFactory.class
  );
  
  private static final Logger l = LogManager.getLogger(ContentFactoryRegistry.class);
  
  // Returns operator content factory
  public static OperatorFactory get(String operatorId){
    var clazz = Optional.ofNullable(REGISTRY.get(operatorId)).orElseThrow();
    try {
      return clazz.getDeclaredConstructor().newInstance();
    } catch (NoSuchMethodException|SecurityException|InstantiationException|IllegalAccessException|IllegalArgumentException|InvocationTargetException ex) {
      throw new IllegalStateException(String.format("ContentFactory does not exist for '%s'", operatorId), ex);
    }
  }
  
}
