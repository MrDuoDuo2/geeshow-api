package com.example.geeweshowapi;

public interface Enity {
    User findByUserId(String user_id);
    void insertUserInfo(String user_id, String git_id);
}
