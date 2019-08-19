package com.github.terminatornl.tiquality.api;

public class Location<K extends Comparable, V extends Comparable> {

    private final K world;
    private final V pos;

    public Location(K world, V pos) {
        this.world = world;
        this.pos = pos;
    }

    public K getWorld() {
        return world;
    }

    public V getPos() {
        return pos;
    }
}
