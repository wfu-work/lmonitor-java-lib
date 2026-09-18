import com.google.gson.GsonBuilder;
import com.navfirst.lmonitor.lib.domains.MonitorTask;
import com.navfirst.lmonitor.lib.exceptions.RtkconvException;
import com.navfirst.lmonitor.lib.library.MonitorStreamInfo;
import com.navfirst.lmonitor.lib.services.impl.MonitorDataServiceImpl;
import com.navfirst.lmonitor.lib.services.impl.MonitorServiceImpl;
import com.navfirst.lmonitor.lib.utils.TimeUtils;
import com.navfirst.lmonitor.lib.enums.NavErrorEnum;
import com.navfirst.lmonitor.lib.enums.ObsErrorEnum;
import com.sun.jna.Structure;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Report-only checks. Does not invoke the native solver. */
public class JavaContractProbe {
    static final List<Map<String, Object>> checks = new ArrayList<>();
    static void require(boolean value) { if (!value) throw new AssertionError(); }
    static void expect(Class<? extends Throwable> type, Runnable action) {
        try { action.run(); } catch (Throwable e) {
            if (type.isInstance(e)) return;
            throw new AssertionError(e);
        }
        throw new AssertionError("Expected " + type.getSimpleName());
    }
    static void check(String name, Runnable action) {
        Map<String,Object> item = new LinkedHashMap<>(); item.put("name", name);
        try { action.run(); item.put("passed", true); }
        catch (Throwable e) { item.put("passed", false); item.put("error", e.toString()); }
        checks.add(item);
    }
    public static void main(String[] args) throws Exception {
        var data = new MonitorDataServiceImpl();
        var service = new MonitorServiceImpl(data);
        var task = MonitorTask.builder().roverName(" ROVER ").baseName(" BASE ")
            .extra("report-id").filterPeriod(7200).processInterval(600).build();
        String row23 = "2026/09/06 14:00:00 2026/09/06 13:59:45 0.1 0.2 0.3 0.9 0.98 0.99 10.1 20.2 30.3 Fixed 1 18 15 15 238 237 0 0 0.500";
        String row24 = row23.replace("Fixed 1 18 15", "Fixed 1 18 999 15");
        String row30 = "2026/07/03 02:00:00 2026/07/03 03:00:00 0.1488 0.0468 0.0205 0.9915 0.9833 0.9833 36.9577 -5.6954 7.4100 -1647153.5438 4602287.6750 4085429.0790 40.077629137 109.692231002 1311.3510 Fixed 1 29 1 15 15 236 236 0 0 1.1s";
        check("time_slash", () -> require(Arrays.equals(TimeUtils.parseEpoch("2026/09/06 13:00:00"),new double[]{2026,9,6,13,0,0})));
        check("time_dash", () -> require(Arrays.equals(TimeUtils.parseEpoch("2026-09-06 13:00:00"),new double[]{2026,9,6,13,0,0})));
        check("time_blank", () -> require(Arrays.equals(TimeUtils.parseEpoch(" "),new double[6])));
        check("time_invalid", () -> expect(IllegalArgumentException.class, () -> TimeUtils.parseEpoch("bad")));
        check("null_task", () -> expect(RtkconvException.class, () -> service.startMonitor(null,"unused")));
        check("empty_rover", () -> expect(RtkconvException.class, () -> service.startMonitor(task,"unused")));
        check("empty_base", () -> expect(RtkconvException.class, () -> service.startMonitor(MonitorTask.builder().roverBytes(new byte[]{1}).build(),"unused")));
        check("memory_copy", () -> {var s = new MonitorStreamInfo(); byte[] b={1,2,3};s.setRover(b);b[0]=9;require(s.roverLen==3 && s.roverBuf.getByte(0)==1);});
        check("memory_empty", () -> {var s=new MonitorStreamInfo();s.setBase(new byte[0]);s.setBrdc(null);require(s.baseBuf==null && s.baseLen==0 && s.brdcBuf==null && s.brdcLen==0);});
        check("parser_23", () -> {var d=data.handlerDataSync(task,row23,"");require(d.getE()==10.1 && d.getN()==20.2 && d.getU()==30.3 && d.getRoverObsNum()==238 && d.getBaseObsNum()==237 && d.getOffTime().equals("0.500"));});
        check("parser_24", () -> {var d=data.handlerDataSync(task,row24,"warning");require(d.getRoverSample()==15 && d.getRoverObsNum()==238 && d.getErrMsg().equals("warning"));});
        check("parser_30", () -> {var d=data.handlerDataSync(task,row30,"");require(d.getX()==-1647153.5438 && d.getY()==4602287.6750 && d.getZ()==4085429.0790 && d.getB()==40.077629137 && d.getL()==109.692231002 && d.getH()==1311.3510 && d.getIsMoved()==1 && d.getBaseObsNum()==236);});
        check("parser_blank_short", () -> require(data.handlerDataSync(task,"","")==null && data.handlerDataSync(task,"1 2 3","")==null));
        check("parser_bad_number", () -> expect(NumberFormatException.class, () -> data.handlerDataSync(task,row23.replace("10.1","bad"),"")));
        check("parser_null_station", () -> expect(NullPointerException.class, () -> data.handlerDataSync(MonitorTask.builder().build(),row23,"")));
        check("metadata_copy", () -> {var d=data.handlerDataSync(task,row23,"");require(d.getRoverName().equals("ROVER") && d.getBaseName().equals("BASE") && d.getExtra().equals("report-id") && d.getFilterPeriod()==7200 && d.getProcessInterval()==600 && d.getGpsTime().equals(d.getLastObsTime()));});
        check("callback_replace_clear", () -> {AtomicInteger a=new AtomicInteger(),b=new AtomicInteger();data.setHandlerData(d->a.incrementAndGet());data.setHandlerData(d->b.incrementAndGet());data.handlerData(task,row23,"");data.setHandlerData(null);data.handlerData(task,row23,"");require(a.get()==0 && b.get()==1);});
        check("callback_exception", () -> {data.setHandlerData(d->{throw new IllegalStateException("probe");});expect(IllegalStateException.class,()->data.handlerData(task,row23,""));data.setHandlerData(null);});
        check("unknown_status", () -> require(NavErrorEnum.getValue(99)==null && ObsErrorEnum.getValue(99).equals(ObsErrorEnum.OTHER.getMsg())));
        var info = new MonitorStreamInfo();
        var method = Structure.class.getDeclaredMethod("fieldOffset", String.class); method.setAccessible(true);
        Map<String,Object> offsets = new LinkedHashMap<>();
        for (String name : info.getClass().getAnnotation(Structure.FieldOrder.class).value())
            offsets.put(name,method.invoke(info,name));
        Map<String,Object> output=new LinkedHashMap<>();
        output.put("checks",checks); output.put("passed",checks.stream().filter(c->Boolean.TRUE.equals(c.get("passed"))).count());
        output.put("count",checks.size()); output.put("jnaSize",info.size());output.put("jnaOffsets",offsets);
        output.put("nativeVersion",service.getVersion());output.put("nativeSolverInvoked",false);
        Files.writeString(Path.of(args[0]),new GsonBuilder().setPrettyPrinting().create().toJson(output));
        if (checks.stream().anyMatch(c->!Boolean.TRUE.equals(c.get("passed")))) System.exit(1);
    }
}
