package ch.postfinance.prometheus;

import io.prometheus.client.Collector;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

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
    public void exportList() {
        ApacheExporter exporter = new ApacheExporter("http://fakehost/status");

        List<Collector.MetricFamilySamples> mfsList;

        try {
            for (int i = 0; i < 5; i++) {
                mfsList = exporter.exportSamplesList();

                for(Collector.MetricFamilySamples element : mfsList) {
                    if(element.name.equals("apache_scrape_errors_total")){
                        for(Collector.MetricFamilySamples.Sample sample : element.samples){
                            assertEquals(i+1, (int)sample.value);
                        }
                    }

                }

                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testProperty(){

        ApacheExporter exporter = new ApacheExporter();

        assertEquals( "http://localhost/server-status?auto", exporter.getStatusUrl());

        System.setProperty("httpdModStatusUrl", "http://e1-xxx-alsu001/server-status?auto");

        assertEquals("http://e1-xxx-alsu001/server-status?auto", exporter.getStatusUrl());

        exporter = new ApacheExporter("http://host:port/midw-status");

        assertEquals( "http://host:port/midw-status", exporter.getStatusUrl());


    }
}