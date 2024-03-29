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

package industries.vocht.viki.model;

import industries.vocht.viki.dao.PennType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/*
 * Created by peter on 17/12/14.
 *
 * UTF8 letters and constants
 *
 */
public class TokenizerConstants {

    public enum Type {
        Space,
        Number,
        Text,
        Special,
        SQuote,
        DQuote,
        Hyphen,
        Punctuation,
        FullStop,
        Possessive,
        GrammarParsed
    }

    // convert a defined type to a Lexicon item type for the nl parsers
    // best effort - this should only be used if the lexicon doesn't have an entry
    // for the word presented
    public static PennType convert(Token token ) {
        if ( token != null && token.getType() != null && token.getText() != null && token.getText().length() > 0 ) {
            switch (token.getType()) {
                case Text: {
                    char ch = token.getText().charAt(0);
                    if ( ch >= 'A' && ch <= 'Z' )
                        return PennType.NP;
                    if (token.getText().endsWith("s") ) // wild guess
                        return PennType.NNS; // don't know
                    else
                        return PennType.NN; // don't know
                }
                case Number:
                    return PennType.CD;
                case FullStop:
                case Hyphen:
                    return PennType.PUN;
                case Possessive:
                    return PennType.POS;
                case Special:
                    return PennType.PUN;
                case Punctuation: {
                    String text = token.getText();
                    if ( text.equals("(") || text.equals("[") || text.equals("{") )
                        return PennType.LRB;
                    if ( text.equals(")") || text.equals("]") || text.equals("}") )
                        return PennType.RRB;

                    return PennType.PUN; // includes "?"
                }
            }
        }
        return PennType.UNC;
    }

    // return true if ch is a number 0..9
    public static boolean isNumeric( char ch ) {
        return ch >= '0' && ch <= '9';
    }

    // clean all special hyphens, quotes, full-stops and white-spaces
    public static String clean_utf8(String str) {
        if (str != null) {
            StringBuilder sb = new StringBuilder();
            for (char ch : str.toCharArray()) {
                if ( isFullStop(ch) ) {
                    sb.append(".");
                } else if ( isSingleQuote(ch) ) {
                    sb.append("'");
                } else if ( isDoubleQuote(ch) ) {
                    sb.append("\"");
                } else if ( isWhiteSpace(ch) ) {
                    sb.append(" ");
                } else if ( isHyphen(ch) ) {
                    sb.append("-");
                } else {
                    sb.append(ch);
                }
            }
            return sb.toString();
        }
        return null;
    }

    // return true if ch is a full-stop
    protected static boolean isFullStop( char ch ) {
        return ch == '\u002e' || ch == '\u06d4' || ch == '\u0701' || ch == '\u0702' ||
               ch == '\ufe12' || ch == '\ufe52' || ch == '\uff0e' || ch == '\uff61';
    }

    // word start symbols (preceded by a space but not followed)
    public static boolean isWordStartSymbol( char ch ) {
        return ch == '\\' || ch == '/' || ch == '[' || ch == '{' || ch == '(';
    }

    // word end symbols (followed, but not preceded by a space)
    public static boolean isWordEndSymbol( char ch ) {
        return ch == ':' || ch == ']' || ch == ')' || ch == '}' || ch == '.' || ch == '!' || ch == '?' || ch == ',' || ch == ';';
    }

    // return true if ch is a full-stop
    protected static boolean isPunctuation( char ch ) {
        return ch == '!' || ch == '?' || ch == ',' || ch == ':' || ch == ';';
    }

    // return true if ch is a single quote character
    protected static boolean isSingleQuote( char ch ) {
        return ch == '\'' || ch == '\u02bc' || ch == '\u055a' || ch == '\u07f4' || ch =='\u07f5' || ch == '\u2019' || ch =='\uff07' ||
               ch == '\u2018' || ch == '\u201a' || ch == '\u201b' || ch == '\u275b' || ch == '\u275c';
    }

