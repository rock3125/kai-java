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

package industries.vocht.viki.model.super_search;

/**
 * Created by peter on 30/05/16.
 *
 * possible date range types, before a date, after a date, between two dates, or one date exact
 *
 */
public enum SSearchDateRangeType {

    Exact,
    Before,
    After,
    Between;

    /**
     * string to enum
     * @param operation the string to convert
     * @return the enum associated
     * @throws SSearchParserException
     */
    public static SSearchDateRangeType convert( String operation ) throws SSearchParserException {
        if ( operation != null ) {
            switch (operation.toLowerCase()) {
                case "before":
                    return Before;
                case "after":
                    return After;
                case "between":
                    return Between;
                case "exact":
                    return Exact;
            }
        }
        throw new SSearchParserException("invalid DateRangeType operation \"" + operation + "\"");
    }

}

