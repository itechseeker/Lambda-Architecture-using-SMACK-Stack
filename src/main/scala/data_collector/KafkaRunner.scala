package data_collector

import java.io.{File, InputStreamReader,BufferedReader}
import akka.actor.{Actor, ActorSystem, Props}


case object StartZookeeper
case object StartKafkaServer
case object StartKafkaCassandra

//Define Actors by extending Actor trait
class KafkaActor extends Actor{
  //Set the directory of Kafka
  val kafkaDir="/home/tinhtb/WorkSpace/ITechSeeker/Tools/kafka_2.12-2.1.0/"

  //The command to run zookeeper, kakfka server and Kafka Cassandra Connector
  val zookeeperCmd="bin/zookeeper-server-start.sh config/zookeeper.properties"
  val serverCmd="bin/kafka-server-start.sh config/server.properties"
  val kafkaCassandraCmd="bin/connect-standalone.sh config/connect-standalone.properties config/cassandra-sink.properties"

  //Implement cmdExecuter method to execute a command string
  def cmdExecuter(cmd: String)={
    //using .exec() method to run the command
    val process = Runtime.getRuntime().exec(cmd, null, new File(kafkaDir))

    //Print the output of the process
    val reader = new BufferedReader(new InputStreamReader(process.getInputStream))
    var line:String= null
    while ((line = reader.readLine()) != null) {
      System.out.println(line)
    }

    //Waiting until the process has been terminated
    process.waitFor()

  }

  //Implement receive method
  def receive = {

    //Start Zookeeper
    case StartZookeeper => {
      println("\nStart Zookeeper...")
      cmdExecuter(zookeeperCmd)
    }

    //Start Kafka Server
    case StartKafkaServer => {
      println("\nStart Kafka Server...")
      cmdExecuter(serverCmd)
    }

    //Start Kafka Cassandra Connector
    case StartKafkaCassandra => {
      println("\nStart Kafka Cassandra Connector...")
      cmdExecuter(kafkaCassandraCmd)
    }
  }
}

object KafkaRunner {
  def main(args: Array[String]): Unit = {
    {
      //Creating an ActorSystem
      var actorSystem = ActorSystem("ActorSystem");

      //Create 3 actors to perform three different jobs parallel
      //Because actors always work sequentially.
      //We cannot force an actor to process more than one message at a time
      val actor1 = actorSystem.actorOf(Props[KafkaActor])
      val actor2 = actorSystem.actorOf(Props[KafkaActor])
      val actor3 = actorSystem.actorOf(Props[KafkaActor])

      //Send message to each actor to ask them doing their job
      actor1 ! StartZookeeper
      //Waiting until the Zookeeper successfully started
      Thread.sleep(10000)

      actor2 ! StartKafkaServer
      Thread.sleep(10000)

      actor3 ! StartKafkaCassandra

    }
  }

}
