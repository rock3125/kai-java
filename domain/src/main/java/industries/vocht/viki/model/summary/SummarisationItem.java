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

import industries.vocht.viki.model.similar.SimilarDocument;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by peter on 17/04/16.
 *
 * a ui summarisation item for display
 *
 */
public class SummarisationItem {

    private List<String> wordList;
    private String sentence;
    private String url;

    // set of similar documents
    private List<SimilarDocument> similarDocumentList;

    public SummarisationItem() {
        wordList = new ArrayList<>();
        similarDocumentList = new ArrayList<>();
    }


    public List<String> getWordList() {
        return wordList;
    }

    public void setWordList(List<String> wordList) {
        this.wordList = wordList;
    }

    public String getSentence() {
        return sentence;
    }

    public void setSentence(String sentence) {
        this.sentence = sentence;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<SimilarDocument> getSimilarDocumentList() {
        return similarDocumentList;
    }

    public void setSimilarDocumentList(List<SimilarDocument> similarDocumentList) {
        this.similarDocumentList = similarDocumentList;
    }

}


