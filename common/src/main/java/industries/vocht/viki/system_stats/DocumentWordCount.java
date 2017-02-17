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

package industries.vocht.viki.system_stats;

import com.hazelcast.core.IMap;
import industries.vocht.viki.IHazelcast;
import industries.vocht.viki.dao.PennType;
import industries.vocht.viki.document.Document;
import industries.vocht.viki.lexicon.Lexicon;
import industries.vocht.viki.lexicon.Undesirables;
import industries.vocht.viki.model.Sentence;
import industries.vocht.viki.model.Token;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by peter on 23/04/16.
 *
 * hazelcast stored document word count sets at global, url, and sentence level
 *
 */
@Component
public class DocumentWordCount {

    public static final String WC_TOTAL_ALL = "{total-count}";
    public static final String WC_TOTAL_CONTENT_BYTES = "{total-content-bytes}";
    public static final String WC_TOTAL_VALID = "{total-valid-count}";
    public static final String WC_DOCUMENT_COUNT = "{document-count}";
    public static final String WC_SENTENCE_COUNT = "{sentence-count}";
    public static final String WC_TOTAL_INDEX_COUNT = "{total-index-count}";

    public static final String WC_NOUN = "{noun}";
    public static final String WC_VERB = "{verb}";
    public static final String WC_ADJECTIVE = "{adjective}";
    public static final String WC_ADVERB = "{adverb}";
    public static final String WC_PROPER_NOUN = "{proper-noun}";

    public static final String WC_PERCENTAGE = "{percent}";
    public static final String WC_DATE = "{date}";
    public static final String WC_URL = "{url}";
    public static final String WC_TIME = "{time}";
    public static final String WC_DECIMAL = "{decimal}";
    public static final String WC_NUMBER = "{number}";
    public static final String WC_EMAIL = "{email}";
    public static final String WC_MONEY = "{money}";
    public static final String WC_PHONE = "{phone}";


    @Autowired
    private IHazelcast hazelcast;

    @Autowired
    private Undesirables undesirables;

    @Autowired
    private Lexicon lexicon;

    public DocumentWordCount() {
    }


    /**
     * @return the system's general statistics on what is going on
     */
    public GeneralStatistics getGeneralStatistics(UUID organisation_id) {
        GeneralStatistics gs = new GeneralStatistics();
        IMap<String, Long> wordCountMap = hazelcast.getWordCountMap(organisation_id, Document.META_BODY);

        if ( wordCountMap != null ) {
            gs.setTotal_count(getLong(wordCountMap, WC_TOTAL_ALL));
            gs.setTotal_valid_count(getLong(wordCountMap, WC_TOTAL_VALID));
            gs.setDocument_count(getLong(wordCountMap, WC_DOCUMENT_COUNT));
            gs.setSentence_count(getLong(wordCountMap, WC_SENTENCE_COUNT));
            gs.setTotal_index_count(getLong(wordCountMap, WC_TOTAL_INDEX_COUNT));
            gs.setTotal_content_bytes(getLong(wordCountMap, WC_TOTAL_CONTENT_BYTES));

            gs.setNoun(getLong(wordCountMap, WC_NOUN));
            gs.setVerb(getLong(wordCountMap, WC_VERB));
            gs.setAdjective(getLong(wordCountMap, WC_ADJECTIVE));
            gs.setAdverb(getLong(wordCountMap, WC_ADVERB));
            gs.setProper_noun(getLong(wordCountMap, WC_PROPER_NOUN));

            gs.setPercent(getLong(wordCountMap, WC_PERCENTAGE));
            gs.setDate(getLong(wordCountMap, WC_DATE));
            gs.setUrl(getLong(wordCountMap, WC_URL));
            gs.setTime(getLong(wordCountMap, WC_TIME));
            gs.setDecimal(getLong(wordCountMap, WC_DECIMAL));
            gs.setNumber(getLong(wordCountMap, WC_NUMBER));
            gs.setEmail(getLong(wordCountMap, WC_EMAIL));
            gs.setMoney(getLong(wordCountMap, WC_MONEY));
            gs.setPhone(getLong(wordCountMap, WC_PHONE));
        }
        return gs;
    }


