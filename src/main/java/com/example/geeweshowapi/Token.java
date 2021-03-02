package com.example.geeweshowapi;

public class Token {
    public String AccessKeyId;
    public String Signature;
    public String SignatureMethod;
    public String SignatureNonce;
    public String Timestamp;

    public String getAccessKeyId() {
        return AccessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.AccessKeyId = accessKeyId;
    }

    public String getSignature() {
        return Signature;
    }

    public void setSignature(String signature) {
        this.Signature = signature;
    }

    public String getSignatureMethod() {
        return SignatureMethod;
    }

    public void setSignatureMethod(String signatureMethod) {
        this.SignatureMethod = signatureMethod;
    }

    public String getSignatureNonce() {
        return SignatureNonce;
    }

    public void setSignatureNonce(String signatureNonce) {
        this.SignatureNonce = signatureNonce;
    }

    public String getTimestamp() {
        return Timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.Timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "Token{" +
                "accessKeyId='" + AccessKeyId + '\'' +
                ", signature='" + Signature + '\'' +
                ", signatureMethod='" + SignatureMethod + '\'' +
                ", signatureNonce='" + SignatureNonce + '\'' +
                ", timestamp='" + Timestamp + '\'' +
                '}';
    }
}
