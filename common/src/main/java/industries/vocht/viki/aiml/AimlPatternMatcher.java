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

package industries.vocht.viki.aiml;

import industries.vocht.viki.model.Token;
import industries.vocht.viki.tokenizer.Tokenizer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by peter on 24/07/16.
 *
 * match a pattern with the current library
 *
 */
public class AimlPatternMatcher {

    public AimlPatternMatcher() {
    }

    /**
     * raw recursive string matcher - the first token must always match one of our constants
     * for this to be able to succeed - so no wildcards for the first character
     * @param str the string to match
     * @param manager the AIML manager library
     * @return null if failed, or a an AI/ML set of matching templates
     */
    public List<AimlTemplate> match(String str, AimlManager manager ) throws IOException {
        if ( str != null && str.length() > 0 && manager != null ) {
            Tokenizer tokenizer = new Tokenizer();
            List<Token> tokenList = tokenizer.filterOutPunctuation(
                    tokenizer.filterOutSpaces(tokenizer.tokenize(str)));
            String currentStr = tokenList.get(0).getText().toLowerCase();

            AimlPattern current = manager.getNodeSet().get(currentStr);
            if ( current != null ) {
                List<AimlTemplate> matchList = new ArrayList<>();
                match( tokenList, 1, current, matchList, new ArrayList<>() );
                if ( matchList.size() > 0 ) {
                    return matchList;
                }
            }
        }
        return null;
    }

    /**
     * recursive matcher helper
     *
     * must become more sophisticated - on a no match - recurse to potential star match patterns
     * and keep multiple possible matches
     *
     * @param tokenList the liste of tokens to match
     * @param index the index into the list of tokens to match
     * @param rule the current rule to use for matching
     */
    private void match(List<Token> tokenList, int index, AimlPattern rule, List<AimlTemplate> matchList,
                       List<AimlBinding> bindingList) {
        if ( tokenList != null && index < tokenList.size() &&
                rule != null && rule.getNodeSet() != null ) {
            // move on to the next token
            String currentStr = tokenList.get(index).getText().toLowerCase();
            AimlPattern current = rule.getNodeSet().get(currentStr);
            if ( current != null ) {
                match( tokenList, index + 1, current, matchList, bindingList );
            }

            // is there a wildcard available per chance?
            current = rule.getNodeSet().get("*");
            if ( current != null ) {
                List<Token> starList = new ArrayList<>();
                // start eating text till we get to a character that is allowed after the
                // wildcard, or we run out of characters
                while ( index < tokenList.size() ) {
                    currentStr = tokenList.get(index).getText().toLowerCase();
                    if ( current.getNodeSet().containsKey(currentStr) ) {
                        // found the next match - stop here
                        current = current.getNodeSet().get(currentStr);
                        index = index + 1;
                        break;
                    }
                    starList.add(tokenList.get(index));
                    index = index + 1;
                }

                // set the star value into the environment
                bindingList.add(new AimlBinding(matchList.size(), starList));
                if ( index < tokenList.size() ) {
                    match(tokenList, index, current, matchList, bindingList);
                } else {
                    if ( current != null ) {
                        finish_bind(matchList, current.getTemplateList(), bindingList);
                    }
                }
            }

        } else if ( tokenList != null && index == tokenList.size() && rule != null ) {
            // finally succeeded?
            // is there a "*" at the end?
            if ( rule.getTemplateList() == null ||  rule.getTemplateList().size() == 0 ) {
                AimlPattern pattern = rule.getNodeSet().get("*");
                if ( pattern == null ) {
                    bindingList.add(new AimlBinding(matchList.size())); // add empty pattern marker
                    finish_bind(matchList, rule.getTemplateList(), bindingList);
                } else {
                    bindingList.add(new AimlBinding(matchList.size()));
                    finish_bind(matchList, pattern.getTemplateList(), bindingList);
                }
            } else {
                bindingList.add(new AimlBinding(matchList.size()));
                finish_bind(matchList, rule.getTemplateList(), bindingList);
            }
        }
    }

    /**
     * resolve / assign the bindings to the matches as required
     * @param matchList the list of matches store
     * @param list the items to add to the match store
     * @param bindings the bindings made along the way
     */
    private void finish_bind(List<AimlTemplate> matchList, List<AimlTemplate> list, List<AimlBinding> bindings ) {
        matchList.addAll(list);
        if ( bindings.size() > 0 ) {
            List<AimlTemplate> newMatchList = new ArrayList<>();
            int stackIndex = bindings.get(0).getStackIndex();
            List<Token> tokenList = bindings.get(0).getTokenList();
            int nextStack = -1;
            int nextIndex = 1;
            if (bindings.size() > 1) {
                nextStack = bindings.get(nextIndex).getStackIndex();
            }
            for (int i = 0; i < matchList.size(); i++ ) {
                AimlTemplate match = matchList.get(i);
                // have we reached the next item in the binding stack?
                while (nextStack >= 0 && i >= nextStack && nextIndex < bindings.size() ) {
                    tokenList = bindings.get(nextIndex).getTokenList();
                    stackIndex = bindings.get(nextIndex).getStackIndex();
                    nextIndex += 1;
                    if (nextIndex < bindings.size()) {
                        nextStack = bindings.get(nextIndex).getStackIndex();
                    } else {
                        nextStack = -1; // there is no more next
                    }
                }
                if (i >= stackIndex && tokenList != null && tokenList.size() > 0) {
                    AimlTemplate newMatch = match.copy();
                    newMatch.setStarList(tokenList);
                    newMatchList.add(newMatch);
                } else {
                    newMatchList.add(match);
                }
            }
            matchList.clear();
            matchList.addAll(newMatchList);
        }
    }

}
