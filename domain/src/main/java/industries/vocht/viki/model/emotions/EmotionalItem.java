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

package industries.vocht.viki.model.emotions;

/**
 * Created by peter on 10/04/16.
 *
 * a single item in an emotional set
 *
 */
public class EmotionalItem {

    private int sentence_id;
    private double value;
    private int acl_hash;

    public EmotionalItem() {
    }

    public EmotionalItem( int sentence_id, double value, int acl_hash ) {
        this.sentence_id = sentence_id;
        this.value = value;
        this.setAcl_hash(acl_hash);
    }

    public int getSentence_id() {
        return sentence_id;
    }

    public void setSentence_id(int sentence_id) {
        this.sentence_id = sentence_id;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public int getAcl_hash() {
        return acl_hash;
    }

    public void setAcl_hash(int acl_hash) {
        this.acl_hash = acl_hash;
    }

}

