package app.metrodelay.server.status;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import app.metrodelay.server.status.UuidGenerator;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class UuidGeneratorTest {

	@Test
  @Tag("fast")
  @DisplayName("Two unique UUID.")
	public void testGenerateUuidNotEq() throws StatusUpdateException {
		UUID uuid1 = UuidGenerator.generate("a");
		UUID uuid2 = UuidGenerator.generate("b");
		assertNotEquals(uuid1.toString(), uuid2.toString());
	}
	
	@Test
  @Tag("fast")
  @DisplayName("Two equals UUID.")
	public void testGenerateUuidEq() throws StatusUpdateException {
		UUID uuid1 = UuidGenerator.generate("abcdafghijklmnopqrstuvwxyz");
		UUID uuid2 = UuidGenerator.generate("abcdafghijklmnopqrstuvwxyz");
		assertEquals(uuid1.toString(), uuid2.toString());
	}
	
	@Test
  @Tag("fast")
  @DisplayName("Random UUID not equal.")
  public void testGenerateRandomUuidNotEq() throws StatusUpdateException {
    UUID uuid1 = UuidGenerator.generate();
    UUID uuid2 = UuidGenerator.generate();
    assertNotEquals(uuid1.toString(), uuid2.toString());
  }

}