    /**
     * @return the a document's specific statistics
     */
    public GeneralStatistics getDocumentStatistics(List<Sentence> sentenceList) {
        GeneralStatistics gs = new GeneralStatistics();

        if ( sentenceList != null ) {
            Map<String, Long> wordCountMap = processDocument(sentenceList);
            gs.setTotal_count(getLong2(wordCountMap, WC_TOTAL_ALL));
            gs.setTotal_valid_count(getLong2(wordCountMap, WC_TOTAL_VALID));
            gs.setSentence_count(getLong2(wordCountMap, WC_SENTENCE_COUNT));

            gs.setNoun(getLong2(wordCountMap, WC_NOUN));
            gs.setVerb(getLong2(wordCountMap, WC_VERB));
            gs.setAdjective(getLong2(wordCountMap, WC_ADJECTIVE));
            gs.setAdverb(getLong2(wordCountMap, WC_ADVERB));
            gs.setProper_noun(getLong2(wordCountMap, WC_PROPER_NOUN));

            gs.setPercent(getLong2(wordCountMap, WC_PERCENTAGE));
            gs.setDate(getLong2(wordCountMap, WC_DATE));
            gs.setUrl(getLong2(wordCountMap, WC_URL));
            gs.setTime(getLong2(wordCountMap, WC_TIME));
            gs.setDecimal(getLong2(wordCountMap, WC_DECIMAL));
            gs.setNumber(getLong2(wordCountMap, WC_NUMBER));
            gs.setEmail(getLong2(wordCountMap, WC_EMAIL));
            gs.setMoney(getLong2(wordCountMap, WC_MONEY));
            gs.setPhone(getLong2(wordCountMap, WC_PHONE));
        }
        return gs;
    }


    /**
     * helper function - return map contents for name or 0 if dne
     * @param map the map to check
     * @param name the entity to get
     * @return 0 if dne, or the value of the map
     */
    private long getLong( IMap<String, Long> map, String name ) {
        Long value = map.get(name);
        if ( value != null ) {
            return value;
        }
        return 0L;
    }

    /**
     * helper function - return map contents for name or 0 if dne
     * @param map the map to check
     * @param name the entity to get
     * @return 0 if dne, or the value of the map
     */
    private long getLong2( Map<String, Long> map, String name ) {
        Long value = map.get(name);
        if ( value != null ) {
            return value;
        }
        return 0L;
    }

    /**
     * put a document set into the hazelcast word count map system
     * @param organisation_id the organisation of all the words
     * @param document the document to "count"
     */
    public void addDocument(UUID organisation_id, String url, List<Sentence> document ) {

        Map<String,Long> documentCountMap = processDocument(document);

        if ( documentCountMap != null && documentCountMap.size() > 0 ) {
            // use hazelcast for the global shared word counts
            documentCountMap.put( WC_DOCUMENT_COUNT, 1L);
            updateGlobalWordCount(organisation_id, documentCountMap);
        }
    }

    /**
     * remove an existing document set into the hazelcast word count map system
     * @param organisation_id the organisation of all the words
     * @param document the document to "count"
     */
    public void removeDocument(UUID organisation_id, String url, List<Sentence> document ) {

        Map<String,Long> documentCountMap = processDocument(document);

        if ( documentCountMap != null && documentCountMap.size() > 0 ) {
            // use hazelcast for the global shared word counts
            documentCountMap.put( WC_DOCUMENT_COUNT, 1L);
            removeGlobalWordCount(organisation_id, documentCountMap);
        }
    }

    /**
     * put a document set into the hazelcast word count map system
     * @param organisation_id the organisation of all the words
     * @param documentCountMap document words -> count map
     */
    private void updateGlobalWordCount( UUID organisation_id, Map<String, Long> documentCountMap) {
        IMap<String, Long> wordCountMap = hazelcast.getWordCountMap(organisation_id, Document.META_BODY);
        for ( String mapKey : documentCountMap.keySet() ) {
            long value = documentCountMap.get(mapKey);
            wordCountMap.lock(mapKey);
            try {
                Long value2 = wordCountMap.get(mapKey);
                if (value2 == null) {
                    value2 = value;
                } else {
                    value2 = value2 + value;
                }
                wordCountMap.set(mapKey, value2);
            } finally {
                wordCountMap.unlock(mapKey);
            }
        }
    }

    /**
     * remove a document set into the hazelcast word count map system
     * @param organisation_id the organisation of all the words
     * @param documentCountMap document words -> count map
     */
    private void removeGlobalWordCount( UUID organisation_id, Map<String, Long> documentCountMap) {
        IMap<String, Long> wordCountMap = hazelcast.getWordCountMap(organisation_id, Document.META_BODY);
        for ( String mapKey : documentCountMap.keySet() ) {
            long value = documentCountMap.get(mapKey);
            wordCountMap.lock(mapKey);
            try {
                Long value2 = wordCountMap.get(mapKey);
                if (value2 != null) {
                    value2 = value2 - value;
                    if ( value2 < 0 ) {
                        value2 = 0L;
                    }
                    wordCountMap.put(mapKey, value2);
                }
            } finally {
                wordCountMap.unlock(mapKey);
            }
        }
    }


