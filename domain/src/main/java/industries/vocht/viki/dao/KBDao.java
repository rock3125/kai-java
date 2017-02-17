package industries.vocht.viki.dao;

import industries.vocht.viki.IDatabase;
import industries.vocht.viki.model.knowledge_base.KBEntry;

import java.util.List;
import java.util.UUID;

/**
 * Created by peter on 3/01/17.
 *
 * address book management
 *
 */
public class KBDao {

    private IDatabase db;

    public KBDao(IDatabase db) {
        this.db = db;
    }

    public void saveKBEntry(KBEntry KBEntry) {
        db.saveKBEntry(KBEntry);
    }

    public KBEntry getKBEntry(UUID organisation_id, String type, UUID id) {
        return db.getKBEntry(organisation_id, type, id);
    }

    public void deleteKBEntry(UUID organisation_id, String type, UUID id) {
        db.deleteKBEntry(organisation_id, type, id);
    }

    public List<KBEntry> getEntityList(UUID organisation_id, String type, UUID prev, int page_size) {
        return db.getEntityList(organisation_id, type, prev, page_size);
    }

}

