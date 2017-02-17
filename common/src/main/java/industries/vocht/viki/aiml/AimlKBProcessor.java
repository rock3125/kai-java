package industries.vocht.viki.aiml;

import industries.vocht.viki.model.Token;
import industries.vocht.viki.tokenizer.Tokenizer;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by peter on 8/01/17.
 *
 * integration point for KBEntry items
 *
 */
public class AimlKBProcessor {

    // item tokenizer
    private Tokenizer tokenizer;
    private Map<String, AimlPattern> nodeSet;
    private int templateCount;
    private int patternCount;

    public AimlKBProcessor(Map<String, AimlPattern> nodeSet) {
        this.tokenizer = new Tokenizer();
        this.nodeSet = nodeSet;
        this.templateCount = 0;
        this.patternCount = 0;
    }

    /**
     * add a pattern for prcessing
     * @param patternList the list of patterns to use
     * @param aimlTemplate the template to associate with this pattern
     */
    public void addPattern(String[] patternList, AimlTemplate aimlTemplate ) throws IOException {
        if ( patternList != null && patternList.length > 0 && aimlTemplate != null ) {
            patternCount = patternCount + patternList.length;
            for ( String pattern1 : patternList ) {
                List<String> expandedPattern = expandBrackets(pattern1);
                for ( String pattern : expandedPattern ) {
                    List<Token> tokenList = tokenizer.filterOutPunctuation(tokenizer.filterOutSpaces(tokenizer.tokenize(pattern)));
                    if (tokenList != null && tokenList.size() > 1) { // must at least have two items in a pattern
                        Token token = tokenList.get(0);
                        String key = token.getText().toLowerCase();
                        if (key.equals("*")) {
                            throw new IOException("error: pattern cannot start with *");
                        }
                        AimlPattern root = nodeSet.get(key);
                        if (root == null) {
                            root = new AimlPattern(key);
                            nodeSet.put(key, root);
                        }
                        addPattern(root, 1, tokenList, aimlTemplate);
                    }
                }
            }
        }
    }

    /**
     * Expand brackets for (item1|item2|)  (last one's empty)
     * @param str the string to examine and expand
     * @return a list of expansions (or string itself if not the case)
     */
    private List<String> expandBrackets( String str ) {
        List<String> resultList = new ArrayList<>();
        if ( str.contains("(") ) {
            List<Token> tokenList = tokenizer.filterOutSpaces(tokenizer.tokenize(str));
            List<String> builder = new ArrayList<>();
            StringBuilder sb = new StringBuilder();
            int i = 0;
            while ( i < tokenList.size() ) {
                String text = tokenList.get(i).getText();
                if ( text.equals("(") ) {

                    // finalise the previous results
                    builder = finish(builder, sb);

                    List<String> itemList = new ArrayList<>();
                    int j = i + 1;
                    StringBuilder item = new StringBuilder();
                    while ( j < tokenList.size() ) {
                        String t2 = tokenList.get(j).getText();
                        if ( t2.equals(")") ) {
                            itemList.add(item.toString());
                            j++;
                            break;
                        } else if ( t2.equals("|") ) {
                            itemList.add(item.toString());
                            item.setLength(0);
                        } else {
                            if ( item.length() > 0 ) {
                                item.append(" ");
                            }
                            item.append(t2);
                        }
                        j++;
                    }

                    // generate new list
                    List<String> newBuilder = new ArrayList<>();
                    for ( String str1 : builder ) {
                        for ( String str2 : itemList ) {
                            String str3 = str1 + " " + str2;
                            newBuilder.add(str3.trim());
                        }
                    }
                    builder = newBuilder;
                    i = j; // advance
                } else {
                    if ( sb.length() > 0 ) {
                        sb.append(" ");
                    }
                    sb.append(text);
                    i++;
                }
            }

            // finalise the results
            return finish(builder, sb);

        } else {
            // no ( | )
            resultList.add(str);
        }
        return resultList;
    }

    /**
     * finalise dealing with the builder string given a string builder that has
     * been collecting information
     * @param builder the builder to add sb to
     * @param sb the string builder
     * @return the modified builder with sb appended
     */
    private List<String> finish(List<String> builder, StringBuilder sb ){
        if ( sb.length() > 0 ) {
            // add the current sb content to all previous builder items
            if ( builder.size() == 0 ) {
                builder.add(sb.toString());
                sb.setLength(0);
            } else {
                List<String> newBuilder = new ArrayList<>();
                for ( String str1 : builder ) {
                    String str3 = str1 + " " + sb.toString();
                    newBuilder.add(str3.trim());
                }
                sb.setLength(0);
                return newBuilder;
            }
        } else if ( builder.size() == 0 ) {
            // make sure the builder has an initial value to proceed with
            builder.add("");
        }
        return builder;
    }

    /**
     * process the patterns and create a tree of patterns that can be matched to user input strings
     * @param nodeSet the parent set of nodes - recursively updated
     * @param index the index into tokenList
     * @param tokenList the list of token making up the pattern
     * @param new_template the template to be added to the last node
     */
    private void addPattern( AimlPattern nodeSet, int index, List<Token> tokenList, AimlTemplate new_template ) {
        if ( index + 1 == tokenList.size() ) {
            // last node
            Token token = tokenList.get(index);
            String key = token.getText().toLowerCase();
            AimlPattern template = nodeSet.getNodeSet().get(key);
            if ( template == null ) {
                template = new AimlPattern(key);
                template.getTemplateList().add(new_template);
                nodeSet.getNodeSet().put(key, template);
            } else { // existing template - all these sets as alternatives
                template.getTemplateList().add(new_template);
            }
        } else if ( index < tokenList.size() ) {
            // in between node
            Token token = tokenList.get(index);
            String key = token.getText().toLowerCase();
            AimlPattern template = nodeSet.getNodeSet().get(key);
            if ( template == null ) {
                template = new AimlPattern(key);
                nodeSet.getNodeSet().put(key, template);
            }
            addPattern(template, index + 1, tokenList, new_template);
        }
    }



    // think consists of many set items internally
    private void processThink( AimlTemplate template, Node node ) {
        NodeList nodeList = node.getChildNodes();
        for ( int i = 0; i < nodeList.getLength(); i++ ) {
            Node child = nodeList.item(i);
            if ( "set".equals(child.getNodeName()) ) {
                processSet(template, child);
            }
        }
    }

    // perform a set into the environment of template, return the value set
    private String processSet( AimlTemplate template, Node node ) {
        String value = null;
        NodeList nodeList = node.getChildNodes();
        for ( int i = 0; i < nodeList.getLength(); i++ ) {
            Node child = nodeList.item(i);
            if ( "#text".equals(child.getNodeName()) ) {
                value = child.getTextContent();
                break;
            }
        }
        if ( value != null ) {
            NamedNodeMap map = node.getAttributes();
            if ( map != null && map.getNamedItem("name") != null ) {
                Node nameNode = map.getNamedItem("name");
                String name = nameNode.getTextContent();
                if ( name != null ) {
                    if ( name.equals("it") ) {
                        NodeList children = node.getChildNodes();
                        for ( int i = 0; i < children.getLength(); i++ ) {
                            Node child = children.item(i);
                            String temp = processSet(template, child); // skip silly "it" nodes
                            if ( temp != null ) {
                                value = temp;
                            }
                        }
                    } else {
                        template.getEnvironment().put(name, value);
                    }
                }
            }
        }
        return value;
    }

    public int getTemplateCount() {
        return templateCount;
    }

    public int getPatternCount() {
        return patternCount;
    }

}

