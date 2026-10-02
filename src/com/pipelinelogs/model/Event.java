package com.pipelinelogs.model;

import java.util.HashMap;
import java.util.Map;

public class Event {

    private final int id;
    private final Map<String, String> data = new HashMap<>();
    private int score = 0;

    public Event(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public String get(String key) {
        return data.get(key);
    }

    public void put(String key, String value) {
        data.put(key, value);
    }

    public int getScore() {
        return score;
    }

    public void addScore(int points) {
        score += points;
    }
}
