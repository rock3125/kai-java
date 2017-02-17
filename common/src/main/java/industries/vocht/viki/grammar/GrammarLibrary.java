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

import industries.vocht.viki.dao.PennType;
import industries.vocht.viki.model.Sentence;
import industries.vocht.viki.model.Token;
import industries.vocht.viki.model.TokenizerConstants;
import industries.vocht.viki.tokenizer.Tokenizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidParameterException;
import java.util.*;

/*
 * Created by peter on 20/12/14.
 *
 * grammar pattern parser system
 *
 */
@Component
public class GrammarLibrary {

    private HashMap<String, GrammarLhs> grammarMap; // the map for looking up items
    private HashMap<String, String> grammarConversionMap; // the map for looking up item's conversion patterns
    private HashMap<String, String> grammarModificationMap; // the map for looking up item's modification patterns
    private HashMap<String, List<GrammarLhs>> startPattern; // a start token map for matching possible rules

    @Value("${lexicon.grammar.rule.file:/opt/kai/data/grammar/grammar-rules.txt}")
    private String grammarRuleFile; // the file loaded for patterns

    // helper
    private Tokenizer tokenizer;

    public GrammarLibrary() {
        grammarMap = new HashMap<>();
        grammarConversionMap = new HashMap<>();
        grammarModificationMap = new HashMap<>();
        startPattern = new HashMap<>();
        tokenizer = new Tokenizer();
    }

    // find any grammar rules that match and apply them - return
    // new tokens based on the grammar rules that applied and that didn't
    public List<Sentence> parseSentenceList(List<Sentence> sentenceList ) {
        if ( sentenceList != null ) {
            List<Sentence> newSentenceList = new ArrayList<>();
            for (Sentence sentence : sentenceList) {
                newSentenceList.add(new Sentence(parse(sentence.getTokenList()), sentence.getTuple()));
            }
            return newSentenceList;
        }
        return null;
    }

    // find any grammar rules that match and apply them - return
    // new tokens based on the grammar rules that applied and that didn't
    public List<Token> parse(List<Token> tokenList ) {
        if (tokenList != null && tokenList.size() > 0) {
            List<Token> newTokenList = new ArrayList<>();

            // split spacy tokens back into basic components where possible
            List<Token> correctedTokenList = new ArrayList<>();
            for (Token token : tokenList) {
                List<Token> manyTokenList = tokenizer.retokenize(token);
                correctedTokenList.addAll(manyTokenList);
            }
            tokenList = correctedTokenList;

            int i = 0;
            while ( i < tokenList.size() ) {
                Token token = tokenList.get(i);

                // literal first - more specific
                Match result = null;
                List<GrammarLhs> ruleSet = getRulesByFirstLetter(token.getText());
                if ( ruleSet != null )
                    result = match( tokenList, i, ruleSet );

                if ( result == null ) {
                    if ( token.getType() == TokenizerConstants.Type.Text ) {
                        ruleSet = getRulesByFirstLetter("abc");
                        result = match(tokenList, i, ruleSet);
                    }
                    else if ( token.getType() == TokenizerConstants.Type.Number ) {
                        ruleSet = getRulesByFirstLetter("number");
                        result = match(tokenList, i, ruleSet);
                    }
                    else if ( token.getType() == TokenizerConstants.Type.Space ) {
                        ruleSet = getRulesByFirstLetter(" ");
                        result = match(tokenList, i, ruleSet);
                    }
                }

                if ( result != null ) {
                    if (grammarModificationMap.containsKey(result.ruleName)) {
                        result.resultList = modifySet(grammarModificationMap.get(result.ruleName), result.resultList);
                    }
                    String resultStr = "";
                    for (Token t : result.resultList) {
                        resultStr += t.getText();
                    }
                    Token newToken = new Token(TokenizerConstants.Type.GrammarParsed, resultStr );
                    newToken.setGrammarRuleName( result.ruleName );
                    newToken.setPennType(PennType.CD);
                    newToken.setSynid(token.getSynid());
                    i = result.index;
                    newTokenList.add( newToken );
                } else {
                    newTokenList.add( token ); // didn't match
                    i++;
                }

            }

            return newTokenList;
        }
        return null;
    }

