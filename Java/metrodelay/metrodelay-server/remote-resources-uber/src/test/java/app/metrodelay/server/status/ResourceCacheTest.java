/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package app.metrodelay.server.status;

import app.metrodelay.server.status.ResourceCache;
import app.metrodelay.server.remoteresources.RemoteResourceException;
import app.metrodelay.server.remoteresources.Resource;
import app.metrodelay.server.remoteresources.ResourceImpl;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import org.apache.commons.io.IOUtils;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 *
 * @author mvolejnik
 */
public class ResourceCacheTest {
    
    private static final String ETAG_1 = "ETAG_1" ;
    private static final String ETAG_2 = "ETAG_2" ;
    private static final URL URL_1;
    private static final Resource RESOURCE_WITH_ETAG;
    static {
        try {
            RESOURCE_WITH_ETAG = new ResourceImpl(IOUtils.toInputStream("Resource with etag", Charset.forName("UTF-8")), ETAG_1);
        } catch (RemoteResourceException ex) {
            throw new RuntimeException("Unable to init.", ex);
        }
        try {
            URL_1 = new URL("http://test.url/");
        } catch (MalformedURLException ex) {
            throw new RuntimeException("Unable to init.", ex);
        }
    }
     
    @Test
    void testResourceCache() throws RemoteResourceException {
        var cache = new ResourceCache();
        var r_t1 = new ResourceImpl(IOUtils.toInputStream("content", Charset.forName("UTF-8")), ETAG_1);
        cache.resource(URL_1, r_t1);
        assertTrue(cache.resource(URL_1).isPresent(), "Cache should not be null");
    }
    
    @Test
    void testResourceEtagUpdateChanged() throws RemoteResourceException {
        var cache = new ResourceCache();
        var r_t1 = new ResourceImpl(IOUtils.toInputStream("content", Charset.forName("UTF-8")), ETAG_1);
        var r_t2 = new ResourceImpl(IOUtils.toInputStream("content updated", Charset.forName("UTF-8")), ETAG_2);
        cache.resource(URL_1, r_t1);
        cache.resource(URL_1, r_t2);
        assertEquals(r_t2.fingerprint(), cache.resource(URL_1).get().fingerprint(), "Fingerprint should equals to the newer resource.");
    }
    
    @Test
    void testResourceEtagDoNotUpdateUnchanged() throws RemoteResourceException, IOException {
        var cache = new ResourceCache();
        var content = "content";
        var r_t1 = new ResourceImpl(IOUtils.toInputStream(content, Charset.forName("UTF-8")), ETAG_1);
        var r_t2 = new ResourceImpl(null, ETAG_1);
        cache.resource(URL_1, r_t1);
        cache.resource(URL_1, r_t2);
        assertEquals(r_t1.fingerprint(), cache.resource(URL_1).get().fingerprint(), "Fingerpring should equals to the resource.");
        assertEquals(content, IOUtils.toString(cache.resource(URL_1).get().content().get(), Charset.forName("UTF-8")), "Fingerprint should equals to the resource.");
    }
    
}
