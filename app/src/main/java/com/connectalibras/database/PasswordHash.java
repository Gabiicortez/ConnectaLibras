package com.connectalibras.database;

import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.nio.charset.StandardCharsets;

public final class PasswordHash {
    private static byte[] derive(String password, byte[] salt, int rounds) throws Exception {
        // PBKDF2-HMAC-SHA256, inclusive Android 7/ API 24.
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(password.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] block = new byte[salt.length + 4];
        System.arraycopy(salt, 0, block, 0, salt.length); block[block.length-1] = 1;
        byte[] u = mac.doFinal(block), result = u.clone();
        for (int i=1;i<rounds;i++) { u=mac.doFinal(u); for(int k=0;k<result.length;k++) result[k]^=u[k]; }
        return result;
    }
    private static String hex(byte[] a) { StringBuilder s=new StringBuilder(); for(byte b:a) s.append(String.format(java.util.Locale.ROOT,"%02x",b&255)); return s.toString(); }
    private static byte[] unhex(String s) { byte[] a=new byte[s.length()/2]; for(int i=0;i<a.length;i++) a[i]=(byte)Integer.parseInt(s.substring(2*i,2*i+2),16); return a; }
    public static String hash(String password) {
        try { byte[] salt=new byte[16]; new SecureRandom().nextBytes(salt);
            return "pbkdf2_sha256$600000$"+hex(salt)+"$"+hex(derive(password,salt,600000));
        } catch(Exception e) { throw new IllegalStateException("Não foi possível proteger a senha",e); }
    }
    public static boolean verify(String password,String hash) {
        try { String[] v=hash.split("\\$"); int rounds=Integer.parseInt(v[1]);
            if(v.length!=4 || !v[0].equals("pbkdf2_sha256") || rounds<1 || rounds>2000000) return false;
            return MessageDigest.isEqual(derive(password,unhex(v[2]),rounds),unhex(v[3]));
        } catch(Exception e) { return false; }
    }
}
