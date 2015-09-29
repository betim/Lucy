package com.pring.lucy.server;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.DiskAttribute;
import io.netty.handler.codec.http.multipart.DiskFileUpload;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.stream.ChunkedWriteHandler;

public class Server {
  static {
    DiskFileUpload.deleteOnExitTemporaryFile = true;
    DiskFileUpload.baseDirectory = null;
    DiskAttribute.deleteOnExitTemporaryFile = true;
    DiskAttribute.baseDirectory = null;
  }
  
  protected static int port = 8080;
  protected static boolean developmentMode = true;
  protected static String staticFileLocation = "/var/www";
  protected static int maxChunkSizeInBytes = 8192;
  protected static String tmpFileLocation = "/tmp";
  protected static boolean withCookies = true;
  protected static String sessionName = "SID";
  protected static int sessionAge = 0;
  protected static boolean compress = false;
  protected static boolean upload = false;
  
  protected static String projectLocation = "";
  protected static boolean inJar = false;
  
  protected static Session DUMMY_SESSION = new Session();
  protected static File DUMMY_FILE = new File("/tmp/dummy");
  
  public static final HttpDataFactory DATA_FACTORY = new DefaultHttpDataFactory(true);
  
  protected static HikariDataSource ds = new HikariDataSource();
  
  protected static Map<String, Class<? extends HttpController>> controllers = 
      new ConcurrentHashMap<>();
  
  protected static Map<String, Method> methods = 
      new ConcurrentHashMap<>();
  
  
  public Server port(int p) {
    port = p;
    return this;
  }
  
  public Server production() {
    developmentMode = false;
    return this;
  }
  
  public Server staticLocation(String p) {
    staticFileLocation = p;
    return this;
  }
  
  public Server upload(String location, boolean yes) {
    tmpFileLocation = location;
    upload = yes;
    return this;
  }

  public Server database(String cs, String u, String p) {
    HikariConfig config = new HikariConfig();

    config.setJdbcUrl(cs);
    config.setUsername(u);
    config.setPassword(p);
    
    config.addDataSourceProperty("cachePrepStmts", "true");
    config.addDataSourceProperty("prepStmtCacheSize", "350");
    config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

    ds = new HikariDataSource(config);
    
    return this;
  }
  
  public Server gzip() {
    compress = true;
    return this;
  }
  
  public Server sessionName(String name) {
    sessionName = name;
    return this;
  }
  
  public Server sessionAge(int seconds) {
    sessionAge = seconds;
    return this;
  }
  
  public int sessionAge() {
    return sessionAge;
  }
  
  public Server withoutCookies() {
    withCookies = false;
    
    return this;
  }
  
  public Server maxChunkSizeInBytes(int maxChunkSizeBytes) {
    maxChunkSizeInBytes = maxChunkSizeBytes;
    
    return this;
  }
  
  final int BUFFER = 2048;
  
  public void init() {
    inJar = Server.class.getResource("Server.class").toString().startsWith("jar");

    projectLocation = getClass()
        .getResource("/" + getClass().getName().replace('.', '/') + ".class")
        .toString().split("file:")[1].replace("Main.class", "../");

    if (inJar && developmentMode == false) {
      projectLocation = "/tmp/netty-1/";

      File tmpDir = new File(projectLocation);
      if (tmpDir.exists() && tmpDir.isDirectory()) {
        deleteDirectory(tmpDir);
      }
      
      tmpDir.mkdir();
      
      try {
        BufferedOutputStream dest = null;
        ZipInputStream zis = new ZipInputStream(new FileInputStream(Server.class
            .getProtectionDomain().getCodeSource().getLocation().toURI().getPath()));
        ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {
          if (entry.isDirectory())
            new File(projectLocation + File.separator + entry.getName()).mkdirs();
          else {
            new File(new File(projectLocation + File.separator + entry.getName()).getParent()).mkdirs();
            
            int count;
            byte data[] = new byte[BUFFER];
            FileOutputStream fos = new FileOutputStream(projectLocation + File.separator + entry.getName());
            dest = new BufferedOutputStream(fos, BUFFER);
            while ((count = zis.read(data, 0, BUFFER)) != -1) {
              dest.write(data, 0, count);
            }
            
            dest.flush();
            dest.close();
          }
        }
        
        zis.close();
      } catch (Exception e) {
        e.printStackTrace();
      }

      projectLocation = "/tmp/netty-1" + (getClass()
          .getResource("/" + getClass().getName().replace('.', '/') + ".class")
          .toString().split("file:")[1].replace("Main.class", "../")).split("!")[1];
    }
    new HotReload(developmentMode).registerAll(Paths.get(projectLocation)).start();

    new Thread(() -> {
      Thread.currentThread().setName("SESSION PURGER");
      
      long sessionAgeMillis = sessionAge * 1000;
      
      while (true) {
        try { Thread.sleep(sessionAge * 1000 + 1000); }
        catch (Exception e) { }
        
        long now = System.currentTimeMillis();
        for (Session s : Session.sessionList.values())
          if (now > s.lastUse() + sessionAgeMillis) {
            Session.sessionList.remove(s.sessionId);
          }
      }
    }).start();
  }

  public static String getProjectLocation() {
    return projectLocation;
  }
  
  public static boolean isDevelopmentMode() {
    return developmentMode;
  }
  
  public void serve() throws InterruptedException {
    init();
    
    /*
    EpollEventLoopGroup bossGroup = new EpollEventLoopGroup();
    EpollEventLoopGroup workerGroup = new EpollEventLoopGroup(4);
     */
    
    EventLoopGroup bossGroup = new NioEventLoopGroup();
    EventLoopGroup workerGroup = new NioEventLoopGroup();
    
    try {
      ServerBootstrap b = new ServerBootstrap();
      
      b.option(ChannelOption.SO_BACKLOG, 1024);
      b.group(bossGroup, workerGroup)
        .channel(NioServerSocketChannel.class)
        .option(ChannelOption.TCP_NODELAY, true)
        // .option(ChannelOption.SO_BACKLOG, 100)

        // .handler(new LoggingHandler(LogLevel.INFO))
        //.channel(EpollServerSocketChannel.class)
        //.childOption(EpollChannelOption.EPOLL_MODE, EpollMode.LEVEL_TRIGGERED)
        .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
        .childHandler(new ChannelInitializer<SocketChannel>() {
          @Override
          public void initChannel(SocketChannel ch) {
            ch.pipeline().addLast(new HttpServerCodec());
            ch.pipeline().addLast(new HttpRequestDecoder(65536, 65536, maxChunkSizeInBytes, false));
            ch.pipeline().addLast(new HttpObjectAggregator(65536));
            ch.pipeline().addLast(new ChunkedWriteHandler());
            
            if (compress)
              ch.pipeline().addLast("deflater", new HttpContentCompressor(1));
            
            ch.pipeline().addLast(new Handler());
            ch.pipeline().addLast(new StaticFileHandler());
          }   
        });
      
      Channel ch = b.bind(port).sync().channel();
      
      System.out.println("Ready. Navigate to http://localhost:" + port + '/');
      ch.closeFuture().sync();
    } finally {
      bossGroup.shutdownGracefully();
      workerGroup.shutdownGracefully();
    }
  }
  
  private static boolean deleteDirectory(File directory) {
    if (directory.exists()) {
      File[] files = directory.listFiles();
      if (null != files) {
        for (int i = 0; i < files.length; i++) {
          if (files[i].isDirectory()) {
            deleteDirectory(files[i]);
          } else {
            files[i].delete();
          }
        }
      }
    }
    
    return (directory.delete());
  }
}
