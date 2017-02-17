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

import industries.vocht.viki.client.PythonClientInterface;
import industries.vocht.viki.grammar.GrammarLibrary;
import industries.vocht.viki.lexicon.Lexicon;
import industries.vocht.viki.model.*;
import industries.vocht.viki.model.semantics.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.*;

/**
 * Created by peter on 6/06/15.
 *
 * A sophisticated NLP and NLU packages with multi-threading
 * using max-entropy for pos tagging, and chunking for NLU tuple creation
 *
 */
@Component
public class NLParser {

    private final static Logger logger = LoggerFactory.getLogger(NLParser.class);

    // these service layers require the nl parser libraries to be loaded on this node
    @Value("${sl.document.comparison.activate:true}")
    private boolean slDocumentComparisonActive;
    @Value("${sl.knowledge.activate:true}")
    private boolean slKnowledgeActive;
    @Value("${sl.nnet.activate:true}")
    private boolean slNNetActive;
    @Value("${sl.parser.activate:true}")
    private boolean slParserActive;
    @Value("${sl.search.activate:true}")
    private boolean slSearchActive;

    // maximum number of words that can make up one "concept"
    @Value("${parser.max.constituent.length:5}")
    private int maxWordConstituentLength;

    @Value("${parser.spacy.server.port:9000}")
    private int spacyParserPort;

    @Value("${parser.spacy.csv.list:localhost}")
    private String spacyParserAddressList;
    private List<PythonClientInterface> pythonClientInterfaceList;
    private int spacyRoundRobit = 0;

    // the lexicon lookup system
    @Autowired
    private Lexicon lexicon;

    // the grammar library parsing system
    @Autowired
    private GrammarLibrary grammarLibrary;

    // how many "bytes" of text / characters to allow per complete document - ignore documents over this size (5MB default)
    @Value("${lexicon.nlparser.workload.max.document.size:5120000}")
    private int workloadMaxDocumentSize;

    // spacy converter system
    private SpacyToTuple spacyToTuple;


    public NLParser() {
    }

    /**
     * initialise the NLParser components (openNLP based)
     */
    public void init() throws IOException, InterruptedException {
        // setup tcp ip comms instead
        logger.info("parser address csv list: " + spacyParserAddressList);
        pythonClientInterfaceList = new ArrayList<>();
        for ( String address : spacyParserAddressList.split(",") ) {
            PythonClientInterface clientIF = new PythonClientInterface(address, spacyParserPort);
            pythonClientInterfaceList.add(clientIF);

            // make sure this node is active - it has to be on startup
            boolean connected = false; // always wait for parsers - vital on servers
            do {
                try {
                    clientIF.parse("test text");
                    connected = true;
                } catch (IOException ex) {
                    String name = address + ":" + spacyParserPort;
                    logger.info("parser \"" + name + "\" not running, waiting five seconds before re-try");
                    Thread.sleep(5000);
                }
            } while ( !connected );
        }
        spacyToTuple = new SpacyToTuple(lexicon);
    }

    ////////////////////////////////////////////////////////////////////////////////////////
    // pos tagger thread control

    /**
     * the sentence and pos tag parser
     * @param documentText the text to be parsed
     * @return a list of sentences made from the text
     */
    public List<Sentence> parseText( String documentText ) throws IOException {
        if ( documentText != null ) {
            int size = documentText.length();
            if ( size > 0 && size < workloadMaxDocumentSize ) {

                // get next spacy parser round robin
                PythonClientInterface client = pythonClientInterfaceList.get(spacyRoundRobit);
                spacyRoundRobit = (spacyRoundRobit + 1) % pythonClientInterfaceList.size();

                String str = TokenizerConstants.clean_utf8(documentText);
                SpacyTokenList spDocument = client.parse(str);
                if ( spDocument != null ) {
                    List<Sentence> sentenceList = spacyToTuple.spaceyDocumentToSentenceList(spDocument);
                    // filter tuples
                    if ( sentenceList != null ) {
                        // apply grammar rules
                        sentenceList = grammarLibrary.parseSentenceList(sentenceList);
                        // filter out bad tuples
                        for (Sentence sentence: sentenceList) {
                            sentence.setTuple( filter(sentence.getTuple()) );
                        }
                        // put longest words together according to the lexicon
                        sentenceList = longestWords(sentenceList);
                    }
                    return sentenceList;
                } else {
                    logger.error("spacy client: returned null");
                }

            } else if ( size >= workloadMaxDocumentSize ) {
                logger.error("document text exceeds maximum allowed size " + size + ", allowed " + workloadMaxDocumentSize);
            }
        }
        return null;
    }

    ////////////////////////////////////////////////////////////////////////////////////////

