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

package industries.vocht.viki.model.super_search;

import industries.vocht.viki.dao.PennType;
import industries.vocht.viki.document.Document;
import industries.vocht.viki.model.Token;
import industries.vocht.viki.model.TokenizerConstants;
import industries.vocht.viki.model.TokenWithIndex;
import industries.vocht.viki.tokenizer.Tokenizer;

import java.util.List;

/**
 * Created by peter on 25/04/16.
 *
 * parse super search statements and convert them to actionable search items
 *
 * grammar:
 *
 * where ->  ['exact'] 'body' '(' text ')'
 *           ['exact'] 'summary' '(' text ')' |
 *           ['exact'] 'author' '(' text ')' |
 *           ['exact'] 'person' '(' text ')' |
 *           ['exact'] 'location' '(' text ')' |
 *           ['exact'] 'title' '(' text ')' |
 *           'url'  '('  text  ')'  |
 *           'date' 'between' time 'and' time |
 *           'date' 'before' time |
 *           'date' 'after' time |
 *           'date' 'exact' time  |
 *           where 'and' where |
 *           where 'and' 'not' where |
 *           where 'or' where |
 *           '(' where ')'
 *
 * text ->   ...  |
 *           ... , tag
 *
 * time ->  yyyy-mm-dd hh ':' mm |
 *          yyyy-mm-dd |
 *          yyyy-mm |
 *          yyyy
 *
 * tag -> verb | noun | adjective | proper noun | penn-tag
 *
 */
public class SSearchParser {

    public SSearchParser() {
    }

    /**
     * convert a super search text to a search item set
     *
     * @param text the text to convert
     * @return the parsed search system
     */
    public ISSearchItem parse(String text) throws SSearchParserException {
        if (text != null && text.trim().length() > 0) {
            Tokenizer tokenizer = new Tokenizer();
            List<Token> tokenList = tokenizer.tokenize(text);
            SSearchIndexToken item = parse(0, tokenList);
            if (item != null) {
                return item.getItem();
            }
        }
        return null;
    }

    /**
     * convert a super search text to a search item set
     *
     * @param index the index into the token list
     * @param tokenList a list of tokens to be converted / parsed
     * @return the parsed search system
     */
    public SSearchIndexToken parse(int index, List<Token> tokenList) throws SSearchParserException {
        if (tokenList != null && index < tokenList.size()) {

            SSearchIndexToken item = null;
            TokenWithIndex tokenWithIndex = getNextSkippingSpace(index, tokenList);
            if ( tokenWithIndex == null ) {
                throw new SSearchParserException("expected statement start @ " + index);
            }
            String tokenStr = tokenWithIndex.getItem().getText();
            switch (tokenStr) {
                case "exact": {
                    item = parseExactWord(tokenWithIndex.getIndex(), tokenList);
                    break;
                }
                case "author":
                case "summary":
                case "title":
                case "location":
                case "person":
                case "url":
                case "body": {
                    item = parseWord(tokenWithIndex.getIndex(), tokenList, tokenStr, false);
                    break;
                }
                case "date": {
                    item = parseDate(index, tokenList);
                    break;
                }
                case "(": {
                    item = parseBrackets(index, tokenList);
                    break;
                }
            }

            if (item == null) {
                throw new SSearchParserException("unknown token @ " + index);
            }

            index = item.getIndex(); // update index to next token

            // or/and ?
            if (index < tokenList.size()) {
                tokenWithIndex = getNextSkippingSpace(index, tokenList);
                if ( tokenWithIndex != null && ( tokenWithIndex.getItem().getText().equals("or") ||
                        tokenWithIndex.getItem().getText().equals("and") ) ) {
                    return parseAndOr( item, index, tokenList );
                }
            }

            // just return the item itself
            return item;

        }
        return null;
    }

