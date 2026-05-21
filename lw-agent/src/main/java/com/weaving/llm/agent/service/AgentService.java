package com.weaving.llm.agent.service;

import com.weaving.llm.agent.skills.CodeAnalysisSkill;
import com.weaving.llm.agent.skills.CodeGenerationSkill;
import com.weaving.llm.agent.skills.SkillRegistry;
import com.weaving.llm.agent.skills.TroubleshootingSkill;
import com.weaving.llm.common.store.PersistentChatMemoryStore;
import com.weaving.llm.agent.tools.CalculationTool;
import com.weaving.llm.agent.tools.DateTimeTool;
import com.weaving.llm.agent.tools.FileTool;
import com.weaving.llm.agent.tools.WebSearchTool;
import com.weaving.llm.agent.tools.registry.AgentAssistant;
import com.weaving.llm.agent.tools.registry.ToolRegistry;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Agent 鏈嶅姟
 * 璐熻矗鍒涘缓鍜岀�鐞?Agent 瀹炰緥
 */
@Slf4j
@Service
public class AgentService {
    
    @Autowired
    private ToolRegistry toolRegistry;
    
    @Autowired
    private SkillRegistry skillRegistry;
    
    @Autowired
    private DateTimeTool dateTimeTool;
    
    @Autowired
    private CalculationTool calculationTool;
    
    @Autowired
    private FileTool fileTool;
    
    @Autowired
    private WebSearchTool webSearchTool;
    
    @Autowired
    private CodeAnalysisSkill codeAnalysisSkill;
    
    @Autowired
    private CodeGenerationSkill codeGenerationSkill;
    
    @Autowired
    private TroubleshootingSkill troubleshootingSkill;
    
    @Autowired
    private ChatModel chatModel;
    
    @Autowired
    private PersistentChatMemoryStore chatMemoryStore;
    
    // 缂撳瓨宸插垱寤虹殑 Agent 瀹炰緥 (鎸?sessionId)
    private final Map<String, AgentAssistant> agentCache = new HashMap<>();
    
    // 缂撳瓨鎸夋ā鍨嬪垎绫荤殑 Agent 閰嶇疆 (鎸?modelName 缂撳瓨閰嶇疆锛岄伩鍏嶉噸澶嶆瀯寤?
    private final Map<String, AiServices<AgentAssistant>> agentBuildersByModel = new ConcurrentHashMap<>();
    
    // 缂撳瓨宸ュ叿鍏冩暟鎹?(棰勭紪璇戠殑宸ュ叿鎻忚堪锛岄伩鍏嶉噸澶嶆壂鎻?
    private volatile boolean toolsMetadataInitialized = false;
    private final Object toolsInitLock = new Object();
    
    /**
     * 鍒涘缓鎴栬幏鍙?Agent 瀹炰緥
     * @param sessionId 浼氳瘽 ID
     * @param modelName 妯″瀷鍚嶇О
     * @return Agent 瀹炰緥
     */
    public AgentAssistant getOrCreateAgent(String sessionId, String modelName) {
        // 纭�繚宸ュ叿鍏冩暟鎹�凡鍒濆�鍖?(澧為噺缂栬瘧鐨勫叧閿?
        initializeToolsMetadata();
        
        // 濡傛灉宸插瓨鍦ㄥ垯鐩存帴杩斿洖
        if (agentCache.containsKey(sessionId)) {
            return agentCache.get(sessionId);
        }
        
        // 鍒涘缓鏂扮殑 Agent
        AgentAssistant agent = createAgent(sessionId, modelName);
        agentCache.put(sessionId, agent);
        
        return agent;
    }
    
    /**
     * 鍒濆�鍖栧苟棰勭紪璇戝伐鍏峰厓鏁版嵁 (鍙�墽琛屼竴娆?
     */
    private void initializeToolsMetadata() {
        if (toolsMetadataInitialized) {
            return;
        }
        
        synchronized (toolsInitLock) {
            if (!toolsMetadataInitialized) {
                log.info("寮€濮嬮�缂栬瘧宸ュ叿鍏冩暟鎹?..");
                // 瑙﹀彂宸ュ叿娉ㄥ唽锛岀‘淇濇墍鏈夊伐鍏烽兘宸插姞杞?
                int toolCount = toolRegistry.getToolCount();
                int skillCount = skillRegistry.getSkillCount();
//                log.info("宸ュ叿鍏冩暟鎹��缂栬瘧瀹屾垚锛屽叡 {} 涓�伐鍏凤紝{} 涓�妧鑳?, toolCount, skillCount);
                toolsMetadataInitialized = true;
            }
        }
    }
    
    /**
     * 鍒涘缓 Agent 瀹炰緥 (浣跨敤缂撳瓨鐨?Builder 閰嶇疆)
     * @param sessionId 浼氳瘽 ID
     * @param modelName 妯″瀷鍚嶇О
     * @return Agent 瀹炰緥
     */
    private AgentAssistant createAgent(String sessionId, String modelName) {
        log.info("鍒涘缓 Agent锛屼細璇?ID: {}, 妯″瀷锛歿}", sessionId, modelName);
        
        // 鑾峰彇鎴栧垱寤鸿�妯″瀷鐨?Builder (澧為噺缂栬瘧锛氬�鐢ㄥ凡鏋勫缓鐨勯厤缃?
        AiServices<AgentAssistant> builder = getOrCreateAgentBuilder(modelName);
        
        // 浣跨敤 Builder 鍒涘缓 Agent 瀹炰緥锛屼粎缁戝畾褰撳墠浼氳瘽鐨勮�蹇?
        return builder
                .chatMemory(MessageWindowChatMemory.builder()
                        .id(sessionId)
                        .maxMessages(20)
                        .chatMemoryStore(chatMemoryStore)
                        .build())
                .build();
    }
    