    // load the complete library from file
    public void init() throws IOException {
        if ( grammarMap.size() > 0 )
            throw new InvalidParameterException("grammar rules already loaded");

        List<String> grammarPatternList = Files.readAllLines(Paths.get(grammarRuleFile));
        if ( grammarPatternList != null ) {
            for ( String pattern : grammarPatternList ) {
                String line = pattern.trim();
                if ( line.trim().length() > 0 && !line.startsWith("//") && !line.startsWith("#") ) {
                    GrammarLhs lhs = processPattern(line);
                    if ( lhs == null ) {
                        throw new IOException("invalid line in grammar file \"" + grammarRuleFile + "\" @ " + line);
                    }
                    if ( lhs.conversionPattern == null && lhs.modifier == null && grammarMap.containsKey( lhs.name ) )
                        throw new InvalidParameterException("duplicate rule '" + lhs.name + "'");

                    // store in the maps
                    if ( lhs.conversionPattern != null ) {
                        grammarConversionMap.put(lhs.name, lhs.conversionPattern);
                    } else if ( lhs.modifier != null ) {
                        grammarModificationMap.put( lhs.name, lhs.modifier );
                    } else {
                        grammarMap.put(lhs.name, lhs);
                    }
                }
            }
        }

        // resolve reference to patterns internally
        resolveReferences();

        // setup first letter lookup
        setupFirstLetterLookup();
    }

    /**
     * return the java conversion pattern for a rule if it exists
     * @param name the name of the rule to check
     * @return the pattern for that rule or null if dne
     */
    public String getConversionPatternForGrammarRule( String name ) {
        return grammarConversionMap.get(name);
    }

    /**
     * access the conversion map for token name -> java date/time pattern
     * @return the map
     */
    public Map<String, String> getGrammarConversionMap() {
        return grammarConversionMap;
    }

    /**
     * apply the specified modification to the token list, for now the only modifcation supported
     * is space@index, insert a space @ index
     * @param modification the requested modification
     * @param list the list to modify
     * @return the modified list
     */
    private List<Token> modifySet(String modification, List<Token> list) {
        if (modification == null || !modification.startsWith("space@") ) {
            throw new InvalidParameterException("bad modification string:" + modification);
        }
        if (list != null ) {
            int index = Integer.parseInt(modification.split("@")[1]);
            list.add(index, new Token(TokenizerConstants.Type.Space, " "));
        }
        return list;
    }

    // match the longest possible chain of rules from a list of rules
    private Match match( List<Token> tokenList, int index, List<GrammarLhs> ruleSet ) {
        if ( tokenList != null && index < tokenList.size() && ruleSet != null ) {
            Match match = new Match();
            match.index = -1;

            for ( GrammarLhs lhs : ruleSet ) {
                Match temp = match( tokenList, index, lhs );
                if ( temp != null ) {
                    // careful - this doesn't allow for two matching rules
                    // if two rules match exactly - the first one will be chosen
                    if ( match.index == -1 || match.index < temp.index ) {
                        match.index = temp.index;
                        match.ruleName = temp.ruleName;
                        match.resultList = temp.resultList;
                    }
                }
            }

            if ( match.index > 0 ) {
                match.index = match.index + index; // offset into token set
                return match;
            }

        }
        return null;
    }

    // match the longest possible chain of rules from a list of rules
    private Match match( List<Token> tokenList, int index, GrammarLhs rule ) {
        if ( tokenList != null && index < tokenList.size() && rule != null && rule.getRhsList() != null ) {
            Match match = new Match();
            match.resultList = new ArrayList<>();
            match.index = 0;

            for ( GrammarRhs rhs : rule.getRhsList() ) {
                if ( index + match.index >= tokenList.size() ) { // failed - no more tokens - but rule not finished
                    match.index = -1;
                    break;
                }

                Match temp = match( tokenList, index + match.index, rhs );
                if ( temp != null ) {
                    match.index += temp.index;
                    match.resultList.addAll(temp.resultList);
                } else {
                    match.index = -1;
                    break;
                }
            }

            if ( match.index > 0 ) {
                match.ruleName = rule.name;
                return match;
            }
        }
        return null;
    }

