package com.example.geeweshowapi.controller;


import com.example.geeweshowapi.*;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import redis.clients.jedis.Jedis;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
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

import static org.springframework.http.HttpMethod.POST;

@RestController

public class Demo {

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

        if (user_id == null || token == null) {
            Cookie userIdCookie = new Cookie("UserId","");
            Cookie tokenCookie = new Cookie("Token","");

            response.addCookie(userIdCookie);
            response.addCookie(tokenCookie);
        }

        if (Objects.equals(user_id, "") || Objects.equals(token, "")) {
            //发送登陆请求
            response.sendRedirect("https://github.com/login/oauth/authorize?client_id=611f387c5bf0d1959f3f&state=1");
        } else{

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

            StringBuilder stringToSign = createSignString(params);
            String signature = createSignature(token, stringToSign);

            params.put("Signature",signature);

            System.out.println(signature);
            System.out.println(stringToSign.toString());


            StringBuilder paramsString = new StringBuilder();

            String[] keyString = params.keySet().toArray(new String[]{});

            int i =0;
            for (String key: keyString) {
                if (i == 0){
                paramsString.append(ThclUrlUtil.percentEncode(key)).append("=")
                        .append(ThclUrlUtil.percentEncode((String) params.get(key)));
                }else{
                    paramsString.append("&")
                            .append(ThclUrlUtil.percentEncode(key)).append("=")
                            .append(ThclUrlUtil.percentEncode((String) params.get(key)));
                }
                i++;
            }

            String url = String.format("http://192.168.2.43:8080/check?%s",paramsString.toString());

            response.sendRedirect(url);
        }
    }

        private StringBuilder createSignString(HashMap<String,String> params) throws UnsupportedEncodingException {
            //参数排序
            String[] sortedParams = params.keySet().toArray(new String[]{});
            Arrays.sort(sortedParams);

            final String HTTP_METHOD = "GET";
            final String SEPARATOR = "&";

            //构造用于签名的字符串
            StringBuilder stringToSign = new StringBuilder();
            stringToSign.append(HTTP_METHOD).append(SEPARATOR);

            stringToSign.append(ThclUrlUtil.percentEncode("/")).append(SEPARATOR);

            StringBuilder canonicalizedQueryString = new StringBuilder();


            for (String param : sortedParams) {
                // 构造查询参数（如&Timestamp=xxx），并追加到canonicalizedQueryString最后
                canonicalizedQueryString.append("&")
                        .append(ThclUrlUtil.percentEncode(param)).append("=")
                        .append(ThclUrlUtil.percentEncode((String) params.get(param)));
            }

            // 构造用于签名的字符串：URL编码后的查询字符串
            stringToSign.append(ThclUrlUtil.percentEncode(
                    canonicalizedQueryString.toString().substring(1)));

            return stringToSign;
        }

        private String createSignature (String secret, StringBuilder stringToSign) throws
        NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeyException {
            final String ALGORITHM = "HmacSHA1";
            final String ENCODING = "UTF-8";

            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(ENCODING), ALGORITHM));
            byte[] signData = mac.doFinal(String.valueOf(stringToSign).getBytes(ENCODING));

            return Base64.getEncoder().encodeToString(signData);
        }

        /*
        *   检查api
        *
        */
        @GetMapping(value = "/check")
        public String check (@RequestParam HashMap<String,String> params,HttpServletResponse response) throws UnsupportedEncodingException, InvalidKeyException, NoSuchAlgorithmException {
        HashMap<String,String> tempParams = (HashMap<String, String>) params.clone();
            tempParams.remove("Signature");

            Jedis jedis = new Jedis("192.168.2.39");
            String token = jedis.get(params.get("UserId"));

            if (token == null){
                Cookie UserIdCookie = new Cookie("UserId",null);
                Cookie TokenCookie = new Cookie("Token",null);

                response.addCookie(UserIdCookie);
                response.addCookie(TokenCookie);
                return "登陆失败";
            }

            StringBuilder stringToSign = createSignString(tempParams);
            String signature = createSignature(token, stringToSign);

            if (signature.equals(params.get("Signature"))){
                GithubUser githubUser = null;
                try {
                    githubUser = getUserInfo(token);
                }catch (NullPointerException e){
                    Cookie UserIdCookie = new Cookie("UserId",null);
                    Cookie TokenCookie = new Cookie("Token",null);

                    response.addCookie(UserIdCookie);
                    response.addCookie(TokenCookie);
                    return "登陆失败";
                }

                if (githubUser!=null){
                    return "登陆成功";
                }else {
                    Cookie UserIdCookie = new Cookie("UserId",null);
                    Cookie TokenCookie = new Cookie("Token",null);

                    response.addCookie(UserIdCookie);
                    response.addCookie(TokenCookie);
                    return "登陆失败";
                }
            }else {
                Cookie UserIdCookie = new Cookie("UserId",null);
                Cookie TokenCookie = new Cookie("Token",null);

                response.addCookie(UserIdCookie);
                response.addCookie(TokenCookie);
                return "登陆鉴权失败";
            }

        }

        @GetMapping(value = "/callback")
        public String callback (@RequestParam String code,
                                @RequestParam String state,
                                HttpServletResponse httpServletResponse) throws IOException {
            System.out.println(code);
            System.out.println(state);

            String client_id = "611f387c5bf0d1959f3f";
            String client_secret = "f95665666e76100e9f018ec91c83fbb6e8f095a1";

            Github github = new Github(client_id, client_secret, code, state);

            String body = JsonUtils.toJson(github);

            //获取token
            String token = getGithubToken(body);

            //获取用户信息
            GithubUser githubUser = null;
            try {
                 githubUser = getUserInfo(token);
            }catch (NullPointerException e){
                return "登陆失败";
            }
            String user_id = githubUser.getId();
            System.out.println(githubUser.getId());

            Cookie userIdCookie = new Cookie("UserId",user_id);
            Cookie tokenCookie = new Cookie("Token",token);



            MysqlController mysqlController = new MysqlController();
            mysqlController.init();
            User user = mysqlController.findByUserId(user_id);

            if (user == null){
                String message = createGitRepository(user_id);

                System.out.println(message);

                if (message == "创建失败"){
                    Cookie UserIdCookie = new Cookie("UserId",null);
                    Cookie TokenCookie = new Cookie("Token",null);

                    httpServletResponse.addCookie(UserIdCookie);
                    httpServletResponse.addCookie(TokenCookie);
                    return "登陆失败";
                }else{
                    mysqlController.insertUserInfo(user_id,message);
                }
            }

            //redis校验
            if (user_id != null) {

                //连接redis
                Jedis jedis = new Jedis("192.168.2.39");

                String redis_token = jedis.get(user_id);

                if (redis_token == null) {
                    jedis.setex(user_id,60, token);

                    httpServletResponse.addCookie(userIdCookie);
                    httpServletResponse.addCookie(tokenCookie);

                    return "登陆成功";
                } else if (redis_token.equals(token)) {
                    Cookie UserIdCookie = new Cookie("UserId",null);
                    Cookie TokenCookie = new Cookie("Token",null);

                    httpServletResponse.addCookie(UserIdCookie);
                    httpServletResponse.addCookie(TokenCookie);
                    return "token重复重新登陆";
                } else {
                    jedis.setex(user_id,60, token);

                    httpServletResponse.addCookie(userIdCookie);
                    httpServletResponse.addCookie(tokenCookie);

                    return "登陆成功";
                }
            } else {
                return "登陆失败";
            }
        }

    private String getGithubToken(String body) throws IOException {
        RestTemplate restTemplate = new RestTemplate();
        String url = "https://github.com/login/oauth/access_token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> request = new HttpEntity<String>(body,headers);
        ResponseEntity<String> responEntity = null;
        try {
             responEntity = restTemplate.exchange(url,POST,request,String.class);
        }catch (HttpClientErrorException e) {
            System.out.println(responEntity.getStatusCode());
            if (responEntity.getStatusCode()!=HttpStatus.OK){
                getGithubToken(body);
            }
        }
        String token;
        String string = responEntity.getBody();
        token = string.split("&")[0].split("=")[1];

            System.out.println(string);
            System.out.println(token);

        return token;
    }

    private GithubUser getUserInfo(String token) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "token " + token);

        HttpEntity request = new HttpEntity(headers);

        String url = "https://api.github.com/user";
        ResponseEntity<GithubUser> responseEntity = null;

        try {
            responseEntity = restTemplate.exchange(url, HttpMethod.GET, request, GithubUser.class);
        }catch (HttpClientErrorException e) {
            if (responseEntity.getStatusCode() != HttpStatus.OK) {
                return null;
            }
        }
        return responseEntity.getBody();
    }

    private String createGitRepository(String user_id) throws IOException {

        String pathName = String.format("/tmp/%s.git/.git", user_id);

        Repository existingRepo = new FileRepositoryBuilder()
                .setGitDir(new File(pathName))
                .build();

        if (existingRepo == null) {

            Repository newlyCreatedRepo;

                newlyCreatedRepo = FileRepositoryBuilder.create(
                        new File(pathName));
                newlyCreatedRepo.create();

                return pathName;
        }else {
            return pathName;
        }

    }


}
