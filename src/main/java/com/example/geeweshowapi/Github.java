package com.example.geeweshowapi;

public class Github {
    String client_id;
    String client_secret;
    String code;
    String state;

    public Github(String client_id, String client_secret, String code, String state) {
        this.client_id = client_id;
        this.client_secret = client_secret;
        this.code = code;
        this.state = state;
    }
}