    /**
     * and / or parser helper
     * @param item1 the first item
     * @param index the current index at which 'or' or 'and' was found
     * @param tokenList the list of tokens
     * @return the and/or parsed entity with updated index
     * @throws SSearchParserException
     */
    private SSearchIndexToken parseAndOr( SSearchIndexToken item1, int index, List<Token> tokenList ) throws SSearchParserException {
        // or/and ?
        if (index < tokenList.size()) {
            TokenWithIndex tokenWithIndex = getNextSkippingSpace(index, tokenList);
            if ( tokenWithIndex != null ) {
                String tokenStr = tokenWithIndex.getItem().getText();
                switch (tokenStr) {
                    case "or": {
                        SSearchIndexToken item2 = parse(tokenWithIndex.getIndex(), tokenList);
                        if (item2 == null) {
                            throw new SSearchParserException("'or' missing rhs expression");
                        }
                        return new SSearchIndexToken(new SSearchOr(item1.getItem(), item2.getItem()), item2.getIndex());
                    }
                    case "and": {

                        // and not?
                        TokenWithIndex next = getNextSkippingSpace(tokenWithIndex.getIndex(), tokenList);
                        if ( next != null && next.getItem().getText().equals("not") ) {
                            SSearchIndexToken item2 = parse(next.getIndex(), tokenList);
                            if (item2 == null) {
                                throw new SSearchParserException("'and not' missing rhs expression");
                            }
                            return new SSearchIndexToken(new SSearchAndNot(item1.getItem(), item2.getItem()), item2.getIndex());
                        } else {
                            SSearchIndexToken item2 = parse(tokenWithIndex.getIndex(), tokenList);
                            if (item2 == null) {
                                throw new SSearchParserException("'and' missing rhs expression");
                            }
                            return new SSearchIndexToken(new SSearchAnd(item1.getItem(), item2.getItem()), item2.getIndex());
                        }
                    }
                }
            }
        }
        throw new SSearchParserException("invalid 'and' / 'or' block @ " + index);
    }

    /**
     * metadata '(' text ')'
     * @param index the offset into the array
     * @param tokenList the array of items
     * @return a parsed item if successful or null
     * @throws SSearchParserException
     */
    private SSearchIndexToken parseWord(int index, List<Token> tokenList, String metadata, boolean exact) throws SSearchParserException {
        if (tokenList != null && index < tokenList.size()) {
            index = getNextCompulsary( index, tokenList, "(" );
            SSearchIndexToken item = parseText(index, tokenList);
            if ( item == null || !(item.getItem() instanceof SSearchWord) ) {
                throw new SSearchParserException("expression word( must be followed by text @ " + index);
            }

            SSearchWord word = (SSearchWord)item.getItem();
            String searchMetadata;
            String pennTag = null;
            String semantic = null;
            switch (metadata) {
                case "author": {
                    searchMetadata = Document.META_AUTHOR;
                    if ( word.getTag() != null ) {
                        throw new SSearchParserException("'author' field cannot be followed by a tag specifier, is assumed NNP");
                    }
                    pennTag = "NNP";
                    break;
                }
                case "title": {
                    searchMetadata = Document.META_TITLE;
                    break;
                }
                case "body": {
                    searchMetadata = Document.META_BODY;
                    break;
                }
                case "url": {
                    searchMetadata = Document.META_URL;
                    break;
                }
                case "summary": {
                    searchMetadata = Document.META_SUMMARIZATION;
                    break;
                }
                case "person": {
                    searchMetadata = Document.META_BODY;
                    if ( word.getTag() != null ) {
                        throw new SSearchParserException("'person' field cannot be followed by a tag specifier, is assumed NNP");
                    }
                    semantic = "person";
                    pennTag = "NNP";
                    break;
                }
                case "location": {
                    searchMetadata = Document.META_BODY;
                    if ( word.getTag() != null ) {
                        throw new SSearchParserException("'location' field cannot be followed by a tag specifier, is assumed NNP");
                    }
                    semantic = "location";
                    pennTag = "NNP";
                    break;
                }
                default: {
                    throw new SSearchParserException("unknown meta-data tag " + metadata);
                }
            }
            word.setMetadata(searchMetadata);
            word.setExact(exact);
            word.setSemantic(semantic);
            if ( pennTag != null ) { // don't overwrite
                word.setTag(pennTag);
            }

            index = item.getIndex();
            index = getNextCompulsary( index, tokenList, ")" );
            item.setIndex(index);
            return item;
        }
        return null;
    }


    /**
     * 'exact' 'word' '(' text ')'
     * @param index the offset into the array
     * @param tokenList the array of items
     * @return a parsed item if successful or null
     * @throws SSearchParserException
     */
    private SSearchIndexToken parseExactWord(int index, List<Token> tokenList) throws SSearchParserException {
        if (tokenList != null && index < tokenList.size()) {

            TokenWithIndex tokenWithIndex = getNextSkippingSpace(index, tokenList);
            if ( tokenWithIndex == null ) {
                throw new SSearchParserException("expression 'exact' must be followed by other tokens @ " + index);
            }
            String metadata = tokenWithIndex.getItem().getText();
            return parseWord( tokenWithIndex.getIndex(), tokenList, metadata, true);
        } else {
            throw new SSearchParserException("expression 'exact' must be followed by other tokens @ " + index);
        }
    }


