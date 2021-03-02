package com.example.geeweshowapi.dao;

import com.example.geeweshowapi.model.User;

import java.util.List;

public interface Enity {
    List<User> findByUserId(String user_id);
    void insertUserInfo(String user_id, String git_id);

    void updateUserInfo(String user_id, String git_id);
}
