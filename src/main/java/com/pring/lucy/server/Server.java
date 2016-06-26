package com.pring.lucy.server;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.ProcessBuilder.Redirect;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollMode;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.DiskAttribute;
import io.netty.handler.codec.http.multipart.DiskFileUpload;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
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
  protected static boolean epoll = false;
  protected static boolean join = false;
  
  protected static boolean database = false;
  private static HikariConfig config;
  
  protected static boolean mqtt = false;
  protected static MqttClient mqttClient;
  
  protected static boolean webSocket = false;
  protected static String webSocketPath = "";
  protected static Class<? extends HttpController> webSocketHandlerClass;
  protected static Method webSocketHandler;
  
  protected static String projectLocation = "";
  protected static boolean inJar = false;
  
  protected static Session DUMMY_SESSION = new Session();
  protected static File DUMMY_FILE = new File("/tmp/dummy");
  
  public static final HttpDataFactory DATA_FACTORY = new DefaultHttpDataFactory(true);
  
  protected static HikariDataSource ds = new HikariDataSource();
  
  protected static Map<String, Class<? extends HttpController>>
    controllers = new ConcurrentHashMap<>();

  protected static Map<String, Method> methods = new ConcurrentHashMap<>();
  
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

  public Server database(String cs, String u, String p, String... driver) {
    config = new HikariConfig();

    config.setJdbcUrl(cs);
    config.setUsername(u);
    config.setPassword(p);

    config.addDataSourceProperty("cachePrepStmts", "true");
    config.addDataSourceProperty("prepStmtCacheSize", "250");
    config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
    config.addDataSourceProperty("useServerPrepStmts", "false");
    
    config.setInitializationFailFast(true);
    config.setIdleTimeout(10000);
    config.setMaximumPoolSize(2);
    
    if (driver.length > 0)
      config.setDriverClassName(driver[0]);
    
    database = true;
    
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
  
  public Server epoll() {
    epoll = true;
    return this;
  }
  
  public Server sync() {
    join = true;
    return this;
  }

  public Server mqtt(String broker, MqttCallbackListener listener, String topic) {
    mqtt = true;

    new Thread(() -> {
      Thread.currentThread().setName("MQTT");
      MemoryPersistence persistence = new MemoryPersistence();

      try {
        mqttClient = new MqttClient(broker, 
            "Lucy-" + String.valueOf(System.currentTimeMillis()), persistence);
        
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);
        
        // System.out.println("Connecting to broker: " + broker);
        mqttClient.connect(connOpts);
        System.out.println("Connected to broker: " + broker);
        
        if (listener != null) {
          mqttClient.subscribe(topic);
          mqttClient.setCallback(new MqttCallback() {
            @Override
            public void messageArrived(String topic, MqttMessage msg) throws Exception {
              listener.handleMessage(topic, msg.toString());
            }
            
            @Override
            public void deliveryComplete(IMqttDeliveryToken token) { }

            @Override
            public void connectionLost(Throwable cause) { }
          });
        }
      } catch (MqttException me) {
        System.err.println("Reason " + me.getReasonCode() + " msg " + me.getMessage());
        me.printStackTrace();
      }
    }).start();

    return this;
  }
  
  public Server webSocket(String webSocketPathEndPoint) {
    webSocket = true;
    webSocketPath = webSocketPathEndPoint;

    return this;
  }
  
  private static final int BUFFER = 2048;
  
  public void init() throws URISyntaxException {
    inJar = Server.class.getResource("Server.class").toString().startsWith("jar");

    if (inJar && !developmentMode) {
      projectLocation = "/tmp/lucy-1/";

      File tmpDir = new File(projectLocation);
      if (tmpDir.exists() && tmpDir.isDirectory()) {
        deleteDirectory(tmpDir);
      }
      tmpDir.mkdir();

      extractJar(Server.class.getProtectionDomain().getCodeSource()
          .getLocation().toURI().toString().split(":")[1], projectLocation);

      try {
        ProcessBuilder pb = new ProcessBuilder("java", "entry.Main");
        pb.directory(new File(projectLocation));
        pb.redirectOutput(Redirect.INHERIT);
        pb.redirectError(Redirect.INHERIT);
        
        Process p = pb.start();
        p.waitFor();
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else {
      try {
        projectLocation = getClass()
            .getResource("/" + getClass().getName().replace('.', '/') + ".class")
            .toString().split("file:")[1].replace("Main.class", "../");
      } catch (Exception e) {
        projectLocation = getClass()
            .getResource("/" + getClass().getName().replace('.', '/') + ".class")
            .toString().split("rsrc:")[1].replace("Main.class", "../");
      }

      new HotReload(developmentMode).registerAll(Paths.get(projectLocation)).start();
      new Thread(() -> {
        Thread.currentThread().setName("SESSION PURGER");
        
        long sessionAgeMillis = sessionAge * 1000;
        
        while (true) {
          try { Thread.sleep(sessionAge * 1000 + 1000); }
          catch (Exception e) { }
          
          long now = System.currentTimeMillis();
          
          Session.sessionList.values().stream()
            .filter(s -> now > s.lastUse() + sessionAgeMillis)
            .forEach(s -> Session.sessionList.remove(s.sessionId));
        }
      }).start();
    }
  }
  
  private static void extractJar(String jar, String location) {
    try {
      BufferedOutputStream dest = null;
      ZipInputStream zis = new ZipInputStream(new FileInputStream(jar));
      ZipEntry entry;
      
      while ((entry = zis.getNextEntry()) != null) {
        if (entry.isDirectory())
          new File(location + entry.getName()).mkdirs();
        else {
          new File(new File(location + entry.getName()).getParent()).mkdirs();
          
          int count;
          byte data[] = new byte[BUFFER];
          FileOutputStream fos = new FileOutputStream(location + entry.getName());
          dest = new BufferedOutputStream(fos, BUFFER);
          while ((count = zis.read(data, 0, BUFFER)) != -1) {
            dest.write(data, 0, count);
          }
          
          dest.flush();
          dest.close();
          
          if (entry.getName().endsWith(".jar")) {
            extractJar(location + entry.getName(), location);
          }
        }
      }
      
      zis.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  public static String getProjectLocation() {
    return projectLocation;
  }
  
  public static boolean isDevelopmentMode() {
    return developmentMode;
  }
  
  public void serve() throws Exception {
    init();
    
    if (database)
      ds = new HikariDataSource(config);
    
    new Thread(() -> {
      Thread.currentThread().setName("NETTY");
      
      EventLoopGroup bossGroup = null;
      EventLoopGroup workerGroup = null;
      
      try {
        ServerBootstrap b = new ServerBootstrap();
    
        if (epoll) {
          bossGroup = new EpollEventLoopGroup();
          workerGroup = new EpollEventLoopGroup();
          
          b.channel(EpollServerSocketChannel.class)
          .childOption(EpollChannelOption.EPOLL_MODE, EpollMode.LEVEL_TRIGGERED);
          
          System.out.println("Using native epoll.");
        } else {
          bossGroup = new NioEventLoopGroup();
          workerGroup = new NioEventLoopGroup();
          
          b.channel(NioServerSocketChannel.class);
          
          System.out.println("Using java NIO.");
        }
        
        b.option(ChannelOption.SO_BACKLOG, 1024);
        b.group(bossGroup, workerGroup)
          .option(ChannelOption.TCP_NODELAY, true)
          // .option(ChannelOption.SO_BACKLOG, 100)
          // .handler(new LoggingHandler(LogLevel.INFO))
          .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
          .childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(final SocketChannel ch) {
              ch.pipeline().addLast(new HttpRequestDecoder(65536, 65536, maxChunkSizeInBytes, false));
              ch.pipeline().addLast(new HttpResponseEncoder());
              ch.pipeline().addLast(new HttpObjectAggregator(65536));
              
              if (webSocket) {
                ch.pipeline().addLast(new WebSocketServerProtocolHandler(webSocketPath, null, true));
                ch.pipeline().addLast(new WebSocketFrameHandler());
              }
              
              ch.pipeline().addLast(new ChunkedWriteHandler());
              if (Server.compress)
                ch.pipeline().channel().pipeline().addLast(new HttpContentCompressor(1));
              
              ch.pipeline().addLast(new Handler());
              ch.pipeline().addLast(new StaticFileHandler());
            } 
          });
        
        Channel ch = b.bind(port).sync().channel();
        
        System.out.println("Ready. Navigate to http://localhost:" + port + '/');
        
        if (join)
          Thread.currentThread().join();
        
        ch.closeFuture().sync();
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        
        try { mqttClient.disconnect(); }
        catch (Exception e) { e.printStackTrace(); }
      }
    }).start();
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
    
    return directory.delete();
  }
}