package com.capstone.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * ScenarioContext — Shared data store for a single Cucumber scenario.
 *
 * WHY: A Cucumber scenario may span multiple step definition classes
 * (LoginStepDefs, NotesStepDefs, ApiStepDefs). Each class needs access
 * to data produced by earlier steps (e.g., auth token from login step,
 * note title from creation step).
 *
 * HOW (Picocontainer DI):
 *   Picocontainer creates ONE ScenarioContext instance per scenario.
 *   It injects the SAME instance into all step classes that declare it
 *   as a constructor parameter. So when LoginStepDefs stores a token,
 *   ApiStepDefs reads the exact same token.
 *
 * Thread safety: Each scenario runs in its own Picocontainer scope,
 * so there is no cross-scenario sharing.
 */
public class ScenarioContext {

    // ==========================================
    // CONTEXT KEY CONSTANTS
    // Define all keys here to avoid string typos
    // ==========================================
    public static final String AUTH_TOKEN      = "AUTH_TOKEN";
    public static final String NOTE_TITLE      = "NOTE_TITLE";
    public static final String NOTE_CATEGORY   = "NOTE_CATEGORY";
    public static final String NOTE_DESC       = "NOTE_DESCRIPTION";
    public static final String NOTE_ID         = "NOTE_ID";
    public static final String USER_EMAIL      = "USER_EMAIL";
    public static final String API_RESPONSE    = "API_RESPONSE";
    public static final String RESPONSE_TIME   = "RESPONSE_TIME";
    public static final String CREATED_NOTE_ID = "CREATED_NOTE_ID";

    // Internal storage — a simple HashMap
    private final Map<String, Object> context = new HashMap<>();

    /**
     * Stores a value in the context with the given key.
     * Use the constants defined above (e.g., ScenarioContext.AUTH_TOKEN).
     *
     * @param key   Context key (use constants to avoid typos)
     * @param value Any object (String, Response, Map, etc.)
     */
    public void set(String key, Object value) {
        context.put(key, value);
    }

    /**
     * Retrieves a value from context and casts it to the expected type.
     *
     * @param key   Context key
     * @param type  Expected class type
     * @param <T>   Generic return type
     * @return Value cast to type T, or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Object value = context.get(key);
        if (value == null) return null;
        return type.cast(value);
    }

    /**
     * Retrieves a String value from context.
     * Convenience method for the most common case.
     *
     * @param key Context key
     * @return String value, or empty string if not found
     */
    public String getString(String key) {
        Object value = context.get(key);
        return value != null ? value.toString() : "";
    }

    /**
     * Checks if a key exists and has a non-null value in context.
     */
    public boolean contains(String key) {
        return context.containsKey(key) && context.get(key) != null;
    }

    /**
     * Removes a key from context.
     */
    public void remove(String key) {
        context.remove(key);
    }

    /**
     * Clears all context data (called between scenarios by Picocontainer).
     */
    public void clear() {
        context.clear();
    }
}
