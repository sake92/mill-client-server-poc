# mill-client-server-poc

Proof of Concept goals:
- one server per workspace/project
- multiple simultaneous clients
- 2-way client-server protocol, in messagepack lightweight format
- per-task in-memory locking (since there is 1 singleton server)

TODOs:
- restart server automatically when its config file changes

## Server 

```sh
./mill server.assembly
java -jar ./out/server/assembly.dest/out.jar
```

## Client

Both JVM and ScalaNative work.

### JVM
```sh
./mill client.jvm.assembly
java -jar ./out/client/jvm/assembly.dest/out.jar
```

### ScalaNative
```sh
./mill show client.native.nativeLink
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
> java -jar ./out/client/jvm/assembly.dest/out.jar -c task1
[server] Task lock busy, waiting for it to be released...
[server] Task lock busy, waiting for it to be released...
[server] Task lock busy, waiting for it to be released...
[server] Working on task 'task1' ...
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
# JVM
PS D:\projects\sake\mill-client-server-poc> hyperfine "java -jar ./out/client/jvm/assembly.dest/out.jar -c noop" --ignore-failure
Benchmark 1: java -jar ./out/client/jvm/assembly.dest/out.jar -c noop
  Time (mean ± σ):     566.8 ms ± 122.6 ms    [User: 113.8 ms, System: 26.3 ms]
  Range (min … max):   470.9 ms … 887.7 ms    10 runs
  
# vs native
PS D:\projects\sake\mill-client-server-poc> hyperfine "./out/client/native/nativeLink.dest/out.exe -c noop" --ignore-failure
Benchmark 1: ./out/client/native/nativeLink.dest/out.exe -c noop
  Time (mean ± σ):       2.3 ms ±   2.5 ms    [User: 0.0 ms, System: 0.4 ms]
  Range (min … max):     0.0 ms …  17.5 ms    109 runs

  Warning: Command took less than 5 ms to complete. Note that the results might be inaccurate because hyperfine can not calibrate the shell startup time much more precise than this limit. You can try to use the `-N`/`--shell=none` option to disable the shell completely.
  Warning: Ignoring non-zero exit code.
```

