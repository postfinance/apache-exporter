package ch.postfinance.prometheus;

import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.common.TextFormat;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;

public class ApacheExporter {

    private String statusUrl;

    public ApacheExporter() {

    }

    public ApacheExporter(String statusUrl) {
        this.statusUrl = statusUrl;
    }

    CollectorRegistry registry = new CollectorRegistry();

    Counter scrapesTotal = Counter.build()
            .name("apache_scrapes_total")
            .help("apache_scrapes_total Current total scrapes of apache status")
            .register(registry);

    Counter scrapeErrorsTotal = Counter.build()
            .name("apache_scrape_errors_total")
            .help("apache_scrape_errors_total Current total scrape errors of apache status")
            .register(registry);

    Counter scrapeDurationSeconds = Counter.build()
            .name("apache_scrape_duration_seconds")
            .help("apache_scrape_duration_seconds Total duration of scrapes")
            .register(registry);

    Gauge serverUpGauge = Gauge.build()
            .name("apache_up")
            .help("apache_up Could the apache server be reached")
            .register(registry);

    Counter accessTotal = Counter.build()
            .name("apache_accesses_total")
            .help("apache_accesses_total Current total apache accesses")
            .register(registry);

    Counter durationTotal = Counter.build()
            .name("apache_duration_seconds_total")
            .help("apache_duration_seconds_total Total duration of all requests")
            .register(registry);

    Counter kiloBytesTotal = Counter.build()
            .name("apache_sent_kilobytes_total")
            .help("apache_sent_kilobytes_total Current total apache accesses (*)")
            .register(registry);

    Gauge cpuloadGauge = Gauge.build()
            .name("apache_cpuload")
            .help("apache_cpuload The current percentage CPU used by each worker and in total by all workers combined")
            .register(registry);

    Gauge workersGauge = Gauge.build()
            .name("apache_workers")
            .help("apache_workers Apache worker statuses")
            .labelNames("state")
            .register(registry);

    Gauge scoreboardGauge = Gauge.build()
            .name("apache_scoreboard")
            .help("apache_scoreboard Apache scoreboard statuses")
            .labelNames("state")
            .register(registry);

    Counter serverUptimeSeconds = Counter.build()
            .name("apache_uptime_seconds_total")
            .help("apache_uptime_seconds_total Current uptime in seconds")
            .register(registry);

    /**
     * Returns an a string containing the apache metrics in prometheus format.
     * Wenn the URL of the exporter is not reachable, it returns a metric
     * "apache_scrape_errors_total" greater then zero.
     * <p>
     *
     * @param  interfaceName  the name of the interface to bind (ex : lo, eth0)
     * @return      the string containing the apache metrics in prometheus format
     */
    public String export(String interfaceName) throws IOException {
        try {
            mapStatusToMetrics(readApacheStatus(interfaceName));
            serverUpGauge.set(1);
        } catch (IOException e) {
            scrapeErrorsTotal.inc();
            serverUpGauge.set(0);
        }

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        OutputStreamWriter osw = new OutputStreamWriter(stream);

        TextFormat.write004(osw,
                registry.metricFamilySamples());

        osw.flush();
        osw.close();

        return new String(stream.toByteArray());
    }

    /**
     * Returns an a string containing the apache metrics in prometheus format.
     * Wenn the URL of the exporter is not reachable, it returns a metric
     * "apache_scrape_errors_total" greater then zero.
     * <p>
     *
     * @return      the string containing the apache metrics in prometheus format
     */
    public String export() throws IOException {

        return export(null);

    }

    /**
     * Returns an ArrayList of Collector.MetricFamilySamples containing the apache metrics in prometheus format.
     * Wenn the URL of the exporter is not reachable, it returns a metric
     * "apache_scrape_errors_total" greater then zero.
     * <p>
     *
     * @param  interfaceName  the name of the interface to bind (ex : lo, eth0)
     * @return      the string containing the apache metrics in prometheus format
     */
    public ArrayList<Collector.MetricFamilySamples> exportSamplesList(String interfaceName) {
        try {
            mapStatusToMetrics(readApacheStatus(interfaceName));
            serverUpGauge.set(1);
        } catch (IOException e) {
            scrapeErrorsTotal.inc();
            serverUpGauge.set(0);
        }

        return Collections.list(registry.metricFamilySamples());

    }

    /**
     * Returns an ArrayList of Collector.MetricFamilySamples containing the apache metrics in prometheus format.
     * Wenn the URL of the exporter is not reachable, it returns a metric
     * "apache_scrape_errors_total" greater then zero.
     * <p>
     *
     * @return      the string containing the apache metrics in prometheus format
     */
    public ArrayList<Collector.MetricFamilySamples> exportSamplesList() {
        return exportSamplesList(null);
    }

