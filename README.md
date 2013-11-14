# pubsub-bench

A micro-benchmark for the performance of a publish/subscribe architecture in a distributed system

## Running

Trivial way:

    mvn compile
    mvn exec:java -Dexec.mainClass="bench.pubsub.Bench" -Djava.net.preferIPv4Stack=true
    
It will run a single thread that publishes messages of 100 bytes.  To change
the defaults use:

    -Dexec.args="NAME-OF-COMM-SYSTEM 3 1000 BOOT-SEQ"
    
Where NAME-OF-COMM-SYSTEM is one of "HazelcastCommSystem" or
"ZeroMQCommSystem" (the default is the former).
    
Second arg is the number of threads and the third is the message payload in
bytes.

If using ZeroMQ you need a sequencer.
Either you run it by itself with

    mvn exec:java -Dexec.mainClass="bench.pubsub.ZeroMQSequencer"

Or you need to use the fourth arg when launching one of the nodes.

To adjust the JVM params on can use e.g.:

    export MAVEN_OPTS="-verbose:gc -Xmx1000M -Xms500M -server -XX:+UseConcMarkSweepGC -XX:+AggressiveOpts -verbose:gc -XX:ParallelGCThreads=4 -XX:NewRatio=5 -XX:SurvivorRatio=2 -Dhazelcast.operation.thread.count=4"
    
    
To start the benchmark without maven the following command may help

    mvn dependency:build-classpath

    

