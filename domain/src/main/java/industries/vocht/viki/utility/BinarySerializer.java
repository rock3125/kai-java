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

import java.io.IOException;

/*
 * Created by peter on 29/11/14.
 *
 * write data into a buffer and read data from a buffer
 *
 */
public class BinarySerializer  {

    private final static int INITIAL_SIZE = 32768;

    private int initialSize;
    private int writeIndex;
    private int readIndex;
    private byte[] data;

    public BinarySerializer() {
        this.initialSize = INITIAL_SIZE;
        this.data = new byte[INITIAL_SIZE];
        this.writeIndex = 0;
        this.readIndex = 0;
    }

    public BinarySerializer( int initialSize ) {
        if ( initialSize < 1 )
            initialSize = INITIAL_SIZE;

        this.initialSize = initialSize;
        this.data = new byte[initialSize];
        this.writeIndex = 0;
        this.readIndex = 0;
    }

    // initialise a serialiser with a COPY of data
    // and set its write index to the end, and its read index to the start of this block
    public BinarySerializer( byte[] data ) {
        initialSize = data.length;
        this.data = new byte[data.length];
        System.arraycopy( data, 0, this.data, 0, data.length );
        this.writeIndex = data.length;
        this.readIndex = 0;
    }

    public void writeInt( int value ) {
        ensure(4);
        data[writeIndex++] = (byte)(value & 0xff);
        data[writeIndex++] = (byte)((value >>> 8) & 0xff);
        data[writeIndex++] = (byte)((value >>> 16) & 0xff);
        data[writeIndex++] = (byte)((value >>> 24) & 0xff);
    }

    public void writeLong( long value ) {
        ensure(8);
        data[writeIndex++] = (byte)(value & 0xff);
        data[writeIndex++] = (byte)((value >>> 8) & 0xff);
        data[writeIndex++] = (byte)((value >>> 16) & 0xff);
        data[writeIndex++] = (byte)((value >>> 24) & 0xff);
        data[writeIndex++] = (byte)((value >>> 32) & 0xff);
        data[writeIndex++] = (byte)((value >>> 40) & 0xff);
        data[writeIndex++] = (byte)((value >>> 48) & 0xff);
        data[writeIndex++] = (byte)((value >>> 56) & 0xff);
    }

    public void writeFloat( float value ) {
        writeInt(Float.floatToIntBits(value));
    }

    public void writeDouble( double value ) {
        writeLong(Double.doubleToLongBits(value));
    }

    public void writeByte( int value ) {
        ensure(1);
        data[writeIndex++] = (byte)(value & 0xff);
    }

    public void writeString( String str ) {
        int size = str.getBytes().length;
        ensure(size + 4);
        writeInt( size );
        System.arraycopy(str.getBytes(), 0, data, writeIndex, size);
        writeIndex += size;
    }

    public void writeByteArray( byte[] value, int start, int length ) {
        ensure(length + 4);
        writeInt( length );
        System.arraycopy( value, start, data, writeIndex, length);
        writeIndex += length;
    }

    public void writeRawByteArray( byte[] value, int start, int length ) {
        ensure(length);
        System.arraycopy( value, start, data, writeIndex, length);
        writeIndex += length;
    }

    public int readInt() throws IOException {
        if ( readIndex + 4 > writeIndex )
            throw new IOException("reading past end of buffer @ " + readIndex);

        return (data[readIndex++] & 0xff) +
                ((data[readIndex++] & 0xff) << 8) +
                ((data[readIndex++] & 0xff) << 16) +
                ((data[readIndex++] & 0xff) << 24);
    }

    public long readLong() throws IOException {
        if ( readIndex + 8 > writeIndex )
            throw new IOException("reading past end of buffer @ " + readIndex);

        return (long)(data[readIndex++] & 0xff) +
                (((long)(data[readIndex++] & 0xff)) << 8) +
                (((long)(data[readIndex++] & 0xff)) << 16) +
                (((long)(data[readIndex++] & 0xff)) << 24) +
                (((long)(data[readIndex++] & 0xff)) << 32) +
                (((long)(data[readIndex++] & 0xff)) << 40) +
                (((long)(data[readIndex++] & 0xff)) << 48) +
                (((long)(data[readIndex++] & 0xff)) << 56);
    }

    public float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }

    public double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }

    public int readByte() throws IOException {
        if ( readIndex + 1 > writeIndex )
            throw new IOException("reading past end of buffer @ " + readIndex);

        return (int)data[readIndex++];
    }

    public byte[] readByteArray() throws IOException {
        int size = readInt();
        if ( size < 0 ) {
            throw new IOException("invalid size field value @ " + readIndex + ", size " + size);
        }
        if ( readIndex + size > writeIndex )
            throw new IOException("reading past end of buffer @ " + readIndex + ", size " + size);

        if ( size > 0 ) {
            byte[] array = new byte[size];
            System.arraycopy(data, readIndex, array, 0, size);
            readIndex = readIndex + size;
            return array;
        }
        return null;
    }

    public String readString() throws IOException {
        byte[] stringArray = readByteArray();
        if ( stringArray != null && stringArray.length > 0 )
            return new String(stringArray);
        return null;
    }

    // grow the buffer?
    private void ensure( int size ) {
        if ( writeIndex + size >= data.length ) {
            byte[] newData = new byte[ writeIndex + size ];
            System.arraycopy(data, 0, newData, 0, writeIndex );
            data = newData;
        }
    }

    // get the data from the serializer in a block that exactly fits
    public byte[] getData() {
        byte[] dataArray = new byte[writeIndex];
        System.arraycopy(data, 0, dataArray, 0, writeIndex);
        return dataArray;
    }

    // get the size of the data block
    public int getSize() {
        return writeIndex;
    }

    // reset the memory used and the indexes of the system
    public void reset() {
        data = new byte[initialSize];
        writeIndex = 0;
        readIndex = 0;
    }
}
