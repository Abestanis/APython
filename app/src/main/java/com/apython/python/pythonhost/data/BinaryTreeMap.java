package com.apython.python.pythonhost.data;

import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Sebastian on 06.07.2017.
 */

public class BinaryTreeMap<Value> {
    private static final int DEFAULT_CAPACITY = 8;
    private int              size;
    private long[]           keys;
    private ArrayList<Value> values;
    
    public class Surrounding {
        private int index;
        public long lesserKey = -1, higherKey = -1;
        public Value lesserObject = null, higherObject = null;

        private Surrounding(int index) {
            this.index = index;
            if (index >= 0) {
                this.lesserKey = keys[index];
                this.lesserObject = values.get(index);
            }
            if (index < size - 1) {
                this.higherKey = keys[index + 1];
                this.higherObject = values.get(index + 1);
            }
        }
        
        public Surrounding getNext() {
            if (this.index >= size || this.higherKey == -1) return null;
            return new Surrounding(this.index + 1);
        }
        
        public Surrounding getPrev() {
            if (this.index <= 0 || this.lesserKey == -1) return null;
            return new Surrounding(this.index - 1);
        }
    }

    public BinaryTreeMap() {
        this(DEFAULT_CAPACITY);
    }
    
    public BinaryTreeMap(int capacity) {
        keys = new long[capacity];
        values = new ArrayList<>(capacity);
        size = 0;
    }
    
    public Surrounding getSurrounding(long key) {
        int index = Arrays.binarySearch(keys, 0, size, key);
        if (index >= 0) { // found key
            return new Surrounding(index);
        } else { // not found
            index = (index + 1) * -1;
            return new Surrounding(index - 1);
        }
    }
    
    public Value get(long key) {
        int index = Arrays.binarySearch(keys, 0, size, key);
        return index < 0 ? null : values.get(index);
    }
    
    public void put(long key, Value value) {
        int index = Arrays.binarySearch(keys, 0, size, key);
        if (index >= 0) { // found key
            values.set(index, value);
        } else { // not found
            index = (index + 1) * -1;
            if (size + 1 > keys.length) {
                long[] newKeys = new long[keys.length < DEFAULT_CAPACITY ?
                        DEFAULT_CAPACITY : (int) (keys.length * 1.5)];
                System.arraycopy(keys, 0, newKeys, 0, index);
                newKeys[index] = key;
                System.arraycopy(keys, index, newKeys, index + 1, size - index);
                keys = newKeys;
            } else {
                System.arraycopy(keys, index, keys, index + 1, size - index);
                keys[index] = key;
            }
            values.add(index, value);
            size++;
        }
    }
    
    public void clear() {
        size = 0;
        values.clear();
    }
    
    public Value remove(long key) {
        int index = Arrays.binarySearch(keys, 0, size, key);
        if (index < 0) throw new IllegalArgumentException("Key " + key + "does not exist");
        System.arraycopy(keys, index + 1, keys, index, size - (index + 1));
        size--;
        return values.remove(index);
    }
    
    public int size() {
        return size;
    }
}
