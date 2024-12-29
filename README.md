# mill-client-server-poc

## Server 

```sh
./mill -i server.assembly
java -jar ./out/server/assembly.dest/out.jar
```

## Client 

### JVM
```sh
./mill -i --no-build-lock client.jvm.assembly
java -jar ./out/client/jvm/assembly.dest/out.jar
```


### ScalaNative
```sh
./mill -i --no-build-lock show client.native.nativeLink
./out/client/native/nativeLink.dest/out.exe
```


### Measuring JVM vs ScalaNative

On windows:
```sh
Measure-Command { start-process  java -argumentlist "-jar ./out/client/jvm/assembly.dest/out.jar"  -Wait }
Measure-Command { start-process  ./out/client/native/nativeLink.dest/out.exe  -Wait }
```

Difference for now is negligible.. maybe a few nanoseconds... ¯/_(ツ)_/¯

