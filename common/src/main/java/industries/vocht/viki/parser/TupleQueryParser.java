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

import industries.vocht.viki.lexicon.Lexicon;
import industries.vocht.viki.lexicon.TupleUndesirables;
import industries.vocht.viki.model.Sentence;
import industries.vocht.viki.model.Token;
import industries.vocht.viki.model.semantics.TupleQuery;
import industries.vocht.viki.model.super_search.*;
import industries.vocht.viki.model.semantics.Tuple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * Created by peter on 9/06/16.
 *
 * special parser for turning nl queries into super search equivalents
 * for tuple searching, initially restricting to who, what, where, when, how kind of queries
 *
 */
@Component
public class TupleQueryParser {

    @Autowired
    private NLParser parser;

    @Autowired
    private Lexicon lexicon;

    @Autowired
    private TupleUndesirables tupleUndesirables;

    @Value("${hazelcast.semantic.shard.offset:1000000}")
    private int semanticShardOffset;

    // words that shouldn't be searched on like "something" or "somewhat", etc.
    // that are used to fill gaps in nl queries
    private static String[] invalidSearchWords = {"something", "somewhat", "somewhere", "sometime",
            "somehow", "someone", "thing", "object", "time", "location", "place", "person"};
    private HashSet<String> invalidSearchWordSet;

    public TupleQueryParser() {
        invalidSearchWordSet = new HashSet<>();
        invalidSearchWordSet.addAll(Arrays.asList(invalidSearchWords));
    }

    /**
     * convert a string to a super search tree
     * @param queryStr the string to convert
     * @return the super search query, or null
     */
    public TupleQuery parseQuery(String queryStr ) throws SSearchParserException, IOException {
        if ( queryStr != null ) {
            List<Sentence> sentenceList = parser.parseText(queryStr);
            if ( sentenceList == null || sentenceList.size() == 0 ) {
                return null;
            }
            if ( sentenceList.size() > 1 ) {
                throw new SSearchParserException("query too complex: " + queryStr);
            }
            List<Token> tokenList = Sentence.sentenceListToTokens(sentenceList);
            return tokensToQuery( tokenList );
        }
        return null;
    }

    /**
     * use who, what, where, when, how to build a super search query
     * @param tokenList the list of tokens to use
     * @return the super search query
     */
    private TupleQuery tokensToQuery(List<Token> tokenList ) {
        if ( tokenList != null ) {
            int meta_c_filter = Tuple.META_C_ALL;
            if ( contains(tokenList, "where", "somewhere", "location", "place") ) {
                meta_c_filter += Tuple.META_C_WHERE;
            }
            if ( contains(tokenList, "who", "someone", "person") ) {
                meta_c_filter += Tuple.META_C_WHO;
            }
            if ( contains(tokenList, "when", "sometime", "time") ) {
                meta_c_filter += Tuple.META_C_WHEN;
            }
            if ( contains(tokenList, "what", "something", "thing", "object") ) {
                meta_c_filter += Tuple.META_C_WHAT;
            }
            if ( contains(tokenList, "how", "somehow") ) {
                meta_c_filter += Tuple.META_C_HOW;
            }
            return buildMetadataQuery(tokenList, meta_c_filter );
        }
        return null;
    }

    /**
     * build a location search query
     * @param tokenList the set of tokens to build the query for
     * @return a search query for either origin or destination metadata searches on the semantic shards
     */
    private TupleQuery buildMetadataQuery(List<Token> tokenList, int meta_c_filter ) {
        if ( tokenList != null ) {

            List<Token> time = new ArrayList<>();
            List<Token> nouns = new ArrayList<>();
            List<Token> verbs = new ArrayList<>();
            List<Token> location = new ArrayList<>();
            breakDown( tokenList, time, nouns, verbs, location);

            return new TupleQuery(Tuple.META_TUPLE, toQuery(meta_c_filter, nouns, verbs, time, location));
        }
        return null;
    }

