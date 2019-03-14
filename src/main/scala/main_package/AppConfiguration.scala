package main_package

import com.typesafe.config.ConfigFactory
import scala.collection.JavaConversions._


object AppConfiguration {
  val config = ConfigFactory.load()

  // Connection keys for Twitter Streaming
  val consumerKey = config.getString("twitter.consumerKey")
  val consumerSecret = config.getString("twitter.consumerSecret")
  val accessToken = config.getString("twitter.accessToken")
  val accessTokenSecret = config.getString("twitter.accessTokenSecret")

  // Kafka
  val kafkaTopic=config.getString("kafka.topic")
  val kafkaKeywords = config.getStringList("kafka.keywords").toList


/*
  val port = config.getInt("cassandra.port")
  val hosts = config.getStringList("cassandra.hosts")
  val cassandraKeyspaces = config.getStringList("cassandra.keyspaces")
  val replicationFactor = config.getString("cassandra.replication_factor").toInt
  val readConsistency = config.getString("cassandra.read_consistency")
  val writeConsistency = config.getString("cassandra.write_consistency")
*/

}
