package com.example.geeweshowapi.dao;

import com.example.geeweshowapi.model.Article;
import com.example.geeweshowapi.model.ArticleVersion;
import com.example.geeweshowapi.model.User;

import java.sql.Timestamp;
import java.util.List;

public interface Enity {
    User findByUserId(String user_id);
    void insertUserInfo(String user_id, String repository_path);

    void updateUserInfo(String user_id, String repository_path);

    void insertArticle(String user_id,String repository_path, String article_title, Timestamp timestamp);

    Article findByArticleTitle(String article_title);

    void addArticleVersion(Integer article_id, String commit_message, String commit_id, Timestamp timestamp);

    void deleteArticle(Integer id);

    void deleteArticleVersion(Integer id);

    void addUser(String user_id, String repository_path);

    List<ArticleVersion> findArticleHistoryByArticleId(Integer id);

    void deleteUser(String user_id);

    List<Article> findArticleByUserId(String user_id);

    Article findArticleByUserIdAndTitle(String user_id, String article_title);
}
