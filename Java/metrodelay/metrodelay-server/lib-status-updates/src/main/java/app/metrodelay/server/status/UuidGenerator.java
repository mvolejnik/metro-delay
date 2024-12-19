package app.metrodelay.server.status;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.UUID;

/**
 * Generates UUID based on String object or as Pseudo Random UUID.
 * 
 * @author mvolejnik
 *
 */
public class UuidGenerator {

  private static final ThreadLocal<Random> PRNG = ThreadLocal.<Random>withInitial(() -> {return new Random();});
  
  private static final String HASH_ALGORITHM = "SHA-256";

  public static UUID generate(String value) throws StatusUpdateException {
    try {
      UUID uuid;
      MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
      byte[] digest = md.digest(value.getBytes());
      if (digest == null || digest.length < 16) {
        throw new StatusUpdateException("Digest not long enough!");
      }
      ByteBuffer bb = ByteBuffer.wrap(digest);
      uuid = new UUID(bb.getLong(), bb.getLong());
      return uuid;
    } catch (NoSuchAlgorithmException | BufferUnderflowException e) {
      throw new StatusUpdateException("Unable to instantiate MessageDigest!", e);
    }
  }
  
  public static UUID generate(){
    return new UUID(PRNG.get().nextLong(), PRNG.get().nextLong());
  }


}
