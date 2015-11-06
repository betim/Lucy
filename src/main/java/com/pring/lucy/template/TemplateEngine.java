package com.pring.lucy.template;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringEscapeUtils;

import com.pring.lucy.server.Server;

public class TemplateEngine {
  private static String imports = 
      "import com.pring.lucy.template.Template;\n"
      + "import com.pring.lucy.server.SqlDB;\n"
      + "import com.pring.lucy.server.SqlDB.ResultSetIterator;\n"
      + "import java.util.HashMap;\n"
      + "import java.util.Iterator;\n"
      + "import java.util.Map;\n"
      + "import java.util.Set;\n"
      + "import java.sql.ResultSet;\n"
      + "import javax.sql.rowset.CachedRowSet;\n"
      + "import io.netty.handler.codec.http.cookie.Cookie;\n";

  private static Compiler javaCompiler = Compiler.instance();
  private static Map<String, Class<? extends Template>> cache =
      new ConcurrentHashMap<String, Class<? extends Template>>();
  
  public static Template getTemplate(String template, Map<String, Object> templateFields)
      throws Exception {
    if (!cache.containsKey(template))
      cache.put(template, compileTemplate(template, templateFields));

    return cache.get(template).newInstance();
  }
  
  public static void recompileTemplate(String path) {
    cache.remove(path);
  }
  
  public static void recompileTemplates() {
    for (String t : cache.keySet())
      cache.remove(t);
  }

  @SuppressWarnings("unchecked")
  private static Class<? extends Template> compileTemplate(String path, Map<String, Object> templateFields)
      throws Exception {
    StringBuilder clazz = new StringBuilder();
    
    String tokens[] = path.split("\\.");
    String name = tokens[tokens.length-1];

    clazz.append(imports)
      .append("\npublic class ")
      .append(name)
      .append(" extends Template { ").append("\npublic static String _1(");

    int i = 0;
    for (String s : templateFields.keySet()) {
      clazz.append(templateFields.get(s).getClass().getTypeName().replace('$', '.'))
        .append(" ").append(s);
      
      if (++i < templateFields.size())
        clazz.append(',');
    }

    clazz.append(") throws Exception { \nStringBuilder sb = new StringBuilder(); ");
    
    clazz.append(parseFile(path, path));
    
    clazz.append("return sb.toString();\n}\n}");
    // System.out.println(clazz.toString());
    
    return (Class<? extends Template>) javaCompiler.compile(name, clazz.toString());
  }

