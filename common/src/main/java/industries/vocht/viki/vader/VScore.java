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

package industries.vocht.viki.vader;

import java.text.DecimalFormat;

/**
 * Created by peter on 18/03/16.
 *
 * a vader score element for a sentence
 *
 */
public class VScore {
    private double positive;
    private double neutral;
    private double negative;
    private double compound;

    public VScore() {
    }

    public VScore( double positive, double neutral, double negative, double compound ) {
        this.positive = positive;
        this.neutral = neutral;
        this.negative = negative;
        this.compound = compound;
    }

    public String toString() {
        DecimalFormat df3 = new DecimalFormat("#.###");
        DecimalFormat df4 = new DecimalFormat("#.####");
        return "{'neg': " + df3.format(negative) + ", 'neu': " + df3.format(neutral) +
                ", 'pos': " + df3.format(positive) + ", 'compound': " + df4.format(compound) +"}";
    }

    public double getPositive() {
        return positive;
    }

    public double getNeutral() {
        return neutral;
    }

    public double getNegative() {
        return negative;
    }

    public double getCompound() {
        return compound;
    }
}
