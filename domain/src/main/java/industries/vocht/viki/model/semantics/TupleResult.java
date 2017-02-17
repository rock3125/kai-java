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

package industries.vocht.viki.model.semantics;

import industries.vocht.viki.model.Token;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by peter on 12/06/16.
 *
 * a UI pretty print version of a case tuple
 * cut-down set for returning and displaying
 *
 */
public class TupleResult {

    private String url; // document url
    private String tupleText; // tuple.toString()
    private String verb; // the verb

    // the sentence offset into the document of this tuple
    private int sentence;


    public TupleResult() {
    }

    public String getTupleText() {
        return tupleText;
    }

    public void setTupleText(String tupleText) {
        this.tupleText = tupleText;
    }

    public String getVerb() {
        return verb;
    }

    public void setVerb(String verb) {
        this.verb = verb;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getSentence() {
        return sentence;
    }

    public void setSentence(int sentence) {
        this.sentence = sentence;
    }

}
