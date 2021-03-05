package com.example.geeweshowapi.controller;


import com.example.geeweshowapi.DTO.AccessTokenDTO;
import com.example.geeweshowapi.DTO.GithubUserDTO;
import com.example.geeweshowapi.Provider.GithubProvider;
import com.example.geeweshowapi.Provider.SignatrueProvider;
import com.example.geeweshowapi.model.User;
import com.example.geeweshowapi.util.JsonUtils;
import com.example.geeweshowapi.util.ThclUrlUtil;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
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
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.*;

@RestController

public class AuthorizationController {
    @Autowired
    public SignatrueProvider signatrueProvider;

    @Autowired
    public GithubProvider githubProvider;

    @Value("${github.client.id}")
    private String clientId;

    @Value("${github.client.secret}")
    private String clientSecret;

    @Value("${github.redirect_uri}")
    private String redirect_uri;

    @Value("${git.path}")
    private String git_path;

    @GetMapping(value = "/")
    public void index(HttpServletRequest request, HttpServletResponse response) throws IOException, NoSuchAlgorithmException, InvalidKeyException {

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
            Cookie userIdCookie = new Cookie("UserId", "");
            Cookie tokenCookie = new Cookie("Token", "");

            response.addCookie(userIdCookie);
            response.addCookie(tokenCookie);
        }else if (user_id.equals("") && token.equals("")){
            Cookie userIdCookie = new Cookie("UserId", "");
            Cookie tokenCookie = new Cookie("Token", "");

            response.addCookie(userIdCookie);
            response.addCookie(tokenCookie);
        }

        if (Objects.equals(user_id, "") || Objects.equals(token, "")) {
            //发送登陆请求
            response.sendRedirect("https://github.com/login/oauth/authorize?client_id=611f387c5bf0d1959f3f&state=1");
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

            String url = String.format("http://192.168.2.43:8080/check?%s", paramsString.toString());

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

        Jedis jedis = new Jedis("192.168.2.39");
        String token = jedis.get(params.get("UserId"));

        if (token == null) {
            Cookie UserIdCookie = new Cookie("UserId", null);
            Cookie TokenCookie = new Cookie("Token", null);

            response.addCookie(UserIdCookie);
            response.addCookie(TokenCookie);
            return "登陆失败";
        }

        StringBuilder stringToSign = signatrueProvider.createSignString(tempParams);
        String signature = signatrueProvider.createSignature(token, stringToSign);

        if (signature.equals(params.get("Signature"))) {
            GithubUserDTO githubUser = null;
            try {
                githubUser = githubProvider.getUserInfo(token);
            } catch (NullPointerException e) {
                Cookie UserIdCookie = new Cookie("UserId", null);
                Cookie TokenCookie = new Cookie("Token", null);

                response.addCookie(UserIdCookie);
                response.addCookie(TokenCookie);
                return "登陆失败";
            }

            if (githubUser != null) {
                return "登陆成功";
            } else {
                Cookie UserIdCookie = new Cookie("UserId", null);
                Cookie TokenCookie = new Cookie("Token", null);

                response.addCookie(UserIdCookie);
                response.addCookie(TokenCookie);
                return "登陆失败";
            }
        } else {
            Cookie UserIdCookie = new Cookie("UserId", null);
            Cookie TokenCookie = new Cookie("Token", null);

            response.addCookie(UserIdCookie);
            response.addCookie(TokenCookie);
            return "登陆鉴权失败";
        }

    }

    @GetMapping(value = "/callback")
    public String callback(@RequestParam(name = "code") String code,
                           @RequestParam(name = "state") String state,
                           HttpServletResponse httpServletResponse) throws IOException {

        AccessTokenDTO accessTokenDTO = new AccessTokenDTO();
        accessTokenDTO.setClient_id(clientId);
        accessTokenDTO.setClient_secret(clientSecret);

        accessTokenDTO.setCode(code);
        accessTokenDTO.setState(state);
        accessTokenDTO.setRedirect_uri(redirect_uri);

        String body = JsonUtils.toJson(accessTokenDTO);

        //获取用户token
        String token = githubProvider.getGithubToken(body);
        GithubUserDTO githubUser;

        try {
            githubUser = githubProvider.getUserInfo(token);
        } catch (NullPointerException e) {
            return "登陆失败";
        }

        //获取用户id
        String user_id = githubUser.getId();

        MysqlController mysqlController = new MysqlController();
        mysqlController.init();

        //查找mysql用户信息
        User user = mysqlController.findByUserId(user_id);

        if (user == null) {
            String message = createGitRepository(user_id);

            System.out.println(message);

            if (message == "创建失败") {
                Cookie UserIdCookie = new Cookie("UserId", null);
                Cookie TokenCookie = new Cookie("Token", null);

                httpServletResponse.addCookie(UserIdCookie);
                httpServletResponse.addCookie(TokenCookie);
                return "登陆失败";
            } else {
                mysqlController.insertUserInfo(user_id, message);
            }
        } else {
            String pathName = String.format("/tmp/%s.git/.git", user_id);

            Git git = null;
            try {
                git = Git.open(new File(String.format("%s/%s.git/.git", git_path, user_id)));

            }catch (RepositoryNotFoundException e) {
                String message = createGitRepository(user_id);

                System.out.println(message);

                if (message == "创建失败") {
                    Cookie UserIdCookie = new Cookie("UserId", null);
                    Cookie TokenCookie = new Cookie("Token", null);

                    httpServletResponse.addCookie(UserIdCookie);
                    httpServletResponse.addCookie(TokenCookie);
                    return "登陆失败";
                } else {
                    mysqlController.updateUserInfo(user_id, message);
                }
            }
        }

        //redis校验
        if (user_id != null) {

            //连接redis
            Jedis jedis = new Jedis("192.168.2.39");

            String redis_token = jedis.get(user_id);

            if (redis_token == null) {
                jedis.setex(user_id, 60, token);

                Cookie userIdCookie = new Cookie("UserId", user_id);
                Cookie tokenCookie = new Cookie("Token", token);

                httpServletResponse.addCookie(userIdCookie);
                httpServletResponse.addCookie(tokenCookie);

                return "登陆成功";
            } else if (redis_token.equals(token)) {
                Cookie UserIdCookie = new Cookie("UserId", null);
                Cookie TokenCookie = new Cookie("Token", null);

                httpServletResponse.addCookie(UserIdCookie);
                httpServletResponse.addCookie(TokenCookie);
                return "token重复重新登陆";
            } else {
                jedis.setex(user_id, 60, token);

                Cookie userIdCookie = new Cookie("UserId", user_id);
                Cookie tokenCookie = new Cookie("Token", token);

                httpServletResponse.addCookie(userIdCookie);
                httpServletResponse.addCookie(tokenCookie);

                return "登陆成功";
            }
        } else {
            return "登陆失败";
        }
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
