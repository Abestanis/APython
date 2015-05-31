package com.apython.python.pythonhost;

/*
 * An enum that assigns each possible message of the Python host - Python app communication an
 * unique identifier.
 *
 * WARNING: We can only add new values, never change existing ones!
 *
 * Created by Sebastian on 31.05.2015.
 */

import java.util.HashMap;
import java.util.Map;

enum MessageIDs {
    PROTOCOL_VERSION_HANDSHAKE(0);


    private int id;
    MessageIDs(final int id) {
        this.id = id;
    }
    private static Map<Integer, MessageIDs> map = new HashMap<>();
    static {
        for (MessageIDs identifier : MessageIDs.values()) {
            map.put(identifier.id, identifier);
        }
    }
    public static MessageIDs valueOf(int identifier) {
        return map.get(identifier);
    }
    public int getId() {
        return this.id;
    }
}
