package com.weaving.llm.common.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import java.util.Map;

/**
 * @Author: 35038
 */
public class JsonUtils {
    /**
     * 深度合并两个 JSON 对象：defaultNode 为基础，userNode 覆盖同名字段
     */
    public static JSONObject deepMerge(JSONObject defaultNode, JSONObject userNode) {
        // 1. 处理空值情况
        if (defaultNode == null && userNode == null) {
            return new JSONObject();
        }
        if (defaultNode == null) {
            return deepCopy(userNode);
        }
        if (userNode == null) {
            return deepCopy(defaultNode);
        }

        // 2. 创建结果对象（深拷贝默认节点）
        JSONObject result = deepCopy(defaultNode);

        // 3. 遍历用户节点，合并到结果中
        for (Map.Entry<String, Object> entry : userNode.entrySet()) {
            String key = entry.getKey();
            Object userValue = entry.getValue();

            // 如果默认节点中也存在这个key
            if (result.containsKey(key)) {
                Object defaultValue = result.get(key);

                // 递归合并：如果都是JSONObject
                if (defaultValue instanceof JSONObject && userValue instanceof JSONObject) {
                    result.put(key, deepMerge((JSONObject) defaultValue, (JSONObject) userValue));
                }
                // 特殊处理：如果都是JSONArray，这里可以选择替换或合并，这里选择替换
                else if (defaultValue instanceof JSONArray && userValue instanceof JSONArray) {
                    // 根据需求选择：
                    // 1. 直接替换（使用用户数组）
                    // result.put(key, userValue);

                    // 2. 合并数组（去重）
                    result.put(key, mergeJsonArray((JSONArray) defaultValue, (JSONArray) userValue));

                    // 3. 追加到末尾
                    // JSONArray mergedArray = deepCopy((JSONArray) defaultValue);
                    // mergedArray.addAll((JSONArray) userValue);
                    // result.put(key, mergedArray);
                }
                // 其他情况：直接使用用户的值（覆盖）
                else {
                    result.put(key, deepCopyValue(userValue));
                }
            } else {
                // 默认节点没有这个key，直接添加用户的值
                result.put(key, deepCopyValue(userValue));
            }
        }

        return result;
    }

    /**
     * 深拷贝 JSONObject
     */
    private static JSONObject deepCopy(JSONObject source) {
        if (source == null) {
            return null;
        }
        return new JSONObject(source); // fastjson2 的构造函数实现了深拷贝
    }

    /**
     * 深拷贝值（处理各种类型）
     */
    private static Object deepCopyValue(Object value) {
        if (value == null) {
            return null;
        }

        // 处理 JSONObject
        if (value instanceof JSONObject) {
            return deepCopy((JSONObject) value);
        }

        // 处理 JSONArray
        if (value instanceof JSONArray) {
            JSONArray sourceArray = (JSONArray) value;
            JSONArray newArray = new JSONArray(sourceArray.size());
            for (Object item : sourceArray) {
                newArray.add(deepCopyValue(item));
            }
            return newArray;
        }

        // 基础类型和String（不可变对象）直接返回
        // 注意：如果value是自定义对象，这里需要特殊处理
        return value;
    }

    /**
     * 合并两个 JSON 数组（去重）
     */
    private static JSONArray mergeJsonArray(JSONArray array1, JSONArray array2) {
        JSONArray result = new JSONArray();

        // 添加第一个数组的所有元素
        for (Object item : array1) {
            if (!containsValue(result, item)) {
                result.add(deepCopyValue(item));
            }
        }

        // 添加第二个数组的所有不重复元素
        for (Object item : array2) {
            if (!containsValue(result, item)) {
                result.add(deepCopyValue(item));
            }
        }

        return result;
    }

    /**
     * 检查 JSONArray 是否包含某个值（深度比较）
     */
    private static boolean containsValue(JSONArray array, Object value) {
        for (Object item : array) {
            if (isEqual(item, value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 深度比较两个对象是否相等
     */
    private static boolean isEqual(Object obj1, Object obj2) {
        // 处理null情况
        if (obj1 == null && obj2 == null) return true;
        if (obj1 == null || obj2 == null) return false;

        // 如果是JSONObject，比较内容
        if (obj1 instanceof JSONObject && obj2 instanceof JSONObject) {
            return ((JSONObject) obj1).equals(obj2);
        }

        // 如果是JSONArray，比较内容
        if (obj1 instanceof JSONArray && obj2 instanceof JSONArray) {
            JSONArray arr1 = (JSONArray) obj1;
            JSONArray arr2 = (JSONArray) obj2;

            if (arr1.size() != arr2.size()) return false;

            for (int i = 0; i < arr1.size(); i++) {
                if (!isEqual(arr1.get(i), arr2.get(i))) {
                    return false;
                }
            }
            return true;
        }

        // 其他类型直接比较
        return obj1.equals(obj2);
    }

    /**
     * 便捷方法：从字符串解析并合并
     */
    public static JSONObject deepMerge(String defaultJson, String userJson) {
        JSONObject defaultObj = JSONObject.parseObject(defaultJson);
        JSONObject userObj = JSONObject.parseObject(userJson);
        return deepMerge(defaultObj, userObj);
    }

    /**
     * 将对象转换为 Map（使用 fastjson2）
     * @param obj 任意对象
     * @return Map<String, Object>
     */
    public static Map<String, Object> toMap(Object obj) {
        if (obj == null) {
            return null;
        }
        return JSONObject.parseObject(JSON.toJSONString(obj), new com.alibaba.fastjson2.TypeReference<Map<String, Object>>() {});
    }

    /**
     * 将对象转换为 JSONObject（使用 fastjson2）
     * @param obj 任意对象
     * @return JSONObject
     */
    public static JSONObject toJsonObject(Object obj) {
        if (obj == null) {
            return null;
        }
        return JSON.parseObject(JSON.toJSONString(obj));
    }

    /**
     * 测试用例示例
     */
    public static void main(String[] args) {
        // 示例1：基本合并
        JSONObject defaultConfig = JSONObject.parseObject(
                "{\"theme\": \"light\", \"fontSize\": 14, \"layout\": {\"sidebar\": true}}"
        );
        JSONObject userConfig = JSONObject.parseObject(
                "{\"theme\": \"dark\", \"layout\": {\"sidebar\": false, \"header\": true}}"
        );

        JSONObject merged = deepMerge(defaultConfig, userConfig);
        System.out.println(merged);
        // 输出：{"theme":"dark","fontSize":14,"layout":{"sidebar":false,"header":true}}

        // 示例2：数组合并
        JSONObject defaultObj = JSONObject.parseObject("{\"tags\": [\"java\", \"spring\"]}");
        JSONObject userObj = JSONObject.parseObject("{\"tags\": [\"java\", \"mybatis\", \"fastjson\"]}");

        JSONObject merged2 = deepMerge(defaultObj, userObj);
        System.out.println(merged2);
        // 输出：{"tags":["java","spring","mybatis","fastjson"]}
    }
}