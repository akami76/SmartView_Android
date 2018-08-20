package com.kt.smartview.utils.rsa;

import android.content.Context;
import android.util.Base64;

import com.kt.smartview.utils.CommonUtil;

import java.security.Key;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;

public class RSACipher implements CipherBase {
    private final Cipher cipher;
    private PublicKey publicKey;
    private PrivateKey privateKey;
    private String transformation = "RSA/ECB/PKCS1Padding";
    private String encoding = "UTF-8";
    private Context context;
    public RSACipher(Context context, String publicKeyPathName, String privateKeyPathName) throws Exception {
        this.context = context;
        this.initKey(publicKeyPathName, privateKeyPathName);
        this.cipher = Cipher.getInstance(this.transformation);
    }

    public RSACipher(Context context, String publicKeyPathName, String privateKeyPathName, String transformation) throws Exception {
        this.context = context;
        this.initKey(publicKeyPathName, privateKeyPathName);
        this.transformation = transformation;
        this.cipher = Cipher.getInstance(this.transformation);
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public String encrypt(String rawText, Key key, String encoding) throws Exception {
        Cipher var4 = this.cipher;
        synchronized(this.cipher) {
            this.cipher.init(1, key);
            return Base64.encodeToString(this.cipher.doFinal(rawText.getBytes(encoding)), Base64.DEFAULT);
        }
    }

    public String decrypt(String cipherText, Key key, String encoding) throws Exception {
        Cipher var4 = this.cipher;
        synchronized(this.cipher) {
            this.cipher.init(2, key);
            return new String(this.cipher.doFinal(Base64.decode(cipherText.getBytes(), Base64.DEFAULT)), encoding);
        }
    }

    private void initKey(String publicKeyPathName, String privateKeyPathName) throws Exception {
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        KeySpec publicKeySpec = loadKeySpec(publicKeyPathName, KeyType.PUBLIC);
        KeySpec privateKeySpec = loadKeySpec(privateKeyPathName, KeyType.PRIVATE);

        if(publicKeySpec != null) {
            this.publicKey = keyFactory.generatePublic(publicKeySpec);
        }

        if(privateKeySpec != null) {
            this.privateKey = keyFactory.generatePrivate(privateKeySpec);
        }
    }

    private KeySpec loadKeySpec(String pathName, KeyType keyType) throws Exception {
        if(pathName == null) return null;
        byte[] encodedKey = CommonUtil.getBytesFromFile(context, pathName);
        if(encodedKey != null){
            if(keyType == KeyType.PUBLIC) {
                return new X509EncodedKeySpec(encodedKey);
            }

            if(keyType == KeyType.PRIVATE) {
                return new PKCS8EncodedKeySpec(encodedKey);
            }
        }
        return null;
    }

    public String encrypt(String rawText) throws Exception {
        if(publicKey == null) return null;
        return this.encrypt(rawText, this.publicKey, this.encoding);
    }

    public String decrypt(String cipherText) throws Exception {
        if(privateKey == null) return null;
        return this.decrypt(cipherText, this.privateKey, this.encoding);
    }

}
