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

package industries.vocht.viki.lexicon;

import industries.vocht.viki.client.PythonClientInterface;
import industries.vocht.viki.parser.SpacyToTuple;
import industries.vocht.viki.relationship.RelatedWord;
import industries.vocht.viki.relationship.SynonymRelationshipProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created by peter on 23/05/16.
 *
 * a special lexicon that keeps track of ambiguous items (semantic synsets)
 *
 */
@Component
public class AmbiguousLexicon {

    private final static Logger logger = LoggerFactory.getLogger(AmbiguousLexicon.class);

    @Value("${svm.semantic.lexicon.filename:/opt/kai/data/lexicon/semantic-nouns.txt}")
    private String petersNounsFilename;

    @Autowired
    private SynonymRelationshipProvider synonymRelationshipProvider; // the synonym based (semantic) relationship provider

    @Value("${parser.spacy.server.port:9000}")
    private int spacyParserPort;

    @Value("${parser.spacy.csv.list:localhost:9000}")
    private String spacyParserAddressList;
    private List<PythonClientInterface> pythonClientInterfaceList;
    private int spacyRoundRobit = 0;

    // string (lowercase) -> list(synset)<relationships>
    private Map<String, List<LexiconSynset>> synsetLookup;

    // convert from a plural ambiguous item back down to a singular item
    private Map<String, String> pluralToSingular;

    public AmbiguousLexicon() {
    }

    /**
     * return the synset for word or null if dne
     * @param word the word to get an ambiguous synset for
     * @return the synset list or null if not ambiguous
     */
    public List<LexiconSynset> getSynset( String word ) {
        return synsetLookup.get(word.toLowerCase());
    }

    /**
     * @return a set of ambiguous nouns to look out for including applicable plurals
     */
    public HashSet<String> getWordFocusSet() {
        // setup what words to look for
        HashSet<String> focus = new HashSet<>();
        focus.addAll( synsetLookup.keySet() );
        return focus;
    }

    /**
     * load Peter's lexicon with synsets
     */
    public void init() throws IOException {
        pluralToSingular = new HashMap<>();
        synsetLookup = new HashMap<>();
        List<String> traingList = Files.readAllLines(Paths.get(petersNounsFilename));
        for ( String line : traingList ) {
            if ( line.length() > 0 && !line.startsWith("//") ) {
                String[] parts = line.split(",");

                String part = parts[0];
                String[] items = part.split("\\|");

                if ( items.length == 2 ) {
                    String word = items[0].trim();
                    String wordPlural = items[1].trim();
                    LexiconSynset synset = process(word, parts);
                    add( word, synset );
                    add( wordPlural, synset );
                    pluralToSingular.put( wordPlural, word );
                } else {
                    String word = items[0].trim();
                    LexiconSynset synset = process(word, parts);
                    add( word, synset );
                }
            }
        }
        // setup tcp ip comms instead
        logger.info("Spacy Parser/WSD addresses: " + spacyParserAddressList);
        pythonClientInterfaceList = new ArrayList<>();
        for ( String address : spacyParserAddressList.split(",") ) {
            pythonClientInterfaceList.add( new PythonClientInterface(address, spacyParserPort) );
        }
    }

    /**
     * find if there is a singular for wordStr, if there is, return it,
     * otherwise just return the word unmodified
     * @param wordStr the word to check
     * @return singular of the plural (or the word itself)
     */
    public String getSingular( String wordStr ) {
        String singular = pluralToSingular.get(wordStr);
        if ( singular != null ) {
            return singular;
        }
        return wordStr;
    }

    /**
     * helper - add a new synset for a word to the existing sets
     * @param word the word to add a synset for
     * @param synset the synset
     */
    private void add(String word, LexiconSynset synset ) {
        if ( synset != null ) {
            List<LexiconSynset> set = synsetLookup.get(word);
            if ( set == null ) {
                set = new ArrayList<>();
                synsetLookup.put(word, set);
            }
            synset.setSynsetId( set.size() );
            set.add( synset );
        }
    }

    /**
     * turn a line from Peter's lexicon into a set of relationships
     * @param parts the line split parts
     * @return a hashset with the relationships
     */
    private LexiconSynset process( String word, String[] parts ) {
        if ( parts.length > 1 ) {
            HashSet<String> newSet = new HashSet<>();
            for (int i = 1; i < parts.length; i++) {
                newSet.add(parts[i].trim());
            }
            return new LexiconSynset(word + " (" + parts[1] + ")", 0, newSet);
        }
        return null;
    }


}

