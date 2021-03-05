package com.example.geeweshowapi.controller;

import com.example.geeweshowapi.model.Article;
import com.example.geeweshowapi.model.ArticleVersion;
import com.example.geeweshowapi.model.User;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.RevertCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.eclipse.jgit.util.FileUtils.mkdir;

@RestController
public class ArticleController {

    @Value("${git.path}")
    private String git_path;

    @PostMapping(value = "/add")
    public String add(@RequestParam HashMap<String, String> params, @RequestBody String body) throws IOException, GitAPIException, InvalidKeyException, NoSuchAlgorithmException {

        String title = params.get("Title");
        String user_id = params.get("UserId");
//
////        连接redis
//        Jedis jedis = new Jedis("192.168.2.39");
//        String redis_token = jedis.get(user_id);


//        //签名
//        SignatrueProvider signatrueProvider = new SignatrueProvider();
//        StringBuilder signToString = signatrueProvider.createSignString(params);
//        String signature = signatrueProvider.createSignature(redis_token, signToString);
//
//        //鉴权
//        if (params.get("Signature").equals(signature)) {

        //链接Mysql
        MysqlController mysqlController = new MysqlController();
        mysqlController.init();

        //获取用户的仓库id

        String fileName = title + ".asc";
        String filePath;

        String user_repository_path;

        try {
            User user = mysqlController.findByUserId(user_id);
            user_repository_path= user.getRepositoryPath();
            File file = new File(user_repository_path);
            if (!file.exists()){
                file.mkdir();
            }

            mysqlController.addUser(user_id,user_repository_path);
        } catch (NullPointerException e){
            File file = new File(String.format("%s/%s", git_path, user_id));
            if (!file.exists()){
                file.mkdir();
            }
            user_repository_path = String.format("%s/%s", git_path, user_id);
            mysqlController.addUser(user_id,user_repository_path);
        }


        Git article_git = null;
        try {
            article_git = Git.open(new File(String.format("%s/%s.git/.git", user_repository_path, title)));
            return "文章仓库存在";
        }catch (RepositoryNotFoundException e) {
            Repository newlyCreatedRepo = FileRepositoryBuilder.create(
                    new File(String.format("%s/%s.git/.git", user_repository_path, title)));
            newlyCreatedRepo.create();
            article_git = new Git(newlyCreatedRepo);
        }

        filePath = String.format("%s/%s.git/%s",user_repository_path,title,fileName);

        //添加文件
        File file = new File(filePath);

        RevCommit commit = null;

        if (!file.exists()) {
            System.out.println(file.exists());
            //创建文件
            try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {

                System.out.println(file.exists());


                file.createNewFile();

                // get the content in bytes
                byte[] contentInBytes = body.getBytes();

                fileOutputStream.write(contentInBytes);
                fileOutputStream.flush();
                fileOutputStream.close();

                System.out.println("Done");

                //git 提交

                article_git.add().addFilepattern(fileName).call();
                commit = article_git.commit().setMessage("addFile " + fileName).call();

                String commit_id = commit.getName();
                String message = commit.getFullMessage();


                //获取当前时间
                Date date = new Date();
                java.sql.Timestamp timestamp = new Timestamp(date.getTime());

                Article articleTmp =mysqlController.findByArticleTitle(title);
                if (articleTmp!=null){
                    mysqlController.deleteArticle(articleTmp.getId());
                }
                //文章信息入库
                mysqlController.addArticle(user_id,String.format("%s/%s.git", user_repository_path, title), title, timestamp);

                Article article =mysqlController.findByArticleTitle(title);
//                文章版本信息入库
                mysqlController.addArticleVersion(article.getId(), message, commit_id, timestamp);

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (file.exists()) {
            return "文件存在";
        }


//        } else {
//            return "鉴权失败";
//        }

        return "保存成功";
    }

    @GetMapping(value = "/delete")
    public String delete(@RequestParam HashMap<String, String> params) throws IOException, GitAPIException, InvalidKeyException, NoSuchAlgorithmException {
        String title = params.get("Title");
        String user_id = params.get("UserId");

        //链接Mysql
        MysqlController mysqlController = new MysqlController();
        mysqlController.init();

        String filePath;
        String fileName = title + ".asc";

        String user_repository_path;

        try {
            User user = mysqlController.findByUserId(user_id);
            user_repository_path= user.getRepositoryPath();
            File file = new File(user_repository_path);
            if (!file.exists()){
                file.mkdir();
            }

        } catch (NullPointerException e){
            File file = new File(String.format("%s/%s", git_path, user_id));
            if (!file.exists()){
                file.mkdir();
            }
            user_repository_path = String.format("%s/%s", git_path, user_id);
        }

//        //连接redis
//        Jedis jedis = new Jedis("192.168.2.39");
//        String redis_token = jedis.get(user_id);

//        //生成签名
//        SignatrueProvider signatrueProvider = new SignatrueProvider();
//        StringBuilder signToString = signatrueProvider.createSignString(params);
//        String signature = signatrueProvider.createSignature(redis_token, signToString);


        //鉴权
//        if (params.get("Signature").equals(signature)) {
        Git article_git = null;
        try {
            article_git = Git.open(new File(String.format("%s/%s.git/.git", user_repository_path, title)));
        }catch (RepositoryNotFoundException e) {
            return "文章仓库不存在";
        }

        filePath = String.format("%s/%s.git",user_repository_path,title);

        File file = new File(filePath);

        if (FileSystemUtils.deleteRecursively(file)) {
            Article article = mysqlController.findByArticleTitle(title);
            if (article == null){
                return "文章不存在";
            }
            //删除文章信息
            mysqlController.deleteArticle(article.getId());
            //删除文章版本
            mysqlController.deleteArticleVersion(article.getId());

            return "删除成功";
        } else {
            return "删除失败";
        }
//        } else {
//            return "鉴权失败";
//        }
    }

    @PostMapping(value = "/update")
    public String update(@RequestParam HashMap<String, String> params, @RequestBody String body) throws IOException, GitAPIException, InvalidKeyException, NoSuchAlgorithmException {
        String user_id = params.get("UserId");
        String title = params.get("Title");

        //链接Mysql
        MysqlController mysqlController = new MysqlController();
        mysqlController.init();

        String filePath;
        String fileName = title + ".asc";

        String user_repository_path;

        try {
            User user = mysqlController.findByUserId(user_id);
            user_repository_path= user.getRepositoryPath();
            File file = new File(user_repository_path);
            if (!file.exists()){
                file.mkdir();
            }

        } catch (NullPointerException e){
            File file = new File(String.format("%s/%s", git_path, user_id));
            if (!file.exists()){
                file.mkdir();
            }
            user_repository_path = String.format("%s/%s", git_path, user_id);
        }


        //连接redis
//        Jedis jedis = new Jedis("192.168.2.39");
//        String redis_token = jedis.get(user_id);

        //生成签名
//        SignatrueProvider signatrueProvider = new SignatrueProvider();
//        StringBuilder signToString = signatrueProvider.createSignString(params);
//        String signature = signatrueProvider.createSignature(redis_token, signToString);

        //鉴权
//        if (params.get("Signature").equals(signature)) {
        Git article_git = null;
        try {
            article_git = Git.open(new File(String.format("%s/%s.git/.git", user_repository_path, title)));
        }catch (RepositoryNotFoundException e) {
            return "文章不存在";
        }

        filePath = String.format("%s/%s.git/%s",user_repository_path,title,fileName);

        File file = new File(filePath);
        //创建文件
        try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {

            if (!file.exists()) {
                return "文件不存在";
            } else {

                // get the content in bytes
                byte[] contentInBytes = body.getBytes();

//                fileOutputStream.write(null);
                fileOutputStream.write(contentInBytes);
                fileOutputStream.flush();
                fileOutputStream.close();

                System.out.println("Done");
                //获取仓库
                article_git.add().addFilepattern(fileName).call();
                RevCommit commit = article_git.commit().setMessage("updateFile " + fileName).call();


                Article article = mysqlController.findArticleByUserIdAndTitle(user_id,title);

                Date date = new Date();
                Timestamp timestamp = new Timestamp(date.getTime());
                //文章版本信息入库
                mysqlController.addArticleVersion(article.getId(), commit.getFullMessage(), commit.getName(), timestamp);
                return "更新成功";

            }

        } catch (IOException e) {
            e.printStackTrace();
        }

//        } else {
//            return "鉴权失败";
//        }
        return "";
    }

    @GetMapping(value = "/articles")
    public String articles(@RequestParam Map<String,String> params) throws IOException {
        String user_id = params.get("UserId");

        MysqlController mysqlController = new MysqlController();
        mysqlController.init();

        List<Article> articles = mysqlController.findArticleByUserId(user_id);

        String articlesString = "";
        for (Article article:articles) {
            articlesString += article.getArticleTitle()+",";
        }

        return articlesString;
    }

    @GetMapping(value = "/history")
    public String articlesHistory(@RequestParam Map<String,String> params) throws IOException {
        String user_id = params.get("UserId");
        String title=params.get("Title");

        MysqlController mysqlController = new MysqlController();
        mysqlController.init();

        List<Article> articles = mysqlController.findArticleByUserId(user_id);


        List<ArticleVersion> articleVersions = null;
        for (Article article:articles) {
            if (article.getArticleTitle().equals(title)){
                articleVersions = mysqlController.findArticleHistoryByArticleId(article.getId());
            }
        }

        if (articleVersions==null) {
            return "文章不存在";
        }
        String commit_ids = "";
        for (ArticleVersion articleVersion:articleVersions) {
            commit_ids += articleVersion.getVersionId() + "\n";
        }

        return commit_ids;
    }



    @PostMapping(value = "/checkout")
    public String checkout(@RequestParam Map<String,String> params) throws IOException, GitAPIException {
        String user_id = params.get("UserId");
        String title = params.get("Title");
        String version = params.get("ArticleVersion");

        MysqlController mysqlController = new MysqlController();
        mysqlController.init();

        String repository_id;
        User user = mysqlController.findByUserId(user_id);

        Article article;
        if (user == null){
            return "用户不存在";
        }else {
            article= mysqlController.findArticleByUserIdAndTitle(user_id,title);

            if (article == null){
                return "文章不存在";
            }
        }

        Git git = null;
        try {
            git = Git.open(new File(String.format("%s/.git", article.getRepositoryPath())));
        } catch (RepositoryNotFoundException e) {
            return "仓库不存在";
        }

        Repository repository = git.getRepository();
        RevWalk revWalk = new RevWalk(repository);
        ObjectId objectId = repository.resolve(version);
        RevCommit revCommit = revWalk.parseCommit(objectId);
        String preVision = revCommit.getParent(0).getName();
        git.reset().setMode(ResetCommand.ResetType.HARD).setRef(preVision).call();
        repository.close();
        return user_id;
    }
}
