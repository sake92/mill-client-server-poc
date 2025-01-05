# mill-client-server-poc

Proof of Concept goals:
- one server per workspace/project
- multiple simultaneous clients
- 2-way client-server protocol, in messagepack lightweight format
- per-task in-memory locking (since there is 1 singleton server)
- subprocesses run by client, to have terminal available, but still using server speed to compile etc.

Comparison of how JVM build tool daemons handle interactive processes:  
https://github.com/sake92/java-build-tool-daemon-interactive  
Spolier: poorly

TODOs:
- reload server config automatically when its config file changes
- restart server automatically when its JVM config file changes

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
PS D:\projects\sake\mill-client-server-poc> hyperfine "java -jar out\client\jvm\assembly.dest\out.jar -c noop"
Benchmark 1: java -jar out\client\jvm\assembly.dest\out.jar -c noop
  Time (mean ± σ):     323.0 ms ±  10.8 ms    [User: 47.8 ms, System: 11.9 ms]
  Range (min … max):   305.7 ms … 346.4 ms    10 runs
  
# vs native
PS D:\projects\sake\mill-client-server-poc> hyperfine "out\client\native\nativeLink.dest\out.exe -c noop"
Benchmark 1: out\client\native\nativeLink.dest\out.exe -c noop
  Time (mean ± σ):      37.8 ms ±   5.5 ms    [User: 1.9 ms, System: 1.8 ms]
  Range (min … max):    27.1 ms …  53.4 ms    59 runs
```

Note these are ran on Windows, you can see the GitHub Actions logs for Linux version.
