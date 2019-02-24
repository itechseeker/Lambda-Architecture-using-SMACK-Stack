package data_collector

import java.util.Properties

import net.liftweb.json.DefaultFormats
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import twitter4j._
import twitter4j.conf._
import net.liftweb.json.Serialization.write

//Define Tweet class
case class Tweet(tweet_id:Long,
                 created_date:Long,
                 content: String)

object KafkaTwitterStreaming {

  def main(args: Array[String]): Unit = {
    //The Kafka Topic
    val topicName = "Big Data"

    //Define a Kafka Producer
    val producer = new KafkaProducer[String, String](getKafkaProp)
    getStreamTweets(producer, topicName)
  }

  def getStreamTweets(producer: Producer[String, String], topicName: String): Unit = {
    val twitterStream = new TwitterStreamFactory(getTwitterConf()).getInstance()
    val listener = new StatusListener() {
      override def onStatus(status: Status): Unit = {

        val tweet_id=status.getId
        val created_date=status.getCreatedAt.getTime
        var content=status.getText()
        val lang=status.getLang()

        //
        if (status.getRetweetedStatus != null)
              content=status.getRetweetedStatus.getText


        //Only collect English tweets
        if(lang.equals("en"))
          {
            //Convert Tweet object to Json using Lift-JSON library
            implicit val formats = DefaultFormats
            val message = write(Tweet(tweet_id,created_date,content))


            val data = new ProducerRecord[String, String]("TwitterStreaming1", message)
            System.out.println(message)

            //Send data
            producer.send(data)
          }
      }

      override

      def onException(ex: Exception): Unit = {
        ex.printStackTrace()
      }

      override

      def onDeletionNotice(statusDeletionNotice: StatusDeletionNotice): Unit = {
      }

      override

      def onTrackLimitationNotice(numberOfLimitedStatuses: Int): Unit = {
      }

      override

      def onScrubGeo(userId: Long, upToStatusId: Long): Unit = {
      }

      override

      def onStallWarning(warning: StallWarning): Unit = {
      }
    }
    twitterStream.addListener(listener)

    //Get Twitter stream about a topic
    twitterStream.filter(topicName)
  }


  //Define kafka properties
  def getKafkaProp():Properties={
    // create instance for properties to access producer configs
    val props = new Properties()
    //Assign localhost id
    props.put("bootstrap.servers", "localhost:9092")
    //Set acknowledgements for producer requests.
    props.put("acks", "all")
    //If the request fails, the producer can automatically retry,
    props.put("retries", 0)
    //Specify buffer size in config
    props.put("batch.size", 16384)
    //Reduce the no of requests less than 0
    props.put("linger.ms", 1)
    //The buffer.memory controls the total amount of memory available to the producer for buffering.
    props.put("buffer.memory", 33554432)
    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")

    return props
  }

  //Config the conection to Twitter
  def getTwitterConf():Configuration = {
    //Config Twitter API key to access Twitter API
    //The String keys here are only examples and not valid.
    //You need to use your own keys
    val cb = new ConfigurationBuilder()
    cb.setDebugEnabled(true)
      .setJSONStoreEnabled(true)
      .setOAuthConsumerKey("Fljmu9Wp1YVNXhqfmDHDyEAz9")
      .setOAuthConsumerSecret("7CZDMiqhaeV7FOsUTYLgi9utt4eYEVaxqVuKZj5VGHLYqO0mLU")
      .setOAuthAccessToken("1060702756430729216-1L9lL05TdEbanhGDFETkKMknmbw70w")
      .setOAuthAccessTokenSecret("Qu41ydcAzTxClfVW4BMU6UjziS6Lv9Kkwz1zBXKh3JWrx")
    return cb.build()
  }

}
