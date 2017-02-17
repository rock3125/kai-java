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

package industries.vocht.viki.lexicon;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;

/**
 * Created by peter on 12/08/16.
 *
 * tuple specific undesirable items
 *
 */
@Component
public class TupleUndesirables {

    // set for lookup - read only
    private HashSet<String> undesirableSet;

    public TupleUndesirables() {
        undesirableSet = new HashSet<>();
        undesirableSet.addAll( Arrays.asList(undesirableList) );
        undesirableSet.addAll( Arrays.asList(specialCharacterList) );
    }

    // return true if the string passed in is null or an undesirable word
    // these are words that shouldn't really form part of any index because
    // of little value and high frequency
    public boolean isUndesirable( String str ) {
        return str == null || undesirableSet.contains(str);
    }

    public final String[] undesirableList = new String[] {
            // articles
            "the", "a", "an",

            // one offs (removed C, the language)
            "b", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p",
            "q", "r", "s", "t", "u", "v", "w", "x", "y", "z",

            // auxilliaries
            "be", "is", "am", "are", "was", "were", "being", "been",
            "do", "did", "doing", "done", "does",
            "have", "had", "having", "has",

            "n't", "''", "`"
    };

    // characters that are sort of noise and shouldn't be indexed
    public final String[] specialCharacterList = {

            // full stops
            "\u002e", "\u06d4", "\u0701", "\u0702",
            "\ufe12", "\ufe52", "\uff0e", "\uff61",

            "!", "?", ",", ":", ";",
            "_", "%", "$", "#", "@", "^", "&", "*", "(", ")", "^",
            "[", "{", "]", "}", "<", ">", "/", "\\", "=", "+", "|", "\"",

            // single quotes
            "\'", "\u02bc", "\u055a", "\u07f4",
            "\u07f5", "\u2019", "\uff07", "\u2018", "\u201a", "\u201b", "\u275b", "\u275c",

            // double quotes
            //"\u0022", "\u00bb", "\u00ab", "\u07f4", "\u07f5", "\u2019", "\uff07",
            "\u201c", "\u201d", "\u201e", "\u201f", "\u2039", "\u203a", "\u275d",
            "\u276e", "\u2760", "\u276f",

            // hyphens
            "\u002d", "\u207b", "\u208b", "\ufe63", "\uff0d",

            // whitespace and noise
            " ",  "\t",  "\r",  "\n", "\u0008",
            "\ufeff", "\u303f", "\u3000", "\u2420", "\u2408", "\u202f", "\u205f",
            "\u2000", "\u2002", "\u2003", "\u2004", "\u2005", "\u2006", "\u2007",
            "\u2008", "\u2009", "\u200a", "\u200b",
    };

}
