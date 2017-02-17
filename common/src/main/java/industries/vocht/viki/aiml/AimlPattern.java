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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by peter on 23/07/16.
 *
 * a pattern library
 *
 */
public class AimlPattern {

    private String text;
    private List<AimlTemplate> templateList;
    private Map<String, AimlPattern> nodeSet;

    public AimlPattern() {
        nodeSet = new HashMap<>();
        templateList = new ArrayList<>();
    }

    public AimlPattern(String text) {
        this.text = text;
        this.nodeSet = new HashMap<>();
        this.templateList = new ArrayList<>();
    }


    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<AimlTemplate> getTemplateList() {
        return templateList;
    }

    public void setTemplateList(List<AimlTemplate> templateList) {
        this.templateList = templateList;
    }

    public Map<String, AimlPattern> getNodeSet() {
        return nodeSet;
    }

    public void setNodeSet(Map<String, AimlPattern> nodeSet) {
        this.nodeSet = nodeSet;
    }


}

