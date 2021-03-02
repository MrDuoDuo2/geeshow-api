package com.example.geeweshowapi.controller;

import com.example.geeweshowapi.dao.Enity;
import com.example.geeweshowapi.model.User;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
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

    public List<User> findByUserId(String user_id){
        List<User> user = enity.findByUserId(user_id);
        return user;
    }

    public void insertUserInfo(String user_id,String git_id){
        enity.insertUserInfo(user_id,git_id);
        session.commit();
    }

    public void updateUserInfo(String user_id,String git_id){
        enity.updateUserInfo(user_id,git_id);
        session.commit();
    }

}
