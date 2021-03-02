package com.example.geeweshowapi.Provider;

import com.example.geeweshowapi.util.ThclUrlUtil;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;

@Component
public class SignatrueProvider {
    public StringBuilder createSignString(HashMap<String,String> params) throws UnsupportedEncodingException {
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

    public String createSignature (String secret, StringBuilder stringToSign) throws
            NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeyException {
        final String ALGORITHM = "HmacSHA1";
        final String ENCODING = "UTF-8";

        Mac mac = Mac.getInstance(ALGORITHM);
        mac.init(new SecretKeySpec(secret.getBytes(ENCODING), ALGORITHM));
        byte[] signData = mac.doFinal(String.valueOf(stringToSign).getBytes(ENCODING));

        return Base64.getEncoder().encodeToString(signData);
    }

}
