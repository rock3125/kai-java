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

package industries.vocht.viki.system_stats;

/**
 * Created by peter on 12/06/16.
 *
 * sample name long value sortable entity
 *
 */
public class NameValue implements Comparable<NameValue> {

    private String name;
    private long value;

    public NameValue() {
    }

    public NameValue( String name, long value ) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getValue() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }

    @Override
    public int compareTo(NameValue o) {
        if ( value < o.value ) return 1;
        if ( value > o.value ) return -1;
        return 0;
    }

}


