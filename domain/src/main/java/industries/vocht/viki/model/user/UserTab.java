package industries.vocht.viki.model.user;

import java.util.List;

/**
 * Created by peter on 7/01/17.
 *
 * user tabs for user specified data-types, names of the tab,
 * and fields to search under
 *
 */
public class UserTab {

    private String type;
    private String tab_name;
    private String html_template;
    private List<String> field_list;

    public UserTab() {
    }

    public UserTab(String type, String tab_name, List<String> field_list) {
        this.type = type;
        this.tab_name = tab_name;
        this.field_list = field_list;
    }


    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTab_name() {
        return tab_name;
    }

    public void setTab_name(String tab_name) {
        this.tab_name = tab_name;
    }

    public List<String> getField_list() {
        return field_list;
    }

    public void setField_list(List<String> field_list) {
        this.field_list = field_list;
    }

    public String getHtml_template() {
        return html_template;
    }

    public void setHtml_template(String html_template) {
        this.html_template = html_template;
    }

}
