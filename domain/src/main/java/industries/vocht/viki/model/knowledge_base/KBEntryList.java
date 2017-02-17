package industries.vocht.viki.model.knowledge_base;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by peter on 3/01/17.
 *
 * list of knowledge base entries
 *
 */
public class KBEntryList {

    private List<KBEntry> list;

    public KBEntryList() {
        list = new ArrayList<>();
    }

    public KBEntryList(List<KBEntry> list) {
        this.list = list;
    }

    public List<KBEntry> getList() {
        return list;
    }

    public void setList(List<KBEntry> list) {
        this.list = list;
    }

}
