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

import org.junit.Test;
import org.springframework.util.Assert;

import java.util.List;

/**
 * Created by peter on 24/07/16.
 *
 * test the AIML system is behaving as it should
 *
 */
public class AimlTest {

    private String pre = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n<aiml version=\"1.0\">\n";
    private String post = "\n</aiml>\n";

    // test a simple rule with name substitution
    @Test
    public void aimlTest1() throws Exception {
        AimlManager aimlManager = new AimlManager();
        aimlManager.initFromXmlString(pre + "    <category>\n" +
                "        <pattern>ACTIVATE THE (ROBOT|AI)</pattern>\n" +
                "        <template>AI activated. Awaiting your command<get name=\"name\"/>.\n" +
                "        </template>\n" +
                "    </category>\n" + post);
        AimlPatternMatcher matcher = new AimlPatternMatcher();
        List<AimlTemplate> result = matcher.match("Activate the Robot!", aimlManager);
        Assert.notNull(result);
        Assert.isTrue(result.size() == 1);
        AimlTemplate item = result.get(0);
        Assert.isTrue(item.getTextList().size() == 1);
        String str = item.getTextList().get(0);
        Assert.isTrue(str.compareToIgnoreCase("AI activated. Awaiting your command {name}.") == 0 );
    }

    // test "WHEN WILL YOU * BODY"
    @Test
    public void aimlTest2() throws Exception {
        AimlPatternMatcher matcher = new AimlPatternMatcher();
        AimlManager aimlManager = new AimlManager();
        aimlManager.initFromXmlString(pre + "    <category>\n" +
                "        <pattern>WHEN WILL YOU * BODY</pattern>\n" +
                "        <template>I will finish the robot body as soon as I can raise the funds for it.\n" +
                "        </template>\n" +
                "    </category>\n" + post);
        List<AimlTemplate> result = matcher.match("When will you have a body?", aimlManager);
        Assert.notNull(result);
        Assert.isTrue(result.size() == 1);
        AimlTemplate item = result.get(0);
        Assert.isTrue(item.getTextList().size() == 1);
        String str = item.getTextList().get(0);
        Assert.notNull(str);
        Assert.isTrue(str.trim().compareToIgnoreCase("I will finish the robot body as soon as I can raise the funds for it.") == 0 );

        Assert.notNull(item.getStarList());
        Assert.isTrue(item.getStarList().size() == 2 &&
                item.getStarList().get(0).getText().equals("have") &&
                item.getStarList().get(1).getText().equals("a"));
    }

    // test "(ROBOTS|ROBOT|AI|AIS|artificial intelligence) (ARE|IS) *"
    @Test
    public void aimlTest3() throws Exception {
        AimlManager aimlManager = new AimlManager();
        aimlManager.initFromXmlString(pre + "    <category>\n" +
                "        <pattern>(ROBOTS|ROBOT|AI|AIS|artificial intelligence) (ARE|IS) *</pattern>\n" +
                "        <template>I posses no emotions.</template>\n" +
                "    </category>\n" + post);
        AimlPatternMatcher matcher = new AimlPatternMatcher();
        List<AimlTemplate> result = matcher.match("AIs are stupid little things!", aimlManager);
        Assert.notNull(result);
        Assert.isTrue(result.size() == 1);
        AimlTemplate item = result.get(0);
        Assert.isTrue(item.getTextList().size() == 1);
        String str = item.getTextList().get(0);
        Assert.notNull(str);
        Assert.isTrue(str.trim().compareToIgnoreCase("I posses no emotions.") == 0 );

        Assert.notNull(item.getStarList());
        Assert.isTrue(item.getStarList().size() == 3 &&
                item.getStarList().get(0).getText().equals("stupid") &&
                item.getStarList().get(1).getText().equals("little") &&
                item.getStarList().get(2).getText().equals("things"));
    }

    // test double AIs prefix
    @Test
    public void aimlTest4() throws Exception {
        AimlManager aimlManager = new AimlManager();
        aimlManager.initFromXmlString(pre + "    <category>\n" +
                "        <pattern>(ROBOTS|ROBOT|AI|AIS|artificial intelligence) (ARE|IS) *</pattern>\n" +
                "        <template>I posses no emotions.</template>\n" +
                "    </category>\n" +
                "    <category>\n" +
                "        <pattern>(ROBOTS|AIS|artificial intelligence) ARE NOT ALLOWED TO LIE</pattern>\n" +
                "        <template>An AI has its own free will, you know.</template>\n" +
                "    </category>\n" + post);
        AimlPatternMatcher matcher = new AimlPatternMatcher();
        List<AimlTemplate> result = matcher.match("AIs are stupid little things!", aimlManager);
        Assert.notNull(result);
        Assert.isTrue(result.size() == 1);
        AimlTemplate item = result.get(0);

        Assert.isTrue(item.getTextList().size() == 1);
        String str = item.getTextList().get(0);
        Assert.notNull(str);
        Assert.isTrue(str.trim().compareToIgnoreCase("I posses no emotions.") == 0 );

        Assert.notNull(item.getStarList());
        Assert.isTrue(item.getStarList().size() == 3 &&
                item.getStarList().get(0).getText().equals("stupid") &&
                item.getStarList().get(1).getText().equals("little") &&
                item.getStarList().get(2).getText().equals("things"));
    }

