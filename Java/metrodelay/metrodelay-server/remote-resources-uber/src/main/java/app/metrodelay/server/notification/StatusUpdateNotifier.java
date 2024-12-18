/*
 * Status update notifier
 */
package app.metrodelay.server.notification;

import java.net.URL;

 /**
 *
 * @author mvolejnik
 */
public interface StatusUpdateNotifier {

    void send(URL url, StatusUpdateNotification notification);

}