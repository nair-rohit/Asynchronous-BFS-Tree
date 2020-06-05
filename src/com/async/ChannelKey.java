package com.async;

import java.util.Objects;

public class ChannelKey {
    int source;
    int destination;

    public ChannelKey(int source, int destination) {
        this.source = source;
        this.destination = destination;
    }

    public int getSource() {
        return source;
    }

    public void setSource(int source) {
        this.source = source;
    }

    public int getDestination() {
        return destination;
    }

    public void setDestination(int destination) {
        this.destination = destination;
    }

    @Override
    public String toString() {
        return "[Source=" + source +
                ", destination=" + destination +
                "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChannelKey that = (ChannelKey) o;
        return source == that.source &&
                destination == that.destination;
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, destination);
    }
}
