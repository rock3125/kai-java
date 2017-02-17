package industries.vocht.viki.model.knowledge_base;

import java.util.UUID;

/**
 * Created by peter on 3/01/17.
 *
 * Knowledge Base item lookup
 *
 */
public class KBEntry {

    private UUID organisation_id;   // owner
    private UUID id;                // unique id of this item
    private String type;            // type of entry (e.g. "address book")
    private String origin;          // external reference uid
    private String json_data;       // actual info carrier

    public KBEntry() {
    }

    public KBEntry(UUID organisation_id, UUID id, String type, String origin, String json_data) {
        this.organisation_id = organisation_id;
        this.id = id;
        this.type = type;
        this.origin = origin;
        this.json_data = json_data;
    }


    public UUID getOrganisation_id() {
        return organisation_id;
    }

    public void setOrganisation_id(UUID organisation_id) {
        this.organisation_id = organisation_id;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getJson_data() {
        return json_data;
    }

    public void setJson_data(String json_data) {
        this.json_data = json_data;
    }

}