    // match the longest possible chain of rules from a list of rules
    private Match match( List<Token> tokenList, int index, GrammarRhs rhs ) {
        if ( rhs != null && tokenList != null && index < tokenList.size() ) {
            Match match = new Match();

            if (rhs.reference != null) { // other rule reference
                Match m = match(tokenList, index + match.index, rhs.reference);
                if (m != null) { // update recursive state
                    match.index = match.index + m.index;
                    match.resultList.addAll(m.resultList);

                    // repeat of reference?
                    if ( rhs.isRepeat ) {
                        m = match(tokenList, index + match.index, rhs.reference);
                        while ( m != null ) {
                            match.index = match.index + m.index;
                            match.resultList.addAll(m.resultList);
                            m = match(tokenList, index + match.index, rhs.reference);
                        }
                    }

                } else { // fail
                    match.index = -1;
                }

            } else if (rhs.text != null) { // literal
                Token token = tokenList.get(index);
                if (rhs.text.equals("abc") && Tokenizer.isABC(token.getText().charAt(0))) { // text is valid
                    match.resultList.add(token);
                    match.index++;
                } else if (rhs.text.equals("number") && Tokenizer.isNumeric(token.getText().charAt(0))) { // text is valid
                    // range check?
                    if ( rhs.numberRangeStart != rhs.numberRangeEnd || rhs.numberRangeStart != 0 ) {
                        // can only really check numbers in the 64 bit range
                        if ( token.getText().length() <= 12 ) {
                            long value = Long.parseLong( token.getText() );
                            if ( rhs.numberRangeStart <= value && value <= rhs.numberRangeEnd ) { // within range?
                                match.resultList.add(token);
                                match.index++;
                            } else // outside range, fail
                                match.index = -1;
                        } else // too big - fail
                            match.index = -1;
                    } else {
                        match.resultList.add(token);
                        match.index++;
                    }
                } else if (rhs.text.equals("space") && token.getText().equals(" ")) {
                    match.resultList.add(token);
                    match.index++;
                } else if (rhs.text.equals(token.getText())) {
                    match.resultList.add(token);
                    match.index++;
                } else { // fail
                    match.index = -1;
                }

            } else if (rhs.patternSet != null) { // literal
                Token token = tokenList.get(index);
                if ( rhs.patternSet.contains("abc") && Tokenizer.isABC( token.getText().charAt(0) ) ) {
                    match.resultList.add(token);
                    match.index++;
                } else if ( rhs.patternSet.contains("number") && Tokenizer.isNumeric( token.getText().charAt(0) ) ) {
                    match.resultList.add(token);
                    match.index++;
                } else if ( rhs.patternSet.contains("space") && token.getText().equals(" ")) {
                    match.resultList.add(token);
                    match.index++;
                } else if (rhs.patternSet.contains(token.getText())) {
                    match.resultList.add(token);
                    match.index++;
                } else { // fail
                    match.index = -1;
                }
            }

            // did we get a valid match
            if ( match.index > 0 ) {
                if ( rhs.isRepeat ) { // try again / recurse?
                    Match temp = match( tokenList, index + match.index, rhs );
                    if ( temp != null ) {
                        match.index += temp.index;
                        match.resultList.addAll(temp.resultList);
                    }
                }
                return match;
            }
        }
        return null;
    }

    // load the complete library from a set of strings
    // useful for unit testing
    public void loadFromString( String[] grammarRuleSet ) throws IOException {
        if ( grammarRuleSet != null ) {
            for ( String pattern : grammarRuleSet ) {
                String line = pattern.trim();
                if ( line.length() > 0 && ! line.startsWith("//") ) {
                    GrammarLhs lhs = processPattern(line);
                    if ( grammarMap.containsKey( lhs.name ) )
                        throw new InvalidParameterException("duplicate rule '" + lhs.name + "'");

                    // store in the map
                    grammarMap.put( lhs.name, lhs );
                }
            }
        }

        // resolve reference to patterns internally
        resolveReferences();

        // setup first letter lookup
        setupFirstLetterLookup();
    }

    // resolve references to other rules where possible
    public void resolveReferences() {
        // resolve any reference
        for ( String name : grammarMap.keySet() ) {
            GrammarLhs lhs = grammarMap.get(name);
            List<GrammarRhs> rhsList = lhs.getRhsList();
            if ( rhsList == null )
                throw new InvalidParameterException("invalid grammar rule, no rhs '" + lhs.name + "'");
            for ( GrammarRhs rhs : rhsList )
                if ( rhs.text != null && rhs.text.length() > 0 ) {
                    // text, but not abc or number marker
                    if ( ! ( rhs.text.equals("abc") || rhs.text.equals("number") || rhs.text.equals("space")) && Tokenizer.isABC(rhs.text.charAt(0)) ) {
                        if ( rhs.text.equals(lhs.name) )

                            throw new InvalidParameterException("rule '" + lhs.name + "' cyclic reference");

                        // can this be resolved?
                        if ( grammarMap.containsKey( rhs.text ) ) {
                            GrammarLhs reference = grammarMap.get(rhs.text);
                            rhs.text = null;
                            rhs.reference = reference;
                        }

                    }
                }
        }
    }

    // setup the lhs start non terminal lookup(s)
    public List<GrammarLhs> getRulesByFirstLetter( String firstLetter ) {
        if ( firstLetter != null )
            return startPattern.get(firstLetter);
        return null;
    }

