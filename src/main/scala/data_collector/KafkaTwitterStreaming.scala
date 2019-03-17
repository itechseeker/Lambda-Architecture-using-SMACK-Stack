package data_collector

import java.util.Properties
import main_package.AppConfiguration
import net.liftweb.json.DefaultFormats
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import twitter4j._
import twitter4j.conf._
import twitter4j.FilterQuery
import net.liftweb.json.Serialization.write

//Define Tweet class
case class Tweet(tweet_id:Long,
                 user_id:Long,
                 user_name: String,
                 user_loc: String,
                 content: String,
                 hashtag: String,
                 created_date:Long
                 )

object KafkaTwitterStreaming {

  def run(): Unit = {
    //The Kafka Topic
    val kafkaTopic = AppConfiguration.kafkaTopic

    //List of Twitter search keywords
    val searchKeywords=AppConfiguration.kafkaKeywords

    //Define a Kafka Producer
    val producer = new KafkaProducer[String, String](getKafkaProp)
    getStreamTweets(producer,kafkaTopic,searchKeywords )
  }

  def getStreamTweets(producer: Producer[String, String],kafkaTopic:String, searchKeywords: List[String]): Unit = {
    val twitterStream = new TwitterStreamFactory(getTwitterConf()).getInstance()
    val listener = new StatusListener() {
      override def onStatus(status: Status): Unit = {

        val tweet_id=status.getId
        val created_date=status.getCreatedAt.getTime
        var content=status.getText()
        val lang=status.getLang()
        val user=status.getUser()

        // Convert a list of HashTagEntity to String seperated by ", "
        val hashtag=status.getHashtagEntities().toList.map(_.getText).mkString(", ")

        //Need to use getRetweetedStatus.getText() in the case of Re-Tweet to get
        // the full content
        if (status.getRetweetedStatus != null)
              content=status.getRetweetedStatus.getText

        //Only collect English tweets
        if(lang.equals("en"))
          {
            //Convert Tweet object to Json using Lift-JSON library
            implicit val formats = DefaultFormats
            val message = write(Tweet(tweet_id,user.getId,user.getName,user.getLocation,content,hashtag,created_date))
            //System.out.println(message)

            //Send data to a Kafka topic
            val data = new ProducerRecord[String, String](kafkaTopic, message)
            producer.send(data)
          }
      }

      override def onException(ex: Exception): Unit = {
        ex.printStackTrace()
      }

      override def onDeletionNotice(statusDeletionNotice: StatusDeletionNotice): Unit = {
      }

      override def onTrackLimitationNotice(numberOfLimitedStatuses: Int): Unit = {
      }

      override def onScrubGeo(userId: Long, upToStatusId: Long): Unit = {
      }

      override def onStallWarning(warning: StallWarning): Unit = {
      }
    }
    twitterStream.addListener(listener)

    //Get tweet that contain a keyword in the list searchKeywords
    val query = new FilterQuery().track(searchKeywords:_*)
    twitterStream.filter(query)
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
    val cb = new ConfigurationBuilder()
    cb.setDebugEnabled(true)
      .setJSONStoreEnabled(true)
      .setOAuthConsumerKey(AppConfiguration.consumerKey)
      .setOAuthConsumerSecret(AppConfiguration.consumerSecret)
      .setOAuthAccessToken(AppConfiguration.accessToken)
      .setOAuthAccessTokenSecret(AppConfiguration.accessTokenSecret)
    return cb.build()
  }
}
