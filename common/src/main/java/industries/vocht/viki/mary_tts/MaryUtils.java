package industries.vocht.viki.mary_tts;

import javax.sound.sampled.AudioFileFormat;
import java.io.*;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * Created by peter on 21/01/17.
 *
 */
public class MaryUtils {

    public static String[] toStringArray(String allInOneLine) {
        if (allInOneLine != null && allInOneLine.length() > 0) {
            Vector<String> result = new Vector<String>();

            StringTokenizer s = new StringTokenizer(allInOneLine, System.getProperty("line.separator"));
            String line = null;
            // Read until either end of file or an empty line
            while (s.hasMoreTokens() && ((line = s.nextToken()) != null) && (!line.equals("")))
                result.add(line);

            return result.toArray(new String[0]);
        } else
            return null;
    }

    public static String getStreamAsString(InputStream inputStream, String encoding) throws IOException {
        return getReaderAsString(new InputStreamReader(inputStream, encoding));
    }

    public static String getReaderAsString(Reader reader) throws IOException {
        StringWriter sw = new StringWriter();
        BufferedReader in = new BufferedReader(reader);
        char[] buf = new char[8192];
        int n;
        while ((n = in.read(buf)) > 0) {
            sw.write(buf, 0, n);
        }
        return sw.toString();

    }

    public static Locale string2locale(String localeString) {
        Locale locale = null;
        StringTokenizer localeST = new StringTokenizer(localeString, "_-");
        String language = localeST.nextToken();
        String country = "";
        String variant = "";
        if (localeST.hasMoreTokens()) {
            country = localeST.nextToken();
            if (localeST.hasMoreTokens()) {
                variant = localeST.nextToken();
            }
        }
        locale = new Locale(language, country, variant);
        return locale;
    }

    /**
     * Return an audio file format type for the given string. In addition to the built-in types, this can deal with MP3 supported
     * by tritonus.
     *
     * @param name
     *            name
     * @return the audio file format type if it is known, or null.
     */
    public static AudioFileFormat.Type getAudioFileFormatType(String name) {
        AudioFileFormat.Type at;
        if (name.equals("MP3")) {
            // Supported by tritonus plugin
            at = new AudioFileFormat.Type("MP3", "mp3");
        } else if (name.equals("Vorbis")) {
            // supported by tritonus plugin
            at = new AudioFileFormat.Type("Vorbis", "ogg");
        } else {
            try {
                at = (AudioFileFormat.Type) AudioFileFormat.Type.class.getField(name).get(null);
            } catch (Exception e) {
                return null;
            }
        }

        return at;
    }

}
