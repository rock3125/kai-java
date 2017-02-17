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

package industries.vocht.viki.semantic_search.util;

import com.carrotsearch.hppc.IntHashSet;
import industries.vocht.viki.datastructures.IntList;
import industries.vocht.viki.lexicon.Lexicon;
import industries.vocht.viki.model.Token;
import industries.vocht.viki.model.semantics.Tuple;
import industries.vocht.viki.model.semantics.TupleResult;
import industries.vocht.viki.tokenizer.Tokenizer;

import java.util.*;

/**
 * Created by peter on 4/06/16.
 *
 * used for highlighting words in the final result sets
 *
 */
public class SSearchHighlighter {

    private Lexicon lexicon;

    public SSearchHighlighter(Lexicon lexicon) {
        this.lexicon = lexicon;
    }


    /**
     * produce a highlight string for an index item given the document (tokenList) and a set of relationships of the query
     * @param offsetList the set of offsets in window spaces
     * @param tokenList the document contents
     * @param searchRelationshipSet query search relationships
     * @return a highlight string for pretty printing the result(s)
     */
    public String getHighlightStringForItem(IntList offsetList, List<Token> tokenList, Map<String, Integer> searchRelationshipSet, int windowSize ) {

        // create a set of offsets for highlighting this fragment
        int left = offsetList.get(0);
        int right = Math.min(left + windowSize * 2, tokenList.size() - 1);

        return toHighlightString(lexicon, tokenList.subList(left, right), searchRelationshipSet, left, offsetList );
    }

    /**
     * highlight the items inside a tuple
     * @param tuple the tuple to highlight
     * @param meta_c_type the things to highlight for query results
     * @param relationshipMap map of word -> strength of relationship
     * @return the tuple result with highlights
     */
    public TupleResult getHighlightStringForTuple(Tuple tuple, int meta_c_type, Map<String, Integer> relationshipMap ) {
        TupleResult tupleResult = new TupleResult();
        tupleResult.setUrl(tuple.getUrl());
        tupleResult.setSentence(tuple.getSentence_id());
        List<Token> tokenList = new ArrayList<>();
        tokenList.addAll(tuple.getRoot().retrieveAllTokens());
        tupleResult.setTupleText( toHighlightString(lexicon, tokenList, meta_c_type, relationshipMap) );
        return tupleResult;
    }

    /**
     * format a sentence using rules for punctuation
     * @param tokenList a list of tokens to format with spaces between words
     * @param wordSet the set of words and their level importants to the keyword that are all related
     * @param left the left hand side index, the actual offset / first index of the tokenList
     * @param offsetList the set of offsets for this index
     * @return a pretty string resembling an ordinary readable sentence for humans
     */
    private String toHighlightString(Lexicon lexicon, List<Token> tokenList, Map<String, Integer> wordSet, int left, IntList offsetList) {
        if ( tokenList != null ) {

            StringBuilder sb = new StringBuilder();
            IntHashSet offsetSet = offsetList.asHashSet();

            Token prev; // the previous token (or null)

            int size = tokenList.size();
            for ( int i = 0; i < size; i++ ) {

                Token curr = tokenList.get(i);
                if ( i > 0 ) {
                    prev = tokenList.get(i-1);
                } else {
                    prev = null;
                }

                // highlight?
                String stem = lexicon.getStem(curr.getText().toLowerCase());

                // three levels for now
                // level 1: exact match, level 2: related term, level 3: the word it found / focuses on
                int highlightLevel = 0;
                if ( wordSet != null ) {
                    if (offsetSet.contains(i + left)) { // is this an index match?
                        Integer level = wordSet.get(stem);
                        if (level != null) {
                            highlightLevel = level;
                        } else {
                            highlightLevel = 3;
                        }
                    } else {
                        Integer level = wordSet.get(stem);
                        if (level != null) {
                            highlightLevel = level;
                        }
                    }
                }

                // start highlight tag
                if ( highlightLevel > 0 ) {
                    sb.append("{hl").append(highlightLevel).append(":}");
                }

                // determine what the prev, next and current chars are
                // w = the word, or otherwise the punctuation mark / character
                char currCh = 'w';
                char prevCh = 'w';
                if ( curr.getText().length() == 1 && !Tokenizer.isABC(curr.getText().charAt(0)) ) {
                    currCh = curr.getText().charAt(0);
                }
                if ( prev != null && prev.getText().length() == 1 ) {
                    prevCh = prev.getText().charAt(0);
                } else if ( prev == null ) {
                    prevCh = 'x'; // no previous character
                }

                if ( Tokenizer.isWordStartSymbol(currCh) ) {
                    // previous character was a word or end of word symbol - this starts a word - append a space before it
                    if ( prevCh == 'w' || Tokenizer.isWordEndSymbol(prevCh) ) {
                        sb.append(" ");
                    }
                    sb.append(currCh);
                } else if ( currCh == 'w' ) {
                    // ordinary word - was previous character a word starter or nothing? then no space
                    if ( Tokenizer.isWordStartSymbol(prevCh) || prevCh == 'x' || prevCh == '-' || curr.getText().equals("'t") || curr.getText().equals("'s") ) {
                        sb.append(curr.getText());
                    } else {
                        sb.append(" ");
                        sb.append(curr.getText());
                    }
                } else {
                    sb.append(currCh);
                }

                // end tag
                if ( highlightLevel > 0 ) {
                    sb.append("{:hl").append(highlightLevel).append("}");
                }

            }
            return sb.toString();
        }
        return "";
    }


