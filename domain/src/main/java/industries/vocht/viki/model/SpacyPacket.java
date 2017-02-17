package industries.vocht.viki.model;

/**
 * Created by peter on 11/12/16.
 *
 * Spcay client interface, a systme of spacy tokens
 *
 */
public class SpacyPacket {

    private String metadata;
    private SpacyTokenList spacyTokenList;

    public SpacyPacket() {
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public SpacyTokenList getSpacyTokenList() {
        return spacyTokenList;
    }

    public void setSpacyTokenList(SpacyTokenList spacyTokenList) {
        this.spacyTokenList = spacyTokenList;
    }

}
