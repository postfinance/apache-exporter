# Apache Exporter for Prometheus
Exports server statistics provided by mod_status of an Apache HTTP server to Prometheus metrics format

## Usage 

It can be built with:

```
mvn clean install
```

It can be importer in your Maven project with

```
<dependency>
  <groupId>com.github.postfinance.prometheus</groupId>
  <artifactId>apache-exporter</artifactId>
  <version>VERSION</version>
</dependency>

```

### ModStatus URL

The Apache ModStatus URL where the metrics are read, can be configured with

* A parameter in the ApacheExporter constructor
* A System Property: httpdModStatusUrl
* An Environment Property: HTTPD_MOD_STATUS_URL

The default value is http://localhost/server-status?auto

### Apache HTTPD Metrics

To obtain the metrics for apache, an istance of the ApacheExporter class should be created 
and one of the following methods called:

* First method

```
public String export() throws IOException
public String export(String interfaceName) throws IOException
```
returns a String in the format:

```
# HELP apache_exporter_build_info A metric with a constant '1' value labeled by version, revision, branch, and goversion from which apache_exporter was built.
# TYPE apache_exporter_build_info gauge
apache_exporter_build_info{branch="HEAD",goversion="go1.12.6",revision="6195241a96c02af175ba2842dfd883682133b066",version="0.7.0"} 1
# HELP apache_scoreboard Apache scoreboard statuses
# TYPE apache_scoreboard gauge
apache_scoreboard{state="closing"} 0
apache_scoreboard{state="dns"} 0
apache_scoreboard{state="graceful_stop"} 0
apache_scoreboard{state="idle"} 6
apache_scoreboard{state="idle_cleanup"} 0
apache_scoreboard{state="keepalive"} 1
apache_scoreboard{state="logging"} 0
apache_scoreboard{state="open_slot"} 142
apache_scoreboard{state="read"} 0
apache_scoreboard{state="reply"} 1
apache_scoreboard{state="startup"} 0
# HELP apache_sent_kilobytes_total Current total kbytes sent (*)
# TYPE apache_sent_kilobytes_total counter
apache_sent_kilobytes_total 5721
# HELP apache_up Could the apache server be reached
# TYPE apache_up gauge
apache_up 1
# HELP apache_uptime_seconds_total Current uptime in seconds (*)
# TYPE apache_uptime_seconds_total counter
apache_uptime_seconds_total 9255
# HELP apache_workers Apache worker statuses
# TYPE apache_workers gauge
apache_workers{state="busy"} 2
apache_workers{state="idle"} 6

```

Example:

```
  ApacheExporter exporter = new ApacheExporter("http://myhost:7070/midw-status?auto");
  exporter.export("lo")
```

* Second method

```
public ArrayList<Collector.MetricFamilySamples> exportSamplesList()
public ArrayList<Collector.MetricFamilySamples> exportSamplesList(String interfaceName)
```

It returns a list of samples of type Collector.MetricFamilySamples


Example:

```
  ApacheExporter exporter = new ApacheExporter("http://myhost:7070/midw-status?auto");
  List<Collector.MetricFamilySamples> mfsList = exporter.exportSamplesList("eth0")
```