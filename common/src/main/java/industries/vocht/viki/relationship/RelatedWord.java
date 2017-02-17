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

package industries.vocht.viki.relationship;

/*
 * Created by peter on 6/02/15.
 *
 * a word related to another word
 *
 */
public class RelatedWord {

    private String word;
    private float relationshipStrength; // between <0..1]

    // void constructor for JSON transport etc.
    public RelatedWord() {
    }

    public String toString() {
        return word;
    }

    public RelatedWord( String word, float relationshipStrength ) {
        this.word = word;
        this.relationshipStrength = relationshipStrength;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public float getRelationshipStrength() {
        return relationshipStrength;
    }

    public void setRelationshipStrength(float relationshipStrength) {
        this.relationshipStrength = relationshipStrength;
    }

}

