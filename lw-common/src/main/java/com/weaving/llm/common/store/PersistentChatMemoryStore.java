package com.weaving.llm.common.store;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.extern.slf4j.Slf4j;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static dev.langchain4j.data.message.ChatMessageDeserializer.messagesFromJson;
import static dev.langchain4j.data.message.ChatMessageSerializer.messagesToJson;
import static org.mapdb.Serializer.STRING;
import static org.mapdb.Serializer.LONG;

@Slf4j
@Component
public class PersistentChatMemoryStore implements ChatMemoryStore {

    private final DB db = DBMaker.fileDB("multi-user-chat-memoryV2.db").transactionEnable().make();
    // 修改这里：使�?STRING 序列化器匹配 String 类型�?key
    private final Map<String, String> map = db.hashMap("messages", Serializer.STRING, Serializer.STRING).createOrOpen();
    // 存储时间�?
    private final Map<String, Long> timestamps = db.hashMap("timestamps", Serializer.STRING, Serializer.LONG).createOrOpen();
    
    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String json = map.get((String) memoryId);
        return messagesFromJson(json);
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String json = messagesToJson(messages);
        map.put((String) memoryId, json);
        // 保存时间�?
        timestamps.put((String) memoryId, System.currentTimeMillis());
        db.commit();
    }

    @Override
    public void deleteMessages(Object memoryId) {
        map.remove((String) memoryId);
        timestamps.remove((String) memoryId);
        db.commit();
    }
    
    /**
     * 获取消息的时间戳
     */
    public Long getTimestamp(Object memoryId) {
        return timestamps.get((String) memoryId);
    }
}