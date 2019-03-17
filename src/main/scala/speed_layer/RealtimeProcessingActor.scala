package speed_layer

import akka.actor.{Actor, ActorSystem, Props}
import com.datastax.spark.connector.cql.CassandraConnector
import main_package.AppConfiguration
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types.{LongType, StringType, StructField, StructType}
import org.apache.spark.sql.functions.{desc, from_json, lower}
import scala.concurrent.duration._
import org.apache.spark.sql.streaming.{StreamingQuery, Trigger}

class RealtimeProcessingSpark{
  //Define a Spark session
  val spark=SparkSession.builder().appName("Lambda Architecture - Speed layer")
    .master("local")
    .getOrCreate()

  //Set the Log file level
  spark.sparkContext.setLogLevel("WARN")

  //Implicit methods available in Scala for converting common Scala objects into DataFrames
  import spark.implicits._

  //Define Schema of received tweet streem
  val twitterDataScheme
  = StructType(
    List(
      StructField("tweet_id", LongType, true),
      StructField("user_id", LongType, true),
      StructField("user_name", StringType, true),
      StructField("user_loc", StringType, true),
      StructField("content", StringType, true),
      StructField("hashtag", StringType, true),
      StructField("created_date", LongType, true)
    )
  )
  // Define the query that is currently running
  var activeQuery:StreamingQuery=null

  def restartQuery()={

    //Connect Spark to Cassandra to remove all existing data from hashtag_realtimeview table
    val connector = CassandraConnector(spark.sparkContext.getConf)
    connector.withSessionDo(session => session.execute("TRUNCATE lambda_architecture.hashtag_realtimeview"))

    // Create a query if it is not exist
    if(activeQuery==null)
      {
        activeQuery=realtimeAnalysis()
      }
    // Stop the existing query and start a new one
    else
      {
        activeQuery.stop()
        activeQuery=realtimeAnalysis()
      }
  }

  def realtimeAnalysis(): StreamingQuery = {
    //Subscribe Spark the defined Kafka topic
    val df = spark.readStream.format("kafka")
      .option("kafka.bootstrap.servers", "localhost:9092")
      .option("subscribe", AppConfiguration.kafkaTopic)
      .load()

    //Reading the streaming json data with its schema
    val twitterStreamData = df.selectExpr("CAST(value AS STRING) as jsonData")
      .select(from_json($"jsonData", schema = twitterDataScheme).as("data"))
      .select("data.*")

    //Only select the hashtag column and convert it to lower case for counting
    var hashtag_df = twitterStreamData.select(lower($"hashtag"))

    //Only analyse tweets that have at least one hashtag
    hashtag_df = hashtag_df.filter($"hashtag".notEqual("null"))

    //Split hashtags string into each individual hashtag
    var hashtag_indv = hashtag_df.as[String].flatMap(_.split(", ")).filter($"value".notEqual(""))

    //Count the occurance of each hashtag and rename value column to hashtag
    val hashtagCount = hashtag_indv.groupBy("value").count().sort(desc("count")).withColumnRenamed("value", "hashtag")

    //Saving data to hashtag_realtimeview table using foreachBatch
    val query = hashtagCount.writeStream.foreachBatch{(batchDF, batchId) =>
          batchDF.write.format("org.apache.spark.sql.cassandra")
                       .mode("Append")
                       .options(Map("table" -> "hashtag_realtimeview", "keyspace" -> "lambda_architecture"))
                       .save()
          }.outputMode("complete").start()

    return query
  }
}

case object StartProcessing

//Define RealtimeProcessing actor
class RealtimeProcessingActor(spark_realtimeProc: RealtimeProcessingSpark) extends Actor{

  //Implement receive method
  def receive = {
    //Start hashtag realtime processing
    case StartProcessing => {
      println("\nRestart hashtag realtime processing...")
      spark_realtimeProc.restartQuery()
    }
  }

}
object Runner {
  def main(args: Array[String]): Unit = {
    //Creating an ActorSystem
    val actorSystem = ActorSystem("ActorSystem");

    //Create realtime processing actor
    val realtimeActor = actorSystem.actorOf(Props(new RealtimeProcessingActor(new RealtimeProcessingSpark)))

    //Using akka scheduler to run the batch processing periodically
    import actorSystem.dispatcher
    val initialDelay = 100 milliseconds
    val batchInterval= 3 minutes

    actorSystem.scheduler.schedule(initialDelay,batchInterval,realtimeActor,StartProcessing)

  }
}
