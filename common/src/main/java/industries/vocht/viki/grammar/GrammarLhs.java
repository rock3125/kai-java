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

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

/*
 * Created by peter on 20/12/14.
 *
 * lhs name for a grammar pattern
 *
 */
public class GrammarLhs {

    public String name;
    public boolean isPublic;
    public String conversionPattern;        // pattern for converting this to a system entity (e.g. date/time)
    public String modifier;                 // correct badly parsed entities
    public List<GrammarRhs> rhsList;

    public GrammarLhs( boolean isPublic, String name, String conversionPattern, String modifier ) {
        this.name = name;
        this.isPublic = isPublic;
        this.conversionPattern = conversionPattern;
        this.modifier = modifier;
        this.rhsList = new ArrayList<>();
    }

    public void setRhs( List<GrammarRhs> rhsList ) {
        this.rhsList = rhsList;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if ( conversionPattern != null ) {
            sb.append("pattern ")
                    .append(name).append(" = ").append(conversionPattern);
        } else {
            sb.append((isPublic ? "public " : "private "));
            sb.append(name).append(" = ");
            sb.append(rhsToString());
        }
        return sb.toString();
    }

    public String rhsToString() {
        StringBuilder sb = new StringBuilder();
        for ( GrammarRhs rhs : rhsList )
            sb.append(rhs.toString()).append(" ");
        sb.setLength( sb.length() - 1 );
        return sb.toString();
    }

    // get the tokens that can start this grammar rule
    public List<String> getStartTokens() {
        if ( rhsList != null && rhsList.size() > 0 ) {
            GrammarRhs rhs = rhsList.get(0);

            // a reference to another rule?
            if ( rhs.reference != null )
                return rhs.reference.getStartTokens();

            if ( rhs.text != null ) {
                List<String> resultSet = new ArrayList<String>();
                resultSet.add( rhs.text );
                return resultSet;
            }

            if ( rhs.patternSet != null ) {
                List<String> resultSet = new ArrayList<String>();
                resultSet.addAll( rhs.patternSet );
                return resultSet;
            }
        }
        throw new InvalidParameterException("invalid pattern - can't get start tokens() for '" + name + "'");
    }

    public List<GrammarRhs> getRhsList() {
        return rhsList;
    }


}

