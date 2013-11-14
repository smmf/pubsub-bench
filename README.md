# pubsub-bench

A micro-benchmark for the performance of a publish/subscribe architecture in a distributed system

## Running

Trivial way:

    mvn clean compile exec:java -Dexec.mainClass="bench.pubsub.Bench" -Djava.net.preferIPv4Stack=true
    
It will run a single thread that publishes messages of 100 bytes.  To change
the defaults use:

    -Dexec.args="3 1000"
    
First arg is the number of threads and the second is the message payload in
bytes.

To adjust the JVM params on can use e.g.:

    export MAVEN_OPTS="-verbose:gc -Xmx1000M -Xms500M -server -XX:+UseConcMarkSweepGC -XX:+AggressiveOpts -verbose:gc -XX:ParallelGCThreads=4 -XX:NewRatio=5 -XX:SurvivorRatio=2 -Dhazelcast.operation.thread.count=4"
    
    
To start the benchmark without maven the following command may help

    mvn dependency:build-classpath

    
    
