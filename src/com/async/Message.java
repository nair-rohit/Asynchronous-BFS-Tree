package com.async;

public class Message {
    String text;
    int source;
    int destination;
    Integer distanceFromRoot;

    public Message(String text, int source, int destination, Integer distanceFromRoot) {
        this.text = text;
        this.source = source;
        this.destination = destination;
        this.distanceFromRoot = distanceFromRoot;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
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

    public Integer getDistanceFromRoot() {
        return distanceFromRoot;
    }

    public void setDistanceFromRoot(int distanceFromRoot) {
        this.distanceFromRoot = distanceFromRoot;
    }

    @Override
    public String toString() {
        return "[Source "+source+" -> " + text + "-> " + destination+"]";
    }
}
