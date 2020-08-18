package ch.postfinance.prometheus;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class ApacheExporterTest {

    @Test
    public void export() {
        ApacheExporter exporter = new ApacheExporter();
        try {
            for (int i = 0; i < 5; i++) {
                System.out.println(exporter.export());
                Thread.sleep(1000);
            }
        } catch (IOException | InterruptedException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testProperty(){

        ApacheExporter exporter = new ApacheExporter();

        assertEquals(exporter.getStatusUrl(), "http://localhost/server-status?auto");

        System.setProperty("httpdModStatusUrl", "http://e1-xxx-alsu001/server-status?auto");

        assertEquals(exporter.getStatusUrl(), "http://e1-xxx-alsu001/server-status?auto");

        exporter = new ApacheExporter("http://host:port/midw-status");

        assertEquals(exporter.getStatusUrl(), "http://host:port/midw-status");


    }
}