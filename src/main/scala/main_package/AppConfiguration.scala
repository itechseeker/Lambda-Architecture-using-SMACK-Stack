package main_package

import com.typesafe.config.ConfigFactory

import scala.collection.JavaConversions._
import scala.concurrent.duration.Duration

object AppConfiguration {
  val config = ConfigFactory.load()

  // Connection keys for Twitter Streaming
  val consumerKey = config.getString("twitter.consumerKey")
  val consumerSecret = config.getString("twitter.consumerSecret")
  val accessToken = config.getString("twitter.accessToken")
  val accessTokenSecret = config.getString("twitter.accessTokenSecret")

  // Kafka Config
  val kafkaTopic=config.getString("kafka.topic")
  val kafkaKeywords = config.getStringList("kafka.keywords").toList

  // Batch processing config
  // Convert Duration to Finite Duration
  val tweetDuration=Duration.fromNanos(config.getDuration("batchProcessing.tweetDuration").toNanos)
  val batchInterval=Duration.fromNanos(config.getDuration("batchProcessing.batchInterval").toNanos)
}
