package org.pring.lucy.http;

import io.netty.handler.codec.http.HttpResponseStatus;

public class HttpStatus {

  /**
   * 100 Continue
   */
  public static final HttpResponseStatus CONTINUE = HttpResponseStatus.CONTINUE;

  /**
   * 101 Switching Protocols
   */
  public static final HttpResponseStatus SWITCHING_PROTOCOLS = HttpResponseStatus.SWITCHING_PROTOCOLS;

  /**
   * 102 Processing (WebDAV, RFC2518)
   */
  public static final HttpResponseStatus PROCESSING = HttpResponseStatus.PROCESSING;

  /**
   * 200 OK
   */
  public static final HttpResponseStatus OK = HttpResponseStatus.OK;

  /**
   * 201 Created
   */
  public static final HttpResponseStatus CREATED = HttpResponseStatus.CREATED;

  /**
   * 202 Accepted
   */
  public static final HttpResponseStatus ACCEPTED = HttpResponseStatus.ACCEPTED;

  /**
   * 203 Non-Authoritative Information (since HTTP/1.1)
   */
  public static final HttpResponseStatus NON_AUTHORITATIVE_INFORMATION = HttpResponseStatus.NON_AUTHORITATIVE_INFORMATION;

  /**
   * 204 No Content
   */
  public static final HttpResponseStatus NO_CONTENT = HttpResponseStatus.NO_CONTENT;

  /**
   * 205 Reset Content
   */
  public static final HttpResponseStatus RESET_CONTENT = HttpResponseStatus.RESET_CONTENT;

  /**
   * 206 Partial Content
   */
  public static final HttpResponseStatus PARTIAL_CONTENT = HttpResponseStatus.PARTIAL_CONTENT;

  /**
   * 207 Multi-Status (WebDAV, RFC2518)
   */
  public static final HttpResponseStatus MULTI_STATUS = HttpResponseStatus.MULTI_STATUS;

  /**
   * 300 Multiple Choices
   */
  public static final HttpResponseStatus MULTIPLE_CHOICES = HttpResponseStatus.MULTIPLE_CHOICES;

  /**
   * 301 Moved Permanently
   */
  public static final HttpResponseStatus MOVED_PERMANENTLY = HttpResponseStatus.MOVED_PERMANENTLY;

  /**
   * 302 Found
   */
  public static final HttpResponseStatus FOUND = HttpResponseStatus.FOUND;

  /**
   * 303 See Other (since HTTP/1.1)
   */
  public static final HttpResponseStatus SEE_OTHER = HttpResponseStatus.SEE_OTHER;

  /**
   * 304 Not Modified
   */
  public static final HttpResponseStatus NOT_MODIFIED = HttpResponseStatus.NOT_MODIFIED;

  /**
   * 305 Use Proxy (since HTTP/1.1)
   */
  public static final HttpResponseStatus USE_PROXY = HttpResponseStatus.USE_PROXY;

  /**
   * 307 Temporary Redirect (since HTTP/1.1)
   */
  public static final HttpResponseStatus TEMPORARY_REDIRECT = HttpResponseStatus.TEMPORARY_REDIRECT;

  /**
   * 400 Bad Request
   */
  public static final HttpResponseStatus BAD_REQUEST = HttpResponseStatus.BAD_REQUEST;

  /**
   * 401 Unauthorized
   */
  public static final HttpResponseStatus UNAUTHORIZED = HttpResponseStatus.UNAUTHORIZED;

  /**
   * 402 Payment Required
   */
  public static final HttpResponseStatus PAYMENT_REQUIRED = HttpResponseStatus.PAYMENT_REQUIRED;

  /**
   * 403 Forbidden
   */
  public static final HttpResponseStatus FORBIDDEN = HttpResponseStatus.FORBIDDEN;

  /**
   * 404 Not Found
   */
  public static final HttpResponseStatus NOT_FOUND = HttpResponseStatus.NOT_FOUND;

  /**
   * 405 Method Not Allowed
   */
  public static final HttpResponseStatus METHOD_NOT_ALLOWED = HttpResponseStatus.METHOD_NOT_ALLOWED;

  /**
   * 406 Not Acceptable
   */
  public static final HttpResponseStatus NOT_ACCEPTABLE = HttpResponseStatus.NOT_ACCEPTABLE;

  /**
   * 407 Proxy Authentication Required
   */
  public static final HttpResponseStatus PROXY_AUTHENTICATION_REQUIRED = HttpResponseStatus.PROXY_AUTHENTICATION_REQUIRED;

  /**
   * 408 Request Timeout
   */
  public static final HttpResponseStatus REQUEST_TIMEOUT = HttpResponseStatus.REQUEST_TIMEOUT;

  /**
   * 409 Conflict
   */
  public static final HttpResponseStatus CONFLICT = HttpResponseStatus.CONFLICT;

  /**
   * 410 Gone
   */
  public static final HttpResponseStatus GONE = HttpResponseStatus.GONE;

