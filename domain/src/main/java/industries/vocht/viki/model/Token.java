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

import com.fasterxml.jackson.annotation.JsonIgnore;
import industries.vocht.viki.dao.PennType;
import industries.vocht.viki.utility.BinarySerializer;

import java.io.IOException;
import java.security.InvalidParameterException;

/*
 * Created by peter on 21/12/14.
 *
 * a token
 *
 */
public class Token {

    private TokenizerConstants.Type type;   // token type
    private String grammarRuleName;         // grammar rule name
    private String text;                    // token text
    private int synid;                      // synset id if applicable
    private String semantic;                // semantic tag
    private PennType pennType;              // penn tag
    private long value;                     // value or date-time if set
    private double wordScore;               // emotional word-score if set

    public Token() {
    }

    public Token(TokenizerConstants.Type type, String text) {
        if (text == null || text.length() == 0)
            throw new InvalidParameterException("invalid token (null or empty)");
        if (type == null)
            throw new InvalidParameterException("invalid type (null)");

        this.type = type;
        this.text = text;
        this.pennType = PennType.UNC;
        this.synid = -1;
    }

    public Token(String text) {
        this.text = text;
        this.pennType = PennType.UNC;
        this.synid = -1;
    }

    public Token(String text, PennType pennType, String semantic ) {
        this.text = text;
        this.pennType = pennType;
        this.semantic = semantic;
        this.synid = -1;
    }

    public Token(String text, PennType pennType) {
        if (text == null || text.length() == 0)
            throw new InvalidParameterException("invalid token (null or empty)");
        if (pennType == null)
            throw new InvalidParameterException("invalid type (null)");

        this.type = TokenizerConstants.Type.Text;
        this.text = text;
        this.pennType = pennType;
        this.synid = -1;
    }

    public String toString() {
        return text;
    }

    public String toTechString() {
        if ( pennType != null ) {
            return text + ":" + pennType.toString();
        } else if ( type != null ) {
            return text + " <" + type.toString() + ">";
        } else {
            return text;
        }
    }

    public Token copy() {
        Token t = new Token(text, pennType, semantic);
        t.type = this.type;
        t.grammarRuleName = this.grammarRuleName;
        t.synid = this.synid;
        return t;
    }

    @JsonIgnore
    public boolean isNoun() {
        return type.ordinal() >= PennType.NN.ordinal() && type.ordinal() <= PennType.NNPS.ordinal();
    }

    @JsonIgnore
    public boolean isAdjective() {
        return type.ordinal() >= PennType.JJ.ordinal() && type.ordinal() <= PennType.JJS.ordinal();
    }

    @JsonIgnore
    public boolean isAdverb() {
        return type.ordinal() >= PennType.RB.ordinal() && type.ordinal() <= PennType.RBS.ordinal();
    }

    @JsonIgnore
    public boolean isNormalVerb() {
        return type.ordinal() >= PennType.VB.ordinal() && type.ordinal() <= PennType.VBZ.ordinal() && !PennType.isAux(text);
    }

    @JsonIgnore
    public boolean isAuxVerb() {
        return type.ordinal() >= PennType.VB.ordinal() && type.ordinal() <= PennType.VBZ.ordinal() && PennType.isAux(text);
    }

    @JsonIgnore
    public boolean isValidPosTag() {
        return type != null && (isNoun() || isNormalVerb() || isAdjective() || isAdverb());
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getGrammarRuleName() {
        return grammarRuleName;
    }

    public void setGrammarRuleName(String grammarRuleName) {
        this.grammarRuleName = grammarRuleName;
    }

    public String getSemantic() {
        return semantic;
    }

    public void setSemantic(String semantic) {
        this.semantic = semantic;
    }

    public TokenizerConstants.Type getType() {
        return type;
    }

    public PennType getPennType() {
        return pennType;
    }

    public void setPennType(PennType pennType) {
        this.pennType = pennType;
    }

    public double getWordScore() {
        return wordScore;
    }

    public void setWordScore(double wordScore) {
        this.wordScore = wordScore;
    }

    public long getValue() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }

    public int getSynid() {
        return synid;
    }

    public void setSynid(int synid) {
        this.synid = synid;
    }

    // serialise into a byte array @ offset
    public void write( BinarySerializer serializer ) {
        serializer.writeByte(0xaa); // write magic marker
        if ( type == null ) {
            serializer.writeByte(0);
        } else {
            serializer.writeByte(1);
            serializer.writeString(type.toString());
        }

        if ( grammarRuleName == null ) {
            serializer.writeByte(0);
        } else {
            serializer.writeByte(1);
            serializer.writeString(grammarRuleName);
        }

        if ( text == null ) {
            serializer.writeByte(0);
        } else {
            serializer.writeByte(1);
            serializer.writeString(text);
        }

        if ( semantic == null ) {
            serializer.writeByte(0);
        } else {
            serializer.writeByte(1);
            serializer.writeString(semantic);
        }

        if ( pennType == null ) {
            serializer.writeByte(0);
        } else {
            serializer.writeByte(1);
            serializer.writeString(pennType.toString());
        }

        serializer.writeLong( value );
        serializer.writeInt( synid );
    }


    // serialise into a byte array @ offset
    public void read( BinarySerializer serializer ) throws IOException {
        int magicMarker = serializer.readByte();
        if ( (magicMarker & 0xff) != 0xaa ) {
            throw new IOException("invalid Token magic marker, invalid data");
        }
        int exists = serializer.readByte();
        if ( exists == 0 ) {
            type = null;
        } else {
            type = TokenizerConstants.Type.valueOf(serializer.readString());
        }

        exists = serializer.readByte();
        if ( exists == 0 ) {
            grammarRuleName = null;
        } else {
            grammarRuleName = serializer.readString();
        }

        exists = serializer.readByte();
        if ( exists == 0 ) {
            text = null;
        } else {
            text = serializer.readString();
        }

        exists = serializer.readByte();
        if ( exists == 0 ) {
            semantic = null;
        } else {
            semantic = serializer.readString();
        }

        exists = serializer.readByte();
        if ( exists == 0 ) {
            pennType = null;
        } else {
            pennType = PennType.valueOf(serializer.readString());
        }

        value = serializer.readLong();
        synid = serializer.readInt();
    }


}

