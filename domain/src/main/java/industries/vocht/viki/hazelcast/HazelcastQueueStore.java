package industries.vocht.viki.hazelcast;

import com.hazelcast.core.QueueStore;
import industries.vocht.viki.IDao;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Created by peter on 1/01/17.
 *
 * cassandra / hazelcast queue item storage
 *
 */
public class HazelcastQueueStore implements QueueStore<DocumentAction> {

    @Autowired
    private IDao dao;

    public HazelcastQueueStore() {
    }


    @Override
    public void store(Long key, DocumentAction value) {
        dao.getQueueDao().hazelcastDocumentActionStore(key, value);
    }

    @Override
    public void storeAll(Map<Long, DocumentAction> map) {
        dao.getQueueDao().hazelcastDocumentActionStoreAll(map);
    }

    @Override
    public void delete(Long key) {
        dao.getQueueDao().hazelcastDocumentActionDelete(key);
    }

    @Override
    public void deleteAll(Collection<Long> keys) {
        dao.getQueueDao().hazelcastDocumentActionDeleteAll(keys);
    }

    @Override
    public DocumentAction load(Long key) {
        return dao.getQueueDao().hazelcastDocumentActionLoad(key);
    }

    @Override
    public Map<Long, DocumentAction> loadAll(Collection<Long> keys) {
        return dao.getQueueDao().hazelcastDocumentActionLoadAll(keys);
    }

    @Override
    public Set<Long> loadAllKeys() {
        return dao.getQueueDao().hazelcastDocumentActionLoadAllKeys();
    }

}

