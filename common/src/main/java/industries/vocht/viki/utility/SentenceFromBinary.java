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

package industries.vocht.viki.utility;

import industries.vocht.viki.model.Sentence;
import industries.vocht.viki.model.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Convert a byte array back to a parse tree and vice versa
 *
 * Created by peter on 28/02/16.
 *
 */
public class SentenceFromBinary {

    // max number of lines in a single document
    private final static int MAX_SIZE = 1_000_000;

    final Logger logger = LoggerFactory.getLogger(SentenceFromBinary.class);

    public SentenceFromBinary() {
    }

    /**
     * convert a set of sentence from a list to a binary blob
     * @param sentenceList the list of sentences
     * @return the byte serialised version
     */
    public byte[] convert( List<Sentence> sentenceList ) {
        // save the parsed version of the document
        BinarySerializer blob = new BinarySerializer(65536);
        blob.writeInt(sentenceList.size());
        for (Sentence sentence : sentenceList) {
            sentence.write(blob);
        }
        return blob.getData();
    }

    /**
     * convert a binary blob back into a valid list of sentences
     * @param data the blob to read
     * @return returns null if invalid / empty
     * @throws IOException
     */
    public List<Sentence> convert(byte[] data) throws IOException {
        if ( data != null && data.length > 4 ) {
            BinarySerializer blob = new BinarySerializer(data);
            int size = blob.readInt();
            if ( size > 0 && size < MAX_SIZE ) {
                List<Sentence> sentenceList = new ArrayList<>(size);
                for ( int i = 0; i < size; i++ ) {
                    Sentence sentence = new Sentence();
                    sentence.read(blob);
                    sentenceList.add(sentence);
                }
                return sentenceList;
            } else {
                logger.error("invalid document size " + size);
            }
        }
        return null;
    }

    /**
     * convert the data blob into a token list (ignore sentence boundaries)
     * @param data the data to convert
     * @return a list of all tokens
     * @throws IOException
     */
    public List<Token> convertToTokenList(byte[] data) throws IOException {
        List<Sentence> sentenceList = convert(data);
        if ( sentenceList != null ) {
            List<Token> tokenList = new ArrayList<>();
            for ( Sentence sentence : sentenceList ) {
                tokenList.addAll(sentence.getTokenList());
            }
            return tokenList;
        }
        return null;
    }


}

