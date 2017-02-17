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

package industries.vocht.viki.model.summary;

import industries.vocht.viki.model.Token;
import industries.vocht.viki.tokenizer.Tokenizer;

import java.util.List;

/**
 * Created by peter on 3/04/16.
 *
 * document summarisation fragment
 *
 */
public class SummarisationFragment implements Comparable<SummarisationFragment> {

    // a fragment of text as summarised
    private List<Token> text;

    // the score of this fragment
    private double score;

    public SummarisationFragment() {
    }

    public SummarisationFragment(List<Token> text, double score) {
        this.text = text;
        this.score = score;
    }

    public String toString() {
        if ( text != null && text.size() > 0 ) {
            Tokenizer tokenizer = new Tokenizer();
            return tokenizer.toString(text);
        }
        return "";
    }

    public List<Token> getText() {
        return text;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    @Override
    public int compareTo(SummarisationFragment other) {
        if (this.score > other.score) {
            return -1;
        } else if (this.score < other.score) {
            return 1;
        } else {
            return this.text.toString().compareTo(other.text.toString());
        }
    }

}