  private static String parseFile(String path, String path2) throws Exception {
    StringBuilder buffer = new StringBuilder();
    
    File f = new File(path);

    if (!f.exists()) {
      String pathTokens[] = path2.split("\\.");
      pathTokens[pathTokens.length-1] = path;
      
      f = new File(Server.getProjectLocation() + 
        String.join(".", pathTokens).replace("controller", "view").replace('.', '/') + ".html");
    }
    
    if (!f.exists())
      f = new File(Server.getProjectLocation() + 
        path.replace("controller", "view").replace('.', '/') + ".html");

    if (f.exists()) {
      InputStreamReader isr = new InputStreamReader(new FileInputStream(f));
      BufferedReader in = new BufferedReader(isr);
  
      boolean inJavaCodeBlock = false;
      StringBuilder javaCodeBlock = new StringBuilder();
      String line;
      while ((line = in.readLine()) != null) {
        if (line.matches("(.*)\\{\\{(.*)include(.*)\\}\\}(.*)")) {
          String[] _tokens = line.trim().replace("{{", "").replace("}}", "").split(" ");
          buffer.append(parseFile(_tokens[2].replace("\"", "").replace(";", ""), path2));
          
          continue;
        } else if (line.matches("(.*)\\{\\{(.*)\\}\\}(.*)")) {
          int pos = 0, lastPos = 0;
          while ((pos = line.indexOf("{{", lastPos)) > -1) {
            int end = line.indexOf("}}", pos);
            String html = line.substring(lastPos, pos);

            if (html.length() > 0)
              buffer.append("sb.append(\"")
                .append(StringEscapeUtils.escapeJava(html))
                .append("\");\n");

            String java = line.substring(pos + 2, end);
            if (java.length() > 0)
              buffer.append(parseLine("$" + java + ";", true, path2));

            lastPos = end + 2;
          }
          
          String _t = line.substring(lastPos, line.length());
          if (_t.length() > 0)
            buffer.append("sb.append(\"")
              .append(StringEscapeUtils.escapeJava(_t))
              .append("\\n").append("\");\n");
        } else if (line.matches("(.*)\\{\\{(.*)")) {
          String tok[] = line.split("\\{\\{");
          if (tok.length > 1) {
            javaCodeBlock.append("sb.append(\"")
              .append(StringEscapeUtils.escapeJava(tok[0]))
              .append("\\n").append("\");\n");
            
            javaCodeBlock.append(parseLine(tok[1], false, path2));
          } else
            javaCodeBlock.append(parseLine(line.replace("{{", ""), false, path2));
          inJavaCodeBlock = true;
        } else if (line.matches("(.*)\\}\\}(.*)")) {
          String tok[] = line.split("\\}\\}");
          if (tok.length > 1) {
            javaCodeBlock.append(parseLine(tok[0], false, path2));
            
            javaCodeBlock.append("sb.append(\"")
              .append(StringEscapeUtils.escapeJava(tok[1]))
              .append("\\n").append("\");\n");
          } else
            javaCodeBlock.append(parseLine(line.replace("}}", ""), false, path2));
  
          buffer.append(javaCodeBlock.toString()
              .replace("echo", "sb.append")
              .replace("System.out.println", "sb.append")
              .replace("System.err.println", "sb.append"));
          
          javaCodeBlock.setLength(0);
          inJavaCodeBlock = false;
        } else if (inJavaCodeBlock) {
          javaCodeBlock.append(parseLine(line, false, path2)).append('\n');
        } else
          buffer.append("sb.append(\"")
            .append(StringEscapeUtils.escapeJava(line))
            .append("\\n").append("\");\n");
      }
      
      in.close();
      isr.close();
    }

    return buffer.toString();
  }
  
  private static String parseLine(String line, boolean oneLiner, String path2) throws Exception {
    StringBuilder buf = new StringBuilder();
    
    if (line.matches("(.*)include(.*)")) {
      String[] _tokens = line.trim().split(" ");
      return parseFile(_tokens[1].replace("\"", "").replace(";", ""), path2);
    } else if (line.matches("(.*)\\<(.*)")) {
      int end = 0;
      int pos = 0;
      
      try {
        while ((pos = line.indexOf("$", end)) != -1) {
          if (line.charAt(pos + 1) == '$') {
            String html = line.substring(end + 1, pos + 1);
            end = pos + 2;

            buf.append("sb.append(\"")
              .append(StringEscapeUtils.escapeJava(html)).append("\");\n");
            
            continue;
          } else {
            String html = line.substring(end + 1, pos);
            
            end = line.indexOf(";", pos);
            
            String jcode = line.substring(pos + 1, end);
  
            if (html.length() > 0)
              buf.append("sb.append(\"")
                .append(StringEscapeUtils.escapeJava(html)).append("\");\n");
            
            if (jcode.length() > 0)
              buf.append("sb.append(").append(jcode).append(");\n");
          }
        }
        
        String html = line.substring(end + 1, line.length());
        if (html.length() > 0)
          buf.append("sb.append(\"")
            .append(StringEscapeUtils.escapeJava(html)).append("\");\n");
        
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else {
      if (oneLiner)
        buf.append("sb.append(")
        .append(line.replace("$", "").replace(";", "")
          .replace("echo", "sb.append")
          .replace("System.out.println", "sb.append")
          .replace("System.err.println", "sb.append")
        ).append(");\n");
      else
        return line;
    }
    
    return buf.toString();
  }
}
