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

import com.carrotsearch.hppc.IntHashSet;
import com.carrotsearch.hppc.procedures.IntProcedure;

import java.security.InvalidParameterException;
import java.util.*;

/**
 * Created by peter on 5/06/16.
 *
 * more efficient way of dealing with integers
 *
 */
public class IntList implements Comparable<IntList>, Iterable<Integer> {

    private int initial_size = 10;
    private int write_index = 0;
    private int[] data;

    public IntList() {
        data = new int[initial_size];
    }

    public IntList(int size) {
        initial_size = size;
        data = new int[size];
    }

    public IntList(int... values) {
        initial_size = values.length;
        data = new int[values.length];
        System.arraycopy(values, 0, data, 0, values.length);
        write_index = values.length;
    }

    public IntList(IntList other) {
        data = new int[other.write_index];
        System.arraycopy(other.data, 0, data, 0, other.write_index);
        write_index = other.write_index;
    }

    /**
     * check if value is insde this array
     * @param value the value to look for
     * @return true if it does
     */
    public boolean contains(int value) {
        for ( int i = 0; i < write_index; i++ ) {
            if ( data[i] == value ) {
                return true;
            }
        }
        return false;
    }

    /**
     * resize to initial size reset the write index
     * clears the array
     */
    public void reset() {
        data = new int[initial_size];
        write_index = 0;
    }

    /**
     * same as reset()
     */
    public void clear() {
        reset();
    }

    /**
     * sort the active part of the array
     */
    public void sort() {
        if ( write_index > 0 ) {
            Arrays.sort(data, 0, write_index);
        }
    }

    /**
     * add contents of set to this array
     * @param set the set to add
     */
    public void addAll(IntHashSet set){
        if ( set != null && set.size() > 0 ) {
            int size = set.size();
            resize(write_index + size);
            set.forEach(new IntProcedure() {
                @Override
                public void apply(int value) {
                    data[write_index++] = value;
                }
            });
        }
    }

    /**
     * get a unique list of this list - filter out duplicates
     */
    public IntList toUniqueList() {
        if ( write_index > 0 ) {
            IntHashSet set = new IntHashSet(write_index);
            IntList newList = new IntList(write_index);
            int[] raw = newList.getRawData();
            int index = 0;
            for ( int i = 0; i < write_index; i++ ) {
                if ( !set.contains(data[i]) ) {
                    set.add(data[i]);
                    raw[index++] = data[i];
                }
            }
            newList.write_index = index;
            return newList;
        }
        return new IntList();
    }

    /**
     * add contents of set to this array
     * @param integerList the list to add
     */
    public void addAll(Set<Integer> integerList){
        if ( integerList != null && integerList.size() > 0 ) {
            int size = integerList.size();
            resize(write_index + size);
            for ( int value : integerList ) {
                data[write_index++] = value;
            }
        }
    }

    public int get( int index ) {
        if ( index >= write_index ) {
            throw new InvalidParameterException("index out of bounds @ " + index + " (writer @ " + write_index + ")");
        }
        return data[index];
    }

    /**
     * add a list to the array if its not null or empty
     * @param list the list to add
     */
    public void add( IntList list ) {
        if ( list != null && list.size() > 0 ) {
            if (write_index + list.size() >= data.length) {
                resize(write_index + list.size());
            }
            for (int value : list) {
                data[write_index++] = value;
            }
        }
    }

    /**
     *  add a single value to the array - growth by 50% of needed
     * @param value the value to add
     */
    public void add( int value ) {
        if ( write_index >= data.length ) {
            resize(write_index + write_index / 2);
        }
        data[write_index++] = value;
    }

    /**
     * add an array of ints to the existing array if not empty or null
     * @param valueArray the items to add
     */
    public void add( int... valueArray ) {
        if ( valueArray != null && valueArray.length > 0 ) {
            if (write_index + valueArray.length >= data.length) {
                resize(write_index + valueArray.length);
            }
            System.arraycopy(valueArray, 0, data, write_index, valueArray.length);
            write_index = write_index + valueArray.length;
        }
    }

    /**
     * dynamically grow the array by a minimum of 50%
     * @param newSize the new size of the array, must be > the current size of the array
     */
    private void resize( int newSize ) {
        if ( newSize < write_index ) {
            throw new InvalidParameterException("invalid resize, new value less than existing value: " + write_index + ", (new = " + newSize + ")");
        }
        if ( newSize >= data.length ) { // exceeds size?
            if (newSize < write_index + write_index / 2) { // min of 50% growth
                newSize = write_index + write_index / 2;
            }
            int[] newData = new int[newSize];
            System.arraycopy(data, 0, newData, 0, data.length);
            data = newData;
        }
    }

    /**
     * @return the size of the array (not its in memory size, but how many items it contains
     */
    public int size() {
        return write_index;
    }

    /**
     * @return the raw contents (underlying data structure) of this int list, this list might contain
     * more items than have been put into the list thusfar
     */
    public int[] getRawData() {
        return data;
    }

    /**
     * @return the exact number of items in this list
     */
    public int[] getData() {
        if ( write_index > 0 ) {
            return getData(0, write_index);
        }
        return new int[0];
    }

    /**
     * @return the data of the array from offset to size
     */
    public int[] getData(int offset, int size) {
        if ( size <= 0 || offset + size > write_index ) {
            throw new InvalidParameterException("invalid index/size for array get @ " + offset + ", for size " + size + ", array @ " + write_index);
        }
        int[] returnData = new int[size];
        System.arraycopy(data, offset, returnData, 0, size);
        return returnData;
    }

    /**
     * @return convert the int[] to a IntList
     */
    public IntList asList() {
        IntList list = new IntList(write_index);
        for ( int i = 0; i < write_index; i++ ) {
            list.add(data[i]);
        }
        return list;
    }

    /**
     * @return convert the int[] to a IntHashSet
     */
    public IntHashSet asHashSet() {
        IntHashSet set = new IntHashSet(write_index);
        for ( int i = 0; i < write_index; i++ ) {
            set.add(data[i]);
        }
        return set;
    }

    // sort by size of the array
    @Override
    public int compareTo(IntList o) {
        if ( write_index < o.write_index ) return 1;
        if ( write_index > o.write_index ) return -1;
        return 0;
    }

    /**
     * implement an iterator for this array
     */
    private class IntIterator implements Iterator<Integer> {
        private int size;
        private int offset;
        private int[] data;

        private IntIterator(int[] data, int offset, int size) {
            this.data = data;
            this.offset = offset;
            this.size = size;
        }

        public boolean hasNext() {
            return offset < size;
        }

        public Integer next() {
            if( offset < size) {
                return data[offset++];
            }
            throw new NoSuchElementException();
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }


    @Override
    public Iterator<Integer> iterator() {
        return new IntIterator(data, 0, write_index);
    }

}