    // return true if ch is a double quote character
    protected static boolean isDoubleQuote( char ch ) {
        return ch == '\u0022' || ch == '\u00ab' || ch == '\u00bb' || ch == '\u07f4' || ch =='\u07f5' || ch == '\u2019' || ch == '\uff07' ||
               ch == '\u201c' || ch == '\u201d' || ch == '\u201e' || ch == '\u201f' || ch =='\u2039' || ch == '\u203a' || ch == '\u275d' ||
               ch == '\u276e' || ch == '\u2760' || ch == '\u276f';
    }

    // return true if ch is a hyphen
    protected static boolean isHyphen( char ch ) {
        return ch == '\u002d' || ch == '\u207b' || ch == '\u208b' || ch == '\ufe63' || ch == '\uff0d' || ch =='\u2014';
    }

    // return true if ch is a unicode letter a..z A..Z
    public static boolean isABC( char ch ) {
        return azLookup.contains(ch);
    }

    // return true if ch is a special character, all the other allowed characters like ? ! [ ] ( ) etc.
    protected static boolean isSpecialCharacter( char ch ) {
        return specialCharacterLookup.contains(ch);
    }

    // return true if ch is a white space character
    protected static boolean isWhiteSpace( char ch ) {
        return ch == ' ' || ch ==  '\t' || ch ==  '\r' || ch ==  '\n' || ch == '\u0008' ||
               ch == '\ufeff' || ch == '\u303f' || ch == '\u3000' || ch == '\u2420' || ch == '\u2408' || ch == '\u202f' || ch == '\u205f' ||
               ch == '\u2000' || ch == '\u2002' || ch == '\u2003' || ch == '\u2004' || ch == '\u2005' || ch == '\u2006' || ch == '\u2007' ||
               ch == '\u2008' || ch == '\u2009' || ch == '\u200a' || ch == '\u200b';
    }

    // fast lookup tables
    private static HashSet<Character> azLookup = null;
    private static HashSet<Character> specialCharacterLookup = null;

    private static String[] extraAZ_a = new String[]
            {
                    // "a" variants
                    "\u00c0", "\u00c1", "\u00c2", "\u00c3", "\u00c4", "\u00c5", "\u00c6",
                    "\u00e0", "\u00e1", "\u00e2", "\u00e3", "\u00e4", "\u00e5", "\u00e6",
                    "\u0100", "\u0101" // macrons
            };

    private static String[] extraAZ_c = new String[]
            {
                    // "c" variants
                    "\u00c7", "\u00e7", "\u0106", "\u0107", "\u0108", "\u0109", "\u010a", "\u010b", "\u010c", "\u010d"
            };

    private static String[] extraAZ_e = new String[]
            {
                    // "e" variants
                    "\u00c8", "\u00c9", "\u00ca", "\u00cb", "\u00d8", "\u00d9", "\u00da", "\u00db", "\u00e8", "\u00e9", "\u00ea", "\u00eb",
                    "\u0112", "\u0113" // macrons
            };

    private static String[] extraAZ_i = new String[]
            {
                    // "i" variants
                    "\u00cc", "\u00cd", "\u00ce", "\u00cf", "\u00ec", "\u00ed", "\u00ee", "\u00ef",
                    "\u012a", "\u012b" // macrons
            };

    private static String[] extraAZ_o = new String[]
            {
                    // "o" variants
                    "\u00d2", "\u00d3", "\u00d4", "\u00d5", "\u00d6", "\u00d7", "\u00d8",
                    "\u00f2", "\u00f3", "\u00f4", "\u00f5", "\u00f6", "\u00f7", "\u00f8",
                    "\u014c", "\u014d" // macrons
            };

    // extra unicode characters that are valid a..z
    private static String[] extraAZ_u = new String[]
            {
                    // "u" variants
                    "\u00d9", "\u00da", "\u00db", "\u00dc", "\u00f9", "\u00fa", "\u00fb", "\u00fc",
                    "\u016a", "\u016b" // macrons
            };

    // allowed special characters
    private static char[] specialCharacters = new char[]
            {
                '_', '%', '$', '#', '@', '^', '&', '*', '(', ')', '^',
                '[', '{', ']', '}', '<', '>', '/', '\\', '=', '+', '|'
            };

