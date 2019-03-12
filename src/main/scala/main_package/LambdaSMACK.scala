package main_package

import data_collector.{CassandraDB, KafkaRunner}

object LambdaSMACK {
  def main(args: Array[String]): Unit = {
    //Start Zookeeper, Kafka server, Cassandra and Kafka Cassandra Connector
    KafkaRunner.start()

    //Connect to Cassandra database and create necessary keyspace and table
    CassandraDB.runDB()
  }

}
