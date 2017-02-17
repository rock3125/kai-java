/*
 * Copyright (c) 2016 by Peter de Vocht
 *
 * All rights reserved. No part of this publication may be reproduced, distributed, or
 * transmitted in any form or by any means, including photocopying, recording, or other
 * electronic or mechanical methods, without the prior written permission of the publisher,
 * except in the case of brief quotations embodied in critical reviews and certain other
 * noncommercial uses permitted by copyright law.
 *
 */

package industries.vocht.viki.aiml;

import industries.vocht.viki.model.Token;
import industries.vocht.viki.tokenizer.Tokenizer;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by peter on 23/07/16.
 *
 * read / process an AIML processor definition
 *
 */
public class AimlXmlProcessor {

    // item tokenizer
    private Tokenizer tokenizer;
    private Map<String, AimlPattern> nodeSet;
    private int templateCount;
    private int patternCount;

    public AimlXmlProcessor() {
        tokenizer = new Tokenizer();
        nodeSet = new HashMap<>();
        templateCount = 0;
        patternCount = 0;
    }

    public AimlXmlProcessor(Map<String, AimlPattern> nodeSet) {
        this.tokenizer = new Tokenizer();
        this.nodeSet = nodeSet;
        this.templateCount = 0;
        this.patternCount = 0;
    }

    /**
     * process an AIML xml file and add it to the database
     * @param xmlString the xml string
     * @throws IOException wrong
     * @throws ParserConfigurationException wrong
     * @throws SAXException wrong
     */
    public void processXmlString( String xmlString ) throws IOException, ParserConfigurationException, SAXException {
        // process the xml
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        InputStream stream = new ByteArrayInputStream(xmlString.getBytes(StandardCharsets.UTF_8));
        Document doc = dBuilder.parse(stream);
        doc.getDocumentElement().normalize();
        processXml(doc);
    }

    /**
     * process an AIML xml file and add it to the database
     * @param filename the file name
     * @throws IOException wrong
     * @throws ParserConfigurationException wrong
     * @throws SAXException wrong
     */
    public void processFile( String filename ) throws IOException, ParserConfigurationException, SAXException {
        // process the xml
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(filename);
        doc.getDocumentElement().normalize();
        processXml(doc);
    }

    /**
     * helper function for both XmlString and processFile
     * @param doc the xml document to process
     * @throws IOException wrong
     */
    private void processXml(Document doc) throws IOException {
        NodeList nList = doc.getElementsByTagName("category");
        for ( int temp = 0; temp < nList.getLength(); temp++ ) {
            Node nNode = nList.item(temp);

            // collect the patterns and template
            List<String> patternList = new ArrayList<>();
            List<AimlTemplate> templateList = new ArrayList<>();
            NodeList xmlPList = nNode.getChildNodes();
            for ( int i = 0; i < xmlPList.getLength(); i++ ) {
                Node child = xmlPList.item(i);
                if ( "pattern".equals(child.getNodeName()) ) {
                    if ( child.getTextContent() != null ) {
                        patternList.add(child.getTextContent());
                    }
                }
                if ( "template".equals(child.getNodeName()) ) {
                    AimlTemplate template = processComplexTemplate(child);
                    if ( template != null ) {
                        templateList.add(template);
                    }
                }
            }
            // add this new information into the system
            addPattern( patternList, templateList );
        }
    }

