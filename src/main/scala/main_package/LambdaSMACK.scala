package main_package

import data_collector.{CassandraDB, KafkaRunner}
import serving_layer.AkkaServer
import serving_layer.AkkaServer.Hashtag

object LambdaSMACK {
  case class Hashtag(value: String, count: Long)
  def main(args: Array[String]): Unit = {
    //Start Zookeeper, Kafka server, Cassandra and Kafka Cassandra Connector
    //KafkaRunner.start()

    //Connect to Cassandra database and create necessary keyspace and table
    //CassandraDB.runDB()
    var hashtagList=List(Hashtag("ai",6),Hashtag("ml",5),Hashtag("ai",4))

    // Group the Hashtag object that have the same value and sum their count
    val temp=hashtagList.groupBy(_.value).map(el => Hashtag(el._1,el._2.map(_.count).sum))
    println(temp)
    println(hashtagList)
  }

}