    /**
     * return true if this word is to be used, cut out stop words, allow all proper nouns
     * and grammar rules to get through - and any other word that is in our lexicon
     * @param token the token to check
     * @return true if its good
     */
    private boolean isValidWord( Token token ) {
        return token != null && token.getText() != null &&
                ( (token.getPennType() == PennType.NNP || token.getPennType() == PennType.NNPS) ||
                   !undesirables.isUndesirable(token.getText()) &&
                   ( lexicon.getByName(token.getText()) != null || token.getGrammarRuleName() != null ) );
    }



    /**
     * create a word-set count for the given document
     * @param sentenceList a list of sentences of the document
     * @return the map of words of the document with frequencies
     */
    public Map<String, Long> processDocument(List<Sentence> sentenceList ) {

        Map<String, Long> documentWordCount = new HashMap<>();

        if (sentenceList != null) {

            long totalCount = 0L;
            long totalCountValid = 0L;

            for (Sentence sentence : sentenceList) {

                for (Token token : sentence.getTokenList()) {
                    totalCount = totalCount + 1L;

                    if ( isValidWord(token) ) {
                        totalCountValid = totalCountValid + 1L;

                        // the word and its syntactic category
                        String pennStr = token.getPennType().toString().toLowerCase();
                        String wordStr = token.getText().toLowerCase() + ":" + pennStr;
                        incrementWordCount( wordStr, documentWordCount );

                        // any of our known grammar parser types?
                        if ( token.getGrammarRuleName() != null ) {

                            String grammar = token.getGrammarRuleName();
                            if ( grammar.startsWith("date.") ) {
                                incrementWordCount(WC_DATE, documentWordCount);
                            } else if ( grammar.startsWith("time.") ) {
                                incrementWordCount(WC_TIME, documentWordCount);
                            } else if ( grammar.startsWith("url.") ) {
                                incrementWordCount(WC_URL, documentWordCount);
                            } else if ( grammar.startsWith("decimal.") ) {
                                incrementWordCount(WC_DECIMAL, documentWordCount);
                            } else if ( grammar.startsWith("percent.") ) {
                                incrementWordCount(WC_PERCENTAGE, documentWordCount);
                            } else if ( grammar.startsWith("money.") ) {
                                incrementWordCount(WC_MONEY, documentWordCount);
                            } else if ( grammar.startsWith("email.") ) {
                                incrementWordCount(WC_EMAIL, documentWordCount);
                            } else if ( grammar.startsWith("phone.") ) {
                                incrementWordCount(WC_PHONE, documentWordCount);
                            }

                        } else {

                            // also do general categories for the word
                            switch (token.getPennType()) {
                                case NN:
                                case NNS: {
                                    incrementWordCount(WC_NOUN, documentWordCount);
                                    break;
                                }
                                case NNP:
                                case NNPS: {
                                    incrementWordCount(WC_PROPER_NOUN, documentWordCount);
                                    break;
                                }
                                case VB:
                                case VBD:
                                case VBG:
                                case VBN:
                                case VBP:
                                case VBZ: {
                                    incrementWordCount(WC_VERB, documentWordCount);
                                    break;
                                }
                                case JJ:
                                case JJR:
                                case JJS: {
                                    incrementWordCount(WC_ADJECTIVE, documentWordCount);
                                    break;
                                }
                                case RB:
                                case RBR:
                                case RBS: {
                                    incrementWordCount(WC_ADVERB, documentWordCount);
                                    break;
                                }
                                case CD: {
                                    incrementWordCount(WC_NUMBER, documentWordCount);
                                    break;
                                }
                            }

                        } // else if no grammar rule

                    }
                }
            }

            // total number of words, and total number of "non-noise" words
            documentWordCount.put(WC_TOTAL_ALL, totalCount);
            documentWordCount.put(WC_TOTAL_VALID, totalCountValid);
            documentWordCount.put(WC_SENTENCE_COUNT, (long)sentenceList.size());

        } // if sentence list != null

        return documentWordCount;
    }

    /**
     * increment the value of the word count
     * @param wordStr the word to do it for
     * @param documentWordCount the map to store them in
     */
    private void incrementWordCount( String wordStr, Map<String, Long> documentWordCount ) {
        Long value = documentWordCount.get(wordStr);
        if (value == null) {
            value = 1L;
        } else {
            value = value + 1L;
        }
        documentWordCount.put(wordStr, value);
    }



}


