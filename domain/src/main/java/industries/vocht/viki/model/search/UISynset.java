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

package industries.vocht.viki.model.search;

/**
 * Created by peter on 24/05/16.
 *
 * a single synset for inside a set of items
 *
 */
public class UISynset {

    private String uniqueWord;
    private int synsetId;

    public UISynset() {
    }

    public UISynset( String uniqueWord, int synsetId ) {
        this.synsetId = synsetId;
        this.uniqueWord = uniqueWord;
    }

    public String getUniqueWord() {
        return uniqueWord;
    }

    public void setUniqueWord(String uniqueWord) {
        this.uniqueWord = uniqueWord;
    }

    public int getSynsetId() {
        return synsetId;
    }

    public void setSynsetId(int synsetId) {
        this.synsetId = synsetId;
    }

}