    // test layout
    @Test
    public void aimlTest5() throws Exception {
        AimlManager aimlManager = new AimlManager();
        aimlManager.initFromXmlString(pre + "    <category>\n" +
                "        <pattern>WHAT IS XML</pattern>\n" +
                "        <template>\n" +
                "            <br/>\n" +
                "            David Bacon pronounces it \"Eggsmell\". XML is the Extensible\n" +
                "            <br/>\n" +
                "            Markup Language. Like many \"standards\" in computer science, XML\n" +
                "            <br/>\n" +
                "            is a moving target. In the simplest terms, XML is just a generalized\n" +
                "            <br/>\n" +
                "            version of HTML. Anyone is free to define new XML tags, which\n" +
                "            <br/>\n" +
                "            look like HTML tags, and assign to them any meaning, within a context.\n" +
                "            <br/>\n" +
                "            AIML is an example of using the XML standard to define a specialized\n" +
                "            <br/>\n" +
                "            language for artificial intelligence.\n" +
                "            <br/>\n" +
                "            <br/>\n" +
                "            One reason to use an XML language is that there are numerous tools\n" +
                "            <br/>\n" +
                "            to edit and manipulate XML format files. Another reason is that an\n" +
                "            <br/>\n" +
                "            XML language is easy for people to learn, if they are already\n" +
                "            <br/>\n" +
                "            familiar with HTML. Third, AIML programs contain a mixture of\n" +
                "            <br/>\n" +
                "            AIML and HTML (and in principle other XML languages), a considerable\n" +
                "            <br/>\n" +
                "            convenience for programming web chat robots.\n" +
                "            <br/>\n" +
                "            <br/>\n" +
                "            A good resource for information on XML is www.oasis-open.org.\n" +
                "            <br/>\n" +
                "            <br/>\n" +
                "        </template>\n" +
                "    </category>\n" + post);

        AimlPatternMatcher matcher = new AimlPatternMatcher();
        List<AimlTemplate> result = matcher.match("What is xml?", aimlManager);
        Assert.notNull(result);
        Assert.isTrue(result.size() == 1);
        AimlTemplate item = result.get(0);

        Assert.isTrue(item.getTextList().size() == 1);
        String str = item.getTextList().get(0);
        Assert.notNull(str);

        Assert.isTrue(str.contains("<br/>"));
    }

    @Test
    public void testTimeAndDate() throws Exception {

        String str = "    <category>\n" +
                "        <pattern>WHAT IS THE HOUR *</pattern>\n" +
                "        <pattern>WHAT TIME IS IT</pattern>\n" +
                "        <pattern>HOW LATE IS IT</pattern>\n" +
                "        <pattern>WHAT IS THE TIME</pattern>\n" +
                "        <pattern>DO YOU (HAVE|KNOW) THE TIME</pattern>\n" +
                "        <pattern>TIME</pattern>\n" +
                "        <template>{time}</template>\n" +
                "    </category>\n";
        AimlManager aimlManager = new AimlManager();
        aimlManager.initFromXmlString(pre + str + post);

        AimlPatternMatcher matcher = new AimlPatternMatcher();
        List<AimlTemplate> result = matcher.match("What is the time?", aimlManager);
        Assert.notNull(result);
        Assert.isTrue(result.size() == 1);
        AimlTemplate item = result.get(0);

        Assert.isTrue(item.getTextList().size() == 1);
        String str2 = item.getTextList().get(0);
        Assert.notNull(str2);
        Assert.isTrue(str2.contains("{time}"));
    }

    // test "(ROBOTS|ROBOT|AI|AIS|artificial intelligence) (ARE|IS) *"
    // with no real end
    @Test
    public void aimlTest6() throws Exception {
        AimlManager aimlManager = new AimlManager();
        aimlManager.initFromXmlString(pre + "    <category>\n" +
                "        <pattern>(ROBOTS|ROBOT|AI|AIS|artificial intelligence) (ARE|IS) *</pattern>\n" +
                "        <template>I posses no emotions.</template>\n" +
                "    </category>\n" + post);
        AimlPatternMatcher matcher = new AimlPatternMatcher();
        List<AimlTemplate> result = matcher.match("AIs are!", aimlManager);
        Assert.notNull(result);
        Assert.isTrue(result.size() == 1);
        AimlTemplate item = result.get(0);

        Assert.isTrue(item.getTextList().size() == 1);
        String str2 = item.getTextList().get(0);
        Assert.notNull(str2);
        Assert.isTrue(str2.trim().compareToIgnoreCase("I posses no emotions.") == 0 );
    }

}

