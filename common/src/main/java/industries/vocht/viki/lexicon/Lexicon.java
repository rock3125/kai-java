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

import com.fasterxml.jackson.databind.ObjectMapper;
import industries.vocht.viki.IDao;
import industries.vocht.viki.dao.PennType;
import industries.vocht.viki.model.Entity;
import industries.vocht.viki.model.Organisation;
import industries.vocht.viki.model.Token;
import industries.vocht.viki.model.knowledge_base.KBEntry;
import industries.vocht.viki.utility.StringUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidParameterException;
import java.util.*;

/*
 * Created by peter on 5/03/14.
 *
 * the system's NL lexicon
 *
 */
@Component
public class Lexicon {

    private final Logger logger = LoggerFactory.getLogger(Lexicon.class);

    // these services require the presence of the lexicon
    @Value("${sl.document.comparison.activate:true}")
    private boolean slDocumentComparisonActive;
    @Value("${sl.index.activate:true}")
    private boolean slIndexActive;
    @Value("${sl.knowledge.activate:true}")
    private boolean slKnowledgeActive;
    @Value("${sl.nnet.activate:true}")
    private boolean slNNetActive;
    @Value("${sl.parser.activate:true}")
    private boolean slParserActive;
    @Value("${sl.rule.activate:true}")
    private boolean slRuleActive;
    @Value("${sl.search.activate:true}")
    private boolean slSearchActive;
    @Value("${sl.stats.activate:true}")
    private boolean slStatsActive;
    @Value("${sl.vectorize.activate:true}")
    private boolean slVectorizeActive;

    // combination of service layers active, set in init()
    private boolean loadLexicon;


    // expansion sets fast access, contains both homonym and polysemous expansions keyed by text or semanticText
    private HashMap<String, List<String>> synonymSet;
    private HashMap<String, List<String>> associatedSet;

    private List<Token> lexicon; // the entire lexicon
    private HashMap<String, List<Token>> lexiconLookupByLCaseName; // the thing lookup by lower case string

    private HashMap<String, String> unStemSet; // remove the stemming from a word to get its basic version
    private HashMap<String, List<String>> stemmingSet; // get all stem related items

    // semantic tags for locations
    private HashSet<String> locationSemantics;

    // semantic tags for people
    private HashSet<String> peopleSemantics;

    // private object mapper to speed things up
    private ObjectMapper mapper;

    @Autowired
    private IDao dao;

    @Autowired
    private Undesirables undesirables;

    @Value("${lexicon.synonym.max.list.size:10}")
    private int maxSynonymListSize;

    // folder / path to the lexicon files
    @Value("${lexicon.base.directory:/opt/kai/data/lexicon}")
    private String lexiconBaseDirectory;

    public Lexicon() {
        this.lexicon = new ArrayList<>();
        this.lexiconLookupByLCaseName = new HashMap<>();
        this.synonymSet = new HashMap<>();
        this.associatedSet = new HashMap<>();
        this.unStemSet = new HashMap<>();
        this.stemmingSet = new HashMap<>();
        this.mapper = new ObjectMapper();
    }

