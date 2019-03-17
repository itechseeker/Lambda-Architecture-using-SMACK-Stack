package batch_layer

import akka.actor.{Actor, ActorRef}
import org.apache.spark.sql.functions.desc
import com.datastax.spark.connector.cql.CassandraConnector
import main_package.AppConfiguration
import org.apache.spark.sql.SaveMode
import org.apache.spark.sql.functions.lower
import speed_layer.StartProcessing

class BatchProcessingSpark{
  //Create a Spark session which connect to Cassandra
  val spark = org.apache.spark.sql.SparkSession
    .builder()
    .master("local[*]")
    .config("spark.cassandra.connection.host", "localhost")
    .appName("Lambda architecture - Batch Processing")
    .getOrCreate()

  //Implicit methods available in Scala for converting common Scala objects into DataFrames
  import spark.implicits._

  //Get Spark Context from Spark session
  val sparkContext = spark.sparkContext

  //Set the Log file level
  sparkContext.setLogLevel("WARN")

  def hashtagAnalysis: Unit ={

    //Read master_dataset table using DataFrame
    val df = spark.read
      .format("org.apache.spark.sql.cassandra")
      .options(Map( "table" -> "master_dataset", "keyspace" -> "lambda_architecture"))
      .load()

    //Only select the hashtag column and convert it to lower case for counting
    var hashtag_df=df.select(lower($"hashtag"))

    //Get the current time
    val current_time=System.currentTimeMillis()

    // Set time to filter tweet (most recent tweet)
    val tweetDuration=AppConfiguration.tweetDuration

    //Only analyse tweets posted in last three hours which have at least one hashtag
    hashtag_df=hashtag_df.filter($"created_date">(current_time-tweetDuration.toMillis) && $"hashtag".notEqual("null"))

    //Split hashtags string into each individual hashtag
    val hashtag_indv=hashtag_df.as[String].flatMap(_.split(", "))

    //Count the occurance of each hashtag
    val hashtagCount=hashtag_indv.groupBy("value").count().sort(desc("count")).withColumnRenamed("value","hashtag")

    //Connect Spark to Cassandra to remove all existing data from hashtag_batchview table
    val connector = CassandraConnector(sparkContext.getConf)
    connector.withSessionDo(session => session.execute("TRUNCATE lambda_architecture.hashtag_batchview"))

    //Save new data to hashtag_batchview table
    hashtagCount.write.format("org.apache.spark.sql.cassandra")
      .options(Map("keyspace"->"lambda_architecture","table"->"hashtag_batchview"))
      .mode(SaveMode.Append)
      .save()
  }
}

case object HashTagProcessing

//Define BatchProcessing actor
class BatchProcessingActor(spark_processor: BatchProcessingSpark, realtimeActor:ActorRef) extends Actor{

  //Implement receive method
  def receive = {
    //Start hashtag batch processing
    case HashTagProcessing => {
      println("\nStart hashtag batch processing...")

      // Send StartProcessing to realtimeActor to start/restart realtime analysis
      realtimeActor!StartProcessing

      // Perform batch processing
      spark_processor.hashtagAnalysis
    }
  }
}