    /**
     * 'date' 'between' time 'and' time |
     * 'date' time |
     * @param index the offset into the array
     * @param tokenList the array of items
     * @return a parsed item if successful or null
     * @throws SSearchParserException
     */
    private SSearchIndexToken parseDate(int index, List<Token> tokenList) throws SSearchParserException {
        if (tokenList != null && index < tokenList.size()) {
            index = getNextCompulsary( index, tokenList, "date" );

            // check next - is it 'between'?
            TokenWithIndex tokenWithIndex = getNextSkippingSpace(index, tokenList);
            if ( tokenWithIndex == null ) {
                throw new SSearchParserException("missing next token after 'time' @ " + index);
            }
            if ( tokenWithIndex.getItem().getText().equals("between") ) {
                index = getNextCompulsary(index, tokenList, "between");
                SSearchIndexToken time1 = parseTimeStamp(null, index, tokenList);
                if (time1 == null || !(time1.getItem() instanceof SSearchDateRange)) {
                    throw new SSearchParserException("expression 'time between' must be followed by a time indication @ " + index);
                }
                index = time1.getIndex();
                index = getNextCompulsary(index, tokenList, "and");
                SSearchIndexToken time2 = parseTimeStamp(null, index, tokenList);
                if (time2 == null || !(time2.getItem() instanceof SSearchDateRange)) {
                    throw new SSearchParserException("expression 'time between t1 and' must be followed by a time indication @ " + index);
                }
                index = time2.getIndex();
                SSearchDateRange t1 = (SSearchDateRange) time1.getItem();
                SSearchDateRange t2 = (SSearchDateRange) time2.getItem();
                return new SSearchIndexToken(new SSearchDateRange( SSearchDateRangeType.Between, t1.getYear1(), t1.getMonth1(), t1.getDay1(), t1.getHour1(), t1.getMin1(),
                                                                   t2.getYear1(), t2.getMonth1(), t2.getDay1(), t2.getHour1(), t2.getMin2() ), index);
            } else {

                String operation = tokenWithIndex.getItem().getText();
                if ( !operation.equals("after") && !operation.equals("before") && !operation.equals("exact") ) {
                    throw new SSearchParserException("expression 'date' must be followed by 'between', 'before', 'after', or 'exact' @ " + index);
                }
                index = getNextCompulsary(index, tokenList, "before", "after", "exact");

                SSearchIndexToken time1 = parseTimeStamp( SSearchDateRangeType.convert(operation), index, tokenList);
                if (time1 == null || !(time1.getItem() instanceof SSearchDateRange)) {
                    throw new SSearchParserException("expression 'time' must be followed by a time indication @ " + index);
                }
                index = time1.getIndex();
                SSearchDateRange t1 = (SSearchDateRange) time1.getItem();
                return new SSearchIndexToken(t1, index);
            }
        }
        return null;
    }


    /**
     * '(' ssearch ')'
     * @param index the offset into the array
     * @param tokenList the array of items
     * @return a parsed item if successful or null
     * @throws SSearchParserException
     */
    private SSearchIndexToken parseBrackets(int index, List<Token> tokenList) throws SSearchParserException {
        if (tokenList != null && index < tokenList.size()) {
            index = getNextCompulsary( index, tokenList, "(" );
            SSearchIndexToken item = parse( index, tokenList );
            if ( item == null ) {
                throw new SSearchParserException("expected 'expression )' after '(' @ " + index );
            }
            index = item.getIndex();

            // next token can be either 'and', 'or' or ')'
            if ( index < tokenList.size() ) {
                TokenWithIndex tokenWithIndex = getNextSkippingSpace(index, tokenList);
                if ( tokenWithIndex != null ) {
                    Token next = tokenWithIndex.getItem();
                    if (next.getText() == null || !(next.getText().equals("or") || next.getText().equals("and") || next.getText().equals(")"))) {
                        throw new SSearchParserException("expected token 'and'/'or'/')' @ " + index);
                    }

                    // update and / or parsing
                    if (next.getText().equals("or") || next.getText().equals("and")) {
                        item = parseAndOr(item, index, tokenList);
                    }
                    index = getNextCompulsary(index, tokenList, ")");
                    item.setIndex(index);
                    return item;

                } else {
                    throw new SSearchParserException("expected ')' @ " + index);
                }

            } else {
                throw new SSearchParserException("expected ')' @ " + index);
            }

        }
        return null;
    }


