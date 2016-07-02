# EnMasseBench

The EnMasseBench or ebench for short is supposed to be a high performance client for EnMasse
benchmarking. It will send messages as fast as it can, and allows scaling the number of
senders/receivers to increase the load.

## Building

    gradle build

## Running
    tar xvf ebench-agent/build/distributions/ebench-agent.tar
    ./ebench-agent/bin/ebench-agent -h 127.0.0.1 -c 1 -a amqp-test -p 5674 -d 60 -p 10 -s 128  


The agent can also be run as part of an EnMasse cluster, where multiple agents can be scaled by
increasing the number of replicas. The ebench-collector can then talk to the ebench-agents and
collect metrics for aggregation and integration with other services such as prometheus.
