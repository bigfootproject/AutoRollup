This repository contains Apache Pig version 0.14.0 with a completely new ROLLUP operator. The new operator outperforms the current implementation of Pig by at least 50%. More details on the design of the operator can be found in the following paper:
- [Efficient and Self-Balanced ROLLUP Aggregates for Large-Scale Data Summarization] (http://www.eurecom.fr/fr/publication/4590/detail/efficient-and-self-balanced-rollup-aggregates-for-large-scale-data-summarization)

The new operator utilizes the MapReduce ROLLUP algorithms proposed in the following paper:
- [On the design space of MapReduce ROLLUP aggregates] (http://www.eurecom.fr/fr/publication/4212/detail/on-the-design-space-of-mapreduce-rollup-aggregates)

Everyone who is interested in the algorithms and the design of the new operator is greatly welcome to check out our papers. For those who are interested in using and seeing the enhancement of the new operator at hands, please find below the compiling guide:

##Compiling Pig with the new ROLLUP operator
Our ROLLUP operator is integrated to the latest Apache Pig version (0.14.0). It is
compiled using Ant. The command line to build Pig for a cluster running on a
Hadoop cluster is as follow:

```
$ ant -Dhadoopversion=23
```
Currently most of major Hadoop distributions are from Hadoop 0.23 branch. If you use the Hadoop 0.20 branch, please replace the hadoopversion to 20.

Users can also build Pig as a standalone and locally executable program by using
the following command:

```
$ ant clean jar-withouthadoop -Dhadoopversion=23
```

##Using the new ROLLUP operator
To take advantages of the new ROLLUP operator, users simply issue a ROLLUP query to
Pig: our optimization automatically perform the rest. It detects the aggregations,
samples the data to gather data and performance statistics, sets the best
operating configuration and finally, triggers the new ROLLUP operator.

An example of a ROLLUP query is as follow:

```
A = LOAD path/file AS (year, month, day, hour, minute, second, value);
B = CUBE A BY ROLLUP (year, month, day, hour, minute, second) RATE samplingRate;
C = FOREACH B GENERATE group, SUM(cube.value);
```

The sampling rate determines how the ROLLUP operator samples the data. In case it
is not specified, the ROLLUP optimization automatically sets an appropriate value.
