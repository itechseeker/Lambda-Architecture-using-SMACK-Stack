package serving_layer

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
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
  case class Hashtag(value: String, count: Long)

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
      hashtagList = Hashtag(row.getString("hashtag"), row.getLong("count"))::hashtagList
    }

    //Get the realtimeview
    val realtimeViewResult= cassandraConn.execute("select * from hashtag_realtimeview").all().toList

    // Convert each row to the Hashtag object
    realtimeViewResult.map { row =>
      // add the hashtag to the list
      hashtagList = Hashtag(row.getString("hashtag"), row.getInt("count"))::hashtagList
    }

    // Group the Hashtag object that have the same value and sum their count
    var finalList=hashtagList.groupBy(_.value).map(el => Hashtag(el._1,el._2.map(_.count).sum)).toList

    // sort the list of hashtag by its count
    finalList=finalList.sortBy(row => row.count).reverse


    return finalList
  }
}