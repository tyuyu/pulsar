/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.pulsar.common.util.collections;

import java.util.Collection;
import java.util.Set;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;

/**
 * A set comprising zero or more ranges type of key-value pair.
 */
public interface LongPairRangeSet<T extends Comparable<T>> {

    /**
     * Adds the specified range (range that contains all values strictly greater than {@code
    * lower} and less than or equal to {@code upper}.) to this {@code RangeSet} (optional operation). That is, for equal
     * range sets a and b, the result of {@code a.add(range)} is that {@code a} will be the minimal range set for which
     * both {@code a.enclosesAll(b)} and {@code a.encloses(range)}.
     * 
     * <pre>
     *  
     * &#64;param lowerKey :  value for key of lowerEndpoint of Range
     * &#64;param lowerValue: value for value of lowerEndpoint of Range
     * &#64;param upperKey  : value for key of upperEndpoint of Range
     * &#64;param upperValue: value for value of upperEndpoint of Range
     * </pre>
     */
    void addOpenClosed(long lowerKey, long lowerValue, long upperKey, long upperValue);

    /** Determines whether any of this range set's member ranges contains {@code value}. */
    boolean contains(long key, long value);

    /**
     * Returns the unique range from this range set that {@linkplain Range#contains contains} {@code value}, or
     * {@code null} if this range set does not contain {@code value}.
     */
    Range<T> rangeContaining(long key, long value);

    /**
     * Remove range that contains all values less than or equal to given key-value.
     * 
     * @param key
     * @param value
     */
    void removeAtMost(long key, long value);

    boolean isEmpty();

    void clear();

    /**
     * Returns the minimal range which {@linkplain Range#encloses(Range) encloses} all ranges in this range set.
     * 
     * @return
     */
    Range<T> span();

    /**
     * Returns a view of the {@linkplain Range#isConnected disconnected} ranges that make up this range set.
     * 
     * @return
     */
    Collection<Range<T>> asRanges();
    
    /**
     * Performs the given action for each entry in this map until all entries have been processed or action returns "false".
     * Unless otherwise specified by the implementing class, actions are performed in the order of entry
     * set iteration (if an iteration order is specified.)
     * 
     * @param action
     */
    void forEach(RangeProcessor<T> action);
    
    /**
     * Performs the given action for each entry in this map until all entries have been processed or action returns "false".
     * Unless otherwise specified by the implementing class, actions are performed in the order of entry
     * set iteration (if an iteration order is specified.)
     * 
     * @param action
     * @param consumer
     */
    void forEach(RangeProcessor<T> action, LongPairConsumer<? extends T> consumer);
    
    /**
     * Returns total number of ranges into the set.
     * 
     * @return
     */
    int size();

    /**
     * It returns very first smallest range in the rangeSet.
     * 
     * @return Range<T> first smallest range into the set
     */
    Range<T> firstRange();

    public static interface LongPairConsumer<T> {
        T apply(long key, long value);
    }

    public static interface RangeProcessor<T extends Comparable<T>> {
        /**
         * 
         * @param range
         * @return false if there is no further processing required
         */
        boolean process(Range<T> range);
    }
    
    public static class LongPair implements Comparable<LongPair> {

        public static final LongPair earliest = new LongPair(-1, -1);
        public static final LongPair latest = new LongPair(Integer.MAX_VALUE, Integer.MAX_VALUE);

        private long key;
        private long value;

        public LongPair(long key, long value) {
            this.key = key;
            this.value = value;
        }

        public long getKey() {
            return this.key;
        }

        public long getValue() {
            return this.value;
        }

        @Override
        public int compareTo(LongPair o) {
            return ComparisonChain.start().compare(key, o.getKey()).compare(value, o.getValue()).result();
        }

        @Override
        public String toString() {
            return String.format("%d:%d", key, value);
        }
    }

    public static class DefaultRangeSet<T extends Comparable<T>> implements LongPairRangeSet<T> {

        RangeSet<T> set = TreeRangeSet.create();

        private final LongPairConsumer<T> consumer;

        public DefaultRangeSet(LongPairConsumer<T> consumer) {
            this.consumer = consumer;
        }

        @Override
        public void clear() {
            set.clear();
        }

        @Override
        public void addOpenClosed(long key1, long value1, long key2, long value2) {
            set.add(Range.openClosed(consumer.apply(key1, value1), consumer.apply(key2, value2)));
        }

        public boolean contains(T position) {
            return set.contains(position);
        }

        public Range<T> rangeContaining(T position) {
            return set.rangeContaining(position);
        }

        @Override
        public Range<T> rangeContaining(long key, long value) {
            return this.rangeContaining(consumer.apply(key, value));
        }

        public void remove(Range<T> range) {
            set.remove(range);
        }

        @Override
        public void removeAtMost(long key, long value) {
            set.remove(Range.atMost(consumer.apply(key, value)));
        }

        @Override
        public boolean isEmpty() {
            return set.isEmpty();
        }

        @Override
        public Range<T> span() {
            return set.span();
        }

        @Override
        public Set<Range<T>> asRanges() {
            return set.asRanges();
        }

        @Override
        public void forEach(RangeProcessor<T> action) {
            forEach(action, consumer);
        }

        @Override
        public void forEach(RangeProcessor<T> action, LongPairConsumer<? extends T> consumer) {
            for (Range<T> range : asRanges()) {
                if (!action.process(range)) {
                    break;
                }
            }
        }
        
        @Override
        public boolean contains(long key, long value) {
            return this.contains(consumer.apply(key, value));
        }

        @Override
        public Range<T> firstRange() {
            return set.asRanges().iterator().next();
        }

        @Override
        public int size() {
            return set.asRanges().size();
        }

        @Override
        public String toString() {
            return set.toString();
        }
    }
}
