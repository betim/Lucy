package com.pring.lucy.server;

public interface MqttCallbackListener {
  abstract public void handleMessage(String topic, String message);
}
