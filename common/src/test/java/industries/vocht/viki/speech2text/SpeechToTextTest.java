package industries.vocht.viki.speech2text;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.util.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by peter on 26/12/16.
 *
 * test the speech to text conversion system works
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/test-context.xml"})
public class SpeechToTextTest {

    @Autowired
    private SphinxSpeechToText speechToText;

    @Test
    public void testSpeechToTextPart1() throws IOException {
        InputStream in = getClass().getResourceAsStream("/common/84-121550-0022.wav");
        if (in != null) {
            STTResult result = speechToText.wavToText(in);
            Assert.notNull(result);
            Assert.notNull(result.getText());
            Assert.isTrue(result.getText().equals("AS SOON AS ON MY PATIENCE MULLET THE POWERS OF THE WIND HAD ALREADY PIERCED ME THROUGH AIR FROM MY BOYHOOD AHEAD YET COME FOR UP"));
        }
    }


}

