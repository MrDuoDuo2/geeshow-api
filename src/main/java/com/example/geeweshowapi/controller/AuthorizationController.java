package com.example.geeweshowapi.controller;


import com.example.geeweshowapi.DTO.AccessTokenDTO;
import com.example.geeweshowapi.DTO.GithubUserDTO;
import com.example.geeweshowapi.Provider.OAuthProvider;
import com.example.geeweshowapi.Provider.RepositoryProvider;
import com.example.geeweshowapi.Provider.SignatrueProvider;
import com.example.geeweshowapi.model.User;
import com.example.geeweshowapi.util.JsonUtils;
import com.example.geeweshowapi.util.ThclUrlUtil;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import redis.clients.jedis.Jedis;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.*;

@RestController

public class AuthorizationController {
    @Autowired
    public SignatrueProvider signatrueProvider;

    @Autowired
    public OAuthProvider OAuthProvider;

    @Autowired
    public RepositoryProvider repositoryProvider;

    @Value("${github.client.id}")
    private String clientId;

    @Value("${gitee.client_id}")
    private String gitee_client_id;

    @Value("${gitee.redirect_uri}")
    public String gitee_redirect_uri;

    @Value("${github.client.secret}")
    private String clientSecret;

    @Value("${git.path}")
    private String git_path;

    @Value("${server.ip}")
    private String server_ip;

    @Value("${server.port}")
    private String server_port;

    @Value("${redis.ip}")
    private String redis_ip;
    @Value("${gitee.client_secret}")
    private String gitee_client_secret;

