package main_package

import data_collector.{CassandraDB, CmdRunner, KafkaTwitterStreaming}
import serving_layer.AkkaServer
import serving_layer.AkkaServer.Hashtag

object LambdaSMACK {
  case class Hashtag(value: String, count: Long)
  def main(args: Array[String]): Unit = {
    // Start Zookeeper, Kafka server, Cassandra and Kafka Cassandra Connector
    CmdRunner.start()

    // Connect to Cassandra database and create lambda_architecture keyspace and
    // master_dataset, hashtag_batchView, hashtag_realtimeView table
    CassandraDB.runDB()

    // Get Twitter streaming data and send to Kafka broker
    KafkaTwitterStreaming.run()

  }

}
