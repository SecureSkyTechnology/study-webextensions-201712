package com.secureskytech.scdemosrv;

import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.dbutils.DbUtils;

import com.secureskytech.scdemosrv.model.JdbcConnectionFactory;

import lombok.Getter;

public class AppContext {
    private static List<String> availableCharsetNames;

    static {
        Map<String, Charset> acs = Charset.availableCharsets();
        List<String> l = new ArrayList<>(acs.size());
        for (Map.Entry<String, Charset> e : acs.entrySet()) {
            l.add(e.getValue().name());
        }
        Collections.sort(l);
        // for usability adjustment in Japan locale
        int sz = l.size();
        List<String> prefs =
            Arrays.asList("UTF-8", "Shift_JIS", "windows-31j", "EUC-JP", "ISO-2022-JP", "ISO-8859-1", "US-ASCII");
        l.removeAll(prefs);
        List<String> l2 = new ArrayList<>(sz);
        l2.addAll(prefs);
        l2.addAll(l);
        availableCharsetNames = Collections.unmodifiableList(l2);
    }

    @Getter
    private final JdbcConnectionFactory jdbcConnectionFactory;
    @Getter
    private final String logContext;

    private final Clock clock;

    private final AtomicInteger screenshotCounter = new AtomicInteger(1);

    public AppContext(Clock clock, JdbcConnectionFactory cf) throws IOException {
        this.clock = clock;
        this.jdbcConnectionFactory = cf;
        LocalDateTime ldt0 = LocalDateTime.now(clock);
        DateTimeFormatter dtf0 = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        this.logContext = ldt0.format(dtf0);
    }

    public static List<String> getAvailableCharsetNames() {
        return AppContext.availableCharsetNames;
    }

    public long now() {
        return this.clock.millis();
    }

    public int getNewScreenshotId() {
        return screenshotCounter.getAndIncrement();
    }

    public Connection createDbConnection() throws SQLException {
        return this.jdbcConnectionFactory.newConnection();
    }

    public void releaseDbConnection(Connection con) {
        DbUtils.closeQuietly(con);
    }
}
