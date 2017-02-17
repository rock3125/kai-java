package industries.vocht.viki.aiml;

import org.junit.Test;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.List;

/**
 * Created by peter on 9/01/17.
 *
 * test the patterns match
 *
 */
public class AimlPatternMatcherTest {

    @Test
    public void patternMatchTest1() throws IOException {
        AimlManager manager = new AimlManager();
        AimlKBProcessor processor = new AimlKBProcessor(manager.getNodeSet());

        processor.addPattern(new String[] { "who is *?" }, new AimlTemplate("type1", "field1"));
        processor.addPattern(new String[] { "who is Peter?" }, new AimlTemplate("type2", "field2"));
        processor.addPattern(new String[] { "who is Peter de Vocht?" }, new AimlTemplate("type3", "field3"));

        AimlPatternMatcher matcher = new AimlPatternMatcher();
        List<AimlTemplate> result = matcher.match("who is Peter?", manager );
        Assert.notNull(result);
        Assert.isTrue(result.size() == 2);
        AimlTemplate f1 = getResultByFieldName("field1", result);
        Assert.notNull(f1);
        Assert.notNull(f1.getStarList()); // check f1 bound its star to "Peter"
        Assert.isTrue(f1.getStarList().size() == 1 &&
                f1.getStarList().get(0).getText().compareToIgnoreCase("Peter") == 0);
        Assert.notNull(getResultByFieldName("field2", result));

        result = matcher.match("who is Peter de Vocht?", manager );
        Assert.notNull(result);
        Assert.isTrue(result.size() == 2);
        f1 = getResultByFieldName("field1", result);
        Assert.notNull(f1);
        Assert.notNull(f1.getStarList()); // check f1 bound its star to "Peter"
        Assert.isTrue(f1.getStarList().size() == 3);
        Assert.notNull(getResultByFieldName("field3", result));
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////
    // helper

    private AimlTemplate getResultByFieldName(String fieldName, List<AimlTemplate> result) {
        if (result != null && fieldName != null) {
            for (AimlTemplate template : result) {
                if (fieldName.equals(template.getKb_field())) {
                    return template;
                }
            }
        }
        return null;
    }

}
