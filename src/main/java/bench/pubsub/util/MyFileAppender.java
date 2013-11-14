package bench.pubsub.util;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.RollingFileAppender;

/**
 * Appends an identifier at the end of the filename.
 */
public class MyFileAppender extends RollingFileAppender {

    public static final String LOG4J_PROP = "log4j.file.prefix";

    @Override
    public void setFile(String s) {
        try {
            File f = File.createTempFile(s.substring(0, s.lastIndexOf('.')) + "-", ".log");
            System.out.println("#####################################################");
            System.out.println("#");
            System.out.println("# Logging will go to file: " + f.getName());
            System.out.println("#");
            System.out.println("#####################################################");
            super.setFile(f.getName());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