    private void mapStatusToMetrics(String statusData) {
        statusData.lines().parallel().forEach(line -> {

            String[] elems = line.split(":");
            if (elems.length < 2) {
                return;
            }

            switch (elems[0]) {
                case "CPULoad":
                    handleGaugeValue(cpuloadGauge, elems[1]);
                    break;
                case "Total Accesses":
                    handleCounterValue(accessTotal, elems[1]);
                    break;
                case "Total kBytes":
                    handleCounterValue(kiloBytesTotal, elems[1]);
                    break;
                case "ServerUptimeSeconds":
                    handleCounterValue(serverUptimeSeconds, elems[1]);
                    break;
                case "Total Duration":
                    handleCounterValue(durationTotal, elems[1]);
                    break;
                case "BusyWorkers":
                    handleGaugeWitLabelsValue(workersGauge, elems[1], "busy");
                    break;
                case "IdleWorkers":
                    handleGaugeWitLabelsValue(workersGauge, elems[1], "idle");
                    break;
                case "Scoreboard":
                    handleScoreboard(scoreboardGauge, elems[1]);
                    break;
            }

        });
    }

    String readApacheStatus(String interfaceName) throws IOException {

        int timeoutSec = 5;
        HttpGet getMethod = new HttpGet(getStatusUrl());

        CloseableHttpClient client;
        RequestConfig.Builder requestConfig = RequestConfig.custom();

        if (interfaceName != null) {

            //System.out.println("interface name: " + interfaceName);

            NetworkInterface nif = NetworkInterface.getByName(interfaceName);
            if (nif == null) {
                throw new IOException("Interface not correct:" + interfaceName);
            }

            InetAddress address = null;
            Enumeration<InetAddress> nifAddresses = nif.getInetAddresses();

            while (nifAddresses.hasMoreElements()) {
                address = nifAddresses.nextElement();
                //System.out.println("address:" + address.getHostAddress());
                if(address instanceof Inet4Address) break;
               
            }
            
            requestConfig.setLocalAddress(address);

        }
        requestConfig.setConnectTimeout(timeoutSec * 1000);
        requestConfig.setConnectionRequestTimeout(timeoutSec * 1000);
        requestConfig.setSocketTimeout(timeoutSec * 1000);


        client = HttpClientBuilder
                .create()
                .setDefaultRequestConfig(requestConfig.build())
                .build();


        scrapesTotal.inc();
        Instant start = Instant.now();

        CloseableHttpResponse response = client.execute(getMethod);
        String body = new BasicResponseHandler().handleResponse(response);

        Instant stop = Instant.now();
        Duration d = Duration.between(start, stop);
        double diff = ((double) d.getNano()) / 1_000_000 / 1000;
        scrapeDurationSeconds.inc(diff);
        if (response.getStatusLine().getStatusCode() >= 400) {
            scrapeErrorsTotal.inc();
        }
        return body;
    }

    void handleGaugeValue(Gauge gauge, String rawValue) {
        gauge.set(Double.parseDouble(rawValue.trim()));
    }

    void handleGaugeWitLabelsValue(Gauge gauge, String rawValue, String... labelValues) {
        gauge.labels(labelValues).set(Double.parseDouble(rawValue.trim()));
    }

    void handleCounterValue(Counter counter, String rawValue) {
        counter.clear();
        counter.inc(Double.parseDouble(rawValue.trim()));
    }

    /**
     * Scoreboard Key:
     * "_" Waiting for Connection, "S" Starting up, "R" Reading Request,
     * "W" Sending Reply, "K" Keepalive (read), "D" DNS Lookup,
     * "C" Closing connection, "L" Logging, "G" Gracefully finishing,
     * "I" Idle cleanup of worker, "." Open slot with no current process
     *
     * @param scoreboardGauge
     * @param elem
     */
    void handleScoreboard(Gauge scoreboardGauge, String elem) {
        scoreboardGauge.clear();
        elem.trim().chars().forEach(
                it -> scoreboardGauge.labels(mapToState(it)).inc()
        );
    }

    static String mapToState(int scoreValue) {
        switch (scoreValue) {
            case '_':
                return "waiting";
            case 'S':
                return "startup";
            case 'R':
                return "read";
            case 'W':
                return "reply";
            case 'K':
                return "keepalive";
            case 'D':
                return "dns";
            case 'C':
                return "closing";
            case 'L':
                return "logging";
            case 'G':
                return "graceful_stop";
            case '.':
                return "open_slot";
        }

        return "unknown";
    }

    String getStatusUrl() {

        if (this.statusUrl != null) {
            return this.statusUrl;
        } else {

            String statusUrl = System.getProperty("httpdModStatusUrl");

            if (statusUrl == null || statusUrl.trim().isEmpty()) {
                statusUrl = System.getenv("HTTPD_MOD_STATUS_URL");

                if (statusUrl == null || statusUrl.trim().isEmpty())
                    statusUrl = "http://localhost/server-status?auto";
            }

            return statusUrl;
        }
    }

}
