package org.pring.lucy.server;

public class HaltException extends Exception {
  private static final long serialVersionUID = 1L;
  public final int status;
  
  protected HaltException(String message, int status) {
    super(message);
    this.status = status;
  }
  
  protected HaltException(String message) {
    super(message);
    this.status = 200;
  }
}
