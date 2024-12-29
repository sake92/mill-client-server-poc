# mill-client-server-poc

## Server 

```sh
./mill -i server.assembly
java -jar ./out/server/assembly.dest/out.jar
```

## Client 

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
## POC behavior
By default the client just prints a version (no server interaction)
```
java -jar ./out/client/jvm/assembly.dest/out.jar
Mill version 0.12
```

---
If you give it a command it will start the server *if needed* (if it can't connect).  
This is not launching the server as a detached/daemon process yet..
```
java -jar ./out/client/jvm/assembly.dest/out.jar
Could not connect to server. Starting a new one...
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