    // setup the lhs start non terminal lookup(s)
    public void setupFirstLetterLookup() throws IOException {
        // resolve any reference
        for ( String name : grammarMap.keySet() ) {
            GrammarLhs lhs = grammarMap.get(name);
            if ( lhs.isPublic ) {
                List<String> tokenList = lhs.getStartTokens();
                if (tokenList == null)
                    throw new InvalidParameterException("invalid return result null");
                for (String str : tokenList) {
                    if (startPattern.containsKey(str))
                        startPattern.get(str).add(lhs);
                    else {
                        List<GrammarLhs> list = new ArrayList<GrammarLhs>();
                        list.add(lhs);
                        startPattern.put(str, list);
                    }
                }
            }
        }
    }

    // process a single line
    public GrammarLhs processPattern( String line ) {
        if ( line != null && line.contains("=") ) {
            int index = line.indexOf('=');
            if ( index > 0 ) {
                String lhs = line.substring(0, index ).trim();
                String rhs = line.substring(index + 1).trim();

                String[] lhsParts = lhs.split(" ");

                if ( lhsParts.length != 2 )
                    throw new InvalidParameterException("grammar pattern must have private/public/pattern name part");
                if ( !lhsParts[0].equals("private") && !lhsParts[0].equals("public") && !lhsParts[0].equals("pattern")
                        && !lhsParts[0].equals("modifier"))
                    throw new InvalidParameterException("grammar pattern must start with 'public', 'private' or 'pattern'");

                // special conversion patterns for rules
                if ( lhsParts[0].equals("pattern") ) {

                    return new GrammarLhs(false, lhsParts[1].trim(), rhs.trim(), null);

                } else if ( lhsParts[0].equals("modifier") ) {

                    return new GrammarLhs(false, lhsParts[1].trim(), null, rhs.trim());

                } else {

                    GrammarLhs grammarLhs = new GrammarLhs(lhsParts[0].equals("public"), lhsParts[1].trim(), null, null);
                    grammarLhs.setRhs(parseGrammarRhs(rhs.trim()));
                    return grammarLhs;
                }
            }
        }
        return null;
    }

    // process a single rhs rule
    public List<GrammarRhs> parseGrammarRhs( String rhs ) {
        if ( rhs == null || rhs.trim().length() == 0 )
            throw new InvalidParameterException("grammar rhs empty");

        List<GrammarRhs> resultList = new ArrayList<>();

        // or bag of words rule?
        if ( rhs.startsWith("[") || rhs.endsWith("]") ) {
            rhs = rhs.substring(1, rhs.length() - 1 ).trim();
            String[] bag = rhs.split(" ");
            GrammarRhs node = new GrammarRhs();
            node.patternSet = new HashSet<String>();
            node.patternSet.addAll(Arrays.asList(bag) );
            resultList.add( node );
        } else {
            // ordinary ordered list
            String[] stringList = rhs.split(" ");
            for ( String str : stringList ) {
                boolean isRepeat = false;
                if ( str.length() > 1 && str.endsWith("+") ) {
                    isRepeat = true;
                    str = str.substring(0, str.length() - 1 );
                }

                GrammarRhs node = new GrammarRhs();
                node.text = parseRange(str, node);
                node.isRepeat = isRepeat;
                resultList.add(node);
            }
        }
        return resultList;
    }

    // parse the .range() part of a potential query
    public String parseRange( String str, GrammarRhs node ) {
        if ( str != null && node != null && str.contains(".range(") )
        {
            int index = str.indexOf(".range(");
            String returnStr = str.substring(0, index);

            String rangeStr = str.substring(index + 7);
            int index2 = rangeStr.indexOf(')');
            if ( index2 < 3 )
                throw new InvalidParameterException(".range() pattern missing )");
            rangeStr = rangeStr.substring(0, index2);
            String[] parts = rangeStr.split(",");
            if ( parts.length != 2 )
                throw new InvalidParameterException(".range() must have two comma separated items");

            node.numberRangeStart = Integer.parseInt(parts[0]);
            node.numberRangeEnd = Integer.parseInt(parts[1]);

            return returnStr;
        }
        return str;
    }

    public void setGrammarRuleFile( String grammarRuleFile ) {
        this.grammarRuleFile = grammarRuleFile;
    }

    public String getGrammarRuleFile() {
        return grammarRuleFile;
    }

    // temporary holder of return values between the calls
    private class Match {
        public Match() {
            resultList = new ArrayList<>();
        }
        public String ruleName;
        public List<Token> resultList;
        public int index;
    }

}

