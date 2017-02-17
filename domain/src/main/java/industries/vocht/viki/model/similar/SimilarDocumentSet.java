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

package industries.vocht.viki.model.similar;

import industries.vocht.viki.document.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by peter on 17/04/16.
 *
 * set of documents similar to each other for a single url
 *
 */
public class SimilarDocumentSet implements Comparable<SimilarDocumentSet> {

    private String url;
    private Document document;
    private List<SimilarDocument> similarDocumentList;

    public SimilarDocumentSet() {
        this.similarDocumentList = new ArrayList<>();
    }

    public SimilarDocumentSet( String url, List<SimilarDocument> similarDocumentList ) {
        this.url = url;
        this.similarDocumentList = similarDocumentList;
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

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    @Override
    public int compareTo(SimilarDocumentSet other) {
        return url.compareTo(other.url);
    }


}


