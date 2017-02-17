package industries.vocht.viki.mary_tts;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Created by peter on 28/08/16.
 *
 */
public class MaryTest {

    public static void main( String[] args ) throws Exception {
        MaryTest test = new MaryTest();
        test.run();
    }


    public MaryTest() {
    }

    public void run() throws Exception {

        String text = "How now, brown cow.";

        // setup a connection to a mary server
        // server address, profiling on/off, quiet yes/no
        MaryHttpClient client = new MaryHttpClient("localhost", 59125);
        byte[] data = client.tts("alice", text);
        if ( data != null ) {
            FileUtils.writeByteArrayToFile(new File("/home/peter/dev/test.wav"), data);
        }
    }


}

