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

package industries.vocht.viki.model;

/**
 * Created by peter on 2/05/16.
 *
 * url with double value
 *
 */
public class UrlValue implements Comparable<UrlValue> {

    private String url;
    private double value;
    private int sentence_id;

    public UrlValue() {
    }

    public UrlValue( String url, double value, int sentence_id ) {
        this.url = url;
        this.value = value;
        this.setSentence_id(sentence_id);
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public int getSentence_id() {
        return sentence_id;
    }

    public void setSentence_id(int sentence_id) {
        this.sentence_id = sentence_id;
    }

    @Override
    public int compareTo(UrlValue o) {
        if ( value < o.value ) return 1;
        if ( value > o.value ) return -1;
        return 0;
    }


}


