### Instructions

```bash
$ ./gradlew run
```

or:

```bash
$ ./gradlew build
$ java -XX:+ExitOnOutOfMemoryError -Xmx1g -jar build/libs/codingchallenge-0.0.1-SNAPSHOT-all.jar
```

There is minimal log output using SLF4J/Logback to **stderr**. This can be redirected to **/dev/null** if the log output is found to be distracting. Logback configuration is in `src/main/resources/logback.xml`.

### Testing

There are some unit tests, but due to time constraints I ended up performing end-to-end tests to verify correctness. Testing strategy:

- Generate 6 files with each containing 10,000,000 random numbers in the expected format
- Write a multi-threaded Java app to send the above files to the server
- Write a Java app to verify that all of the numbers sent are present in `numbers.log`

6 files are used to verify that the server limits the maximum number of concurrent clients to 5.

Random number generator:

```bash
awk 'BEGIN {
  srand()
  for (i = 0; i < 10000000; i++) {
    printf("%09d\n", int(rand() * 1000000000))
  }
}'
```

### Performance numbers

- Java client and server running on the same system with 1GB memory each
- 2018 MacBook Pro
- 2.2 GHz 6-Core Intel Core i7
- 32 GB 2400 MHz DDR4
- Amazon Corretto OpenJDK 64-Bit 11.0.6.10.1

A typical run of the client looks like:

```
10000000 messages sent in: 24308 milliseconds
10000000 messages sent in: 24413 milliseconds
10000000 messages sent in: 24740 milliseconds
10000000 messages sent in: 24812 milliseconds
10000000 messages sent in: 25452 milliseconds
10000000 messages sent in: 41567 milliseconds
```

Note that the 6th client is an outlier, as it must wait for another client to finish before it can begin sending data. While the first 5 clients are sending data, we average around 1.9M messages per second. Disabling batching reduces the rate to 165K per second.

### Design Considerations

Java, for all of its robust tooling and ecosystem, has limitations when it comes to high performance software. The lack of [value types](https://openjdk.java.net/jeps/169) and garbage collection overhead requires a different approach to maximize throughput.

Ordinarily, I would focus more on [functional design patterns](https://fsharpforfunandprofit.com/fppatterns/) (immutability, pure methods, etc.) but ingestion demands a more [low-level approach](https://www.lessjava.com/2019/04/waste-free-coding.html).

### Implementation Decisions

- Minimize object allocations
- Batch operations
- Avoid syntactic sugar
- Minimize contention
- Single writer for persistent data

Minimizing calls to `new` keeps garbage collection overhead to a minimum. One other strategy is [off-heap storage](https://flink.apache.org/news/2015/09/16/off-heap-memory.html), but that requires very careful management.

Batching items is a popular [strategy](https://docs.cloudera.com/documentation/kafka/latest/topics/kafka_performance.html#kafka_performance_tuning) for boosting throughput. The batch size is by default set to 1000.

Though Java features such as `Streams` increase developer productivity, they typically come with a runtime cost. We are almost always willing to make that tradeoff, but not in this case. Of course a profiler is your friend and this strategy may be a micro-optimization.

The scalability benefits of kernel threads decrease as contention rises. The amount of data shared between threads should be minimized along with the size of critical sections.

Limiting write access to a [single thread or process](https://mechanical-sympathy.blogspot.com/2011/09/single-writer-principle.html) may yield performance benefits as well as removing race conditions. Examples include Kafka, HBase, Disruptor, etc. Allowing multi-masters (Cassandra, Riak) leads to complicated conflict resolution designs such as CRDTs and vector clocks.

### Implementation Notes

The server uses a [semaphore](https://en.wikipedia.org/wiki/Semaphore_%28programming%28) to limit the number of outstanding clients. The semaphore is initialized to 5, and the server must call P() before accepting a new connection. Each client will call V() after closing their connection.

It is possible that the server will be blocked waiting for a new connection when the **terminate** record is received. To unblock the server, the client that receives the **terminate** record will close the server socket. This action will cause the server to unblock via an `IOException`.

The application uses a blocking queue to share data between the clients ingesting records, and the consumer responsible for de-duping and persisting records. The blocking behavior provides backpressure when the consumer is falling behind without skyrocketing memory usage.

`Executors` are used to manage threads and scheduled tasks. Threads will exit if they detect they have been interrupted, typically during executor shutdown.

A `BitSet` is used to de-dupe records.
