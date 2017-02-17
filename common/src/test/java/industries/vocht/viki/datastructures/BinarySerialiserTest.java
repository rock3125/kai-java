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

package industries.vocht.viki.datastructures;

import industries.vocht.viki.utility.BinarySerializer;
import org.junit.Test;
import org.springframework.util.Assert;

import java.io.IOException;

/**
 * Created by peter on 27/04/16.
 *
 *
 */
public class BinarySerialiserTest {

    @Test
    public void testSerialiser1() throws Exception {
        BinarySerializer serializer = new BinarySerializer();

        addString( serializer, "Test string 1");
        addLong( serializer, 1234123123123L);

        testString(serializer, "Test string 1");
        testLong( serializer, 1234123123123L);
    }

    @Test
    public void testSerialiser2() throws Exception {
        BinarySerializer serializer = new BinarySerializer();

        addString(serializer, "Test string 2");
        addLong(serializer, 1234123123123L);
        addLong(serializer, 4123123123L);
        addDouble(serializer, 12.3);
        addFloat(serializer, 33.5f);
        addInt(serializer, 31415);

        testString(serializer, "Test string 2");
        testLong(serializer, 1234123123123L);
        testLong(serializer, 4123123123L);
        testDouble(serializer, 12.3);
        testFloat(serializer, 33.5f);
        testInt(serializer, 31415);
    }



    private void addString( BinarySerializer serializer, String text ) {
        serializer.writeString(text);
    }

    private void addLong( BinarySerializer serializer, long value ) {
        serializer.writeLong(value);
    }

    private void addDouble( BinarySerializer serializer, double value ) {
        serializer.writeDouble(value);
    }

    private void addFloat( BinarySerializer serializer, float value ) {
        serializer.writeFloat(value);
    }

    private void addInt( BinarySerializer serializer, int value ) {
        serializer.writeInt(value);
    }

    private void testString( BinarySerializer serializer, String text ) throws IOException {
        String str = serializer.readString();
        Assert.notNull(str);
        Assert.isTrue( str.equals(text) );
    }

    private void testLong( BinarySerializer serializer, long value ) throws IOException {
        long  l = serializer.readLong();
        Assert.isTrue( l == value );
    }

    private void testDouble( BinarySerializer serializer, double value ) throws IOException {
        double d = serializer.readDouble();
        Assert.isTrue( d == value );
    }

    private void testFloat( BinarySerializer serializer, float value ) throws IOException {
        float f = serializer.readFloat();
        Assert.isTrue( f == value );
    }

    private void testInt( BinarySerializer serializer, int value ) throws IOException {
        int i = serializer.readInt();
        Assert.isTrue( i == value );
    }


}
