package com.example.servicioWeb;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class CacheService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${app.redis.ttl.cache:86400}")
    private long cacheTtl;

    private static final String CACHE_KEY_PREFIX = "ia:cache:";

    public String generarHash(String descripcion) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(descripcion.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (Exception e) {
            return String.valueOf(descripcion.hashCode());
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, String>> obtenerDelCache(String descripcionHash) {
        String key = CACHE_KEY_PREFIX + descripcionHash;
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached instanceof List) {
            System.out.println("✅ Cache hit para: " + descripcionHash);
            return (List<Map<String, String>>) cached;
        }
        System.out.println("❌ Cache miss para: " + descripcionHash);
        return null;
    }

    public void guardarEnCache(String descripcionHash, List<Map<String, String>> materiales) {
        String key = CACHE_KEY_PREFIX + descripcionHash;
        redisTemplate.opsForValue().set(key, materiales, cacheTtl, TimeUnit.SECONDS);
        System.out.println("💾 Guardado en caché: " + descripcionHash);
    }
}