package batch_layer

import akka.actor.{Actor, ActorSystem, Props}
import org.apache.spark.sql.functions.desc
import com.datastax.spark.connector.cql.CassandraConnector
import main_package.AppConfiguration
import org.apache.spark.sql.SaveMode
import scala.concurrent.duration._
import org.apache.spark.sql.functions.lower

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

    //Display some data of master_dataset
    println("Total number of rows: "+df.count())
    println("First 15 rows of the DataFrame: ")
    df.show(15)

    //Only select the hashtag column and convert it to lower case for counting
    var hashtag_df=df.select(lower($"hashtag"))

    //Get the current time
    val current_time=System.currentTimeMillis()

    // Set time to filter tweet (most recent tweet)
    val tweetDuration=AppConfiguration.tweetDuration

    //Only analyse tweets posted in last three hours which have at least one hashtag
    hashtag_df=hashtag_df.filter($"created_date">(current_time-tweetDuration.toMillis) && $"hashtag".notEqual("null"))

    println("The first ten tweets containing hashtag: ")
    hashtag_df.show(10)

    //Split hashtags string into each individual hashtag
    var hashtag_indv=hashtag_df.as[String].flatMap(_.split(", "))


    println("The first ten hashtags: ")
    hashtag_indv.show(10)

    //Count the occurance of each hashtag
    val hashtagCount=hashtag_indv.groupBy("value").count().sort(desc("count")).withColumnRenamed("value","hashtag")
    println("The top ten popular hashtags: ")
    hashtagCount.show(10)

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
class BatchProcessingActor(spark_processor: BatchProcessingSpark) extends Actor{

  //Implement receive method
  def receive = {
    //Start hashtag batch processing
    case HashTagProcessing => {
      println("\nStart hashtag batch processing...")
      spark_processor.hashtagAnalysis
    }
  }

}

object BatchProcessor {
  def main(args: Array[String]): Unit = {

    //Creating an ActorSystem
    val actorSystem = ActorSystem("ActorSystem");

    //Create batch actor
    val batchActor = actorSystem.actorOf(Props(new BatchProcessingActor(new BatchProcessingSpark)))


    //Using akka scheduler to run the batch processing periodically
    import actorSystem.dispatcher
    val initialDelay = 100 milliseconds
    val batchInterval=AppConfiguration.batchInterval //running batch processing after each 30 mins

    actorSystem.scheduler.schedule(initialDelay,batchInterval,batchActor,HashTagProcessing)

  }
}
