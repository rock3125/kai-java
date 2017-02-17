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

package industries.vocht.viki.model.semantics;

import industries.vocht.viki.model.Token;
import industries.vocht.viki.utility.BinarySerializer;

import java.io.IOException;
import java.util.*;

/**
 * Created by peter on 8/06/16.
 *
 * a tuple of grammar cases for statements
 *
 */
public class Tuple {

    // reserved metadata items for case tuples
    public static final String META_TUPLE = "{tuple}";

    public static final int META_C_ALL = 0;     // no filtering, anything goes
    public static final int META_C_WHO = 1;     // tuple contains person or PNP
    public static final int META_C_WHERE = 2;   // tuple contains location
    public static final int META_C_WHEN = 4;    // tuple contains time
    public static final int META_C_HOW = 8;     // tuple contains how
    public static final int META_C_WHAT = 16;   // tuple contains what

    private UUID organisation_id; // owner
    private UUID id; // unique id of this tuple

    private String url; // the owner document url
    private int sentence_id; // the index / id of the sentence_id this came from

    private TupleTree root; // the root of the tuple

    public Tuple() {
    }

    public void add(String srl, int offset, List<Token> tokenList) {
        if ( srl != null && tokenList != null && tokenList.size() > 0 ) {
            if ( root == null ) {
                root = new TupleTree(srl, offset, tokenList);
            } else {
                root.add(srl, offset, tokenList);
            }
        }
    }

    public void add(String srl, int offset, Token token) {
        if ( srl != null && token != null ) {
            List<Token> tokenList = new ArrayList<>();
            tokenList.add(token);
            if ( root == null ) {
                root = new TupleTree(srl, offset, tokenList);
            } else {
                root.add(srl, offset, tokenList);
            }
        }
    }

    // pretty print
    public String toString() {
        if ( root != null ) {
            return root.subTreeToString(" ");
        } else {
            return "";
        }
    }

    /**
     * convert a tuple to a UI friendly display item
     * @return the converted tuple
     */
    public TupleResult convert() {
        TupleResult result = new TupleResult();
        result.setUrl(url);
        result.setSentence(sentence_id);
        result.setTupleText(toString());
        if ( root != null ) {
            result.setVerb(root.toString());
        }
        return result;
    }

    /**
     * @return a deep copy of this item
     */
    public Tuple copy() {
        Tuple t = new Tuple();
        t.setOrganisation_id(getOrganisation_id());
        t.setSentence_id(getSentence_id());
        t.setId(getId());
        t.setUrl(getUrl());
        if ( root != null ) {
            t.setRoot(TupleTree.copy(root));
        }
        return t;
    }

    public UUID getOrganisation_id() {
        return organisation_id;
    }

    public void setOrganisation_id(UUID organisation_id) {
        this.organisation_id = organisation_id;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getSentence_id() {
        return sentence_id;
    }

    public void setSentence_id(int sentence_id) {
        this.sentence_id = sentence_id;
    }

    public TupleTree getRoot() {
        return root;
    }

    public void setRoot(TupleTree root) {
        this.root = root;
    }

    // serialise into a byte array @ offset
    public void write( BinarySerializer serializer ) {
        serializer.writeByte(0xfa); // write magic marker

        if ( organisation_id == null ) {
            serializer.writeByte(0);
        } else {
            serializer.writeByte(1);
            serializer.writeString(organisation_id.toString());
        }

        if ( id == null ) {
            serializer.writeByte(0);
        } else {
            serializer.writeByte(1);
            serializer.writeString(id.toString());
        }

        if ( url == null ) {
            serializer.writeByte(0);
        } else {
            serializer.writeByte(1);
            serializer.writeString(url);
        }

        serializer.writeInt(sentence_id);

        if ( root == null ) {
            serializer.writeByte(0);
        } else {
            serializer.writeByte(1);
            root.write(serializer);
        }
    }


    // read from byte array @ offset
    public void read( BinarySerializer serializer ) throws IOException {
        int magicMarker = serializer.readByte();
        if ( (magicMarker & 0xff) != 0xfa ) {
            throw new IOException("invalid Tuple magic marker, invalid data");
        }
        int exists = serializer.readByte();
        if ( exists == 0 ) {
            organisation_id = null;
        } else {
            organisation_id = UUID.fromString(serializer.readString());
        }

        exists = serializer.readByte();
        if ( exists == 0 ) {
            id = null;
        } else {
            id = UUID.fromString(serializer.readString());
        }

        exists = serializer.readByte();
        if ( exists == 0 ) {
            url = null;
        } else {
            url = serializer.readString();
        }

        sentence_id = serializer.readInt();

        exists = serializer.readByte();
        if ( exists == 0 ) {
            root = null;
        } else {
            root = new TupleTree();
            root.read(serializer);
        }
    }


}

