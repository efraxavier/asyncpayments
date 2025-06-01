package com.example.asyncpayments.api;

import java.io.IOException;
import java.util.Properties;

public class TestUsers {
    private static final Properties props = new Properties();

    static {
        try {
            props.load(TestUsers.class.getClassLoader().getResourceAsStream("test-users.properties"));
        } catch (IOException e) {
            throw new RuntimeException("Não foi possível carregar test-users.properties", e);
        }
    }

    public static String get(String key) {
        return props.getProperty(key);
    }

    public static String adminEmail() { return get("test.admin.email"); }
    public static String adminPassword() { return get("test.admin.password"); }

    public static String peterEmail() { return get("test.peter.email"); }
    public static String peterPassword() { return get("test.peter.password"); }
    public static Long peterId() { return Long.valueOf(get("test.peter.id")); }

    public static String loisEmail() { return get("test.lois.email"); }
    public static String loisPassword() { return get("test.lois.password"); }
    public static Long loisId() { return Long.valueOf(get("test.lois.id")); }

   public static String megEmail() { return get("test.meg.email"); }
   public static String megPassword() { return get("test.meg.password"); }
   public static Long megId() { return Long.valueOf(get("test.meg.id")); }

   public static String chrisEmail() { return get("test.chris.email"); }
   public static String chrisPassword() { return get("test.chris.password"); }
   public static Long chrisId() { return Long.valueOf(get("test.chris.id")); }

   public static String stewieEmail() { return get("test.stewie.email"); }
   public static String stewiePassword() { return get("test.stewie.password"); }
   public static Long stewieId() { return Long.valueOf(get("test.stewie.id")); }

   public static String brianEmail() { return get("test.brian.email"); }
   public static String brianPassword() { return get("test.brian.password"); }
   public static Long brianId() { return Long.valueOf(get("test.brian.id")); }
}