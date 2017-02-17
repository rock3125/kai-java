package industries.vocht.viki.model;

/**
 * Created by peter on 19/12/16.
 *
 */
public class SpacyPacketList {

    private SpacyPacket[] packetList;
    private int num_tokens;
    private int num_sentences;

    public SpacyPacketList() {
    }

    public SpacyPacket[] getPacketList() {
        return packetList;
    }

    public void setPacketList(SpacyPacket[] packetList) {
        this.packetList = packetList;
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

}
