package industries.vocht.viki.dao;

import industries.vocht.viki.IDatabase;
import industries.vocht.viki.hazelcast.DocumentAction;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by peter on 1/01/17.
 *
 * persistent store for queue items in cassandra/hazelcast
 *
 */
public class QueueDao {

    private IDatabase db;

    public QueueDao(IDatabase db) {
        this.db = db;
    }

    /////////////////////////////////////////////////////////////////////////

    public DocumentAction hazelcastDocumentActionLoad(Long key) {
        return db.hazelcastDocumentActionLoad(key);
    }

    public Set<Long> hazelcastDocumentActionLoadAllKeys() {
        return db.hazelcastDocumentActionLoadAllKeys();
    }

    public Map<Long, DocumentAction> hazelcastDocumentActionLoadAll(Collection<Long> keys) {
        return db.hazelcastDocumentActionLoadAll(keys);
    }

    public void hazelcastDocumentActionStore(Long key, DocumentAction value) {
        db.hazelcastDocumentActionStore(key, value);
    }

    public void hazelcastDocumentActionStoreAll(Map<Long, DocumentAction> map) {
        db.hazelcastDocumentActionStoreAll(map);
    }

    public void hazelcastDocumentActionDelete(Long key) {
        db.hazelcastDocumentActionDelete(key);
    }

    public void hazelcastDocumentActionDeleteAll(Collection<Long> keys) {
        db.hazelcastDocumentActionDeleteAll(keys);
    }


}

