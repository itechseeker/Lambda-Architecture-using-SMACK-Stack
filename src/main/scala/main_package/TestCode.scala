package main_package

import data_collector.KafkaRunner
import org.apache.spark.sql.SparkSession

object TestCode {
  def main(args: Array[String]): Unit = {

    //KafkaRunner.start()
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

    var hashtags = List(new Hashtag("Oscar", 5),
      new Hashtag("Otto",  9))

    println(hashtags.toString())
    var newHash=new Hashtag("Oscar", 15)
    if(hashtags.contains(newHash))
      {
        val oldHash=hashtags(hashtags.indexOf(newHash))
        oldHash.setCount(oldHash.count+newHash.count)
      }




    hashtags=hashtags.sortBy(r => r.count).reverse
    println(hashtags.toString())
    println(hashtags.contains(new Hashtag("Oscar", 15)))

  }

}