    /**
     * add a pattern for prcessing
     * @param patternList the list of patterns to use
     * @param aimlTemplateList the list of templates to associate with this pattern
     */
    private void addPattern(List<String> patternList, List<AimlTemplate> aimlTemplateList ) throws IOException {
        if ( patternList != null && patternList.size() > 0 && aimlTemplateList != null && aimlTemplateList.size() > 0 ) {
            patternCount = patternCount + patternList.size();
            for ( String pattern1 : patternList ) {
                List<String> expandedPattern = expandBrackets(pattern1);
                for ( String pattern : expandedPattern ) {
                    List<Token> tokenList = tokenizer.filterOutSpaces(tokenizer.tokenize(pattern));
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
                        addPattern(root, 1, tokenList, aimlTemplateList);
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
     * @param templateList the list of template to be added to the last node
     */
    private void addPattern( AimlPattern nodeSet, int index, List<Token> tokenList, List<AimlTemplate> templateList ) {
        if ( index + 1 == tokenList.size() ) {
            // last node
            Token token = tokenList.get(index);
            String key = token.getText().toLowerCase();
            AimlPattern template = nodeSet.getNodeSet().get(key);
            if ( template == null ) {
                template = new AimlPattern(key);
                template.getTemplateList().addAll(templateList);
                nodeSet.getNodeSet().put(key, template);
            } else { // existing template - all these sets as alternatives
                template.getTemplateList().addAll(templateList);
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
            addPattern(template, index + 1, tokenList, templateList);
        }
    }


    /**
     * process a more complex AIML template with children
     * @param templateNode the AIML template node to process
     * @return the processed AIML template for these children
     */
    private AimlTemplate processComplexTemplate( Node templateNode ) throws IOException {
        if ( templateNode != null ) {
            NodeList nodeList = templateNode.getChildNodes();
            if (nodeList != null && nodeList.getLength() > 0) {
                AimlTemplate template = new AimlTemplate();
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < nodeList.getLength(); i++) {
                    Node node = nodeList.item(i);
                    String nodeStr = node.getNodeName();
                    if ("#text".equals(nodeStr)) {
                        if (sb.length() > 0) {
                            sb.append(" ");
                        }
                        sb.append(node.getTextContent());
                    } else if ("think".equals(nodeStr)) {
                        processThink(template, node);
                    } else if ("set".equals(nodeStr)) {
                        String value = processSet(template, node);
                        if ( value != null ) {
                            if (sb.length() > 0) {
                                sb.append(" ");
                            }
                            sb.append(value);
                        }
                    } else if ("random".equals(nodeStr)) {
                        processRandom(template, node);
                    } else if ("br".equals(nodeStr)) {
                        if (sb.length() > 0) {
                            sb.append(" ");
                        }
                        sb.append("<br/>");
                    } else if ("a".equals(nodeStr)) {
                        processA(template, node);
                    } else if ("srai".equals(nodeStr)) {
                        processSrai(template, node);
                    } else if ("get".equals(nodeStr)) {
                        String getStr = processGet(template, node);
                        if (getStr != null) {
                            if (sb.length() > 0) {
                                sb.append(" ");
                            }
                            sb.append(getStr);
                        }
                    } else if ("bot".equals(nodeStr)) {
                        if (sb.length() > 0) {
                            sb.append(" ");
                        }
                        sb.append("KAI");
                    } else {
                        throw new IOException("unknown template tag <" + nodeStr + "> in AIML");
                    }
                }
                if (sb.length() > 0) {
                    List<Token> tokenList = tokenizer.filterOutSpaces(tokenizer.tokenize(sb.toString()));
                    if (tokenList != null && tokenList.size() > 0) {
                        template.setText(tokenList);
                    }
                }
                templateCount = templateCount + 1;
                return template;
            }
        }
        return null;
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

    // add random elements to the template
    private void processRandom( AimlTemplate template, Node node ) {
        NodeList nodeList = node.getChildNodes();
        for ( int i = 0; i < nodeList.getLength(); i++ ) {
            Node child = nodeList.item(i);
            if ( "li".equals(child.getNodeName()) ) {
                template.addText(child.getTextContent());
            }
        }
    }

    private void processA( AimlTemplate template, Node node ) throws IOException {
        throw new IOException("<a template not implemented");
    }

    private void processSrai( AimlTemplate template, Node node ) throws IOException {
        throw new IOException("<srai template not implemented");
    }

    // insert a get token for a aiml session variable
    private String processGet( AimlTemplate template, Node node ) {
        NamedNodeMap map = node.getAttributes();
        if ( map != null && map.getNamedItem("name") != null ) {
            Node nameNode = map.getNamedItem("name");
            String name = nameNode.getTextContent();
            if ( name != null ) {
                return "{" + name + "}";
            }
        }
        return null;
    }

    public int getTemplateCount() {
        return templateCount;
    }

    public int getPatternCount() {
        return patternCount;
    }
}


