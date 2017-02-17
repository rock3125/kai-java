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

package industries.vocht.viki.parser;

import industries.vocht.viki.model.Token;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by peter on 20/04/16.
 *
 * figure out what times are being used
 * for missing times and dates to turn them all into
 * absolute reference points
 *
 */
public class NLTimeResolver {

    private static final Logger logger = LoggerFactory.getLogger(NLTimeResolver.class);


    public NLTimeResolver() {
    }

    /**
     * convert a set of tokens to actual dates and times where possible
     * @param dateTimeTokenList the list of tokens detected containing time
     * @param referenceDate an outside reference for figuring out dates if needed (from meta-data)
     */
    public void resolveTimeTokens(List<Token> dateTimeTokenList, DateTime referenceDate, Map<String, String> grammarConversionMap ) {

        if ( dateTimeTokenList != null && dateTimeTokenList.size() > 0 ) {

            // keep track of the current date for time variations without a date
            long previousDate = 0L;
            if ( referenceDate != null ) {
                previousDate = referenceDate.toDate().getTime();
            }

            for ( Token dateTime : dateTimeTokenList ) {

                // convert it to a java date/time
                String str = dateTime.getText();
                str = str.replace('-', '/'); // always forward slashes for dates
                String grammar = dateTime.getGrammarRuleName();
                String pattern = grammarConversionMap.get(grammar);
                if ( pattern != null ) {
                    SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);
                    try {
                        Date parsedDate = dateFormat.parse(str);
                        DateTime dateTime1 = new DateTime(parsedDate.getTime());

                        // a date rule?
                        if ( grammar.startsWith("date.") ) {
                            // strip off any time info
                            previousDate = dateTime1.toLocalDate().toDate().getTime();

                            // add the full thing to the list of items that was successfully processed
                            // taking the long of the date-time as the word of the index, do not use local-date here!
                            dateTime.setValue( dateTime1.toDate().getTime() );
                        }

                        // time only?
                        else if (grammar.startsWith("time.") ) {

                            if  (previousDate == 0L ) {
                                logger.warn("time string without date-reference, ignored " + str);
                            } else {
                                dateTime.setValue( previousDate + dateTime1.toDate().getTime() );
                            }

                        }

                    } catch ( ParseException ex ) {
                        logger.error("invalid date-time string " + str + " for pattern " + pattern);
                    }
                } else {
                    logger.error("invalid date-time rule without pattern " + str);
                }

            } // for each time token

        } // if valid parameters

    }



}