    /**
     * ' text ' |
     * ' text ' , tag
     * @param index the offset into the array
     * @param tokenList the array of items
     * @return a parsed item if successful or null
     * @throws SSearchParserException
     */
    private SSearchIndexToken parseText(int index, List<Token> tokenList) throws SSearchParserException {
        if (tokenList != null && index < tokenList.size()) {
            StringBuilder text = new StringBuilder();
            Token token = tokenList.get(index);
            while ( token != null && !token.getText().equals(")") && !token.getText().equals(",") && index < tokenList.size() ) {
                text.append(token.getText());
                index++;
                if ( index < tokenList.size() ) {
                    token = tokenList.get(index);
                } else {
                    token = null;
                }
            }
            if ( token == null || (!token.getText().equals(")") && !token.getText().equals(",")) ) {
                throw new SSearchParserException("unterminated text @ " + (index - 1));
            }
            index++;

            // is it followed by an optional tag?
            if ( token.getText().equals(",") ) {
                TokenWithIndex next = getNextSkippingSpace(index, tokenList);
                if ( next == null || next.getItem().getType() != TokenizerConstants.Type.Text ) {
                    throw new SSearchParserException("invalid token following text , penn-type");
                }

                index = next.getIndex();
                String tag = next.getItem().getText();
                if ( !tag.equals("noun") && !tag.equals("proper noun") && !tag.equals("adjective") &&
                     !tag.equals("verb") && PennType.fromString(tag.toUpperCase()) == PennType.UNC ) {
                    throw new SSearchParserException("invalid token following text , penn-type: " + tag);
                }

                return new SSearchIndexToken(new SSearchWord(text.toString(), tag.toUpperCase(), null, 0, 0, false), index);

            } else {
                return new SSearchIndexToken(new SSearchWord(text.toString(), null, null, 0, 0, false), index - 1);
            }
        }
        return null;
    }


    /**
     * time ->  yyyy-mm-dd hh ':' mm |
     *          yyyy-mm-dd |
     *          yyyy-mm |
     *          yyyy
     * @param index the offset into the array
     * @param tokenList the array of items
     * @return a parsed item if successful or null
     * @throws SSearchParserException
     */
    private SSearchIndexToken parseTimeStamp( SSearchDateRangeType operation, int index, List<Token> tokenList) throws SSearchParserException {
        if (tokenList != null && index < tokenList.size()) {

            int year1;
            int month1 = -1;
            int day1 = -1;
            int hour1 = -1;
            int min1 = 0;

            // yyyy
            TokenWithIndex item = getNextCompulsaryType(index, tokenList, TokenizerConstants.Type.Number);
            index = item.getIndex();
            year1 = Integer.parseInt(item.getItem().getText());
            if ( year1 < 1000 || year1 > 9999 ) {
                throw new SSearchParserException("invalid year value, must lie between 1000 and 9999");
            }

            // nex token '-' month ?
            if ( index < tokenList.size() ) {
                TokenWithIndex next = getNextSkippingSpace(index, tokenList);
                if ( next != null && (next.getItem().getText().equals("-") || next.getItem().getText().equals("/")) ) {
                    index = next.getIndex();

                    item = getNextCompulsaryType(index, tokenList, TokenizerConstants.Type.Number);
                    index = item.getIndex();
                    month1 = Integer.parseInt(item.getItem().getText());
                    if ( month1 < 1 || month1 > 12 ) {
                        throw new SSearchParserException("invalid month value, must lie between 1 and 12");
                    }

                    // - day?
                    next = getNextSkippingSpace(index, tokenList);
                    if ( next != null && (next.getItem().getText().equals("-") || next.getItem().getText().equals("/")) ) {
                        index = next.getIndex();

                        item = getNextCompulsaryType(index, tokenList, TokenizerConstants.Type.Number);
                        index = item.getIndex();
                        day1 = Integer.parseInt(item.getItem().getText());
                        if ( day1 < 1 || day1 > 31 ) {
                            throw new SSearchParserException("invalid day value, must lie between 1 and 31");
                        }

                        // space hour?
                        next = getNextSkippingSpace(index, tokenList);
                        if ( next != null && next.getItem().getType() ==  TokenizerConstants.Type.Number ) {
                            index = next.getIndex();
                            hour1 = Integer.parseInt(next.getItem().getText());
                            if ( hour1 < 0 || hour1 > 24 ) {
                                throw new SSearchParserException("invalid hour value, must lie between 0 and 24");
                            }

                            // must be followed by : mins
                            index = getNextCompulsary( index, tokenList, ":" );

                            item = getNextCompulsaryType(index, tokenList, TokenizerConstants.Type.Number);
                            min1 = Integer.parseInt(item.getItem().getText());
                            if ( min1 < 0 || min1 > 59 ) {
                                throw new SSearchParserException("invalid minute value, must lie between 0 and 59");
                            }
                            index = item.getIndex();
                            int prevIndex = index;

                            // optional seconds field
                            next = getNextSkippingSpace(index, tokenList);
                            if ( next != null && next.getItem().getText().equals(":") ) {
                                index = getNextCompulsary( index, tokenList, ":" );
                                item = getNextCompulsaryType(index, tokenList, TokenizerConstants.Type.Number);
                                index = item.getIndex();
                            } else if ( next != null ){
                                index = prevIndex;
                            } else {
                                index = index + 1;
                            }

                        }
                    }

                }
            }

            // return the result
            return new SSearchIndexToken( new SSearchDateRange( operation, year1, month1, day1, hour1, min1,
                                                                year1, month1, day1, hour1, min1 ), index);

        }
        return null;
    }


