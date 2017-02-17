package industries.vocht.viki.model;

import industries.vocht.viki.dao.PennType;

/**
 * Created by peter on 11/12/16.
 *
 * a simple spacy token
 *
 */
public class SpacyToken implements Comparable<SpacyToken> {

    private int index;
    private String text;
    private String tag;
    private String dep;
    private int synid;
    private int[] list;

    public SpacyToken() {
    }

    public String toString() {
        if ( dep != null ) {
            return text + " (" + dep + ")";
        }
        return text;
    }

    public Token convertToToken() {
        Token t = new Token(text, PennType.fromString(tag));
        t.setSynid(synid);
        return t;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public int[] getList() {
        return list;
    }

    public void setList(int[] list) {
        this.list = list;
    }

    public String getDep() {
        return dep;
    }

    public void setDep(String dep) {
        this.dep = dep;
    }

    public int getSynid() {
        return synid;
    }

    public void setSynid(int synid) {
        this.synid = synid;
    }

    @Override
    public int compareTo(SpacyToken o) {
        if ( index < o.index ) return -1;
        if ( index > o.index ) return 1;
        return 0;
    }

}
