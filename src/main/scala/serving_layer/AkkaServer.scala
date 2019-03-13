package serving_layer

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.Done
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import com.datastax.driver.core.Cluster
import spray.json.DefaultJsonProtocol._
import scala.io.StdIn
import scala.collection.JavaConversions._


object AkkaServer {

  // To run the route
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  // Used for Future flatMap/onComplete/Done
  implicit val executionContext = system.dispatcher

  //creating Cluster object
  val cluster = Cluster.builder.addContactPoint("127.0.0.1").build()

  //Connect to the lambda_architecture keyspace
  val cassandraConn = cluster.connect("lambda_architecture")


  //Define Hashtag class
  case class Hashtag(value: String, var count: Long){

    // Set new value for count variable
    def setCount(newValue:Long)={
      count=newValue
    }

    // Override the equals method to use List contains() method
    override def equals(otherObj: Any): Boolean =
      otherObj match {
        case otherObj: Hashtag => (otherObj.value).equals(this.value)
        case _ => false
      }
  }

  //Formats for unmarshalling and marshalling
  //Using jsonFormat2 as Hashtag has 2 input parameters
  implicit val empFormat = jsonFormat2(Hashtag)

  def main(args: Array[String]) {

    //Define a route with Get and POST
    val route: Route =
      get {
        path("getAll" ) {
          complete( getViews()   )
        }
      }
    //Binding to the host and port
    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)
    println(s"Server online at http://localhost:8080/\nPress Enter to stop...")
    StdIn.readLine() // let the server run until user presses Enter

    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ ⇒ system.terminate()) // and shutdown when done

    //Close cassandra connection
    cluster.close()

  }


  /**
    * Combine the hashtag of both batch view and realtime view
    * @return a list of Hashtag
    */
  def getViews():List[Hashtag] = {
    var hashtagList:List[Hashtag] = Nil

    //Get the batchview
    val batchViewResult= cassandraConn.execute("select * from hashtag_batchview").all().toList

    // Convert each row to the Hashtag object
    batchViewResult.map { row =>
      // add the hashtag to the list
      hashtagList = Hashtag(row.getString("value"), row.getLong("count"))::hashtagList
    }

    //Get the realtimeview
    val realtimeViewResult= cassandraConn.execute("select * from hashtag_realtimeview").all().toList

    // Convert each row to the Hashtag object
    realtimeViewResult.map { row =>
      val newHashtag=new Hashtag(row.getString("hashtag"), row.getInt("count"))

      // Increase the count if the hashtag is already in the batch view
      if(hashtagList.contains(newHashtag))
      {
        val oldHash=hashtagList(hashtagList.indexOf(newHashtag))
        oldHash.setCount(oldHash.count+newHashtag.count)
      }
      else
        hashtagList=newHashtag :: hashtagList // add the the list if the hashtag is not in the batch view
    }

    // sort the list of hashtag by its count
    hashtagList=hashtagList.sortBy(row => row.count).reverse

    return hashtagList
  }
}