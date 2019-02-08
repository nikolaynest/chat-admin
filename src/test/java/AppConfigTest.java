import nikochat.com.app.AppConfig;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by nikolay on 22.08.14.
 */
public class AppConfigTest {

    @Test @Ignore
    public void configTestHost(){
        assertEquals("127.0.0.1", AppConfig.HOST);
    }
    @Test @Ignore
    public void configTestPort(){
        assertEquals(7777, AppConfig.PORT);
    }


}
