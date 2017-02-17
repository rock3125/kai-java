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

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * A class representing a mathematical vector. Supports basic vector operations like add, multiply,
 * divide etc.
 *
 */
public class Vector {
    private final double[] elements;

    /**
     * Construct a Vector with size elements.
     */
    public Vector(int size) {
        elements = new double[size];
    }

    /**
     * Construct a Vector with size elements from a count-map
     */
    public Vector(int size, HashMap<String, Integer> countMap) {
        elements = new double[size];
        for ( String key : countMap.keySet() ) {
            double value = (double)countMap.get(key);
            set( hashWord(key), value );
        }
    }

    /**
     * Construct a Vector with size elements.
     */
    public Vector(double[] data) {
        elements = Arrays.copyOf(data, data.length);
    }

    /**
     * Construct a Vector by copying the elements of the provided Vector.
     */
    public Vector(Vector vector) {
        elements = Arrays.copyOf(vector.elements, vector.elements.length);
    }

    /**
     * Add the provided Vector to this Vector.
     */
    public void add(Vector operand) {
        for (int i = 0; i < elements.length; i++) {
            elements[i] = elements[i] + operand.elements[i];
        }
    }

    /**
     * is this vector all zeros?
     */
    public boolean isZero() {
        for (double element : elements) {
            if ( element != 0.0 ) {
                return false;
            }
        }
        return true;
    }

    /**
     * copy values from other into this vector
     * @param other the other vector
     */
    public void copyValues( Vector other ) {
        System.arraycopy(other.elements, 0, elements, 0, elements.length);
    }

    public double length() {
        double total = 0.0;
        for (double element : elements) {
            total = total + element * element;
        }
        return Math.sqrt(total);
    }

    /**
     * Divide this Vector by the provided divisor.
     */
    public Vector divide(double divisor) {
        Vector result = new Vector(size());
        for (int i = 0; i < elements.length; i++) {
            result.set(i, get(i) / divisor);
        }
        return result;
    }

    /**
     * Divide this Vector by the provided divisor.
     */
    public void divideThis(double divisor) {
        if ( divisor != 0.0 ) {
            for (int i = 0; i < elements.length; i++) {
                elements[i] = elements[i] / divisor;
            }
        } else {
            clear();
        }
    }

    /**
     * set the vector (all elements) to 0.0
     */
    public void clear() {
        for (int i = 0; i < elements.length; i++) {
            elements[i] = 0.0;
        }
    }

    /**
     * Get the element of this Vector at the specified index.
     */
    public double get(int i) {
        return elements[i];
    }

    /**
     * Apply elementwise increment to specified element of this Vector.
     */
    public void increment(int i) {
        set(i, get(i) + 1);
    }

    /**
     * Calculate the inner product of this Vector with the provided Vector.
     */
    public double innerProduct(Vector vector) {
        double innerProduct = 0;
        for (int i = 0; i < elements.length; i++) {
            innerProduct += get(i) * vector.get(i);
        }
        return innerProduct;
    }

    /**
     * access the data inside the vector
     * @return the data
     */
    public List<Double> getAsList() {
        List<Double> list = new ArrayList<>();
        for (double element : elements) {
            list.add(element);
        }
        return list;
    }

    /**
     * access the data inside the vector
     * @return the data
     */
    public double[] getAsArray() {
        return elements;
    }

    /**
     * Apply element-wise inversion (1/x) to this Vector.
     */
    public Vector invert() {
        Vector result = new Vector(size());
        for (int i = 0; i < elements.length; i++) {
            result.set(i, 1 / get(i));
        }
        return result;
    }

    /**
     * Apply element-wise log(x) to this.
     */
    public Vector log() {
        Vector result = new Vector(size());
        for (int i = 0; i < elements.length; i++) {
            result.set(i, Math.log(get(i)));
        }
        return result;
    }

    /**
     * Return maximal element.
     */
    public double max() {
        double maxValue = Double.MIN_VALUE;
        for (int i = 0; i < elements.length; i++) {
            maxValue = Math.max(maxValue, get(i));
        }
        return maxValue;
    }

    /**
     * Multiply this with the provided scalar multiplier.
     */
    public Vector multiply(double multiplier) {
        Vector result = new Vector(size());
        for (int i = 0; i < elements.length; i++) {
            result.set(i, get(i) * multiplier);
        }
        return result;
    }

    /**
     * Multiply this with the provided vector multiplier.
     */
    public Vector multiply(Vector multiplier) {
        if (multiplier == null || multiplier.size() != size()) {
            throw new InvalidParameterException("vector sizes must match");
        }
        Vector result = new Vector(size());
        for (int i = 0; i < elements.length; i++) {
            if (get(i) == 0 || multiplier.get(i) == 0) {
                result.set(i, 0);
            } else {
                result.set(i, multiplier.get(i) * get(i));
            }
        }
        return result;
    }

    /**
     * Calculate the length of this vector
     */
    public double norm() {
        double normSquared = 0.0;
        for (int i = 0; i < elements.length; i++) {
            normSquared += get(i) * get(i);
        }
        return Math.sqrt(normSquared);
    }

    /**
     * Set the specified element of this.
     */
    public void set(int i, double value) {
        elements[i] = value;
    }

    /**
     * Return the number of elements in this.
     */
    public int size() {
        return elements.length;
    }

    /**
     * Hash word into integer between 0 and numFeatures - 1. Used to form document feature vector.
     */
    private int hashWord(String word) {
        return Math.abs(word.toLowerCase().hashCode()) % elements.length;
    }

    @Override
    public String toString() {
        return Arrays.toString(elements);
    }

}
