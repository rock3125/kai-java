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

package industries.vocht.viki.utility;

import industries.vocht.viki.ApplicationException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/*
 * Created by peter on 19/12/14.
 *
 *
 */
public class StringUtility {

    public StringUtility() {
    }

    // put some commas between the values - returning a pretty-printable and
    // read-able object
    public String prettyCommaPrint( long value ) {
        List<String> values = new ArrayList<>();

        long temp = value;
        while ( temp > 0 )
        {
            long thousand = temp % 1000;
            temp = temp / 1000;
            if ( temp > 0 ) values.add( threePrint(thousand) );
            else values.add( "" + thousand );
        }
        StringBuilder sb = new StringBuilder();
        for ( int i = values.size() - 1; i >= 0; i-- )
        {
            if ( sb.length() > 0 ) sb.append(",");
            sb.append(values.get(i));
        }
        return sb.toString();
    }

    /**
     * load a text file as a string in its entirety - helper function
     * @param fileName the name of the file
     * @return the string that is the file contents
     * @throws IOException
     */
    public String loadTextFile( String fileName ) throws IOException {
        return new String(Files.readAllBytes(Paths.get(fileName)));
    }

    /**
     * load a text file as a string in its entirety - helper function
     * @param fileName the name of the file
     * @return the string that is the file contents
     * @throws ApplicationException
     */
    public String loadTextFileFromResource( String fileName ) throws ApplicationException {
        InputStream in = getClass().getResourceAsStream(fileName);
        if ( in != null ) {
            StringBuilder sb = new StringBuilder();
            try {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                    String str;
                    while ((str = reader.readLine()) != null) {
                        sb.append(str).append("\n");
                    }
                }
            } catch (IOException ex) {
                throw new ApplicationException(ex.getMessage());
            }
            return sb.toString();
        }
        return null;
    }

    // copy a string
    public String copy( String str ) {
        if ( str != null ) {
            try {
                return new String(str.getBytes("UTF-8"));
            }
            catch (UnsupportedEncodingException ex) {
                return null;
            }
        }
        return null;
    }

    // prefix items with zeros (up to three)
    private String threePrint( long value ) {
        if ( value < 0 || value > 999 ) return "000";
        if ( value < 10 ) return "00" + value;
        if ( value < 100 ) return "0" + value;
        return "" + value;
    }

}