    // return an analysis of the tag counts for all items in the lexicon
    public String analyseTags() {
        HashMap<String, Integer> tagCount = new HashMap<String, Integer>();
        for ( Token item : lexicon ) {
            PennType type = item.getPennType();
            String typeStr = type.toString();
            if ( tagCount.containsKey(typeStr) ) {
                tagCount.put( typeStr, tagCount.get(typeStr) + 1 );
            } else {
                tagCount.put( typeStr, 1 );
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append( "Lexicon tag count ").append(tagCount.size()).append(", total lexicon size ").append(lexicon.size()).append("\n");

        List<String> orderedKeyList = new ArrayList<String>();
        for ( String key : tagCount.keySet() ) orderedKeyList.add( key );
        Collections.sort(orderedKeyList);

        for ( String tagStr : orderedKeyList ) {
            sb.append(tagStr).append(" = ").append(tagCount.get(tagStr) ).append("\n");
        }
        return sb.toString();
    }

    // access a lexicon item by name (case insensitive)
    public List<Token> getByName( String wordStr ) {
        if ( wordStr != null ) {
            String key = wordStr.toLowerCase().trim();
            return lexiconLookupByLCaseName.get(key);
        }
        return null;
    }

    // return the stem version of a word - or the word itself if it has no stemming
    public String getStem( String word ) {
        if ( unStemSet != null && word != null ) {
            String unStemmed = unStemSet.get(word.trim().toLowerCase());
            if ( unStemmed != null )
                return unStemmed;
        }
        return word;
    }

    // access the entire lexicon
    public List<Token> getLexicon() {
        return lexicon;
    }

    // load the lexicon into the system
    public void init() throws IOException {
        loadLexicon = slDocumentComparisonActive || slIndexActive || slKnowledgeActive || slNNetActive || slParserActive ||
                slRuleActive || slSearchActive || slStatsActive || slVectorizeActive;
        if ( loadLexicon ) {
            if (!lexiconBaseDirectory.endsWith("/")) {
                lexiconBaseDirectory = lexiconBaseDirectory + "/";
            }

            // what thing's semantics are locations?
            locationSemantics = new HashSet<>();
            locationSemantics.add("location");
            locationSemantics.add("city");
            locationSemantics.add("state");
            locationSemantics.add("country");

            // what thing's semantics are people?
            peopleSemantics = new HashSet<>();
            peopleSemantics.add("person");
            peopleSemantics.add("man");
            peopleSemantics.add("woman");

            // load the various resources at our disposal to create a lexicon
            // wordnet created files
            loadSetWithTags(lexiconBaseDirectory + "adjectives.txt");
            loadSetWithTags(lexiconBaseDirectory + "adverbs.txt");

            loadNounSet(lexiconBaseDirectory + "nouns.txt");

            // load simple text files
            loadSetWithTags(lexiconBaseDirectory + "prepositions.txt");
            loadSetWithTags(lexiconBaseDirectory + "pronouns.txt");

            // auxiliary verbs
            loadSetWithTags(lexiconBaseDirectory + "auxiliaries.txt");
            loadSetWithTags(lexiconBaseDirectory + "determiner.txt");
            loadSetWithTags(lexiconBaseDirectory + "modals.txt");

            // setup the plurals for all nouns and setup semantics
            loadPlurals(lexiconBaseDirectory + "plurals.txt");

            // setup the verb conjugations for all verbs
            loadVerbs(lexiconBaseDirectory + "verbs.txt");

            // add other noise words such as brackets and punctuation
            addPunctuation();

            // add interjections
            loadSetWithTags(lexiconBaseDirectory + "interjections.txt");

            // add conjunctions
            loadSetWithTags(lexiconBaseDirectory + "conjunctions.txt");

            // special words like to
            add("to", PennType.TO);
            add("not", PennType.RB);
            add("there", PennType.EX);

            // setup the word lookup
            lexiconLookupByLCaseName = new HashMap<>((lexicon.size() * 3) / 2);
            for (Token lexiconItem : lexicon) {
                String key = lexiconItem.getText().toLowerCase();
                if (lexiconLookupByLCaseName.containsKey(key)) {
                    lexiconLookupByLCaseName.get(key).add(lexiconItem);
                } else {
                    List<Token> lexiconItemList = new ArrayList<Token>();
                    lexiconItemList.add(lexiconItem);
                    lexiconLookupByLCaseName.put(key, lexiconItemList);
                }
            }

            // setup the reverse stemming (setup the unStemSet)
            setupUnStemSet();

            // wipe any items that have important items like Conjunctives or Auxiliary verbs, or articles in them
            cleanupImportantGrammarItems();

            // setup the synonyms
            loadSynonyms();

            // load the word associations
            loadWordAssociations();

            // load the semantic entities into the lexicon
            loadSemanticEntities();

            logger.info("lexicon: " + new StringUtility().prettyCommaPrint(lexicon.size()) + " items");
        } else {
            logger.warn("lexicon not loaded");
        }
    }

    /**
     * load the semantic entities for all organisations and add them to the lexicon
     */
    public void loadSemanticEntities() throws IOException {
        if ( loadLexicon ) {
            logger.info("adding semantic entities to lexicon");
            int count = 0;
            for (Organisation organisation : dao.getOrganisationDao().getOrganisationList()) {
                UUID prev = null;
                int pageSize = 1000;
                List<KBEntry> entityList;
                do {
                    entityList = dao.getKBDao().getEntityList(organisation.getId(), "entity", prev, pageSize);
                    if (entityList != null && entityList.size() > 0) {
                        addSemanticEntities(entityList, false);
                        prev = entityList.get(entityList.size() - 1).getId();
                        count = count + entityList.size();
                    }
                } while (entityList != null && entityList.size() == pageSize);
                logger.debug("lexicon: " + new StringUtility().prettyCommaPrint(lexicon.size()) + " items");
            }
        } else {
            logger.warn("not adding semantic entities - lexicon not loaded");
        }
    }

    /**
     * incorporate a list of semantic entities into the lexicon
     * marks up existing items, or adds new items if they dne.
     * @param entityList the list of entities to incorporate
     */
    public void addSemanticEntities( List<KBEntry> entityList, boolean displayInfo ) throws IOException {
        if ( loadLexicon ) {
            if (entityList != null) {
                for (KBEntry entity : entityList) {
                    addSemanticEntity(entity);
                }
                if (displayInfo) {
                    logger.info("lexicon: " + new StringUtility().prettyCommaPrint(lexicon.size()) + " items");
                }
            }
        }
    }

    /**
     * incorporate a semantic entity into the lexicon
     * marks up existing items, or adds new items if they dne.
     * @param entity the entity to add
     */
    public void addSemanticEntity(KBEntry entity ) throws IOException {
        if ( entity != null && loadLexicon ) {
            Entity entity1 = mapper.readValue(entity.getJson_data(), Entity.class);
            addSemanticEntity( entity1.getName(), entity1.getIsa(), PennType.NNP );
            if ( entity1.getAlias_list() != null ) {
                for ( String alias : entity1.getAlias_list() ) {
                    addSemanticEntity( alias, entity1.getIsa(), PennType.NNP );
                }
            }
        }
    }

    /**
     * remove a semantic entity from the lexicon
     * @param entity the entity to remove
     */
    public void removeSemanticEntity(KBEntry entity ) throws IOException {
        if ( entity != null && loadLexicon ) {
            Entity entity1 = mapper.readValue(entity.getJson_data(), Entity.class);
            removeSemanticEntity( entity1.getName(), entity1.getIsa(), PennType.NNP );
            if ( entity1.getAlias_list() != null ) {
                for ( String alias : entity1.getAlias_list() ) {
                    removeSemanticEntity( alias, entity1.getIsa(), PennType.NNP );
                }
            }
            logger.debug("lexicon: " + new StringUtility().prettyCommaPrint(lexicon.size()) + " items");
        }
    }

    /**
     * helper for the above addSemanticEntities - do the actual adding if no such token exists
     * leave existing tokens alone, add semantics to matching penn-type tokens
     * @param wordStr the word to look for
     * @param semantic the semantic to set
     * @param pennType the penn-type to set
     */
    private void addSemanticEntity( String wordStr, String semantic, PennType pennType ) {
        if ( wordStr != null && semantic != null && pennType != null ) {
            List<Token> tokenList = getByName(wordStr);
            if ( tokenList == null ) { // add a new entry for it?
                tokenList = new ArrayList<>();
                lexiconLookupByLCaseName.put(wordStr.toLowerCase().trim(), tokenList);
            }

            // see if it already exists
            Token existingToken = null;
            Token rightPennTag = null;
            int numSemanticTags = 0;
            for ( Token token : tokenList ) {
                if ( token.getPennType() == pennType ) {
                    rightPennTag = token;
                    if ( token.getSemantic() != null ) {
                        numSemanticTags = numSemanticTags + 1;
                        if ( token.getSemantic().compareToIgnoreCase(semantic) == 0 ) {
                            existingToken = token;
                        }
                    }
                }
            }

            // we did not find an exact match?
            if ( existingToken == null ) {
                // did we find a penn-tag match?
                // if we did, and no semantics were set on it - appropriate it
                if ( numSemanticTags == 0 && rightPennTag != null ) {
                    rightPennTag.setSemantic(semantic);
                } else {
                    // we need to add a new token - we have either not the right tag
                    // or we already have a semantic with that tag
                    Token t1 = new Token(wordStr, pennType, semantic);
                    tokenList.add( t1 );
                    lexicon.add( t1 );
                }
            }

        }
    }

    /**
     * helper for the above removeSemanticEntity
     * @param wordStr the word to look for
     * @param semantic the semantic to set
     * @param pennType the penn-type to set
     */
    private void removeSemanticEntity( String wordStr, String semantic, PennType pennType ) {
        if ( wordStr != null && semantic != null && pennType != null ) {
            List<Token> tokenList = getByName(wordStr);
            if ( tokenList != null ) { // add a new entry for it?
                Token existingToken = null;
                for (Token token : tokenList) {
                    if (token.getPennType() == pennType) {
                        if (token.getSemantic() != null && token.getSemantic().compareToIgnoreCase(semantic) == 0) {
                            existingToken = token;
                        }
                    }
                }
                // we did not find an exact match?
                if ( existingToken != null ) {
                    existingToken.setSemantic(null); // nuke its semantic
                }
            }
        }
    }

    private void setupUnStemSet() throws IOException {
        List<String> stemList = Files.readAllLines(Paths.get(lexiconBaseDirectory + "stemming.txt"));
        if ( stemList != null ) {
            for ( String line : stemList ) {
                if ( !line.startsWith("#") ) {
                    String[] items = line.split(",");
                    if ( items.length > 1 ) {
                        String stem = items[0].trim();
                        List<String> stemRelations = new ArrayList<String>();
                        for ( int i = 1; i < items.length; i++ ) {
                            String unStemmed = items[i].trim();
                            unStemSet.put( unStemmed, stem ); // resolve unStemmed -> stem
                            stemRelations.add( unStemmed );
                        }
                        stemmingSet.put( stem, stemRelations );
                    }
                }
            }
        }
    }

    // setup a synonym i1 -> i2
    public void addSynonym( Token item1, Token item2 ) {
        if ( synonymSet != null && item1 != null && item2 != null && loadLexicon ) {
            String text1 = item1.getText().toLowerCase();
            String text2 = item2.getText().toLowerCase();
            if ( text1.compareTo(text2) != 0 ) {
                if ( undesirables == null || (!undesirables.isUndesirable(text1) && !undesirables.isUndesirable(text2))) {
                    if (synonymSet.containsKey(text1)) {
                        List<String> stringList = synonymSet.get(text1);
                        if (!stringList.contains(text2) && stringList.size() < maxSynonymListSize ) {
                            stringList.add(text2);
                        }
                    } else {
                        List<String> synonymList = new ArrayList<>();
                        synonymList.add(text2);
                        synonymSet.put(text1, synonymList);
                    }
                }
            }
        }
    }

    // setup a association i1 -> i2
    public void addAssociation( Token item1, Token item2 ) {
        if ( associatedSet != null && item1 != null && item2 != null && loadLexicon ) {
            String text1 = item1.getText().toLowerCase();
            String text2 = item2.getText().toLowerCase();
            if ( text1.compareTo(text2) != 0 ) {
                if ( undesirables == null || (!undesirables.isUndesirable(text1) && !undesirables.isUndesirable(text2) ) ) {
                    if ( associatedSet.containsKey(text1) ) {
                        if (!containsCaseInsensitive(associatedSet.get(text1),text2))
                            associatedSet.get(text1).add(item2.getText());
                    } else {
                        List<String> associationList = new ArrayList<>();
                        associationList.add(item2.getText());
                        associatedSet.put(text1, associationList);
                    }
                }
            }
        }
    }

    // case insensitive contains
    private boolean containsCaseInsensitive( List<String> list, String item ) {
        if ( list != null && item != null ) {
            for ( String listItem : list ) {
                if ( item.compareToIgnoreCase(listItem) == 0 )
                    return true;
            }
            return false;
        }
        throw new InvalidParameterException("invalid parameter, items cannot be null");
    }

    // get the word part of a word:tag
    private String getWord( String str ) {
        if ( str != null && str.contains(":") ) {
            return str.split(":")[0];
        }
        return str;
    }

    // get the tag part of a word:tag
    private PennType getTag( String str ) throws IOException {
        if ( str != null && str.contains(":") ) {
            return PennType.fromString(str.split(":")[1]);
        }
        throw new IOException("getTag() must contain : (" + str + ")");
    }

    // load a word-net based set
    private void loadSetWithTags( String streamName ) throws IOException {
        // load a generic wn31 set from the resource
        List<String> lineList = Files.readAllLines(Paths.get( streamName ) );
        for ( String line : lineList ) {
            if ( !line.startsWith("#") && line.length() > 0 ) {
                String[] items = line.split(",");
                for ( String item : items )
                    add(getWord(item), getTag(item));
            }
        }
    }

    // load a word-net based set
    private void loadNounSet( String streamName ) throws IOException {
        // load a generic wn31 set from the resource
        List<String> lineList = Files.readAllLines(Paths.get( streamName ) );
        for ( String line : lineList ) {
            if ( line.length() > 0 && !line.startsWith("#") ) {
                // nn0, or np0?
                if (firstLetterCapital(line))
                    add(line, PennType.NNP);
                else
                    add(line, PennType.NN);
            }
        }
    }

    // setup synonyms between existing words
    private void loadSynonyms() throws IOException {
        List<String> lineList = Files.readAllLines(Paths.get( lexiconBaseDirectory + "synonyms-filtered.txt" ) );
        for ( String line : lineList ) {
            if ( line.contains(",") ) { // synonyms?
                String[] synonymArray = line.split(",");
                String synonym1 = synonymArray[0];
                List<Token> w1 = getByName(synonym1);
                if ( w1 != null && w1.size() > 0 )
                    for ( int i = 1; i < synonymArray.length; i++ ) {
                        List<Token> w2 = getByName(synonymArray[i]);
                        if ( w2 != null && w2.size() > 0 )
                            if (!synonym1.equals(synonymArray[i]))
                            {
                                addSynonym(w1.get(0), w2.get(0));
                                addSynonym(w2.get(0), w1.get(0));
                            }
                    }
            }
        }
    }

    // setup synonyms between existing words
    private void loadWordAssociations() throws IOException {
        List<String> lineList = Files.readAllLines(Paths.get( lexiconBaseDirectory + "wordassociations.txt" ) );
        for ( String line : lineList ) {
            if ( line.contains(",") ) { // associations?
                String[] associationArray = line.split(",");
                String association1 = associationArray[0];
                List<Token> w1 = getByName(association1);
                if ( w1 != null && w1.size() > 0 )
                    for ( int i = 1; i < associationArray.length; i++ ) {
                        List<Token> w2 = getByName(associationArray[i]);
                        if ( w2 != null && w2.size() > 0 )
                            if (!association1.equals(associationArray[i]))
                                addAssociation(w1.get(0), w2.get(0));
                    }
            }
        }
    }

    // return true if the first letter is a capital A..Z
    private boolean firstLetterCapital( String str ) {
        if ( str != null && str.length() > 0 ) {
            char ch = str.charAt(0);
            return (ch >= 'A' && ch <= 'Z');
        }
        return false;
    }

    // setup the plurals for all nouns
    private void loadPlurals( String streamName ) throws IOException {
        // load the semantics for the nouns
        File folder = new File(lexiconBaseDirectory + "semantics/");
        Map<String, String> word2semantic = new HashMap<>();
        File[] listOfFiles = folder.listFiles();
        if ( listOfFiles != null ) {
            for ( File file : listOfFiles ) {
                String file_name = file.getAbsolutePath();
                if ( file_name.endsWith(".txt") ) {
                    List<String> s_list = Files.readAllLines(Paths.get(file_name));
                    for (String semantic : s_list) {
                        String[] set = semantic.split(":");
                        if (set.length == 2) {
                            word2semantic.put(set[0].replace('_', ' '), set[1]);
                        }
                    }
                }
            }
        }

        // load the plural set generated by specialist lexicon
        HashMap<String, List<String>> pluralLookup = new HashMap<String, List<String>>();
        List<String> lineList = Files.readAllLines(Paths.get( streamName ) );
        for ( String line : lineList ) {
            String[] items = line.split("\\|");
            if ( items.length == 2 ) {
                String key = items[0].toLowerCase();
                String[] set1 = key.split(":");
                String[] set2 = items[1].split(":");
                if ( set1.length == 2 && set2.length == 2 ) {
                    key = set1[0];
                    if (pluralLookup.containsKey(key))
                        pluralLookup.get(key).add(set2[0]);
                    else {
                        List<String> pluralList = new ArrayList<>();
                        pluralList.add(set2[0]);
                        pluralLookup.put(key, pluralList);
                    }
                }
            }
        }

        // go through the lexicon and add plurals to nouns that have them
        List<Token> newPluralList = new ArrayList<Token>();
        for ( Token item : lexicon ) {
            if ( word2semantic.containsKey(item.getText()) ) {
                item.setSemantic(word2semantic.get(item.getText()));
            }
            if ( item.getPennType() == PennType.NN && pluralLookup.containsKey( item.getText().toLowerCase() ) ) {
                item.setPennType( PennType.NN );
                List<String> pluralList = pluralLookup.get( item.getText().toLowerCase() );
                for ( String plural : pluralList ) {
                    if ( plural.compareToIgnoreCase( item.getText() ) != 0 ) {
                        // add the new plural
                        Token pluralItem = new Token(plural, PennType.NNS, item.getSemantic() );
                        newPluralList.add(pluralItem);
                    }
                }
            }
        }

        // finally add all the plurals to the database
        for ( Token newPlural : newPluralList ) {
            lexicon.add( newPlural );
        }
    }

    // get / load the plurals for all nouns, map<singular,plural>
    public Map<String, String> loadPlurals() throws IOException {
        HashMap<String, String> pluralLookup = new HashMap<>();
        if ( loadLexicon ) {
            // load the plural set generated by specialist lexicon
            List<String> lineList = Files.readAllLines(Paths.get(lexiconBaseDirectory + "plurals.txt"));
            for (String line : lineList) {
                String[] items = line.split("\\|");
                if (items.length == 2) {
                    String key = items[0].toLowerCase();
                    String[] set1 = key.split(":");
                    String[] set2 = items[1].split(":");
                    if (set1.length == 2 && set2.length == 2) {
                        pluralLookup.put(set1[0], set2[0]);
                    }
                }
            }
        }
        return pluralLookup;
    }

    // setup all the verbs
    private void loadVerbs(String streamName) throws IOException {
        // load the plural set generated by specialist lexicon
        List<String> lineList = Files.readAllLines(Paths.get( streamName ) );
        for ( String line : lineList ) {
            String[] items = line.split("\\|");
            if ( items.length >= 6 ) {
                for ( String item : items ) {
                    Token lexiconItem = new Token(getWord(item), getTag(item));
                    lexicon.add( lexiconItem );
                }
            }
        }
    }

    // setup all the verbs
    public Map<String, List<Token>> loadVerbs() throws IOException {
        // load the plural set generated by specialist lexicon
        Map<String, List<Token>> verbMap = new HashMap<>();
        if ( loadLexicon ) {
            List<String> lineList = Files.readAllLines(Paths.get(lexiconBaseDirectory + "verbs.txt"));
            for (String line : lineList) {
                String[] items = line.split("\\|");
                List<Token> verbList = new ArrayList<>();
                Token vb = null;
                if (items.length >= 6) {
                    for (String item : items) {
                        Token t = new Token(getWord(item), getTag(item));
                        if (t.getPennType() == PennType.VB) {
                            vb = t;
                        }
                        verbList.add(t);
                    }
                }
                if (vb != null) {
                    verbMap.put(vb.getText(), verbList);
                }
            }
        }
        return verbMap;
    }

    // add lots of little in between the language parts
    private void addPunctuation() {
        final String lBrackets = "([{<";
        for ( int i = 0; i < lBrackets.length(); i++ ) add( lBrackets.substring(i,i + 1), PennType.LRB );

        final String rBrackets = ")]}>";
        for ( int i = 0; i < rBrackets.length(); i++ ) add( rBrackets.substring(i,i + 1), PennType.RRB );

        final String punctuation = ".,!;:";
        for ( int i = 0; i < punctuation.length(); i++ ) add( punctuation.substring(i,i + 1), PennType.PUN);
        add( "?", PennType.PUN);

        final String quotes = "'`\"";
        for ( int i = 0; i < quotes.length(); i++ ) add( quotes.substring(i,i + 1), PennType.SQT );
    }

    // remove any ambiguity from the important tags
    // tag importance in this order:
    // Article, Auxiliary, Not, Pronoun, Preposition
    private void cleanupImportantGrammarItems() throws IOException {
        List<String> tagLines = Files.readAllLines(Paths.get(lexiconBaseDirectory + "prime-tags.txt"));
        HashSet<PennType> typeSet = new HashSet<>();
        for ( String tagLine : tagLines )
            if ( !tagLine.startsWith("#") && tagLine.length() == 3 )
                typeSet.add(PennType.fromString(tagLine));

        for ( String key : lexiconLookupByLCaseName.keySet() ) {
            List<Token> itemList = lexiconLookupByLCaseName.get(key);
            if ( itemList.size() > 1 ) {
                for ( Token item : itemList )
                    if ( typeSet.contains(item.getType()) )
                        lexiconLookupByLCaseName.put(key, filter(itemList, item.getPennType()));

            } // if item ambiguous

        } // for each item in the lexicon

    }

    // filter out an item by type
    private List<Token> filter( List<Token> itemList, PennType type ) {
        List<Token> newList = new ArrayList<Token>();
        for ( Token item : itemList )
            if ( item.getType().equals(type) )
                newList.add( item );
        return newList;
    }

    // add a new word to the lexicon and return it
    private Token add( String word, PennType type ) {
        Token item = new Token( word, type );
        lexicon.add( item );
        return item;
    }

    // add a new word to the lexicon and return it
    private Token add( String word, PennType type, String semantic ) {
        Token item = new Token( word, type, semantic );
        lexicon.add( item );
        return item;
    }

    // add a new word to the lexicon and return it - used by unit testing
    public Token addAndIndex( String word, PennType type ) {
        if ( word != null && type != null && loadLexicon ) {
            Token item = new Token( word, type );

            lexicon.add( item );

            String key = word.toLowerCase();
            if ( lexiconLookupByLCaseName.containsKey( key ) )
                lexiconLookupByLCaseName.get(key).add( item );
            else {
                List<Token> list = new ArrayList<Token>();
                list.add(item);
                lexiconLookupByLCaseName.put( key, list );
            }

            return item;
        }
        return null;
    }

    // get the synonyms for this word
    public List<String> getSynonymList(String homonymStr) {
        if ( homonymStr != null && loadLexicon )
            return synonymSet.get( homonymStr.toLowerCase() );
        return null;
    }

    // get the associations for this word
    public List<String> getAssociationList(String homonymStr) {
        if ( homonymStr != null && loadLexicon )
            return associatedSet.get( homonymStr.toLowerCase() );
        return null;
    }

    // get the stemmed relations
    public List<String> getStemmedList(String homonymStr) {
        if ( homonymStr != null && loadLexicon )
            return stemmingSet.get( getStem( homonymStr) );
        return null;
    }

    /**
     * is the semantic for an item a location?
     * @param semanticStr the string to check
     * @return true if semantic string is a known location semantic type
     */
    public boolean isLocationSemantic(String semanticStr ) {
        return semanticStr != null && locationSemantics != null && locationSemantics.contains(semanticStr.toLowerCase());
    }

    /**
     * is the semantic for an item a person?
     * @param semanticStr the string to check
     * @return true if semantic string is a known person semantic type
     */
    public boolean isPersonSemantic(String semanticStr ) {
        return semanticStr != null && peopleSemantics != null && peopleSemantics.contains(semanticStr.toLowerCase());
    }

}

