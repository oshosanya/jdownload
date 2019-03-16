package com.oshosanya.jdownload.observer;

public interface Subject {
    void notifyObserver();
    void register(Observer o);
    void unregister(Observer o);
}
