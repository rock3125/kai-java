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

package industries.vocht.viki.tokenizer;

import industries.vocht.viki.model.SpacyToken;
import industries.vocht.viki.model.Token;
import industries.vocht.viki.model.TokenizerConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/*
 * Created by peter on 17/12/14.
 *
 * turn a string into a stream of tokens
 *
 */
public class Tokenizer extends TokenizerConstants {

    public Tokenizer() {
    }

    /**
     * take a string apart into tokens
     * @param str the stirng to take apart
     * @return a list of tokens that makes the string
     */
    public List<Token> tokenize(String str ) {
        if ( str != null && str.length() > 0 ) {
            List<Token> tokenList = new ArrayList<>();

            StringBuilder helper = new StringBuilder();

            char[] chArray = str.toCharArray();
            int length = chArray.length;

            int i = 0;
            while ( i < length ) {
                boolean tokenHandled = false;

                // whitespace scanner
                char ch = chArray[i];
                while ( isWhiteSpace(ch) && i < length ) {
                    tokenHandled = true;
                    i = i + 1;
                    if ( i < length ) ch = chArray[i];
                }

                if ( tokenHandled )
                    tokenList.add( new Token(Type.Space, " ") );

                // add full-stops?
                while ( isFullStop(ch) && i < length ) {
                    tokenHandled = true;
                    tokenList.add(new Token(Type.FullStop, "."));
                    i = i + 1;
                    if ( i < length ) ch = chArray[i];
                }

                // add hyphens?
                while ( isHyphen(ch) && i < length ) {
                    tokenHandled = true;
                    tokenList.add(new Token(Type.Hyphen, "-"));
                    i = i + 1;
                    if ( i < length ) ch = chArray[i];
                }

                // add single quotes?
                while ( isSingleQuote(ch) && i < length ) {
                    tokenHandled = true;
                    tokenList.add(new Token(Type.SQuote, "'"));
                    i = i + 1;
                    if ( i < length ) ch = chArray[i];
                }

                // add single quotes?
                while ( isDoubleQuote(ch) && i < length ) {
                    tokenHandled = true;
                    tokenList.add(new Token(Type.DQuote, "\""));
                    i = i + 1;
                    if ( i < length ) ch = chArray[i];
                }

                // add special characters ( ) etc.
                while ( isSpecialCharacter(ch) && i < length ) {
                    tokenHandled = true;
                    tokenList.add(new Token( Type.Special, Character.toString(ch) ));
                    i = i + 1;
                    if ( i < length ) ch = chArray[i];
                }

                // add punctuation ! ? etc.
                while ( isPunctuation(ch) && i < length ) {
                    tokenHandled = true;
                    tokenList.add(new Token( Type.Punctuation, Character.toString(ch) ));
                    i = i + 1;
                    if ( i < length ) ch = chArray[i];
                }

                // numeric processor
                helper.setLength(0);
                while ( isNumeric(ch) && i < length ) {
                    tokenHandled = true;
                    helper.append(ch);
                    i = i + 1;
                    if ( i < length ) ch = chArray[i];
                }
                if ( helper.length() > 0 )
                    tokenList.add( new Token(Type.Number, helper.toString()) );

                // text processor
                helper.setLength(0);
                while ( isABC(ch) && i < length ) {
                    tokenHandled = true;
                    helper.append(ch);
                    i = i + 1;
                    if ( i < length ) ch = chArray[i];
                }
                if ( helper.length() > 0 )
                    tokenList.add( new Token(Type.Text, helper.toString()) );

                // discard unknown token?
                if ( !tokenHandled ) {
                    i++; // skip
                }

            }

            // return the list if we have something
            if ( tokenList.size() > 0 )
                return handleContractions(tokenList);
        }
        return null;
    }

    /**
     * re-tokenize a spacey sentence because its tokenizer sucks - return null if there was no change
     * @param token a single token
     * @return a proper list of tokens
     */
    public List<Token> retokenize(Token token) {
        if ( token != null ) {
            List<Token> tokenList = tokenize(token.getText());
            if ( tokenList != null ) {
                for (Token t : tokenList) {
                    t.setPennType(token.getPennType());
                    t.setSemantic(token.getSemantic());
                    t.setSynid(token.getSynid());
                }
            } else {
                tokenList = new ArrayList<>();
                tokenList.add(token);
            }
            return tokenList;
        }
        return null;
    }

