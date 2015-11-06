package com.pring.lucy.server;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.pring.lucy.template.TemplateEngine;

public class HotReload extends ClassLoader implements Runnable {
  public final boolean developmentMode;
  private Reloader loader = new Reloader();
  
  private Map<WatchKey, Path> keys = new HashMap<WatchKey, Path>();
  private WatchService watcher;
  
  protected HotReload(boolean d) {
    developmentMode = d;
    
    try {
      this.watcher = FileSystems.getDefault().newWatchService();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  public void start() {
    new Thread(this).start();
  }
  
  public void registerFolderModification(Path start) throws IOException {
    Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
      public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
        registerAll(dir);
        return FileVisitResult.CONTINUE;
      }
    });
  }
  
  public HotReload registerAll(Path d) {
    File f = d.toFile();
    
    if (f.isDirectory()) {
      try {
        WatchKey key = d.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        keys.put(key, d);
        
        for(File file: f.listFiles())
          registerAll(file.toPath());
        
      } catch (IOException e) {
        e.printStackTrace();
      }
    } else if (f.getName().endsWith(".class")) {
      if (f.getAbsolutePath().matches("(.*)controller(.*)")) {
        Class<? extends HttpController> cls = loader.findClass(f.getAbsolutePath());
        String clsPkg = f.getAbsolutePath().toString().split("/../")[1].toLowerCase()
            .replace('/', '.').replace(".class", "");

        Server.controllers.put(clsPkg, cls);
        
        for (Method m : cls.getDeclaredMethods()) {
          Server.methods.put(clsPkg + '.' + m.getName(), m);
        }
        // System.out.printf("Loaded controller: %s\n", f.getName());
      }
    } else if (f.getName().endsWith(".html")) {
      // System.out.printf("Loaded template: %s\n", f.getName());
    }
    
    return this;
  }
  
  @Override
  public void run() {
    if (developmentMode) {
      while (true) {
        try {
          WatchKey key = watcher.take();
          
          if (keys.get(key) != null) {
            Path dir = keys.get(key);
            
            Iterator<WatchEvent<?>> it = key.pollEvents().iterator();
            while (it.hasNext()) {
              WatchEvent<?> event = it.next();
                          
              if (event.kind() != OVERFLOW) {
                @SuppressWarnings("unchecked")
                WatchEvent<Path> ev = (WatchEvent<Path>) event;
  
                Path name = ev.context();
                Path child = dir.resolve(name);
                if (event.kind() == ENTRY_CREATE || 
                    event.kind() == ENTRY_MODIFY ||
                    event.kind() == ENTRY_DELETE) {
                  
                  if (event.kind() == ENTRY_CREATE && Files.isDirectory(child)) {
                    try {
                      registerFolderModification(child);
                    } catch (IOException e) {
                      e.printStackTrace();
                    }
                  }
                  
                  if (child.toString().endsWith(".class")) {
                    loader = new Reloader();
                    
                    if (child.toAbsolutePath().toString().matches("(.*)controller(.*)")) {
                      if (event.kind() == ENTRY_DELETE) {
                        Server.controllers.remove(
                            child.toAbsolutePath().toString().split("/../")[1]
                                .toLowerCase().replace('/', '.').replace(".class", ""), 
                            loader.findClass(child.toAbsolutePath().toString()));
                      } else {
                        Class<? extends HttpController> cls =
                            loader.findClass(child.toAbsolutePath().toString());
                        String clsPkg = child.toAbsolutePath().toString().split("/../")[1]
                            .toLowerCase().replace('/', '.').replace(".class", "");
                        
                        Server.controllers.put(clsPkg, cls);
                        
                        for (Method m : cls.getMethods()) {
                          Server.methods.put(clsPkg + '.' + m.getName(), m);
                        }
                      }
                    }
                  } else if (child.toString().endsWith(".html")) {
                    /*
                    TemplateEngine.recompileTemplate(
                        child.toAbsolutePath().toString().split("/../")[1]
                            .replace('/', '.').replace(".html", "")
                            .replace("view", "controller"));
                            */
                    
                    TemplateEngine.recompileTemplates();
                  }
                }
              }
              
              key.reset();
            }
          }
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
  }
  
  class Reloader extends ClassLoader {
    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends HttpController> findClass(String path) {
      byte[] bytes;
      try {
        bytes = loadClassData(path);

        return 
            (Class<? extends HttpController>) 
              defineClass(
                  path.split("/../")[1].replace('/', '.').replace(".class", ""),
                  bytes, 0, bytes.length);
      } catch (Exception e) {
        e.printStackTrace();
      }
      
      return null;
    }

    private byte[] loadClassData(String classPath) throws IOException {
      File file = new File(classPath);
      byte[] buffer = new byte[(int) file.length()];

      if (file.exists()) {
        try {
          FileInputStream fis = new FileInputStream(file);
          DataInputStream dis = new DataInputStream(fis);
          dis.readFully(buffer);
          dis.close();
          
          return buffer;
        } catch (FileNotFoundException e) {
          e.printStackTrace();
        }
      }
      return null;
    }
  }
}