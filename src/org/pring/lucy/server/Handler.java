package org.pring.lucy.server;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_GATEWAY;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.commons.lang3.StringUtils;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;

public class Handler extends ChannelInboundHandlerAdapter {  
  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    ctx.flush();
  }
  
  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof HttpRequest) {
      FullHttpRequest request = (FullHttpRequest) msg;

      if (!request.getDecoderResult().isSuccess()) {
        Http.sendError(ctx, BAD_REQUEST);
        return;
      }
      
      String tokens[] = Http.sanitizeUri(request.getUri()).split("/");
      int tokCnt = 0;

      String pkg = "root.controller.index";
      if (tokens.length > 1) {
        String __r[] = tokens[1].split(":");
        
        if (__r.length == 2)
          pkg = (__r[0].equals("") ? "root" : __r[0]) + ".controller." + __r[1];
        else if (tokens[1].endsWith(":"))
          pkg = __r[0] + ".controller.index";
        else
          pkg = "root.controller." + __r[0];
        
        tokCnt = 2;
      }

      String method = "index";
      if (tokens.length >= 3) {
        method = tokens[2];
        tokCnt++;
      }
      

      Class<? extends HttpController> controller = Server.controllers.get(pkg);
      if (controller != null) {
        controller.getConstructor().newInstance()
          .fire(ctx, request, pkg, method, tokens, tokCnt);
      } else
        ctx.fireChannelRead(msg);
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    if (cause instanceof HaltException)
      Http.sendException(ctx, HttpResponseStatus.valueOf(((HaltException) cause).status), cause.getMessage());
    else {
      if (Server.developmentMode) {
        if (StringUtils.contains(cause.getMessage(), "cannot find symbol")) {
          String ex[] = cause.getMessage().split("\n");
          
          Http.sendException(ctx, BAD_GATEWAY, "Error while compiling template: " +
              ex[0].split(" ")[0].replace("/", "")
                .replace("java", "html").replaceFirst(":", " around line ").replace(":", "")
              + ex[3].replace(" symbol:   ", "cannot find "));
        } else {
          StringWriter stackTrace = new StringWriter();
          cause.printStackTrace(new PrintWriter(stackTrace));
          Http.sendException(ctx, BAD_GATEWAY, stackTrace.toString());
        }
      } else {
        Http.sendRedirect(ctx, "/");
      }
    }
  }
}
