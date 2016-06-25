package com.pring.lucy.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

public class WebSocketFrameHandler extends SimpleChannelInboundHandler<WebSocketFrame> {
  @Override
  protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
    // ping and pong frames already handled

    if (frame instanceof TextWebSocketFrame) {
      // Send the uppercase string back.
      String request = ((TextWebSocketFrame) frame).text();

      try {
        ctx.channel().writeAndFlush(new TextWebSocketFrame(
            String.valueOf(Server.webSocketHandler.invoke(Server.webSocketHandlerClass.getConstructor().newInstance(), request))));
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else {
      String message = "unsupported frame type: " + frame.getClass().getName();
      throw new UnsupportedOperationException(message);
    }
  }
}