package com.pring.lucy.server;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaders.Names.IF_MODIFIED_SINCE;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.METHOD_NOT_ALLOWED;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.pring.lucy.http.HeaderNames;
import com.pring.lucy.http.MimeType;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelProgressiveFuture;
import io.netty.channel.ChannelProgressiveFutureListener;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpChunkedInput;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedFile;
import io.netty.handler.stream.ChunkedStream;

public class StaticFileHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
  public static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
  public static final String HTTP_DATE_GMT_TIMEZONE = "GMT";
  public static final int HTTP_CACHE_SECONDS = 60;

  public static final String STATIC_MAPPING = SimpleChannelInboundHandler.class.getName() + ".staticMapping";
  public static final String SERVICED = SimpleChannelInboundHandler.class.getName() + ".serviced";

  private static Map<String, Object[]> cache = new HashMap<String, Object[]>();
  
  private static SimpleDateFormat sdf = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
    if (request.getMethod() != GET) {
      Http.sendError(ctx, METHOD_NOT_ALLOWED);
      return;
    }
    
    sendFile(ctx, Server.staticFileLocation + Http.sanitizeFileUri(request.getUri()), request, false);
  }
  
  protected static void sendFile(ChannelHandlerContext ctx, String path,
      FullHttpRequest request, boolean transfer) throws Exception {
    File file = new File(path);
    
    if (file.isHidden() || file.exists() || file.isFile()) {
      RandomAccessFile raf = new RandomAccessFile(file, "r");

      request.headers().add(SERVICED, "true");

      // Cache Validation
      String ifModifiedSince = request.headers().get(IF_MODIFIED_SINCE);
      if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
        Date ifModifiedSinceDate = dateFormatter.parse(ifModifiedSince);

        long ifModifiedSinceDateSeconds = ifModifiedSinceDate.getTime() / 1000;
        long fileLastModifiedSeconds = file.lastModified() / 1000;
        
        if (ifModifiedSinceDateSeconds == fileLastModifiedSeconds) {
          raf.close();
          Http.sendNotModified(ctx);
          return;
        }
      }
      
      long fileLength = raf.length();

      HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
      
      HttpHeaders.setContentLength(response, fileLength);
      MimeType.contentType(request, response, file);
      Http.setDateAndCacheHeaders(response, file);

      if (transfer) {
        response.headers().add("Content-Description", "File Transfer");
        response.headers().add(HeaderNames.CONTENT_TYPE, "application/octet-stream");
        response.headers().add("Content-Disposition", "attachment; filename=" + path);
        response.headers().add(HeaderNames.CONTENT_TRANSFER_ENCODING, "binary");
      }
      
      if (HttpHeaders.isKeepAlive(request)) {
        response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
      }
      
      ctx.write(response);

      ChannelFuture sendFileFuture;
      ChannelFuture lastContentFuture;
      if (ctx.pipeline().get(SslHandler.class) == null) {
        sendFileFuture = ctx.write(new DefaultFileRegion(raf.getChannel(), 0, fileLength), ctx.newProgressivePromise());
        lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
      } else {
        sendFileFuture = ctx.writeAndFlush(
            new HttpChunkedInput(new ChunkedFile(raf, 0, fileLength, Server.maxChunkSizeInBytes)), ctx.newProgressivePromise());
        lastContentFuture = sendFileFuture;
      }
      
      lastContentFuture.addListener(ChannelFutureListener.CLOSE);
    } /* else if (Server.inJar) {
      InputStream stream = Server.class.getClass().getResourceAsStream("/" + path);
      sendResource(ctx, stream, path, request, transfer);
    } */ else {
      Http.sendError(ctx, NOT_FOUND);
    }
  }
  
  protected static void sendResource(ChannelHandlerContext ctx, InputStream stream,
      String name, FullHttpRequest request, boolean transfer) throws Exception {

    String lastModified = ""; 
    byte[] data = null;

    if (cache.containsKey(name)) {
      lastModified = (String) cache.get(name)[0];
      data = (byte[]) cache.get(name)[1];
    } else {
      ByteArrayOutputStream buffer = new ByteArrayOutputStream();

      byte[] _data = new byte[16384];
      lastModified = sdf.format(new Date());
      
      int nRead;
      while ((nRead = stream.read(_data, 0, _data.length)) != -1)
        buffer.write(_data, 0, nRead);

      buffer.flush();
      stream.close();
      
      data = buffer.toByteArray();
      
      if (!transfer)
        cache.put(name, new Object[] { lastModified, data });
    }
    
    if (!transfer) {
      // Cache Validation
      String ifModifiedSince = request.headers().get(IF_MODIFIED_SINCE);
      if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
        if (lastModified.equals(ifModifiedSince)) {
          Http.sendNotModified(ctx);
          return;
        }
      }
    }
    
    sendResource(ctx, data, name, request, transfer);
  }
  
  protected static void sendResource(ChannelHandlerContext ctx, byte[] data,
      String name, FullHttpRequest request, boolean transfer) throws Exception {
    long length = data.length;
    
    HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
    HttpHeaders.setContentLength(response, length);
    
    if (transfer) {
      response.headers().add("Content-Description", "File Transfer");
      response.headers().add("Content-Disposition", "attachment; filename=" + name);
      response.headers().add(HeaderNames.CONTENT_TYPE, "application/octet-stream");
      response.headers().add(HeaderNames.CONTENT_TRANSFER_ENCODING, "binary");
    }
    
    ctx.write(response);
    
    final ByteArrayInputStream bais = new ByteArrayInputStream(data);
    
    ChannelFuture sendFileFuture = ctx.write(new HttpChunkedInput(
        new ChunkedStream(bais, Server.maxChunkSizeInBytes)), ctx.newProgressivePromise());
    
    sendFileFuture.addListener(new ChannelProgressiveFutureListener() {
      @Override
      public void operationProgressed(ChannelProgressiveFuture future,
          long progress, long total) throws Exception { }

      @Override
      public void operationComplete(ChannelProgressiveFuture future)
          throws Exception {
        bais.close();
      }      
    });
    
    ChannelFuture lastContentFuture = ctx
        .writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
    
    if (HttpHeaders.isKeepAlive(request)) {
      lastContentFuture.addListener(ChannelFutureListener.CLOSE);
    }
  }
  
  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable t) {
    Channel ch = ctx.channel();

    if (t.getCause() instanceof TooLongFrameException) {
      Http.sendError(ctx, BAD_REQUEST);
      return;
    }

    if (ch.isOpen() || ch.isActive()) {
      Http.sendError(ctx, INTERNAL_SERVER_ERROR);
    }
  }
}