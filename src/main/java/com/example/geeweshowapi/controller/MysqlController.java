package com.example.geeweshowapi.controller;

import com.example.geeweshowapi.dao.Enity;
import com.example.geeweshowapi.model.Article;
import com.example.geeweshowapi.model.ArticleVersion;
import com.example.geeweshowapi.model.User;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.List;

public class MysqlController {
    public InputStream inputStream;
    public SqlSession session;
    private Enity enity;
    public void init() throws IOException {

        inputStream = Resources.getResourceAsStream("config.xml");
        SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(inputStream);
        session = factory.openSession();
        enity = session.getMapper(Enity.class);
    }

    public User findByUserId(String user_id){
        User user = enity.findByUserId(user_id);
        return user;
    }

    public void insertUserInfo(String user_id,String repository_path){
        enity.insertUserInfo(user_id,repository_path);
        session.commit();
    }

    public void updateUserInfo(String user_id,String repository_path){
        enity.updateUserInfo(user_id,repository_path);
        session.commit();
    }

    public void addArticle(String user_id,String repository_path, String article_title, Timestamp timestamp) {
        enity.insertArticle(user_id,repository_path,article_title,timestamp);
        session.commit();
    }

    public Article findByArticleTitle(String article_title) {
        Article article = enity.findByArticleTitle(article_title);
        return article;
    }

    public void addArticleVersion(Integer article_id, String commit_message, String commit_id, Timestamp timestamp) {
        enity.addArticleVersion(article_id,commit_message,commit_id,timestamp);
        session.commit();
    }

    public void deleteArticle(Integer id) {
        enity.deleteArticle(id);
        session.commit();
    }

    public void deleteArticleVersion(Integer id) {
        enity.deleteArticleVersion(id);
        session.commit();
    }

    public void addUser(String user_id, String repository_path) {
        enity.addUser(user_id,repository_path);
        session.commit();
    }

    public List<Article> findArticleByUserId(String user_id) {
        List<Article> articles = enity.findArticleByUserId(user_id);
        return articles;
    }

    public List<ArticleVersion> findArticleHistoryByArticleId(Integer id) {
        List<ArticleVersion> articleVersions = enity.findArticleHistoryByArticleId(id);
        return articleVersions;
    }

    public void deleteUser(String user_id) {
        enity.deleteUser(user_id);
        session.commit();
    }

    public Article findArticleByUserIdAndTitle(String user_id, String title) {
        Article article = enity.findArticleByUserIdAndTitle(user_id, title);
        return article;
    }
}
