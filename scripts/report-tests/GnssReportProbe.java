import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.navfirst.lmonitor.lib.domains.MonitorData;
import com.navfirst.lmonitor.lib.domains.MonitorTask;
import com.navfirst.lmonitor.lib.services.impl.MonitorDataServiceImpl;
import com.navfirst.lmonitor.lib.services.impl.MonitorServiceImpl;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Standalone evidence collector. Run in a child JVM because native failures cannot be caught. */
public class GnssReportProbe {
    static final Gson JSON = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
    record Config(String id, String rover, String base, String nav, String start, String end,
                  int mode, int outMode, String outFile) {}
    record Input(Config config, byte[] rover, byte[] base, byte[] nav) {}
    record Sample(String id, int round, double queueMs, double callMs, int callbackCount,
                  MonitorData result, String exception) {}
    public static void main(String[] args) throws Exception {
        // config.json license output.json concurrency rounds serializeNative
        Config[] configs = JSON.fromJson(Files.readString(Path.of(args[0])), Config[].class);
        String license = args[1]; Path output = Path.of(args[2]);
        int concurrency = Integer.parseInt(args[3]), rounds = Integer.parseInt(args[4]);
        boolean serialize = Boolean.parseBoolean(args[5]);
        List<Input> inputs = new ArrayList<>();
        long readStart = System.nanoTime();
        for (Config c : configs) inputs.add(new Input(c, Files.readAllBytes(Path.of(c.rover())),
            Files.readAllBytes(Path.of(c.base())), Files.readAllBytes(Path.of(c.nav()))));
        double readMs = (System.nanoTime() - readStart) / 1e6;
        MonitorDataServiceImpl data = new MonitorDataServiceImpl();
        MonitorServiceImpl service = new MonitorServiceImpl(data);
        // One stable shared callback. Never replace it during concurrent work.
        Map<String, List<MonitorData>> callbacks = new ConcurrentHashMap<>();
        service.setHandlerData(d -> callbacks.computeIfAbsent(d.getExtra(), k ->
            Collections.synchronizedList(new ArrayList<>())).add(d));
        String version = service.getVersion();
        List<Sample> samples = new ArrayList<>(); List<Double> wallMs = new ArrayList<>();
        ExecutorService pool = Executors.newFixedThreadPool(concurrency);
        Object lock = new Object();
        for (int round = 0; round < rounds; round++) {
            final int r = round; CountDownLatch gate = new CountDownLatch(1);
            List<Future<Sample>> futures = new ArrayList<>();
            long submitted = System.nanoTime();
            for (Input in : inputs) futures.add(pool.submit(() -> {
                gate.await(); Config c = in.config(); String key = c.id() + "-" + r;
                MonitorTask task = MonitorTask.builder().rtMode(c.mode()).timeStart(c.start())
                    .timeEnd(c.end()).sample(0).vrs(0).roverName(c.id()).baseName("BASE")
                    .brdcBytes(in.nav()).roverBytes(in.rover()).baseBytes(in.base())
                    .outMode(c.outMode()).minFixedRate(0.75).navSys("1,4,8,32").bds(1)
                    .outFile(c.outFile() == null ? "" : c.outFile()).extra(key).build();
                long start = System.nanoTime(); String error = null;
                try {
                    if (serialize) { synchronized (lock) { service.startMonitor(task, license); } }
                    else service.startMonitor(task, license);
                } catch (Throwable e) { error = e.toString(); }
                double elapsed = (System.nanoTime() - start) / 1e6;
                List<MonitorData> got = callbacks.getOrDefault(key, List.of());
                return new Sample(c.id(), r, (start - submitted) / 1e6, elapsed,
                    got.size(), got.isEmpty() ? null : got.get(0), error);
            }));
            gate.countDown();
            for (Future<Sample> f : futures) samples.add(f.get());
            wallMs.add((System.nanoTime() - submitted) / 1e6);
            Map<String, Object> report = new LinkedHashMap<>();
            report.put("nativeVersion", version); report.put("javaVersion", System.getProperty("java.version"));
            report.put("concurrency", concurrency); report.put("serializeNative", serialize);
            report.put("readMs", readMs); report.put("roundWallMs", wallMs);
            report.put("configs", configs); report.put("samples", samples);
            Files.writeString(output, JSON.toJson(report));
        }
        pool.shutdown();
    }
}
