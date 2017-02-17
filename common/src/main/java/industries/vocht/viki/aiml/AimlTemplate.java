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

import java.util.*;

/**
 * Created by peter on 23/07/16.
 *
 * an aiml template
 *
 */
public class AimlTemplate {

    private List<String> textList;
    private Map<String, String> environment;    // sort of a leftover from the original AI/ML
    private List<Token> starList;               // bindings for *

    private String kb_type;  // knowledge base type
    private String kb_field; // the field to query on

    public AimlTemplate() {
        this.environment = new HashMap<>();
        this.textList = new ArrayList<>();
    }

    public AimlTemplate(String kb_type, String kb_field) {
        this.environment = new HashMap<>();
        this.textList = new ArrayList<>();
        this.kb_type = kb_type;
        this.kb_field = kb_field;
    }

    // pretty print content of this template
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (kb_field != null && kb_type != null) {
            sb.append(kb_type).append("[").append(kb_field).append("]");
            if ( starList != null && starList.size() > 0 ) {
                Tokenizer tokenizer = new Tokenizer();
                sb.append(" => ").append(tokenizer.toString(starList));
            }
        } else if ( textList != null ) {
            for (String text : textList ) {
                sb.append(text).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * create a copy of this template to avoid polluting it and allowing bindings
     * @return a copy of this template
     */
    public AimlTemplate copy() {
        AimlTemplate t = new AimlTemplate(this.kb_type, this.kb_field);
        t.textList.addAll(this.textList);
        for (String key : this.environment.keySet()) {
            String value = environment.get(key);
            t.environment.put(key, value);
        }
        return t;
    }

    public List<String> getTextList() {
        return textList;
    }

    public void setText(List<Token> tokenList) {
        if ( tokenList != null ) {
            String text = new Tokenizer().toString(tokenList);
            text = text.replace("< br / >", "<br/>"); // special case
            textList.add(text);
        }
    }

    public void addText(String str) {
        if ( str != null ) {
            textList.add(str);
        }
    }

    public Map<String, String> getEnvironment() {
        return environment;
    }

    public void setEnvironment(Map<String, String> environment) {
        this.environment = environment;
    }

    public String getKb_type() {
        return kb_type;
    }

    public void setKb_type(String kb_type) {
        this.kb_type = kb_type;
    }

    public String getKb_field() {
        return kb_field;
    }

    public void setKb_field(String kb_field) {
        this.kb_field = kb_field;
    }

    public List<Token> getStarList() {
        return starList;
    }

    public void setStarList(List<Token> starList) {
        this.starList = starList;
    }
}


