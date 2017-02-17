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

/**
 * Created by peter on 17/04/16.
 *
 * two documents that are similar - and how they are
 *
 */
public class SimilarDocument implements Comparable<SimilarDocument> {

    private String url1;
    private String url2;
    private double similarity;
    private Document documentForUrl2;

    public SimilarDocument() {
    }

    public SimilarDocument( String url1, String url2, double similarity ) {
        this.url1 = url1;
        this.url2 = url2;
        this.similarity = similarity;
    }


    public String getUrl1() {
        return url1;
    }

    public void setUrl1(String url1) {
        this.url1 = url1;
    }

    public String getUrl2() {
        return url2;
    }

    public void setUrl2(String url2) {
        this.url2 = url2;
    }

    public double getSimilarity() {
        return similarity;
    }

    public void setSimilarity(double similarity) {
        this.similarity = similarity;
    }

    public Document getDocumentForUrl2() {
        return documentForUrl2;
    }

    public void setDocumentForUrl2(Document documentForUrl2) {
        this.documentForUrl2 = documentForUrl2;
    }

    @Override
    public int compareTo(SimilarDocument other) {
        if ( similarity < other.similarity ) return 1;
        if ( similarity > other.similarity ) return -1;
        return 0;
    }


}