    /**
     * the sentence and pos tag parser for multiple metadata items (optimization)
     * @param documentPackage the text to be parsed (a package)
     * @return a list of sentences made from the text
     */
    public Map<String, List<Sentence>> parsePackage( String documentPackage ) throws IOException {
        if ( documentPackage != null ) {
            int size = documentPackage.length();
            if ( size > 0 && size < workloadMaxDocumentSize ) {

                // get next spacy parser round robin
                PythonClientInterface client = pythonClientInterfaceList.get(spacyRoundRobit);
                spacyRoundRobit = (spacyRoundRobit + 1) % pythonClientInterfaceList.size();

                Map<String, List<Sentence>> resultPacket = new HashMap<>();
                String str = TokenizerConstants.clean_utf8(documentPackage);
                SpacyPacketList spDocument = client.parsePackage(str);
                if ( spDocument != null && spDocument.getPacketList() != null ) {
                    for (SpacyPacket packet : spDocument.getPacketList()) {
                        List<Sentence> sentenceList = spacyToTuple.spaceyDocumentToSentenceList(packet.getSpacyTokenList());
                        // filter tuples
                        if (sentenceList != null) {
                            // apply grammar rules
                            sentenceList = grammarLibrary.parseSentenceList(sentenceList);
                            // filter out bad tuples
                            for (Sentence sentence : sentenceList) {
                                sentence.setTuple(filter(sentence.getTuple()));
                            }
                            // put longest words together according to the lexicon
                            sentenceList = longestWords(sentenceList);
                            resultPacket.put(packet.getMetadata(), sentenceList);
                        }
                    }
                    return resultPacket;

                } else {
                    logger.error("spacy client: returned null");
                }

            } else if ( size >= workloadMaxDocumentSize ) {
                logger.error("document text exceeds maximum allowed size " + size + ", allowed " + workloadMaxDocumentSize);
            }
        }
        return null;
    }

    ////////////////////////////////////////////////////////////////////////////////////////

    /**
     * filter out tuples that aren't deemed of enough information value
     * @param tuple the tuple
     * @return a filtered list of tuples
     */
    private Tuple filter(Tuple tuple) {
        if ( tuple != null && hasEnoughInformation(tuple) ) {
            return tuple;
        }
        return null;
    }

    /*
     * check the info content of a tuple
     * @param tupleList list of tuples
     * @return true if it has information that needs to be indexed, this implies it has at least one verb
     *         and either at least one noun or an adjective
     */
    private boolean hasEnoughInformation(Tuple tuple) {
        if ( tuple != null ) {
            int numVerbs = 0;
            int numNounsJJ = 0;
            if ( tuple.getRoot() != null ) {
                numVerbs = tuple.getRoot().verbCount();
                numNounsJJ = tuple.getRoot().nounAndAdjectiveCount();
            }
            return numVerbs > 0 && numNounsJJ > 0;
        }
        return false;
    }


    /**
     * sticht lexicon words back together for the sentences after spacy
     * @param sentenceList the list of sentences to investigate
     * @return a list of improved sentences with improved concepts
     */
    private List<Sentence> longestWords(List<Sentence> sentenceList) {
        if ( sentenceList != null ) {
            List<Sentence> new_sentenceList = new ArrayList<>();
            for (Sentence sentence: sentenceList) {
                Sentence new_sentence = longestWords(sentence);
                new_sentence.setTuple(sentence.getTuple());
                new_sentenceList.add(new_sentence);
            }
            return new_sentenceList;
        }
        return null;
    }

    // get longest words for a single sentence
    private Sentence longestWords(Sentence sentence) {
        if ( sentence != null ) {
            return new Sentence(tokenListToWordList(sentence.getTokenList()));
        }
        return null;
    }

    // use the text parser to convert a list of tokens into a list of lexicon tokens
    private List<Token> tokenListToWordList( List<Token> tokenList ) {

        if ( tokenList != null ) {
            List<Token> newTokenList = new ArrayList<>();

            int i = 0;
            while ( i < tokenList.size() ) {
                String wordStr = tokenList.get(i).getText();
                if ( wordStr != null && wordStr.length() > 0 ) {
                    MatchingResult result = getLargestMatching( tokenList, i );
                    if ( result != null ) {
                        newTokenList.add(convert( result.itemList.get(0)));
                        i = result.newIndex;
                    } else { // not in the lexicon
                        newTokenList.add(tokenList.get(i));
                        i++;
                    }
                } else
                    i++;
            }
            if ( newTokenList.size() > 0 )
                return newTokenList;
        }
        return null;
    }

    // hold return values of the getLargestMatching system
    private class MatchingResult {
        int newIndex;
        List<Token> itemList;
    }

