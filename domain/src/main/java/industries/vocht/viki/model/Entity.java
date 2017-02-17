package industries.vocht.viki.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by peter on 4/01/17.
 *
 * entity as stored inside a KBEntry field
 *
 */
public class Entity {

    private String name;
    private String isa;
    private List<String> alias_list;

    public Entity() {
        alias_list = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIsa() {
        return isa;
    }

    public void setIsa(String isa) {
        this.isa = isa;
    }

    public List<String> getAlias_list() {
        return alias_list;
    }

    public void setAlias_list(List<String> alias_list) {
        this.alias_list = alias_list;
    }

}