    /**
     * format a sentence using rules for punctuation
     * @param tokenList a list of tokens to format with spaces between words
     * @param tuple_c_type the semantic types the user is looking for
     * @param relationshipMap a map of string word -> relationship strength
     * @return a pretty string resembling an ordinary readable sentence for humans
     */
    private String toHighlightString(Lexicon lexicon, List<Token> tokenList,
                                     int tuple_c_type, Map<String, Integer> relationshipMap) {
        if ( tokenList != null && tuple_c_type != 0 ) {

            StringBuilder sb = new StringBuilder();

            Token prev; // the previous token (or null)

            int size = tokenList.size();
            for ( int i = 0; i < size; i++ ) {

                Token curr = tokenList.get(i);
                if ( i > 0 ) {
                    prev = tokenList.get(i-1);
                } else {
                    prev = null;
                }

                // three levels for now
                // level 1: exact match, level 2: related term, level 3: the word it found / focuses on
                int highlightLevel = 0;
                if ( (tuple_c_type & Tuple.META_C_WHO) != 0 ) {
                    if (( curr.getSemantic() != null && lexicon.isPersonSemantic(curr.getSemantic()) ) ||
                          curr.getPennType() != null && curr.getPennType().toString().startsWith("PR")  ) {
                        highlightLevel = 1;
                    }
                }
                if ( (tuple_c_type & Tuple.META_C_HOW) != 0 ) {
                    if (curr.getPennType() != null && curr.getPennType().toString().startsWith("VB")) {
                        highlightLevel = 2;
                    }
                }
                if ( (tuple_c_type & Tuple.META_C_WHAT) != 0 ) {
                    if (curr.getPennType() != null && curr.getPennType().toString().startsWith("NN")) {
                        highlightLevel = 2;
                    }
                }
                if ( (tuple_c_type & Tuple.META_C_WHERE) != 0 ) {
                    if (curr.getSemantic() != null && lexicon.isLocationSemantic(curr.getSemantic())) {
                        highlightLevel = 1;
                    }
                }
                if ( (tuple_c_type & Tuple.META_C_WHEN) != 0 ) {
                    if (curr.getGrammarRuleName() != null &&
                            ( curr.getGrammarRuleName().startsWith("date.") ||
                              curr.getGrammarRuleName().startsWith("time.")) ) {
                        highlightLevel = 1;
                    }
                }

                // highlight through text
                if ( highlightLevel == 0 && relationshipMap.containsKey(curr.getText()) ) {
                    highlightLevel = relationshipMap.get(curr.getText());
                }

                // start highlight tag
                if ( highlightLevel > 0 ) {
                    sb.append(" {hl").append(highlightLevel).append(":}");
                }

                // determine what the prev, next and current chars are
                // w = the word, or otherwise the punctuation mark / character
                char currCh = 'w';
                char prevCh = 'w';
                if ( curr.getText().length() == 1 && !Tokenizer.isABC(curr.getText().charAt(0)) ) {
                    currCh = curr.getText().charAt(0);
                }
                if ( prev != null && prev.getText().length() == 1 ) {
                    prevCh = prev.getText().charAt(0);
                } else if ( prev == null ) {
                    prevCh = 'x'; // no previous character
                }

                if ( Tokenizer.isWordStartSymbol(currCh) ) {
                    // previous character was a word or end of word symbol - this starts a word - append a space before it
                    if ( prevCh == 'w' || Tokenizer.isWordEndSymbol(prevCh) ) {
                        sb.append(" ");
                    }
                    sb.append(currCh);
                } else if ( currCh == 'w' ) {
                    // ordinary word - was previous character a word starter or nothing? then no space
                    if ( Tokenizer.isWordStartSymbol(prevCh) || prevCh == 'x' || prevCh == '-' || curr.getText().equals("'t") || curr.getText().equals("'s") ) {
                        sb.append(curr.getText());
                    } else {
                        sb.append(" ");
                        sb.append(curr.getText());
                    }
                } else {
                    sb.append(currCh);
                }

                // end tag
                if ( highlightLevel > 0 ) {
                    sb.append(" {:hl").append(highlightLevel).append("}");
                }

            }
            return sb.toString();
        } else if ( tokenList != null ) {
            StringBuilder sb = new StringBuilder();
            for (Token token : tokenList) {
                sb.append(token.getText()).append(" ");
            }
            return sb.toString();
        }
        return "";
    }

}
