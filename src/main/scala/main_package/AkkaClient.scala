package main_package

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import spray.json.DefaultJsonProtocol._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.unmarshalling._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._

import scala.concurrent.Future
import scala.util.{Failure, Success}

object AkkaClient {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher

    val responseFuture: Future[HttpResponse] = Http().singleRequest(HttpRequest(uri = "http://localhost:8080/getAll"))


    //Define Employee class
    case class Employee(emp_id:Long, emp_name: String,emp_city: String, emp_phone: Long,emp_sal: Long)
    //Formats for unmarshalling and marshalling
    //Using jsonFormat5 as Employee has 5 input parameters
    implicit val empFormat = jsonFormat5(Employee)

    responseFuture
      .onComplete {
        case Success(res) =>
          val entity = Unmarshal(res.entity).to[Employee]
          println(entity)
        case Failure(_)   => sys.error("something wrong")
      }

  }
}