    /**
     * make sure the token @ index is word
     * @param index the index of the system
     * @param tokenList the  list of tokens / stream
     * @param words the word(s) to check
     * @throws SSearchParserException
     */
    private int getNextCompulsary( int index, List<Token> tokenList, String ... words ) throws SSearchParserException {

        String wordList = "";
        for ( String word : words ) {
            if ( wordList.length() > 0 ) {
                wordList = wordList + ", ";
            }
            wordList = wordList + word;
        }

        if ( index < tokenList.size() ) {

            // skip any white-spaces automatically
            TokenWithIndex next = getNextSkippingSpace( index, tokenList );
            if ( next == null || next.getItem() == null ) {
                throw new SSearchParserException("expected token(s) '" + wordList + "' @ " + index);
            }

            // check it is the word(s)
            boolean found = false;
            for ( String word : words ) {
                if ( word.equals(next.getItem().getText()) ) {
                    found = true;
                    break;
                }
            }

            if ( !found ) {
                throw new SSearchParserException("expected token(s) '" + wordList + "' @ " + index);
            }

            return next.getIndex();

        } else {
            throw new SSearchParserException("expected token(s) '" + wordList + "' @ " + index);
        }
    }

    /**
     * make sure the token @ index is word
     * @param index the index of the system
     * @param tokenList the  list of tokens / stream
     * @param type the type to check
     * @throws SSearchParserException
     */
    private TokenWithIndex getNextCompulsaryType(int index, List<Token> tokenList, TokenizerConstants.Type type) throws SSearchParserException {
        if ( index < tokenList.size() ) {

            // skip any white-spaces automatically
            TokenWithIndex next = getNextSkippingSpace( index, tokenList );
            if ( next == null || next.getItem() == null || next.getItem().getType() != type ) {
                throw new SSearchParserException("expected token type " + type.toString() + "' @ " + index);
            }

            return next;

        } else {
            throw new SSearchParserException("expected token type " + type.toString() + "' @ " + index);
        }
    }

    /**
     * get the next token skipping any spaces and return its index
     * @param index the index to start @
     * @param tokenList the token-list to scan
     * @return the token with its index
     */
    private TokenWithIndex getNextSkippingSpace( int index, List<Token> tokenList ) {
        // skip any white-spaces automatically
        if ( index < tokenList.size() ) {
            Token next = tokenList.get(index);
            while (next != null && next.getText().equals(" ") && index < tokenList.size()) {
                index++;
                if (index < tokenList.size()) {
                    next = tokenList.get(index);
                } else {
                    next = null;
                }
            }
            if (next != null) {
                return new TokenWithIndex(next, index + 1);
            }
        }
        return null;
    }

}