    /**
     * 鑾峰彇鎴栧垱寤烘寚瀹氭ā鍨嬬殑 Agent Builder (鏍稿績澧為噺缂栬瘧閫昏緫)
     * @param modelName 妯″瀷鍚嶇О
     * @return 宸查厤缃�殑 Builder
     */
    private AiServices<AgentAssistant> getOrCreateAgentBuilder(String modelName) {
        // 濡傛灉宸叉湁缂撳瓨锛岀洿鎺ヨ繑鍥?(閬垮厤閲嶅�缂栬瘧)
        if (agentBuildersByModel.containsKey(modelName)) {
            return agentBuildersByModel.get(modelName);
        }
        
        synchronized (agentBuildersByModel) {
            // 鍙岄噸妫€鏌ラ攣瀹?
            if (agentBuildersByModel.containsKey(modelName)) {
                return agentBuildersByModel.get(modelName);
            }
            
            log.info("棣栨�涓烘ā鍨?[{}] 鏋勫缓 Agent 閰嶇疆 (鎵ц�瀹屾暣缂栬瘧)...", modelName != null ? modelName : "default");
            long startTime = System.currentTimeMillis();
            
            // 鑾峰彇鎸囧畾妯″瀷
           ChatModel model = getChatLanguageModel(modelName);
           
           // 鍒涘缓 Builder锛屾敞鍐屾墍鏈夊伐鍏峰�璞″拰鎶€鑳?(鍙�墽琛屼竴娆?
           // LangChain4j 1.0.0-beta3 浣跨敤 tools() 鏂规硶鎵归噺娉ㄥ唽
           AiServices<AgentAssistant> builder = AiServices.builder(AgentAssistant.class)
                   .chatModel(model)
                   // 娉ㄥ唽鍩虹�宸ュ叿
                   .tools(dateTimeTool, calculationTool, fileTool, webSearchTool)
                   // 娉ㄥ唽鎶€鑳?(鎶€鑳界被涓�殑@Tool 鏂规硶)
                   .tools(codeAnalysisSkill, codeGenerationSkill, troubleshootingSkill);
           
           // 缂撳瓨 Builder 渚涘悗缁��鐢?
            agentBuildersByModel.put(modelName, builder);
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("妯″瀷 [{}] 鐨?Agent 閰嶇疆鏋勫缓瀹屾垚锛岃€楁椂锛歿}ms", modelName != null ? modelName : "default", duration);
            
            return builder;
        }
    }
    
    /**
     * 鑾峰彇鑱婂ぉ妯″瀷
     * @param modelName 妯″瀷鍚嶇О
     * @return 鑱婂ぉ妯″瀷瀹炰緥
     */
    private ChatModel getChatLanguageModel(String modelName) {
        // 濡傛灉娌℃湁鎸囧畾妯″瀷鍚嶇О锛屼娇鐢ㄩ粯璁ゆā鍨?
        if (modelName == null || modelName.isEmpty()) {
            return chatModel;
        }
        
        // TODO: 鏍规嵁妯″瀷鍚嶇О鍒涘缓涓嶅悓鐨勬ā鍨嬪疄渚?
        // 鐩�墠鏆傛椂浣跨敤榛樿�妯″瀷
        return chatModel;
    }
    
    /**
     * 娓呴櫎 Agent 缂撳瓨
     * @param sessionId 浼氳瘽 ID
     */
    public void clearAgentCache(String sessionId) {
        agentCache.remove(sessionId);
        log.info("娓呴櫎 Agent 缂撳瓨锛歿}", sessionId);
    }
    
    /**
     * 娓呴櫎鎵€鏈夌紦瀛?(鍖呮嫭 Builder 缂撳瓨)
     */
    public void clearAllCaches() {
        agentCache.clear();
        agentBuildersByModel.clear();
        toolsMetadataInitialized = false;
        log.info("娓呴櫎鎵€鏈?Agent 缂撳瓨");
    }
    
    /**
     * 鑾峰彇宸ュ叿鍒楄〃淇℃伅
     * @return 宸ュ叿淇℃伅 Map
     */
    public Map<String, Object> getToolInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("total", toolRegistry.getToolCount());
        info.put("categories", toolRegistry.getCategories());
        info.put("tools", toolRegistry.getAllTools().stream()
                .map(tool -> Map.of(
                        "name", tool.getName(),
                        "description", tool.getDescription(),
                        "category", tool.getCategory()
                ))
                .toList());
        return info;
    }
    
    /**
     * 鑾峰彇鎶€鑳藉垪琛ㄤ俊鎭?
     * @return 鎶€鑳戒俊鎭?Map
     */
    public Map<String, Object> getSkillInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("total", skillRegistry.getSkillCount());
        info.put("categories", skillRegistry.getCategories());
        info.put("skills", skillRegistry.getAllSkills().stream()
                .map(skill -> Map.of(
                        "name", skill.getName(),
                        "description", skill.getDescription(),
                        "category", skill.getCategory(),
                        "triggerCondition", skill.getTriggerCondition(),
                        "priority", skill.getPriority()
                ))
                .toList());
        return info;
    }
}