  /**
   * 411 Length Required
   */
  public static final HttpResponseStatus LENGTH_REQUIRED = HttpResponseStatus.LENGTH_REQUIRED;

  /**
   * 412 Precondition Failed
   */
  public static final HttpResponseStatus PRECONDITION_FAILED = HttpResponseStatus.PRECONDITION_FAILED;

  /**
   * 413 Request Entity Too Large
   */
  public static final HttpResponseStatus REQUEST_ENTITY_TOO_LARGE = HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE;

  /**
   * 414 Request-URI Too Long
   */
  public static final HttpResponseStatus REQUEST_URI_TOO_LONG = HttpResponseStatus.REQUEST_URI_TOO_LONG;

  /**
   * 415 Unsupported Media Type
   */
  public static final HttpResponseStatus UNSUPPORTED_MEDIA_TYPE = HttpResponseStatus.UNSUPPORTED_MEDIA_TYPE;

  /**
   * 416 Requested Range Not Satisfiable
   */
  public static final HttpResponseStatus REQUESTED_RANGE_NOT_SATISFIABLE = HttpResponseStatus.REQUESTED_RANGE_NOT_SATISFIABLE;

  /**
   * 417 Expectation Failed
   */
  public static final HttpResponseStatus EXPECTATION_FAILED = HttpResponseStatus.EXPECTATION_FAILED;

  /**
   * 422 Unprocessable Entity (WebDAV, RFC4918)
   */
  public static final HttpResponseStatus UNPROCESSABLE_ENTITY = HttpResponseStatus.UNPROCESSABLE_ENTITY;

  /**
   * 423 Locked (WebDAV, RFC4918)
   */
  public static final HttpResponseStatus LOCKED = HttpResponseStatus.LOCKED;

  /**
   * 424 Failed Dependency (WebDAV, RFC4918)
   */
  public static final HttpResponseStatus FAILED_DEPENDENCY = HttpResponseStatus.FAILED_DEPENDENCY;

  /**
   * 425 Unordered Collection (WebDAV, RFC3648)
   */
  public static final HttpResponseStatus UNORDERED_COLLECTION = HttpResponseStatus.UNORDERED_COLLECTION;

  /**
   * 426 Upgrade Required (RFC2817)
   */
  public static final HttpResponseStatus UPGRADE_REQUIRED = HttpResponseStatus.UPGRADE_REQUIRED;

  /**
   * 428 Precondition Required (RFC6585)
   */
  public static final HttpResponseStatus PRECONDITION_REQUIRED = HttpResponseStatus.PRECONDITION_REQUIRED;

  /**
   * 429 Too Many Requests (RFC6585)
   */
  public static final HttpResponseStatus TOO_MANY_REQUESTS = HttpResponseStatus.TOO_MANY_REQUESTS;

  /**
   * 431 Request Header Fields Too Large (RFC6585)
   */
  public static final HttpResponseStatus REQUEST_HEADER_FIELDS_TOO_LARGE = HttpResponseStatus.REQUEST_HEADER_FIELDS_TOO_LARGE;

  /**
   * 500 Internal Server Error
   */
  public static final HttpResponseStatus INTERNAL_SERVER_ERROR = HttpResponseStatus.INTERNAL_SERVER_ERROR;

  /**
   * 501 Not Implemented
   */
  public static final HttpResponseStatus NOT_IMPLEMENTED = HttpResponseStatus.NOT_IMPLEMENTED;

  /**
   * 502 Bad Gateway
   */
  public static final HttpResponseStatus BAD_GATEWAY = HttpResponseStatus.BAD_GATEWAY;

  /**
   * 503 Service Unavailable
   */
  public static final HttpResponseStatus SERVICE_UNAVAILABLE = HttpResponseStatus.SERVICE_UNAVAILABLE;

  /**
   * 504 Gateway Timeout
   */
  public static final HttpResponseStatus GATEWAY_TIMEOUT = HttpResponseStatus.GATEWAY_TIMEOUT;

  /**
   * 505 HTTP Version Not Supported
   */
  public static final HttpResponseStatus HTTP_VERSION_NOT_SUPPORTED = HttpResponseStatus.HTTP_VERSION_NOT_SUPPORTED;

  /**
   * 506 Variant Also Negotiates (RFC2295)
   */
  public static final HttpResponseStatus VARIANT_ALSO_NEGOTIATES = HttpResponseStatus.VARIANT_ALSO_NEGOTIATES;

  /**
   * 507 Insufficient Storage (WebDAV, RFC4918)
   */
  public static final HttpResponseStatus INSUFFICIENT_STORAGE = HttpResponseStatus.INSUFFICIENT_STORAGE;

  /**
   * 510 Not Extended (RFC2774)
   */
  public static final HttpResponseStatus NOT_EXTENDED = HttpResponseStatus.NOT_EXTENDED;

  /**
   * 511 Network Authentication Required (RFC6585)
   */
  public static final HttpResponseStatus NETWORK_AUTHENTICATION_REQUIRED = HttpResponseStatus.NETWORK_AUTHENTICATION_REQUIRED;
  
  private HttpStatus() { }
}
