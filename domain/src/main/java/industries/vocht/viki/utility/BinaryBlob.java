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

/*
 * Created by peter on 29/11/14.
 *
 * write data into a buffer and read data from a buffer
 *
 */
public class BinaryBlob {

    // a preset initial size for the void constructor
    private final static int INITIAL_SIZE = 32768;

    private int writeIndex; // how many bytes have been written into this buffer
    private int initialSize; // the initial size of the byte[] data
    private byte[] data; // the data container for this buffer

    public BinaryBlob() {
        initialSize = INITIAL_SIZE;
        data = new byte[INITIAL_SIZE];
        writeIndex = 0;
    }

    /**
     * create a blob of specified size
     * @param initialCapacity the capacity for the buffer
     */
    public BinaryBlob(int initialCapacity) {
        if ( initialCapacity < 1 )
            initialCapacity = INITIAL_SIZE;
        initialSize = initialCapacity;

        data = new byte[initialCapacity];
        writeIndex = 0;
    }

    /**
     * write an integer into the buffer (4 bytes) and grow the buffer if
     * it is too small
     * @param value the int to write into the buffer
     */
    public void writeInt( int value ) {
        ensure(4);
        data[writeIndex++] = (byte)(value & 0xff);
        data[writeIndex++] = (byte)((value >>> 8) & 0xff);
        data[writeIndex++] = (byte)((value >>> 16) & 0xff);
        data[writeIndex++] = (byte)((value >>> 24) & 0xff);
    }

    /**
     * write a byte into the buffer (1 byte) and grow the buffer if
     * it is too small
     * @param value the byte to write (0xff of the int) into the buffer
     */
    public void writeByte( int value ) {
        ensure(1);
        data[writeIndex++] = (byte)(value & 0xff);
    }

    /**
     * write an array specified by the limits into this buffer
     * grow the buffer it is too small
     * @param value the buffer to copy / write
     * @param start the start into the buffer (offset in bytes)
     * @param length the length starting at offset to take from the buffer (in bytes)
     */
    public void writeByteArray( byte[] value, int start, int length ) {
        ensure(length);
        System.arraycopy(value, start, data, writeIndex, length);
        writeIndex += length;
    }

    /**
     * ensure the buffer can take another size bytes - auto-grow
     * copying existing bytes into a new internal buffer on grow
     * @param size the number of additional bytes to write
     */
    private void ensure( int size ) {
        if ( writeIndex + size >= data.length ) {
            byte[] newData = new byte[ writeIndex + size ];
            System.arraycopy(data, 0, newData, 0, writeIndex );
            data = newData;
        }
    }

    /**
     * return a new buffer of the exact size for the amount of information
     * written into this buffer - this returns a copy of the internal buffer
     * @return a copy of the internal buffer at the right size for the amount of information contained
     */
    public byte[] getData() {
        byte[] dataArray = new byte[writeIndex];
        System.arraycopy(data, 0, dataArray, 0, writeIndex);
        return dataArray;
    }

    /**
     * return the size of the amount of information written into the buffer
     * @return the number of bytes written into the buffer
     */
    public int getSize() {
        return writeIndex;
    }

    /**
     * reset the buffer by clearing the number of bytes written
     * does not clear/zero the buffer itself but resets it fast
     */
    public void clear() {
        writeIndex = 0;
    }

    /**
     * clear the buffer (see clear()) and reset it to its initial size
     * if it has exceeded that size
     */
    public void shrink() {
        writeIndex = 0;
        if ( data.length > initialSize )
            data = new byte[initialSize];
    }

    /**
     * @return the initial size of the buffer when it was created
     */
    public int getInitialSize() {
        return initialSize;
    }

}

