package com.example.geeweshowapi.Provider;

import com.example.geeweshowapi.DTO.GithubUserDTO;
import com.example.geeweshowapi.util.JsonUtils;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.springframework.http.HttpMethod.POST;

@Component
public class OAuthProvider {
    public String getGithubToken(String body,String url) throws IOException {
        RestTemplate restTemplate = new RestTemplate();

        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        //默认值我GET
        con.setRequestMethod("POST");

        con.setDoOutput(false);
        //添加请求头
//        con.setRequestProperty("User-Agent", USER_AGENT);

//        try(OutputStream os = con.getOutputStream()) {
//            byte[] input = body.getBytes(StandardCharsets.UTF_8);
//            os.write(input, 0, input.length);
//        }

        int responseCode = con.getResponseCode();
        System.out.println("\nSending 'POST' request to URL : " + url);
        System.out.println("Response Code : " + responseCode);

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        //打印结果
        System.out.println(response.toString());

        Map<Object, Object> map = JsonUtils.fromJson(response.toString());
        Map<String,String> newMap =new HashMap<String,String>();
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
            if(entry.getValue() instanceof String){
                newMap.put(String.valueOf(entry.getKey()), (String) entry.getValue());
            }
        }


        String token = newMap.get("access_token");

        System.out.println(token);

        return token;
    }

    public GithubUserDTO getUserInfo(String token) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "token " + token);

        HttpEntity request = new HttpEntity(headers);

        String url = "https://api.github.com/user";
        ResponseEntity<GithubUserDTO> responseEntity = null;

        try {
            responseEntity = restTemplate.exchange(url, HttpMethod.GET, request, GithubUserDTO.class);
        }catch (HttpClientErrorException e) {
            if (responseEntity.getStatusCode() != HttpStatus.OK) {
                return null;
            }
        }
        return responseEntity.getBody();
    }


    public GithubUserDTO getGiteeUserInfo(String token) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
//        headers.set("Authorization", "token " + token);

        HttpEntity request = new HttpEntity(headers);

        String url = String.format("https://gitee.com/api/v5/user?access_token=%s",token);
        ResponseEntity<GithubUserDTO> responseEntity = null;

        try {
            responseEntity = restTemplate.exchange(url, HttpMethod.GET, request, GithubUserDTO.class);
        }catch (HttpClientErrorException e) {
            if (responseEntity.getStatusCode() != HttpStatus.OK) {
                return null;
            }
        }
        return responseEntity.getBody();
    }
}
