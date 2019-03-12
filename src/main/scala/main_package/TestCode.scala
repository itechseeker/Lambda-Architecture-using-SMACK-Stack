package main_package

import data_collector.KafkaRunner
import org.apache.spark.sql.SparkSession

object TestCode {
  def main(args: Array[String]): Unit = {

    KafkaRunner.start()

  }

}
