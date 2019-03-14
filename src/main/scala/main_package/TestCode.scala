package main_package

import java.io.File

import com.typesafe.config.ConfigFactory
import data_collector.{CassandraDB, KafkaRunner}
import org.apache.spark.sql.SparkSession
import twitter4j.FilterQuery

import collection.JavaConverters._
import scala.collection.JavaConversions._

object TestCode {
  def main(args: Array[String]): Unit = {

   KafkaRunner.start()
  //CassandraDB.runDB()
  }

}
