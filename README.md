# mill-client-server-poc

Proof of Concept goals:
- one server per workspace/project
- multiple simultaneous clients
- messagepack 2-way client-server protocol
- in-memory task locking

TODOs:
- restart server automatically when its config file changes

## Server 

```sh
./mill -i server.assembly
java -jar ./out/server/assembly.dest/out.jar
```

## Client

Both JVM and ScalaNative work.

### JVM
```sh
./mill -i client.jvm.assembly
java -jar ./out/client/jvm/assembly.dest/out.jar
```

### ScalaNative
```sh
./mill -i show client.native.nativeLink
./out/client/native/nativeLink.dest/out.exe
```

---
## Behavior
By default the client just prints a version (no server interaction)
```
java -jar ./out/client/jvm/assembly.dest/out.jar
Mill version 0.12
```

---
It will start the server *if needed* (if it can't connect).  
Note that it is not launching the server as a detached/daemon process yet..
```
java -jar ./out/client/jvm/assembly.dest/out.jar
Could not connect to server. Starting a new one...
GOT MSG: Working on task 'mytask' ...
```

---
If 2 clients run the same task, it will be done with an in-memory lock being held.  
This is output from the client2 (while client1 was already running the task):
```sh
> java -jar ./out/client/jvm/assembly.dest/out.jar -c mytask
GOT MSG: Task lock busy, waiting for it to be released...
GOT MSG: Task lock busy, waiting for it to be released...
GOT MSG: Task lock busy, waiting for it to be released...
GOT MSG: Working on task 'mytask' ...
```


---
The only "real" command you can run at the moment is "SHUTDOWN", which stops the server.
```
java -jar ./out/client/jvm/assembly.dest/out.jar -c SHUTDOWN
```


----

### Measuring JVM vs ScalaNative

On windows:
```sh
Measure-Command { start-process  java -argumentlist "-jar ./out/client/jvm/assembly.dest/out.jar"  -Wait }
Measure-Command { start-process  ./out/client/native/nativeLink.dest/out.exe  -Wait }
```

Difference for now is negligible.. maybe a few nanoseconds... ¯/_(ツ)_/¯  
Maybe it will be noticeable in bigger tasks.

