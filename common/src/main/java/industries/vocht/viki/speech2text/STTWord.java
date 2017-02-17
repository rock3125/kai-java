package industries.vocht.viki.speech2text;

/**
 * Created by peter on 26/12/16.
 *
 * a single word as recognized by the STT system
 *
 */
public class STTWord {

    private String word;  // the word recognized
    // time start and end of word in stream
    private long start;
    private long end;
    private double score;  // word's score in recognition

    public STTWord() {
    }

    public STTWord(String word, long start, long end, double score) {
        this.word = word;
        this.start = start;
        this.end = end;
        this.score = score;
    }

    public String toString() {
        return word;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

}