    @GetMapping(value = "/")
    public void index(HttpServletRequest request, HttpServletResponse response,@RequestParam Map<String,String> param) throws IOException, NoSuchAlgorithmException, InvalidKeyException {

        String type = param.get("Type");
        //检测cookie
        Cookie[] cookies = request.getCookies();
        String user_id = null;
        String token = null;

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("UserId")) {
                    user_id = cookie.getValue();
                }
                if (cookie.getName().equals("Token")) {
                    token = cookie.getValue();
                }
            }
        }

        if (user_id == null && token == null) {
            user_id = "";
            token = "";
        }

        if (Objects.equals(user_id, "") || Objects.equals(token, "")) {
            //发送登陆请求
            if (type.equals("github")) {
                response.sendRedirect(String.format("https://github.com/login/oauth/authorize?client_id=611f387c5bf0d1959f3f&state=1"));
            }else if (type.equals("gitee")){
                response.sendRedirect("https://gitee.com/oauth/authorize?client_id="+gitee_client_id+"&redirect_uri="+gitee_redirect_uri +"&response_type=code&scope=user_info%20projects%20pull_requests");
            }
        } else {

            String SignatureMethod = "HmacSHA1";
            String SignatureNonce = UUID.randomUUID().toString();

            Date date = new Date();
            Timestamp timestamp = new Timestamp(date.getTime());
            String timestampString = timestamp.toString();

            System.out.println(timestamp.toString());

            HashMap<String, String> params = new HashMap<>();
            params.put("SignatureMethod", SignatureMethod);
            params.put("SignatureNonce", SignatureNonce);
            params.put("Timestamp", timestampString);
            params.put("UserId", user_id);

            StringBuilder stringToSign = signatrueProvider.createSignString(params);
            assert token != null;
            String signature = signatrueProvider.createSignature(token, stringToSign);

            params.put("Signature", signature);


            System.out.println(signature);
            System.out.println(stringToSign.toString());


            StringBuilder paramsString = new StringBuilder();

            String[] keyString = params.keySet().toArray(new String[]{});

            int i = 0;
            for (String key : keyString) {
                if (i == 0) {
                    paramsString.append(ThclUrlUtil.percentEncode(key)).append("=")
                            .append(ThclUrlUtil.percentEncode((String) params.get(key)));
                } else {
                    paramsString.append("&")
                            .append(ThclUrlUtil.percentEncode(key)).append("=")
                            .append(ThclUrlUtil.percentEncode((String) params.get(key)));
                }
                i++;
            }

            String url = String.format("http://%s:%s/check?%s", server_ip, server_port, paramsString.toString());

            System.out.println(url);

            response.sendRedirect(url);
        }
    }

    /*
     *   检查api
     *
     */
    @GetMapping(value = "/check")
    public String check(@RequestParam HashMap<String, String> params, HttpServletResponse response) throws UnsupportedEncodingException, InvalidKeyException, NoSuchAlgorithmException {

        HashMap<String, String> tempParams = (HashMap<String, String>) params.clone();
        tempParams.remove("Signature");

        Jedis jedis = new Jedis(redis_ip);
        String token = jedis.get(params.get("UserId"));

        if (token == null) {

            set_cookie(response,null,null);
            return "登陆失败";
        }

        StringBuilder stringToSign = signatrueProvider.createSignString(tempParams);
        String signature = signatrueProvider.createSignature(token, stringToSign);

        if (signature.equals(params.get("Signature"))) {
            GithubUserDTO githubUser = null;
            try {
                githubUser = OAuthProvider.getUserInfo(token);
            } catch (NullPointerException e) {
                set_cookie(response,null,null);
                return "登陆失败";
            }

            if (githubUser != null) {
                return "登陆成功";
            } else {
                set_cookie(response,null,null);
                return "登陆失败";
            }
        } else {

            set_cookie(response,null,null);

            return "登陆鉴权失败";
        }

    }

    @GetMapping(value = "/giteeCallback")
    public String giteeCallback(@RequestParam(name = "code") String code,
                                HttpServletResponse httpServletResponse) throws IOException, SocketTimeoutException {


        String body = null;

        String url = String.format("https://gitee.com/oauth/token?grant_type=authorization_code&code=%s&client_id=%s&redirect_uri=%s&client_secret=%s", code, gitee_client_id, gitee_redirect_uri, gitee_client_secret);
        //获取用户token
        String token = OAuthProvider.getGithubToken(body, url);

        if (token == null) {
            return "登陆失败";
        }

        GithubUserDTO githubUser;

        try {
            githubUser = OAuthProvider.getGiteeUserInfo(token);
        } catch (NullPointerException e) {
            return "登陆失败";
        }

        //获取用户id
        String user_id = githubUser.getId();

        //redis校验
        if (user_id != null) {

            //连接redis
            Jedis jedis = new Jedis(redis_ip);

            String redis_token = jedis.get(user_id);

            if (redis_token == null) {
                jedis.setex(user_id, 60, token);
                set_cookie(httpServletResponse, user_id, token);
            } else if (redis_token.equals(token)) {

                set_cookie(httpServletResponse, null, null);

                return "token重复重新登陆";
            } else {
                jedis.setex(user_id, 60, token);

                set_cookie(httpServletResponse, user_id, token);
            }
        } else {
            return "登陆失败";
        }


        MysqlController mysqlController = new MysqlController();
        mysqlController.init();

        //查找mysql用户信息
        User user = mysqlController.findByUserId(user_id);

        if (user == null || user.getRepositoryPath() == null) {
            String message = repositoryProvider.addUserRepository(user_id);

            System.out.println(message);

            mysqlController.insertUserInfo(user_id, message);

            return "登陆成功";
        } else {
            File file = new File(user.getRepositoryPath());

            if (!file.exists()) {
                file.mkdir();
                mysqlController.deleteUser(user_id);
                mysqlController.insertUserInfo(user_id, user.getRepositoryPath());
            }

            return "登陆成功";
        }
    }

    @GetMapping(value = "/callback")
    public String callback(@RequestParam(name = "code") String code,
                           @RequestParam(name = "state") String state,
                           HttpServletResponse httpServletResponse) throws IOException, SocketTimeoutException {

        AccessTokenDTO accessTokenDTO = new AccessTokenDTO();
        accessTokenDTO.setClient_id(clientId);
        accessTokenDTO.setClient_secret(clientSecret);

        accessTokenDTO.setCode(code);
        accessTokenDTO.setState(state);

        String body = JsonUtils.toJson(accessTokenDTO);

        String url = "https://github.com/login/oauth/access_token";

        //获取用户token
        String token = OAuthProvider.getGithubToken(body,url);

        if (token == null) {
            return "登陆失败";
        }

        GithubUserDTO githubUser;

        try {
            githubUser = OAuthProvider.getUserInfo(token);
        } catch (NullPointerException e) {
            return "登陆失败";
        }

        //获取用户id
        String user_id = githubUser.getId();


        //redis校验
        if (user_id != null) {

            //连接redis
            Jedis jedis = new Jedis(redis_ip);

            String redis_token = jedis.get(user_id);

            if (redis_token == null) {
                jedis.setex(user_id, 60, token);
                set_cookie(httpServletResponse, user_id, token);
            } else if (redis_token.equals(token)) {

                set_cookie(httpServletResponse, null, null);

                return "token重复重新登陆";
            } else {
                jedis.setex(user_id, 60, token);

                set_cookie(httpServletResponse, user_id, token);
            }
        } else {
            return "登陆失败";
        }


        MysqlController mysqlController = new MysqlController();
        mysqlController.init();

        //查找mysql用户信息
        User user = mysqlController.findByUserId(user_id);

        if (user == null||user.getRepositoryPath()==null) {
            String message = repositoryProvider.addUserRepository(user_id);

            System.out.println(message);

            mysqlController.insertUserInfo(user_id, message);

            return "登陆成功";
        } else {
            File file = new File(user.getRepositoryPath());

            if (!file.exists()) {
                file.mkdir();
                mysqlController.deleteUser(user_id);
                mysqlController.insertUserInfo(user_id, user.getRepositoryPath());
            }

            return "登陆成功";
        }
    }

    private void set_cookie(HttpServletResponse httpServletResponse, String user_id, String token) {

        Cookie userIdCookie = new Cookie("UserId", user_id);
        Cookie tokenCookie = new Cookie("Token", token);

        httpServletResponse.addCookie(userIdCookie);
        httpServletResponse.addCookie(tokenCookie);

    }


    private String createGitRepository(String user_id) throws IOException {

        String pathName = String.format("%s/%s.git/.git", git_path, user_id);

        Repository existingRepo = new FileRepositoryBuilder()
                .setGitDir(new File(pathName))
                .build();

        if (existingRepo == null) {
            Repository newlyCreatedRepo;

            newlyCreatedRepo = FileRepositoryBuilder.create(
                    new File(pathName));
            newlyCreatedRepo.create();

            return pathName;
        } else {
            return pathName;
        }

    }

}
