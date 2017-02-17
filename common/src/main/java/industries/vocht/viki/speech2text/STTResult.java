package industries.vocht.viki.speech2text;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by peter on 26/12/16.
 *
 * result of a speech to text conversion
 *
 */
public class STTResult {

    private String text;
    private List<STTWord> wordList;

    public STTResult() {
        wordList = new ArrayList<>();
    }

    public STTResult(String text, List<STTWord> wordList) {
        this.text = text;
        this.wordList = wordList;
    }

    public String toString() {
        return text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<STTWord> getWordList() {
        return wordList;
    }

    public void setWordList(List<STTWord> wordList) {
        this.wordList = wordList;
    }

}

