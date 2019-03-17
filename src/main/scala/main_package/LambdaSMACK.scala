package main_package

import akka.actor.{ActorSystem, Props}
import batch_layer.{BatchProcessingActor, BatchProcessingSpark, HashTagProcessing}
import data_collector.{CassandraDB, EnvRunner, KafkaTwitterStreaming}
import serving_layer.AkkaServer
import speed_layer.{RealtimeProcessingActor, RealtimeProcessingSpark}
import scala.concurrent.duration._

object LambdaSMACK {
  def main(args: Array[String]): Unit = {
    // Start Zookeeper, Kafka server, Cassandra and Kafka Cassandra Connector
    println("Setting environment...")
    EnvRunner.start()

    // Connect to Cassandra database and create lambda_architecture keyspace and
    // master_dataset, hashtag_batchView, hashtag_realtimeView table
    println("Creating database...")
    CassandraDB.runDB()

    // Get Twitter streaming data and send to Kafka broker
    println("Getting data from Twitter...")
    KafkaTwitterStreaming.run()

    // Run Batch processing and realtime processing
    println("Run batch processing and realtime processing...")
    runProcessing()

    // Start Akka Server
    println("Start Akka Server...")
    AkkaServer.start()
  }
  def runProcessing()={
    //Creating an ActorSystem
    val actorSystem = ActorSystem("ActorSystem");

    //Create realtime processing actor
    val realtimeActor = actorSystem.actorOf(Props(new RealtimeProcessingActor(new RealtimeProcessingSpark)))

    //Create batch actor
    val batchActor = actorSystem.actorOf(Props(new BatchProcessingActor(new BatchProcessingSpark,realtimeActor)))

    //Using akka scheduler to run the batch processing periodically
    import actorSystem.dispatcher
    val initialDelay = 100 milliseconds
    val batchInterval=AppConfiguration.batchInterval //running batch processing after each 30 mins

    actorSystem.scheduler.schedule(initialDelay,batchInterval,batchActor,HashTagProcessing)
  }
}