    private static String[] contractionsPrefix =
            {
                    "couldn", "didn", "doesn", "don", "hadn", "hasn", "haven", "he",
                    "how", "i", "isn", "it", "might", "mightn", "must", "mustn",
                    "she", "we", "weren", "what", "when", "where", "who", "would",
                    "wouldn", "you", "should", "shouldn", "won", "wont"
            };

    private static String[] contractionsPostfix =
            {
                    "ll", "d", "re", "s", "t", "ve", "m"
            };

    // contraction -> verb with penn-type
    private static String[] contractionVerbList =
            {
                    "didn't", "did", "VBD",
                    "doesn't", "does", "VBZ",
                    "don't", "do", "VB",
                    "hadn't", "had", "VBD",
                    "hasn't", "has", "VBZ",
                    "haven't", "have", "VB",
                    "I'm", "am", "VBP",
                    "isn't", "is", "VBP",
                    "might've", "have", "VB",
                    "would've", "have", "VB",
                    "you've", "have", "VB",
                    "should've", "have", "VB",
                    "wont've", "have", "VB",
            };

    /**
     * get the item @ index in tokenList a contraction?
     * a contraction is always 3 symbols in the tokenizer
     * @param tokenList the list to check
     * @param index the index into the list
     * @return the contracted token or null if there is none there
     */
    protected static Token getContraction(List<Token> tokenList, int index) {
        if ( index + 2 < tokenList.size() ) {
            Token t1 = tokenList.get(index);
            Token t2 = tokenList.get(index+1);
            Token t3 = tokenList.get(index+2);
            if ( contractionsPrefixSet.contains(t1.getText().toLowerCase()) &&
                    "'".equals(t2.getText()) &&
                    contractionsPostfixSet.contains(t3.getText().toLowerCase()) ) {
                String contractionStr = t1.getText() + "'" + t3.getText();
                return new Token(contractionStr, t1.getPennType(), "contraction");
            } else if ( "'".equals(t2.getText()) && "s".equals(t3.getText().toLowerCase())) {
                return new Token(t1.getText() + "'s", t1.getPennType(), "contraction");
            }
        }
        return null;
    }

    /**
     * indexes have problems with contractions - if we see a contraction, we can
     * extract a verb of it if applicable (null otherwise)
     * @param contraction the text contraction to check
     * @return the associated verb token or null if dne
     */
    public static Token contractionToVerbToken(String contraction) {
        return contractionLookupSet.get(contraction);
    }

    private static HashSet<String> contractionsPrefixSet = null;
    private static HashSet<String> contractionsPostfixSet = null;
    private static HashMap<String, Token> contractionLookupSet = null;

    static
    {
        azLookup = new HashSet<>( 200 );
        for ( char ch = 'a'; ch <= 'z'; ch++ )
            azLookup.add( ch );
        for ( char ch = 'A'; ch <= 'Z'; ch++ )
            azLookup.add( ch );
        for ( String str : extraAZ_a )
            azLookup.add( str.toCharArray()[0] );
        for ( String str : extraAZ_c )
            azLookup.add( str.toCharArray()[0] );
        for ( String str : extraAZ_e )
            azLookup.add( str.toCharArray()[0] );
        for ( String str : extraAZ_i )
            azLookup.add( str.toCharArray()[0] );
        for ( String str : extraAZ_o )
            azLookup.add( str.toCharArray()[0] );
        for ( String str : extraAZ_u )
            azLookup.add( str.toCharArray()[0] );

        specialCharacterLookup = new HashSet<>();
        for ( char ch : specialCharacters ) {
            specialCharacterLookup.add(ch);
        }

        contractionsPrefixSet = new HashSet<>();
        contractionsPrefixSet.addAll(Arrays.asList(contractionsPrefix));

        contractionsPostfixSet = new HashSet<>();
        contractionsPostfixSet.addAll(Arrays.asList(contractionsPostfix));

        contractionLookupSet = new HashMap<>();
        for (int i = 0; i < contractionVerbList.length; i += 3) {
            String contraction = contractionVerbList[i];
            String verb = contractionVerbList[i+1];
            PennType pennType = PennType.fromString(contractionVerbList[i+2]);
            contractionLookupSet.put(contraction, new Token(verb, pennType));
        }
    }


}

