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
java -jar ./out/client/jvm/assembly.dest/out.jar -c noop
```

### ScalaNative
```sh
./mill show client.native.nativeLink
./out/client/native/nativeLink.dest/out.exe -c noop
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
PS D:\projects\sake\mill-client-server-poc> hyperfine --show-output --shell powershell "java -jar ./out/client/jvm/assembly.dest/out.jar -c noop"
Benchmark 1: java -jar ./out/client/jvm/assembly.dest/out.jar -c noop
  Time (mean ± σ):     638.0 ms ±  83.6 ms    [User: 94.7 ms, System: 20.2 ms]
  Range (min … max):   515.8 ms … 766.2 ms    10 runs
  
# vs native
PS D:\projects\sake\mill-client-server-poc> hyperfine --show-output --shell powershell "./out/client/native/nativeLink.dest/out.exe -c noop"
Benchmark 1: ./out/client/native/nativeLink.dest/out.exe -c noop
  Time (mean ± σ):     185.1 ms ±  19.6 ms    [User: 40.1 ms, System: 44.1 ms]
  Range (min … max):   152.0 ms … 207.9 ms    10 runs
```

Note these are ran on Windows, you'll probably need to remove `--shell powershell` if using *nix.
