package com.pring.lucy.server;

import static io.netty.handler.codec.http.HttpHeaders.Names.CACHE_CONTROL;
import static io.netty.handler.codec.http.HttpHeaders.Names.DATE;
import static io.netty.handler.codec.http.HttpHeaders.Names.EXPIRES;
import static io.netty.handler.codec.http.HttpHeaders.Names.LAST_MODIFIED;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import com.pring.lucy.http.HeaderNames;
import com.pring.lucy.http.HttpStatus;
import com.pring.lucy.http.Values;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.CharsetUtil;

class Http {
  public static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
  public static final String HTTP_DATE_GMT_TIMEZONE = "GMT";
  public static final int    HTTP_CACHE_SECONDS = 60;
  
  public static String sanitizeUri(String uri) {
    try {
      uri = URLDecoder.decode(uri, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      try {
        uri = URLDecoder.decode(uri, "ISO-8859-1");
      } catch (UnsupportedEncodingException e1) {
        throw new Error();
      }
    }

    uri = uri.replace('/', File.separatorChar);

    if (uri.contains(File.separator + ".")
        || uri.contains("." + File.separator)
        || uri.startsWith(".") || uri.endsWith(".")) {
      return null;
    }

    int pos = uri.indexOf("?");
    if (pos != -1) {
      uri = uri.substring(0, pos);
    }
    
    return uri;
  }

  public static void write(boolean isKeepAlive, FullHttpResponse response, ChannelHandlerContext ctx) {
    if (isKeepAlive) {
      response.headers().set(HeaderNames.CONNECTION, Values.KEEP_ALIVE);
      ctx.write(response);
    } else {
      ctx.write(response).addListener(ChannelFutureListener.CLOSE);
    }
  }

  public static void setDateAndCacheHeaders(HttpResponse response, long lastModified) {
    SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT,
        Locale.US);
    dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

    // Date header
    Calendar time = new GregorianCalendar();
    response.headers().set(DATE, dateFormatter.format(time.getTime()));

    // Add cache headers
    time.add(Calendar.SECOND, HTTP_CACHE_SECONDS);
    response.headers().set(EXPIRES, dateFormatter.format(time.getTime()));
    response.headers().set(CACHE_CONTROL, "private, max-age=" + HTTP_CACHE_SECONDS);
    response.headers().set(LAST_MODIFIED,
        dateFormatter.format(new Date(lastModified)));
  }
  
  public static void sendRedirect(ChannelHandlerContext ctx, String newUri) {
    FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, HttpStatus.FOUND);
    response.headers().set(HeaderNames.LOCATION, newUri);

    ctx.write(response).addListener(ChannelFutureListener.CLOSE);
  }
  
  public static void sendRedirect(FullHttpResponse response, String newUri, ChannelHandlerContext ctx) {
    response.headers().set(HeaderNames.LOCATION, newUri);
    ctx.write(response).addListener(ChannelFutureListener.CLOSE);
  }

  public static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
    FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, status,
        Unpooled.copiedBuffer(status + "\r\n", CharsetUtil.UTF_8));
    
    response.headers().set(HeaderNames.CONTENT_TYPE, "text/plain");

    ctx.write(response).addListener(ChannelFutureListener.CLOSE);
  }
  
  public static void sendException(ChannelHandlerContext ctx,
      HttpResponseStatus status, String buffer) {
    FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, status, 
        Unpooled.copiedBuffer(buffer, CharsetUtil.UTF_8));
    
    response.headers().set(HeaderNames.CONTENT_TYPE, "text/plain");
    response.headers().set(HeaderNames.CONTENT_LENGTH, response.content().readableBytes());

    ctx.write(response).addListener(ChannelFutureListener.CLOSE);
  }

  public static void sendNotModified(ChannelHandlerContext ctx) {
    FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, HttpStatus.NOT_MODIFIED);
    setDateHeader(response);

    ctx.write(response).addListener(ChannelFutureListener.CLOSE);
  }

  public static void setDateHeader(FullHttpResponse response) {
    SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
    dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

    Calendar time = new GregorianCalendar();
    response.headers().set(HeaderNames.DATE, dateFormatter.format(time.getTime()));
  }

  public static void setDateAndCacheHeaders(HttpResponse response, File fileToCache) {
    SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
    dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

    Calendar time = new GregorianCalendar();
    response.headers().set(HeaderNames.DATE, dateFormatter.format(time.getTime()));

    // Add cache headers
    time.add(Calendar.SECOND, HTTP_CACHE_SECONDS);
    response.headers().set(HeaderNames.EXPIRES, dateFormatter.format(time.getTime()));
    response.headers().set(HeaderNames.CACHE_CONTROL, "private, max-age=" + HTTP_CACHE_SECONDS);
    response.headers().set(HeaderNames.LAST_MODIFIED, dateFormatter.format(new Date(fileToCache.lastModified())));
  }
}
