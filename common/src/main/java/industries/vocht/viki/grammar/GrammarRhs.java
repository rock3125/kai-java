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

package industries.vocht.viki.grammar;

import java.util.HashSet;

/*
 * Created by peter on 20/12/14.
 *
 * part of the grammar rule / list
 *
 */
public class GrammarRhs {

    // the text / literal
    public String text;

    // a reference to another rule
    public GrammarLhs reference;

    // + at the end of strings length > 1
    public boolean isRepeat;

    // bag of words / or equivalent
    public HashSet<String> patternSet;

    // number.range(1,31) type of filtering
    public int numberRangeStart;
    public int numberRangeEnd;

    public GrammarRhs() {
        text = null;
        reference = null;
        isRepeat = false;
        patternSet = null;
        numberRangeStart = 0;
        numberRangeEnd = 0;
    }

    public String toString() {
        if ( text != null ) {
            if ( isRepeat )
                return text + "+";
            if ( numberRangeStart != 0 || numberRangeEnd != 0 )
                return text + ".range(" + numberRangeStart + "," + numberRangeEnd + ")";
            return text;
        }

        if ( reference != null )
            return "<" + reference.rhsToString() + ">";

        if ( patternSet != null ) {
            StringBuilder sb = new StringBuilder();
            sb.append("[ ");
            for (String str : patternSet)
                sb.append(str).append(" ");
            sb.append("]");
            return sb.toString();
        }
        return "<null>";
    }

}