    // get the largest matching item from the lexicon
    private MatchingResult getLargestMatching(List<Token> textArray, int index ) {
        if ( lexicon != null && index < textArray.size() ) {
            int size = maxWordConstituentLength;
            if ( index + size > textArray.size() )
                size = textArray.size() - index;

            List<Token> resultList = null;
            String resultListString = null;
            int resultSize = 0;

            StringBuilder sb = new StringBuilder();
            for ( int i = 0; i < size; i++ ) {
                String wordStr = textArray.get(index + i).getText();
                if ( wordStr != null && wordStr.length() > 0 ) {
                    sb.append(wordStr);
                    String theString = sb.toString();
                    List<Token> tempList = lexicon.getByName(theString);
                    if ( tempList != null ) {
                        resultListString = theString;
                        resultList = tempList;
                        resultSize = i + 1;
                    }
                    sb.append(" ");
                }
            }

            // return if we have a matching item
            if ( resultSize > 1 && resultList.size() > 0 ) {
                resultList = filterByCase(resultList, resultListString, index == 0);
                MatchingResult result = new MatchingResult();
                result.itemList = resultList;
                result.newIndex = index + resultSize;
                return result;
            }

        }
        return null;
    }

    /**
     * given a word, and items in a list from the lexicon, filter out those items
     * (if possible) that do not conform enough to the case wanted
     * @param list list of lexicon entries
     * @param word the word examined (from the original text)
     * @return an adjusted, or the original list
     */
    private List<Token> filterByCase( List<Token> list, String word, boolean isStartOfSentence ) {
        if ( word != null && word.length() > 0 && list != null && list.size() > 1 ) {

            List<Token> returnList = new ArrayList<>();

            int checkSize = isStartOfSentence ? 1 : 0; // one difference allowed for words @ start of sentenc
            if ( word.length() == 1 ) {
                checkSize = 0;
            }

            // are any of the words of the right case?
            int rightCaseCount = 0;
            for ( Token item : list ) {
                if ( diffInCase(item.getText(), word) <= checkSize ) {
                    rightCaseCount++;
                    returnList.add(item);
                }
            }
            // none of the words, or all of the words match
            if ( rightCaseCount == 0 || rightCaseCount == list.size() ) {
                return list;
            } else {
                return returnList;
            }
        }
        return list;
    }

    /**
     * Return the number of differences in case for two identical words
     * @param str1 word 1
     * @param str2 word 2
     * @return the number of differences in case
     */
    private int diffInCase( String str1, String str2 ) {
        if ( str1 != null && str2 != null && str1.length() == str2.length() ) {
            int numDiff = 0;
            for ( int i = 0; i < str1.length(); i++ ) {
                if ( str1.charAt(i) != str2.charAt(i) )
                    numDiff++;
            }
            return numDiff;
        }
        return Integer.MAX_VALUE; // infinite
    }

    // convert a defined type to a Lexicon item type for the nl parsers
    // best effort - this should only be used if the lexicon doesn't have an entry
    // for the word presented
    private static Token convert( Token item ) {
        if ( item != null ) {
            switch (item.getPennType()) {
                case NP:
                case NNS:
                case NN: {
                    Token t = new Token(TokenizerConstants.Type.Text, item.getText() );
                    t.setSemantic( item.getSemantic() );
                    t.setSynid( item.getSynid() );
                    return t;
                }
                case CD: {
                    Token t = new Token(TokenizerConstants.Type.Number, item.getText() );
                    t.setPennType( item.getPennType() );
                    return t;
                }
                case PUN: {
                    if ( item.getText().equals("-") )
                        return new Token(TokenizerConstants.Type.Hyphen, item.getText() );
                    if ( item.getText().equals(".") )
                        return new Token(TokenizerConstants.Type.FullStop, item.getText() );
                    Token t = new Token(TokenizerConstants.Type.Special, item.getText() );
                    t.setPennType( item.getPennType() );
                    return t;
                }
                case LRB:
                case RRB: {
                    Token t = new Token(TokenizerConstants.Type.Punctuation, item.getText() );
                    t.setPennType( item.getPennType() );
                    return t;
                }
                case POS: {
                    Token t = new Token(TokenizerConstants.Type.Possessive, item.getText() );
                    t.setPennType( item.getPennType() );
                    return t;
                }
                default: {
                    Token t = new Token(TokenizerConstants.Type.Text, item.getText());
                    t.setSemantic( item.getSemantic() );
                    t.setPennType( item.getPennType() );
                    t.setSynid( item.getSynid() );
                    return t;
                }
            }
        }
        throw new InvalidParameterException("invalid parameter, not allowed null");
    }


}

