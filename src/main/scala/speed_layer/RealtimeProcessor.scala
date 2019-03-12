package speed_layer

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types.{LongType, StringType, StructField, StructType}
import org.apache.spark.sql.functions.{desc, from_json, lower}
import scala.concurrent.duration._
import org.apache.spark.sql.streaming.{OutputMode, Trigger}


object RealtimeProcessor {
  def main(args: Array[String]): Unit = {

    //Define a Spark session
    val spark=SparkSession.builder().appName("Spark Kafka Integration for real time processing")
      .master("local")
      .getOrCreate()

    //Set the Log file level
    spark.sparkContext.setLogLevel("WARN")

    //Implicit methods available in Scala for converting common Scala objects into DataFrames
    import spark.implicits._

    //Subscribe Spark to topic 'TwitterStreaming1'
    val df=spark.readStream.format("kafka")
      .option("kafka.bootstrap.servers","localhost:9092")
      .option("subscribe","TwitterStreaming1")
      .load()


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

    //Reading the streaming json data with its schema
    val twitterStreamData=df.selectExpr( "CAST(value AS STRING) as jsonData")
      .select(from_json($"jsonData",schema = twitterDataScheme).as("data"))
      .select("data.*")

    //Only select the hashtag column and convert it to lower case for counting
    var hashtag_df=twitterStreamData.select(lower($"hashtag"))

    //Get the current time
    val current_time=System.currentTimeMillis()

    //Set batch_interval = 3 hours
    val batch_interval=3

    //Only analyse tweets posted in last three hours which have at least one hashtag
    hashtag_df=hashtag_df.filter($"created_date">(current_time-batch_interval*60*60*1000) && $"hashtag".notEqual("null"))

    //Split hashtags string into each individual hashtag
    var hashtag_indv=hashtag_df.as[String].flatMap(_.split(", ")).filter($"value".notEqual(""))

    //Count the occurance of each hashtag
    //Rename value column to hashtag
    val hashtagCount=hashtag_indv.groupBy("value").count().sort(desc("count")).withColumnRenamed("value","hashtag")


    // Display output to console
    val query = hashtagCount
      .writeStream
      .outputMode("complete")
      .format("console")
      .start()

    //Saving data to hashtag_realtimeview table using foreachBatch
    val query_cassandra = hashtagCount.writeStream
      .foreachBatch((batchDF, batchId) =>
        batchDF.write
          .format("org.apache.spark.sql.cassandra")
          .mode("Append")
          .options(Map("table" -> "hashtag_realtimeview", "keyspace" -> "lambda_architecture"))
          .save())
      .outputMode("complete")
      .trigger(Trigger.ProcessingTime(30 seconds))
      .start()

    query.awaitTermination()
    query_cassandra.awaitTermination()

  }

}