    /**
     * convert a list of items into a large OR query with all the required meta-data tags and sharding setup
     * @param meta_c_filter what metadata field is required (e.g. Tuple.META_C_WHO), or zero for any
     * @param nouns any nouns?
     * @param verbs any verbs?
     * @param times and time components
     * @param locations and location components
     * @return a query with all the items incorporated
     */
    private ISSearchItem toQuery( int meta_c_filter, List<Token> nouns, List<Token> verbs,
                                  List<Token> times, List<Token> locations ) {
        ISSearchItem root = null;

        if ( nouns != null ) {
            ISSearchItem agentRoot = null;
            for ( Token noun : nouns ) {
                String stemmedNoun = lexicon.getStem(noun.getText());
                if (!invalidSearchWordSet.contains(stemmedNoun)) {
                    agentRoot = addAnd(agentRoot, new SSearchWord(stemmedNoun, null, Tuple.META_TUPLE,
                            semanticShardOffset, meta_c_filter, false));
                }
            }
            for ( Token noun : locations ) {
                if (!invalidSearchWordSet.contains(noun.getText())) {
                    agentRoot = addAnd(agentRoot, new SSearchWord(noun.getText(), null, Tuple.META_TUPLE,
                            semanticShardOffset, meta_c_filter, false));
                }
            }
            root = agentRoot;
        }

        // todo: implement time
        if ( times != null ) {
            for ( Token time : times ) {
            }
        }

        if ( verbs != null ) {
            ISSearchItem verbRoot = null;
            for ( Token verb : verbs ) {
                String stemmedVerb = lexicon.getStem(verb.getText());
                if (!invalidSearchWordSet.contains(stemmedVerb)) {
                    verbRoot = addOr(verbRoot, new SSearchWord(stemmedVerb, null, Tuple.META_TUPLE,
                            semanticShardOffset, meta_c_filter, false));
                }
            }
            if ( root != null ) {
                root = addAnd(verbRoot, root); // and VERB with others
            } else {
                root = verbRoot;
            }
        }

        return root;
    }

    /**
     * combine the root with an or with searchWord
     * @param root the root (can be null)
     * @param searchWord the word to search for
     * @return the OR of the two in a super search statement
     */
    private ISSearchItem addOr( ISSearchItem root, ISSearchItem searchWord ) {
        if ( root == null ) {
            return searchWord;
        } else {
            return new SSearchOr(root, searchWord);
        }
    }

    /**
     * combine the root with an AND with searchWord
     * @param root the root (can be null)
     * @param searchWord the word to search for
     * @return the AND of the two in a super search statement
     */
    private ISSearchItem addAnd( ISSearchItem root, ISSearchItem searchWord ) {
        if ( root == null ) {
            return searchWord;
        } else {
            return new SSearchAnd(root, searchWord);
        }
    }

    /**
     * breakdown a list of tokens (tokenList) into constituent parts that matter
     * @param tokenList the list of tokens to check
     * @param time the time components in tokenList
     * @param nouns the nouns in tokenList
     * @param verbs the verbs in tokenList
     * @param location the locations in tokenList
     */
    private void breakDown( List<Token> tokenList, List<Token> time, List<Token> nouns, List<Token> verbs, List<Token> location ) {
        if ( tokenList != null ) {
            for ( Token token : tokenList ) {
                if ( token.getGrammarRuleName() != null && token.getValue() > 0L &&
                        (token.getGrammarRuleName().startsWith("date.") || token.getGrammarRuleName().startsWith("time.")) ) {
                    time.add( token );
                } else if ( token.getSemantic() != null && lexicon.isLocationSemantic(token.getSemantic()) ) {
                    location.add(token);
                } else if ( token.getPennType().toString().startsWith("VB") ) { // also RB?
                    if ( !tupleUndesirables.isUndesirable(token.getText()) ) {
                        verbs.add(token);
                    }
                } else if ( token.getPennType().toString().startsWith("NN") ||
                            token.getPennType().toString().startsWith("JJ") ) {
                    if ( !tupleUndesirables.isUndesirable(token.getText()) ) {
                        nouns.add(token);
                    }
                }
            }
        }
    }

    /**
     * return true if tokenList contains str
     * @param tokenList where to look
     * @param str_array items to look for
     * @return true if tokenList contains str
     */
    private boolean contains( List<Token> tokenList, String...str_array ) {
        if ( str_array != null && str_array.length > 0 && tokenList != null ) {
            for ( Token token : tokenList ) {
                for (String str : str_array) {
                    if (token.getText().compareToIgnoreCase(str) == 0) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

}

