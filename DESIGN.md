### Instructions

```bash
$ ./gradlew run
```

or:

```bash
$ ./gradlew build
$ java -XX:+ExitOnOutOfMemoryError -Xmx1g -jar build/libs/codingchallenge-0.0.1-SNAPSHOT-all.jar
```

There is minimal log output with SLF4J/Logback to **stderr**. This can be redirected to **/dev/null** if districting. Logback configuration is in `src/main/resources/logback.xml`.
