package org.pring.lucy.template;

import java.lang.reflect.Method;
import java.util.Map;

public class Template {
  private static Method m;
  
  public Template() {
    Class<?> clazz = getClass();
    
    try {
      for (Method mm: clazz.getMethods()) {
        if (mm.getName().equals("_1")) {
          m = mm;
          break;
        }
      }
    } catch (SecurityException e) {
      e.printStackTrace();
    }
  }

  public String render(Map<String, Object> args) throws Exception {
    Object arg[] = new Object[args.size()];
    
    int i = 0;
    for (Object o : args.values())
      arg[i++] = o;
    
    return (String) m.invoke(null, arg);
  }
}
