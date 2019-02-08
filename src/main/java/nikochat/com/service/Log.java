package nikochat.com.service;

import nikochat.com.app.AppConfig;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

/**
 * Created by nikolay on 01.09.14.
 */
public class Log {

    private static PrintWriter log;

    static {
        try {
            log = new PrintWriter(AppConfig.RELATIVE_LOG_PATH);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public synchronized static void write(String str) {
        log.println(str);
        log.flush();
    }

    public static void close() {
        log.close();
    }
}
