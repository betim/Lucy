# Lucy
A micro HTTP framework based on Netty for Java.

## Requirements
- JDK8+
- Eclipse

## Maven
```
<dependency>
  <groupId>com.pr-ing</groupId>
  <artifactId>lucy</artifactId>
  <version>$VERSION</version>
</dependency>
```

## Getting Started
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
- Define a handler class:
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
- Test it:
```
http://localhost:8080/
- or -
http://localhost:8080/index
- or -
http://localhost:8080/:index/index
- or -
http://localhost:8080/root:index/index
                      ^    ^     ^
                      |    |     method name
                      |    class name
                      package name
```
Create a ``root.view`` ``package`` and a matching ``html`` template in it if you want templating. So if you have ``Index.java`` you must have ``index.html`` (lowercase) in `root.view`.

```
/ -> binds to root.*
```
## Configuration
```java
...
    new Main()
      //  1. withoutCookies()
      //  2. sessionAge(60)
      //  3. gzip()
      //  4. database("jdbc:mysql://localhost:3306/information_schema", "root", "root")
      //  5. production()
      //  6. staticLocation("/var/www")
      //  7. port(8080)
      //  8. epoll()
      //  9. sync()
      // 10. mqtt(broker, listener, topic)
      // 11. serve();
...
```
- ``withoutCookies()`` <BR> Will not set any cookies at all. In case you have a static website.
- ``sessionAge(60)`` <BR> Defines session age in seconds.
- ``gzip()`` <BR> Will gzip the response sent to client.
- ``database("jdbc:...", "user", "pass")`` <BR> Will initiate a database pool.
- ``production()`` <BR> Will load the clases only once. No hot reloading. It must be uncommented when exporting to final JAR.
- ``staticLocation("absolute path")`` <BR> Sets the location of static files.
- ``port(8080)`` <BR> Sets the port where to listen.
- ``epoll()`` <BR> Enables native Linux epoll.
- ``sync()`` <BR> Joins the main thread and blocks. Omit this if you have anything else after (which of course blocks main thread).
- ``mqtt(broker, listener, topic)`` <BR> Connects to a specified `broker` and handles incoming messages through a `MqttCallbackListener` coming to subscribed `topic`. Use `#` as a wild card. `publish(topic, message)` will publish your messages from any controller.
- ``serve()`` <BR> Kicks off everything.

## Features
- Hot reloading
- Templating
- Sessions
- Database
- Very simple interface
- Lightweight and somewhat fast

## Methods
Method | Function
------------ | -------------
`redirect(to)` | Redirects to specified URL
`redirectWithTemplate(to, templateName)` | Redirects to specified URL and sets a desired template
`status(code)` | Sets HTTP status code, overrides `@Status` annotation
`method()` | Returns request HTTP method
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
`template(name)` | Easy template selection (overrides `@View(name)`)
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
`SqlDB.query("QRY")` | Returns a one row `CachedRowSet`. Use this if you're not going to loop through results
`SqlDB.query("QRY", args)` | Returns a one row `CachedRowSet`. Use this if you're not going to loop through results
`SqlDB.selectCell("QRY")` | Returns one row with one cell
`SqlDB.selectCell("QRY", args[])` | Returns one row with one cell
`SqlDB.select("QRY")` | Returns an Iterable<ResultSet> so you can loop through it
`SqlDB.select("QRY", args)` | Returns an Iterable<ResultSet> so you can loop through it
`SqlDB.insert("QRY", args)` | Returns mysql-last-id
`SqlDB.update("QRY", args)` | Returns manipulated row nums
`SqlDB.delete("QRY", args)` | Returns manipulated row nums (Alias to `.update`)
`publish(topic, message)` | If mqtt is called on `Main` then this will publish a message to a desired topic.
`broadcast(message)` | If you have a `@WebSocket` method then this will broadcast a message to all active WebSocket channels.

## Method Annotations
Annotation | Function
------------ | -------------
`@API` | Marks method as an API method and ignores templating
`@API("text/plain")` | Marks method as API and sets Content-Type and ignores templating
`@NoSession` | Does not bother with session stuff
`@Status(200)` | Sets HTTP response code
`@View("index1")` | Sets a custom view
`@View("")` | Removes associated template
`@NoAccessCheck` | Does not check `hasAccess()` which returns boolean, for e.x: isLoggedIn
`@WebSocket` | Marks method as WebSocket handler

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
    view("myVar", aVeryComplexCalculation());
  }
  
  private static int aVeryComplexCalculation() {
    return 42;
  }
}
```
`buyer.view.index`:
```html
<B> {{ $myVar; }} </B>
```
Then access it like this:
```
http://localhost:8080/buyer:/
```

`seller.controller.Index`:
```java
package seller.controller;

import org.pring.lucy.server.HttpController;

public class Index extends HttpController {
  @Override
  @API
  public void index() throws Exception {
    for (ResultSet r : SqlDB.select("select * from `sellers`;")) {
      echo(r.getString("id") + " => " + r.getString("name") + '\n');
    }
  }

  @API
  public void update(int id, String name) {
    SqlDB.update("UPDATE `sellers` SET `name` = ? WHERE `id` = ?", new Object[] { name, id });
    redirect("/");
  }
}
```
Then access it like this:
```
http://localhost:8080/seller:/
```
Or update like this:
```
http://localhost:8080/seller:index/update/1/Awesome
                      ^      ^     ^      ^ ^ 
                      |      |     |      type safe args: id/name
                      |      |     method name
                      |      class name
                      package name
```
Arguments are typesafe, so if you try to pass a `String` where `int` is expected, you'll get an exception if you're under development mode, otherwise it will politely redirect you to `/`.

## Template Engine
Template engine is pure Java code plus for a short hand when writing `for` loops:
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
<ul>
{{
  for (ResultSet r : SqlDB.select("select * from `sellers`;")) {
    <li>$r.getString(1);</li>
  }
}}
</ul>
```
Also Session through a `session` variable:
```
I'm  {{
  if (session.getBoolean("isLoggedIn"))
    echo("<b>logged in. WOHOOOO</b>");
  else
    echo("<i>OUT :(</i>");
}}
```
## Roadmap
- [x] Maven
- [x] WebSockets
- [ ] ~~Spdy~~
- [x] MQTT
- [x] Some Optimizations
- [x] Writing code with Xtend

## Download
https://github.com/betim/Lucy/releases/tag/1.1

## Dependencies
- Netty 4.0.37.Final
- commons-lang3 3.4
- HikariCP 2.4.6
- slf4j-api 1.7.21
- mysql-connector-java 5.1.36
- org.eclipse.paho 1.0.2

--------------------------------------------------------

* Currently works only on Unix/Linux.

## License
Copyright 2015 Lucy

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
