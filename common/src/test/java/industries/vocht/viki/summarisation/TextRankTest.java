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

package industries.vocht.viki.summarisation;

import industries.vocht.viki.relationship.RelatedWord;
import industries.vocht.viki.relationship.SynonymRelationshipProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.Assert;

import java.io.*;
import java.util.List;

/**
 * Created by peter on 31/03/16.
 *
 * test various aspects of the text rank system
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/test-context.xml"})
public class TextRankTest {

    @Autowired
    private SynonymRelationshipProvider relationshipProvider;

    @Test
    public void getRelationshipForWordWithPos() {
        List<RelatedWord> relationshipList = relationshipProvider.getRelationships("car");
        Assert.notNull(relationshipList);
        Assert.isTrue(relationshipList.size() > 0);
        checkRelationships( relationshipList, "vehicle" );
        checkRelationships( relationshipList, "car" );
    }


    ////////////////////////////////////////////////////////////////////////////////

    private void checkRelationships( List<RelatedWord> relationshipList, String str ) {
        Assert.isTrue(contains(relationshipList, str));
    }

    /**
     * helper function - does the relationship list contain the item
     * @param relationshipList the list of relationships
     * @param itemStr the item to look for
     * @return true if the item was found
     */
    private boolean contains( List<RelatedWord> relationshipList, String itemStr ) {
        for ( RelatedWord relationship : relationshipList ) {
            if ( relationship.toString().contains(itemStr) ) {
                return true;
            }
        }
        return false;
    }

    private String loadSherlockHolmes() throws IOException {
        InputStream in = getClass().getResourceAsStream("/sherlock-holmes-parsed-text.json");
        Assert.notNull(in);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder out = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            out.append(line);
        }
        return out.toString();
    }

}

