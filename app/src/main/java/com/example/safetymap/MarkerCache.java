package com.example.safetymap;

import com.google.android.gms.maps.model.Marker;

import java.util.LinkedList;

/**
 * The MarkerCache object is simply a queue that can only store
 * the specified number of google Markers.  If a new marker is added
 * the other Marker is removed the map and the MarkerCache
 */
public class MarkerCache<E> extends LinkedList<E> {

    private int limit;

    public MarkerCache(int limit) {
        this.limit = limit;
    }

    @Override
    public boolean add(E o) {
        if (o.getClass() == Marker.class) {
            super.add(o);
            while(this.size() > limit) {
                Marker m = (Marker) super.remove();
                m.remove();
            }
            return true;
        }
        return false;
    }
}
