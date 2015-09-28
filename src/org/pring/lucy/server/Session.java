package org.pring.lucy.server;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Session {
  protected static Map<String, Session> sessionList = new ConcurrentHashMap<String, Session>();

  public static Session getSession(String sessionId) {
    if (!sessionList.containsKey(sessionId))
      sessionList.put(sessionId, new Session(sessionId, System.currentTimeMillis()));

    return sessionList.get(sessionId);
  }
  
  private Map<String, Object> sessionData = new HashMap<String, Object>();
  public final String sessionId;
  private long lastUse;

  protected Session(String sessionId, long lastUsed) {
    this.sessionId = sessionId;
    this.lastUse = lastUsed;
    
    if (!sessionData.containsKey(sessionId))
      sessionData.put(sessionId, new HashMap<String, Object>());
  }
  
  protected Session() { sessionId = ""; }
  
  public void lastUse(long lastUse) {
    this.lastUse = lastUse;
  }
  
  public long lastUse() {
    return lastUse;
  }
  
  public Object get(String key) {
    if (sessionData.containsKey(key))
      return sessionData.get(key);
    else
      return new Object();
  }
  
  public boolean getBoolean(String key) {
    if (sessionData.containsKey(key))
      return (boolean) sessionData.get(key);
    else
      return false;
  }
  
  public <T> void put(String key, T value) {
    sessionData.put(key, value);
  }
  
  public void remove(String key) {
    sessionData.remove(key);
  }
  
  public Map<String, Object> map() {
    return sessionData;
  }
  
  public void destroy() {
    sessionData = null;
    sessionList.remove(sessionId);
  }
}
