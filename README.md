# Lucy
A simple HTTP framework based on Netty for Java.

## Requirements
- JDK8+
- Eclipse

## Getting Started
- Define handler class:
```java
package root.controller;

import org.pring.lucy.server.HttpController;

public class Index extends HttpController {
  @Override
  public void index() throws Exception {
    echo("Hello World");
  }
}
```
- Define an entry point:
```java
package entry;

import org.pring.lucy.server.Server;

public class Main extends Server {
  public static void main(String[] args) throws Exception {
    new Main()
      // .withoutCookies()
      // .sessionAge(60)
      // .gzip()
      // .database("jdbc:mysql://localhost:3306/information_schema", "root", "root")
      // .production()
      .staticLocation("/var/www")
      .port(8080)
      .serve();
  }
}
```
- Test it:
```
http://localhost:8080/
- or -
http://localhost:8080/index
- or -
http://localhost:8080/:index/index
- or -
http://localhost:8080/root:index/index
```
You also have to create a root.view package and a matching template there. So if you have Index.java you have to have index.html in root.view.

```
/ -> binds to root.*
```
## Configuration
```java
...
    new Main()
      // 1. withoutCookies()
      // 2. sessionAge(60)
      // 3. gzip()
      // 4. database("jdbc:mysql://localhost:3306/information_schema", "root", "root")
      // 5. production()
      // 6. staticLocation("/var/www")
      // 7. port(8080)
      // 8. serve();
...
```
1. ``withoutCookies()`` <BR> Will not set any cookies at all. In case you have a static website.
2. ``sessionAge(60)`` <BR> Defines session age in seconds.
3. ``gzip()`` <BR> Will gzip the response sent to client.
4. ``database("jdbc:...", "user", "pass")`` <BR> Will initiate a database pool.
5. ``production()`` <BR> Will load the clases only once. No hot reloading. It must be uncommented when exporting to final JAR.
6. ``staticLocation("absolute path")`` <BR> Sets the location of static files.
7. ``port(8080)`` <BR> Sets the port where to listen.
8. ``serve()`` <BR> Kicks off everything.

## Features
1. Hot reloading -- No more restarts 
2. Templating
3. Sessions
4. Database
5. Very simple interface
6. Lightweight

## Methods
Method | Function
------------ | -------------
`redirect(to)` | Redirects to specified URL
`status(status)` | Sets HTTP status code
`method()` | Returns HTTP method
`halt()` | Halts execution
`halt(status)` | Halts execution with status code
`halt(status, message)` | Halts execution with status code and a message
`header(name)` | Gets header name from request
`header(name, value)` | Sets a header on response
`headers()` | List of request headers
`cookie(name)` | Gets cookie name
`cookie(name, value)` | Sets cookie value to name on response
`cookie(name, value, ttl)` | Sets cookie value and age to name on response
`cookie(name, value, ttl, isSecure)` | Sets cookie value and age and security to name on response
`cookie(name, null)` | Removes cookie 
`cookies()` | Gets cookie list
`queryParams(key)` | Gets a query param List for key
`GET(key)` | Gets a String value for key
`GETint(key)` | Gets an int value for key
`GETdouble(key)` | Gets a double value for key
`POST(key)` | Gets a String value for key
`POSTint(key)` | Gets an int value for key
`POSTdouble(key)` | Gets a double value for key
`session(key)` | Gets session value for key
`session(key, value)` | Sets session value for key
`session(key, null)` | Removes key from session 
`session()` | Gets a key, value Map
`sessionId()` | Gets session id
`sessionDestroy()` | Destroys session
`echo(msg)` | Echoes stuff
`debug(msg)` | Prints stuff on running terminal
`view(key, value)` | Passes key and value to template
`file(name)` | Gets File from uploaded file
`files()` | Gets a List with uploaded `File`s
`sendFile(path)` | Forces browser to download given file from path
`sendFile(data[], name)` | Creates a temporary file from data and sends to client
`clientIp()` | Gets client IP
`port()` | Gets client port
`requestMethod()` | Gets HTTP request method
`url()` | Gets request URL
`userAgent()` | Does not work
`isKeepAlive()` | Gets connection type
`DB.selectCell("QRY")` | Returns one row with one cell
`DB.selectCell("QRY", args[])` | Returns one row with one cell
`DB.select("QRY")` | Returns an Iterable<ResultSet> so you can loop through it
`DB.select("QRY", args)` | Returns an Iterable<ResultSet> so you can loop through it
`DB.insert("QRY", args)` | Returns mysql-last-id
`DB.update("QRY", args)` | Returns manipulated row nums

## Annotations
Annotation | Function
------------ | -------------
`@Api` | Marks method as API -- ignores any template rendering
`@Api("text/plain")` | Marks method as API -- ignores any template rendering and sets Content-Type
`@NoSession` | Does not bother with session stuff
`@Status(200)` | Sets HTTP response code
`@View("index1")` | Sets custom view
`@View("")` | Removes templating -- much like `@Api`

## Examples
Say you have an API with buyer and seller. You can have the code separately like this:
```
buyer.controller.*
buyer.view.*
- and -
seller.controller.*
seller.view.*
```

`buyer.controller.Index`:
```java
package buyer.controller;

import org.pring.lucy.server.HttpController;

public class Index extends HttpController {
  @Override
  public void index() throws Exception {
    echo("TEST");
  }
}
```
`buyer.view.index`:
```html
<B>
  {{ myVar; }}
</B>
```
Then access it like this:
```
http://localhost:8080/buyer:/
```
## Template Engine
Template engine is pure Java code except for a short hand when writing for loops:
```html
<ul>
{{
  for (String s : String[] { "first" }) {
    <li>$s;</li>
  }
}}
</ul>
```
... and `include`:
```
{{ include $myTemp; }}
- or -
{{ include "some.other.package.file" }}
- or -
{{ include "someOtherFile" }}
- or -
{{ include "/var/www/file/on/disk.html" }}
```
Database access obviously:
```html
{{
  for (String s : ResultSet r : DB.select("select * from COLLATION_CHARACTER_SET_APPLICABILITY;")) {
    <li>$s.getString(1);</li>
  }
}}
</ul>
```
Also Session through a `session` variable:
``
I'm 
{{
  if (session.getBoolean("isLoggedIn"))
    echo("<b>logged in. WOHOOOO</b>");
  else
    echo("<i>OUT :(</i>");
}}
## License
http://www.apache.org/licenses/LICENSE-2.0.txt
