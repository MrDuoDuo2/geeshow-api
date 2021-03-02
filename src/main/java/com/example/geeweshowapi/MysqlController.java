package com.example.geeweshowapi;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;

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

    public void insertUserInfo(String user_id,String git_id){
        enity.insertUserInfo(user_id,git_id);
        session.commit();
    }

}
