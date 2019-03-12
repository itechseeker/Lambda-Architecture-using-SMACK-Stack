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
import spray.json._
import scala.io.StdIn
import scala.concurrent.Future

object AkkaServer {

  // To run the route
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  // Used for Future flatMap/onComplete/Done
  implicit val executionContext = system.dispatcher

  //creating Cluster object
  val cluster = Cluster.builder.addContactPoint("127.0.0.1").build()

  //Connect to the lambda_architecture keyspace
  val cassandraConn = cluster.connect("testkeyspace")


  //Define Employee class
  case class Employee(emp_id:Long, emp_name: String,emp_city: String, emp_phone: Long,emp_sal: Long)
  //Formats for unmarshalling and marshalling
  //Using jsonFormat5 as Employee has 5 input parameters
  implicit val empFormat = jsonFormat5(Employee)

  def main(args: Array[String]) {

    //Define a route with Get and POST
    val route: Route =
      get {
        path("getAll" ) {
          complete( cassandraReader("select JSON * from emp")   )
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
    * Read the Cassandra Data and convert each Row to Employee object
    * @param query the query to execute
    * @return the list of Employee
    */
  def cassandraReader(query: String):List[Employee] = {
    var empList:List[Employee] = Nil

    //Get the result set from query execution
    val resultSet= cassandraConn.execute(query)

    //Get the iterator of the result set
    val it=resultSet.iterator()
    while(it.hasNext)
    {
      //Convert each row of json data to Employee object
      val jsonString=resultSet.one().getString(0)
      val jsonObj=jsonString.parseJson.convertTo[Employee]

      //Add to empList
      empList=  jsonObj :: empList
    }

    return empList
  }
}