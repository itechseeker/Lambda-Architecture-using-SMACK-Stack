package data_collector

import java.io.{File, InputStreamReader,BufferedReader}
import akka.actor.{Actor, ActorSystem, Props}

case object StartZookeeper
case object StartKafkaServer
case object StartCassandra
case object StartKafkaCassandra

//Define Kafka actor
class EnvActor extends Actor{
  //Set the directory of Kafka and Cassandra
  val kafkaDir="/home/itechseeker/WorkSpace/ITechSeeker/Tools/kafka_2.12-2.1.0"
  val cassandraDir="/home/itechseeker/WorkSpace/ITechSeeker/Tools/apache-cassandra-3.11.4"

  //The command to run zookeeper, kafka server and Kafka Cassandra Connector
  val zookeeperCmd="bin/zookeeper-server-start.sh config/zookeeper.properties"
  val serverCmd="bin/kafka-server-start.sh config/server.properties"
  val cassandraCmd="bin/cassandra -f" //Run in foreground as it runs in background by default
  val kafkaCassandraCmd="bin/connect-standalone.sh config/connect-standalone.properties config/cassandra-sink.properties"

  //Implement cmdExecuter method to execute a command string
  def cmdExecuter(cmd: String, dir : String)={
    //using .exec() method to run the command
    val process = Runtime.getRuntime().exec(cmd, null, new File(dir))

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
      cmdExecuter(zookeeperCmd,kafkaDir)
    }

    //Start Kafka Server
    case StartKafkaServer => {
      println("\nStart Kafka Server...")
      cmdExecuter(serverCmd,kafkaDir)
    }

    //Start Cassandra
    case StartCassandra => {
      println("\nStart Cassandra ...")
      cmdExecuter(cassandraCmd,cassandraDir)
    }

    //Start Kafka Cassandra Connector
    case StartKafkaCassandra => {
      println("\nStart Kafka Cassandra Connector...")
      cmdExecuter(kafkaCassandraCmd,kafkaDir)
    }
  }
}

object EnvRunner {
  def start(): Unit = {
    {
      //Creating an ActorSystem
      val actorSystem = ActorSystem("ActorSystem");

      //Create 4 actors to perform four different jobs parallel
      //Because actors always work sequentially.
      //We cannot force an actor to process more than one message at a time
      val actor1 = actorSystem.actorOf(Props[EnvActor])
      val actor2 = actorSystem.actorOf(Props[EnvActor])
      val actor3 = actorSystem.actorOf(Props[EnvActor])
      val actor4 = actorSystem.actorOf(Props[EnvActor])

      //Send message to each actor to ask them doing their job
      actor1 ! StartZookeeper
      //Waiting until the Zookeeper successfully started
      Thread.sleep(10000)

      actor2 ! StartKafkaServer
      Thread.sleep(10000)

      actor3 ! StartCassandra
      Thread.sleep(5000)

      actor4 ! StartKafkaCassandra
      Thread.sleep(5000)
    }
  }
}
