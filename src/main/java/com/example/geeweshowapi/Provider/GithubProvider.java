package com.example.geeweshowapi.Provider;

import com.example.geeweshowapi.DTO.GithubUserDTO;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

import static org.springframework.http.HttpMethod.POST;

@Component
public class GithubProvider {
    public String getGithubToken(String body) throws IOException {
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
            if (responEntity.getStatusCode()!= HttpStatus.OK){
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
}
