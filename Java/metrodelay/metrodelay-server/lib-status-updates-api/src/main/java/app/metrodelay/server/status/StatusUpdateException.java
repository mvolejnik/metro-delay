package app.metrodelay.server.status;

public class StatusUpdateException extends Exception {

  public StatusUpdateException() {
  }

  public StatusUpdateException(String message) {
    super(message);
  }

  public StatusUpdateException(Throwable cause) {
    super(cause);
  }

  public StatusUpdateException(String message, Throwable cause) {
    super(message, cause);
  }

  public StatusUpdateException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

}
