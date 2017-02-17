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

package industries.vocht.viki.model;

import industries.vocht.viki.dao.PennType;
import industries.vocht.viki.model.semantics.Tuple;
import industries.vocht.viki.tokenizer.Tokenizer;
import industries.vocht.viki.utility.BinarySerializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/*
 * Created by peter on 21/12/14.
 *
 * a sentence, basic unit of language for KAI
 *
 */
public class Sentence {

    protected List<Token> tokenList;    // list of tokens making a sentence
    private Tuple tuple;                // the tuple for this sentence

    public Sentence() {
        this.tokenList = new ArrayList<>();
    }

    public Sentence(List<Token> tokenList) {
        this.tokenList = tokenList;
    }

    public Sentence(List<Token> tokenList, Tuple tuple) {
        this.tokenList = tokenList;
        this.tuple = tuple;
    }

    public Sentence(String wordStr, PennType type) {
        this.tokenList = new ArrayList<>();
        this.tokenList.add( new Token(wordStr, type) );
    }

    // pretty print
    public String toString() {
        if ( tokenList != null && tokenList.size() > 0 ) {
            Tokenizer tokenizer = new Tokenizer();
            return tokenizer.toString(tokenList);
        }
        return "";
    }

    public int size() {
        if ( tokenList != null )
            return tokenList.size();
        return 0;
    }

    // access item lists
    public List<Token> getTokenList() {
        return tokenList;
    }

    public Tuple getTuple() {
        return tuple;
    }

    public void setTuple(Tuple tuple) {
        this.tuple = tuple;
    }

    /**
     * helper: turn a list of sentences into a single list of tokens
     * @param sentenceList the list of sentences
     * @return a list of tokens
     */
    public static List<Token> sentenceListToTokens(List<Sentence> sentenceList) {
        List<Token> tokenList = new ArrayList<>();
        if ( sentenceList != null ) {
            for ( Sentence sentence : sentenceList ) {
                tokenList.addAll( sentence.getTokenList() );
            }
        }
        return tokenList;
    }

    /**
     * write this sentence into a serialiser
     * @param serializer the binary repository to write to
     */
    public void write( BinarySerializer serializer ) {
        serializer.writeByte(0xee); // write magic marker
        if ( tokenList == null || tokenList.size() == 0 ) {
            serializer.writeInt(0);
        } else {
            serializer.writeInt( tokenList.size() );

            for ( Token token : tokenList ) {
                token.write(serializer);
            }
        }
        if ( tuple == null ) {
            serializer.writeByte(0);
        } else {
            serializer.writeByte(1);
            tuple.write(serializer);
        }
    }

    /**
     * read from the serialiser into a sentence structure
     * @param serializer the serialiser to read from
     */
    public void read( BinarySerializer serializer ) throws IOException {
        int magicMarker = serializer.readByte();
        if ( (magicMarker & 0xff) != 0xee ) {
            throw new IOException("invalid Sentence magic marker, invalid data");
        }
        tokenList = new ArrayList<>();
        int size = serializer.readInt();
        if ( size > 0 ) {
            for ( int i = 0; i < size; i++ ) {
                Token token = new Token();
                token.read( serializer );
                tokenList.add( token );
            }
        }
        int exists = serializer.readByte();
        if ( exists == 1 ) {
            tuple = new Tuple();
            tuple.read( serializer );
        } else {
            tuple = null;
        }
    }


}

