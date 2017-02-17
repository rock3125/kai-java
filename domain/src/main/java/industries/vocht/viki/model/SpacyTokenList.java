package industries.vocht.viki.model;

/**
 * Created by peter on 11/12/16.
 *
 * Spcay client interface, a systme of spacy tokens
 *
 */
public class SpacyTokenList {

    private SpacyToken[][] sentence_list;
    private int num_tokens;
    private int num_sentences;
    private long processing_time;

    public SpacyTokenList() {
    }


    public SpacyToken[][] getSentence_list() {
        return sentence_list;
    }

    public void setSentence_list(SpacyToken[][] sentence_list) {
        this.sentence_list = sentence_list;
    }

    public int getNum_tokens() {
        return num_tokens;
    }

    public void setNum_tokens(int num_tokens) {
        this.num_tokens = num_tokens;
    }

    public int getNum_sentences() {
        return num_sentences;
    }

    public void setNum_sentences(int num_sentences) {
        this.num_sentences = num_sentences;
    }

    public long getProcessing_time() {
        return processing_time;
    }

    public void setProcessing_time(long processing_time) {
        this.processing_time = processing_time;
    }

}
