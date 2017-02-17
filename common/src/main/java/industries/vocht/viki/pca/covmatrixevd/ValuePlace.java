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

package industries.vocht.viki.pca.covmatrixevd;

/**
 * Created by peter on 22/04/16.
 *
 */
class ValuePlace implements Comparable<ValuePlace>{
    public double value;
    public int place;

    public ValuePlace(double value, int place){
        this.value = value;
        this.place = place;
    }

    /**
     * Reverse comparison values to make the sorting in descending order
     */
    @Override
    public int compareTo(ValuePlace other) {
        if(this.value < other.value) return 1;
        if(this.value==other.value) return 0;
        return -1;
    }
}

