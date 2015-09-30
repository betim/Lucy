package com.pring.lucy.server;

import static com.pring.lucy.template.TemplateEngine.getTemplate;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.pring.lucy.annotations.Api;
import com.pring.lucy.annotations.NoSession;
import com.pring.lucy.annotations.Status;
import com.pring.lucy.annotations.View;
import com.pring.lucy.http.HeaderNames;
import com.pring.lucy.http.HttpStatus;
import com.pring.lucy.http.MimeType;

import java.util.Set;
import java.util.TreeSet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.InterfaceHttpData.HttpDataType;
import io.netty.util.CharsetUtil;

public abstract class HttpController {
  private ChannelHandlerContext ctx;
  private FullHttpRequest request;
  private FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, HttpStatus.OK);
  private boolean isKeepAlive = false;
  
  private StringBuilder buffer = new StringBuilder();
  private Map<String, List<String>> queryParameters = new HashMap<String, List<String>>();
  private Map<String, List<String>> form = new HashMap<String, List<String>>();
  private Map<String, File> files = new HashMap<String, File>();
  private Map<String, Object> templateFields = new HashMap<String, Object>();
  private Set<Cookie> cookies = new TreeSet<Cookie>();

  private Session session = Server.DUMMY_SESSION;
  private String sessionId = String.valueOf(System.currentTimeMillis());
  
  public final SqlDatabase DB = new SqlDatabase();
  
  private String redirect = "";
  private HaltException halt = null;
  
  public void fire(ChannelHandlerContext _ctx, FullHttpRequest _request,
      String pkg, String method, String[] params, int tokOffset) throws Exception {
    ctx = _ctx;
    request = _request;
    isKeepAlive = HttpHeaders.isKeepAlive(request);

    if (HttpHeaders.is100ContinueExpected(request)) {
      ctx.write(new DefaultFullHttpResponse(HTTP_1_1, HttpStatus.CONTINUE));
    }

    response.headers().add(HeaderNames.CONTENT_TYPE, MimeType.defaultContentType);

    if (Server.withCookies && request.headers().get(HeaderNames.COOKIE) != null) {
      for (Cookie cookie : ServerCookieDecoder
          .STRICT.decode(request.headers().get(HeaderNames.COOKIE))) {
        if (cookie.name().equals(Server.sessionName))
          sessionId = cookie.value();
        else
          cookie(cookie.name(), cookie.value());
      }
    }
    
    Method _method = Server.methods.get(pkg + '.' + method);
    if (_method != null) {      
      if (Server.withCookies && _method.getAnnotation(NoSession.class) == null) {
        cookie(Server.sessionName, sessionId, Server.sessionAge);
        
        session = Session.getSession(sessionId);
        session.lastUse(System.currentTimeMillis());
      }
      
      view("session", session);
      view("DB", DB);
      
      if (request.headers().contains(HeaderNames.CONTENT_TYPE)
          && request.headers().get(HeaderNames.CONTENT_TYPE).startsWith("application/x-www-form-urlencoded"))
        form = new QueryStringDecoder(request.content().toString(CharsetUtil.ISO_8859_1), false).parameters();
      else if (request.headers().contains(HeaderNames.CONTENT_TYPE)
          && request.headers().get(HeaderNames.CONTENT_TYPE).startsWith("multipart/form-data")) {
        HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(Server.DATA_FACTORY, request);

        InterfaceHttpData data = decoder.next();
        if (data != null) {
          if (data.getHttpDataType() == HttpDataType.Attribute) {
            Attribute attribute = (Attribute) data;
            String value = null;
            try {
              value = attribute.getValue();

              if (form.containsKey(attribute.getName())) {
                form.get(attribute.getName()).add(value);
              } else {
                List<String> list = new ArrayList<String>();
                list.add(value);
                form.put(attribute.getName(), list);
              }

            } catch (IOException e) {
              e.printStackTrace();
            }
          } else if (data.getHttpDataType() == HttpDataType.FileUpload) {
            FileUpload fileUpload = (FileUpload) data;
            if (fileUpload.isCompleted()) {
              try {
                files.put(fileUpload.getName(), fileUpload.getFile());
              } catch (IOException e) {
                e.printStackTrace();
              }
            }
          }
        }
      }

      if (request.getUri().contains("?")) {
        String qp[] = request.getUri().split("\\?");
        queryParameters = new QueryStringDecoder(qp[1], false).parameters();
      }
      
      if (_method.getAnnotation(Status.class) != null) {
        status(HttpResponseStatus.valueOf(_method.getAnnotation(Status.class).value()));
      }  

      Object[] parameters = new Object[params.length - tokOffset];
      int idx = 0;
      for (Type t : _method.getGenericParameterTypes()) {
        if (t.getTypeName().equals("char"))
          parameters[idx] = params[tokOffset + idx].charAt(0);
        else if (t.getTypeName().equals("short"))
          parameters[idx] = Short.parseShort(params[tokOffset + idx]);
        else if (t.getTypeName().equals("int"))
          parameters[idx] = Integer.parseInt(params[tokOffset + idx]);
        else if (t.getTypeName().equals("float"))
          parameters[idx] = Float.parseFloat(params[tokOffset + idx]);
        else if (t.getTypeName().equals("double"))
          parameters[idx] = Double.parseDouble(params[tokOffset + idx]);
        else
          parameters[idx] = params[tokOffset + idx];

        idx++;
      }

      _method.invoke(this, parameters);

      if (halt != null) {
        throw halt;
      }
      
      if (_method.getAnnotation(Api.class) == null) {
        if (_method.getAnnotation(View.class) != null) {
          if (!_method.getAnnotation(View.class).value().equals("")) {
            String[] elems = pkg.split("\\.");
            elems[elems.length - 1] = _method.getAnnotation(View.class).value();

            echo(getTemplate(String.join(".", elems), templateFields).render(templateFields));
          }
        } else {
          echo(getTemplate(pkg, templateFields).render(templateFields));
        }
      } else
        response.headers().set(HeaderNames.CONTENT_TYPE, _method.getAnnotation(Api.class).value());
      
      
      if (Server.withCookies && cookies.size() > 0) {
        for (String cookie : ServerCookieEncoder.STRICT.encode(cookies))
          response.headers().add(HeaderNames.SET_COOKIE, cookie);
      }
      
      if (redirect.length() > 0)
        Http.sendRedirect(response, redirect, ctx);
      else {
        ByteBuf b = Unpooled.copiedBuffer(buffer.toString(), CharsetUtil.UTF_8);
        response.content().writeBytes(b);
        b.release();
        
        response.headers().add(HeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        
        Http.write(isKeepAlive, response, ctx);
      }
    } else throw new Exception("No such handler: " + method);
  }

  public void redirect(String to) {
    redirect = to;
  }
  
  public void status(HttpResponseStatus status) {
    response.setStatus(status);
  }

  public String method() {
    return request.getMethod().toString();
  }
  
  public void halt(int status, String msg) {
    halt = new HaltException("Stopped. Status: " + status + ". Message: " + msg, status);
  }
  
  public void halt(int status) {
    halt = new HaltException("Status: " + status, status);
  }
  
  public void halt() { 
    halt = new HaltException("Serving stopped.");
  }
  
  public String header(String key) {
    if (request.headers().contains(key))
      return request.headers().get(key);
    
    return "";
  }
  
  public void header(String key, int value) {
    response.headers().add(key, value);
  }
  
  public void header(String key, String value) {
    response.headers().add(key, value);
  }
  
  public List<Entry<String, String>> headers() {
    return response.headers().entries();
  }

  public String cookie(String key) { 
    for (Cookie c : cookies)
      if (c.name().equals(key))
        return c.value();
    
    return "";
  }

  public void cookie(String key, String value) {
    for (Cookie c : cookies)
      if (c.name().equals(key))
        c.setMaxAge(-3600);
    
    if (value == null)
      return;
    
    Cookie cookie = new DefaultCookie(key, value);
    cookie.setMaxAge(Server.sessionAge);
    cookie.setPath("/");
    
    cookies.add(cookie);
  }
  
  public void cookie(String key, String value, long ttl) {
    Cookie cookie = new DefaultCookie(key, value);
    cookie.setMaxAge(ttl);
    cookie.setPath("/");

    cookies.add(cookie);
  }
  
  public void cookie(String key, String value, long ttl, boolean sec) {
    System.err.println(key + " " + ttl);
    
    Cookie cookie = new DefaultCookie(key, value);
    cookie.setMaxAge(ttl);
    cookie.setPath("/");
    cookie.setSecure(sec);
    
    cookies.add(cookie);
  }

  public Set<Cookie> cookies() {
    return cookies;
  }

  private List<String> queryParams(String key, boolean number) {
    if (queryParameters.containsKey(key))
      return queryParameters.get(key);

    List<String> l = new ArrayList<String>(1);
    l.add(number ? "0" : "");

    return l;
  }
  
  public List<String> queryParams(String key) {
    return queryParams(key, false);
  }
  
  public String GET(String key) {
    return queryParams(key).get(0);
  }
  
  public int GETint(String key) {
    return Integer.valueOf(queryParams(key, true).get(0));
  }
  
  public double GETdouble(String key) {
    return Double.valueOf(queryParams(key, true).get(0));
  }
  
  
  private List<String> formParams(String key, boolean number) {
    if (form.containsKey(key))
      return form.get(key);

    List<String> l = new ArrayList<String>(1);
    l.add(number ? "0" : "");

    return l;
  }
  
  public String POST(String key) {
    return formParams(key, false).get(0);
  }
  
  public int POSTint(String key) {
    return Integer.valueOf(formParams(key, true).get(0));
  }
  
  public double POSTdouble(String key) {
    return Double.valueOf(formParams(key, true).get(0));
  }
  
  public Object session(String key) {
    return session.get(key);
  }
  
  public void session(String key, Object value) {
    if (value == null)
      session.remove(key);
    else
      session.put(key, value);
  }

  public Map<String, Object> session() {
    return session.map();
  }

  public String sessionId() {
    return sessionId;
  }
  
  public void sessionDestroy() {
    session.destroy();
  }

  public void echo(char b) {
    buffer.append(b);
  }

  public void echo(int b) {
    buffer.append(b);
  }
  
  public void echo(long b) {
    buffer.append(b);
  }

  public void echo(float b) {
    buffer.append(b);
  }

  public void echo(double b) {
    buffer.append(b);
  }

  public void echo(String b) {
    buffer.append(b);
  }

  public void echo(Object b) {
    buffer.append(b);
  }
  
  public void debug(String... b) {
    System.out.println(Arrays.asList(b));
  }

  public void view(String label, Object o) {
    templateFields.put(label, o);
  }

  public File file(String fileName) {
    return files.getOrDefault(fileName, Server.DUMMY_FILE);
  }
  
  public Map<String, File> files() {
    return files;
  }
  
  public void sendFile(String filePath) throws Exception {
    StaticFileHandler.sendFile(ctx, filePath, request, true);
  }

  public void sendFile(byte[] data, String fileName) throws Exception {
    StaticFileHandler.sendResource(ctx, data, fileName, request, true);
  }

  public String clientIp() {
    return ctx.channel().remoteAddress().toString();
  }
  
  public int port() {
    return Server.port;
  }
  
  public String requestMethod() {
    return request.getMethod().name();
  }
  
  public String url() {
    return request.getUri();
  }
  
  public String userAgent() {
    return "";
  }
  
  public boolean isKeepAlive() {
    return isKeepAlive;
  }

  public abstract void index() throws Exception;
}
