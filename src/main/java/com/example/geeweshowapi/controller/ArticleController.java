package com.example.geeweshowapi.controller;

import com.example.geeweshowapi.Provider.SignatrueProvider;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import redis.clients.jedis.Jedis;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

@RestController
public class ArticleController {

    @Value("${git.path}")
    private String git_path;

    @PostMapping(value = "/add")
    public String add(@RequestParam HashMap<String, String> params, @RequestBody String body) throws IOException, GitAPIException, InvalidKeyException, NoSuchAlgorithmException {
        String title = params.get("Title");
        String user_id = params.get("UserId");

        String fileName = title + ".asc";
        String filePath = String.format("/tmp/%s.git/%s", user_id, fileName);

        //连接redis
        Jedis jedis = new Jedis("192.168.2.39");
        String redis_token = jedis.get(user_id);

        //签名
        SignatrueProvider signatrueProvider = new SignatrueProvider();
        StringBuilder signToString = signatrueProvider.createSignString(params);
        String signature = signatrueProvider.createSignature(redis_token, signToString);

        //鉴权
        if (params.get("Signature").equals(signature)) {
            Git git = null;
            try {
                git = Git.open(new File(String.format("%s/%s.git/.git", git_path, user_id)));

            } catch (RepositoryNotFoundException e) {
                Repository newlyCreatedRepo = FileRepositoryBuilder.create(
                        new File(String.format("%s/%s.git/.git", git_path, user_id)));
                newlyCreatedRepo.create();
            }


            //添加文件
            File file = new File(filePath);
            //创建文件
            try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {

                if (!file.exists()) {
                    file.createNewFile();
                } else {
                    return "文件存在";
                }

                // get the content in bytes
                byte[] contentInBytes = body.getBytes();

                fileOutputStream.write(contentInBytes);
                fileOutputStream.flush();
                fileOutputStream.close();

                System.out.println("Done");

            } catch (IOException e) {
                e.printStackTrace();
            }

            //获取仓库
            git.add().addFilepattern(fileName).call();
            RevCommit commit = git.commit().setMessage("addFile " + fileName).call();
        } else {
            return "鉴权失败";
        }

        return "保存成功";
    }


    @GetMapping(value = "/delete")
    public String delete(@RequestParam HashMap<String, String> params) throws IOException, GitAPIException, InvalidKeyException, NoSuchAlgorithmException {
        String title = params.get("Title");
        String user_id = params.get("UserId");

        String fileName = title + ".asc";
        String filePath = String.format("%s/%s.git/%s", git_path, user_id, fileName);

        //连接redis
        Jedis jedis = new Jedis("192.168.2.39");
        String redis_token = jedis.get(user_id);

        //生成签名
        SignatrueProvider signatrueProvider = new SignatrueProvider();
        StringBuilder signToString = signatrueProvider.createSignString(params);
        String signature = signatrueProvider.createSignature(redis_token, signToString);

        //鉴权
        if (params.get("Signature").equals(signature)) {
            Git git = null;
            try {
                git = Git.open(new File(String.format("%s/%s.git/.git", git_path, user_id)));
            } catch (RepositoryNotFoundException e) {
                Repository newlyCreatedRepo = FileRepositoryBuilder.create(
                        new File(String.format("%s/%s.git/.git", git_path, user_id)));
                newlyCreatedRepo.create();
            }

            File file = new File(filePath);

            if (file.delete()) {
                git.add().addFilepattern(".").call();
                RevCommit commit = git.commit().setMessage("deleteFile " + fileName).call();

                return "删除成功";
            } else {
                return "删除失败";
            }
        } else {
            return "鉴权失败";
        }
    }

    @PostMapping(value = "/update")
    public String update(@RequestParam HashMap<String, String> params, @RequestBody String body) throws IOException, GitAPIException, InvalidKeyException, NoSuchAlgorithmException {
        String user_id = params.get("UserId");
        String title = params.get("Title");

        String fileName = title + ".asc";
        String filePath = String.format("%s/%s.git/%s", git_path, user_id, fileName);


        //连接redis
        Jedis jedis = new Jedis("192.168.2.39");
        String redis_token = jedis.get(user_id);

        //生成签名
        SignatrueProvider signatrueProvider = new SignatrueProvider();
        StringBuilder signToString = signatrueProvider.createSignString(params);
        String signature = signatrueProvider.createSignature(redis_token, signToString);

        //鉴权
        if (params.get("Signature").equals(signature)) {
            Git git = null;
            try {
                git = Git.open(new File(String.format("%s/%s.git/.git", git_path, user_id)));
            } catch (RepositoryNotFoundException e) {
                Repository newlyCreatedRepo = FileRepositoryBuilder.create(
                        new File(String.format("%s/%s.git/.git", git_path, user_id)));
                newlyCreatedRepo.create();
            }

            File file = new File(filePath);
            //创建文件
            try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {

                if (!file.exists()) {
                    return "文件不存在";
                } else {

                    // get the content in bytes
                    byte[] contentInBytes = body.getBytes();

                    fileOutputStream.write(null);
                    fileOutputStream.write(contentInBytes);
                    fileOutputStream.flush();
                    fileOutputStream.close();

                    System.out.println("Done");

                }

            } catch (IOException e) {
                e.printStackTrace();
            }

            //获取仓库
            git.add().addFilepattern(fileName).call();
            RevCommit commit = git.commit().setMessage("updateFile " + fileName).call();

            return "更新成功";
        }else {
            return "鉴权失败";
        }
    }

    @GetMapping(value = "/list")
    public void list() {


    }

    @PostMapping(value = "/check")
    public void checkout() {

    }
}
