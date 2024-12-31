# mill-client-server-poc

Proof of Concept goals:
- one server per workspace/project
- multiple simultaneous clients
- 2-way client-server protocol, in messagepack lightweight format
- task locking

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

---
### Automatic server start
It will start the server *if needed* (if it can't connect).  
Note that it is not launching the server as a detached/daemon process yet..
```
java -jar ./out/client/jvm/assembly.dest/out.jar -c task
Could not connect to server. Starting a new one...
........
```

---
### Task-level locking
If 2 clients run the same task, it will be done with an in-memory lock being held.  
This is output from the client2 (while client1 was already running the task):
```sh
> java -jar ./out/client/jvm/assembly.dest/out.jar -c task
[server] Task lock busy, waiting for it to be released...
[server] Task lock busy, waiting for it to be released...
[server] Task lock busy, waiting for it to be released...
[server] Working on task 'mytask' ...
```


---
There are a few commands implemented:
- `version`, prints the version (no server interaction at all)
- `noop`, does nothing on server, just sends back a "done" command
- `subprocess`, tells the client to run a subprocess (not interactive)
- `interactiveSubprocess`, tells the client to run a subprocess (interactive, requires input from user)
- `task1`, runs a slow task, so you can test the task-level locking behavior
- `task2`, same as `task1`, but using a different lock, so they can run independently
- `shutdown`, stops the server

----

### Measuring JVM vs ScalaNative

```sh
hyperfine "java -jar .\out\client\jvm\assembly.dest\out.jar -c noop"
# vs
hyperfine "./out/client/native/nativeLink.dest/out.exe"
```

Difference for now is negligible.. maybe a few nanoseconds... ¯/_(ツ)_/¯  
Maybe it will be noticeable in bigger tasks.