    /**
     * fix contractions - just put them back into one word and the possessive
     * @param tokenList the list of tokens to fix
     * @return a fixed list of tokens
     */
    private List<Token> handleContractions( List<Token> tokenList ) {
        List<Token> tokenListWithContractions = new ArrayList<>();
        int index = 0;
        while ( index < tokenList.size() ) {
            Token contraction = getContraction(tokenList, index);
            if ( contraction != null ) {
                tokenListWithContractions.add(contraction);
                index = index + 3;
            } else {
                tokenListWithContractions.add( tokenList.get(index) );
                index = index + 1;
            }
        }
        return tokenListWithContractions;
    }

    /**
     * given a list of tokens, remove all the white list items
     * @param tokenList a list of tokens in
     * @return the modified list of tokens with all white spaces removed
     */
    public List<Token> filterOutSpaces( List<Token> tokenList ) {
        if ( tokenList != null ) {
            return tokenList.stream().filter(token -> token.getType() != Type.Space).collect(Collectors.toList());
        }
        return null;
    }

    /**
     * given a list of tokens, remove all the punctuation marks
     * @param tokenList a list of tokens in
     * @return the modified list of tokens with all white spaces removed
     */
    public List<Token> filterOutPunctuation( List<Token> tokenList ) {
        if ( tokenList != null ) {
            return tokenList.stream().filter(token ->
                    (token.getType() != Type.Punctuation && token.getType() != Type.FullStop))
                    .collect(Collectors.toList());
        }
        return null;
    }

    /**
     * format a sentence using rules for punctuation
     * @param tokenList a list of tokens to format with spaces between words
     * @return a pretty string resembling an ordinary readable sentence for humans
     */
    public String toString( List<Token> tokenList ) {
        if ( tokenList != null ) {

            StringBuilder sb = new StringBuilder();

            Token prev; // the previous token (or null)

            boolean insideDoubleQuotes = false;
            int size = tokenList.size();
            for ( int i = 0; i < size; i++ ) {

                Token curr = tokenList.get(i);
                if ( i > 0 ) {
                    prev = tokenList.get(i-1);
                } else {
                    prev = null;
                }

                String currentText = curr.getText();

                // determine what the prev, next and current chars are
                // w = the word, i = image, or otherwise the punctuation mark / character
                char currCh = 'w';
                char prevCh = 'w';
                if ( curr.getText().length() == 1 ) {
                    currCh = curr.getText().charAt(0);
                    if ( currCh == '"' ) {
                        insideDoubleQuotes = !insideDoubleQuotes;
                    }
                }
                if ( prev != null && prev.getText().length() == 1 ) {
                    prevCh = prev.getText().charAt(0);
                } else if ( prev == null ) {
                    prevCh = 'x'; // no previous character
                }

                if ( isWordStartSymbol(currCh) ) {
                    // previous character was a word or end of word symbol - this starts a word - append a space before it
                    if ( prevCh == 'w' || isWordEndSymbol(prevCh) ) {
                        sb.append(" ");
                    }
                    sb.append(currCh);
                } else if ( currCh == 'w' ) {
                    // ordinary word - was previous character a word starter or nothing? then no space
                    if ( isWordStartSymbol(prevCh) || prevCh == 'x' || currentText.equals("'s") ||
                            currentText.equals("'t") || (prevCh == '"' && insideDoubleQuotes) ) {
                        sb.append(curr.getText());
                    } else {
                        sb.append(" ").append(curr.getText());
                    }
                } else if ( isWordEndSymbol(currCh) ) {
                    sb.append(currCh);
                } else if ( currCh != ' ' ) {
                    if ( currCh == '"' && !insideDoubleQuotes) {
                        sb.append(currCh);
                    } else {
                        sb.append(" ").append(currCh);
                    }
                }

            }
            return sb.toString();
        }
        return "";
    }

}

