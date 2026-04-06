package com.example.servicioWeb;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
public class ConversationService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${app.redis.ttl.conversation:1800}")
    private long conversationTtl;

    private static final String CONVERSATION_KEY_PREFIX = "agent:conv:";

    /**
     * Guardar conversación en Redis
     */
    public void saveConversation(String sessionId, AgentConversation conversation) {
        String key = CONVERSATION_KEY_PREFIX + sessionId;
        redisTemplate.opsForValue().set(key, conversation, Duration.ofSeconds(conversationTtl));
        System.out.println("💾 Conversación guardada en Redis: " + key);
    }

    /**
     * Obtener conversación de Redis
     */
    @SuppressWarnings("unchecked")
    public AgentConversation getConversation(String sessionId) {
        String key = CONVERSATION_KEY_PREFIX + sessionId;
        Object value = redisTemplate.opsForValue().get(key);

        if (value instanceof AgentConversation) {
            AgentConversation conversation = (AgentConversation) value;
            // Refrescar TTL
            redisTemplate.expire(key, conversationTtl, TimeUnit.SECONDS);
            System.out.println("📖 Conversación recuperada de Redis: " + key);
            return conversation;
        }

        System.out.println("⚠️ No se encontró conversación en Redis: " + key);
        return null;
    }

    /**
     * Eliminar conversación
     */
    public void deleteConversation(String sessionId) {
        redisTemplate.delete(CONVERSATION_KEY_PREFIX + sessionId);
        System.out.println("🗑️ Conversación eliminada de Redis: " + sessionId);
    }
}