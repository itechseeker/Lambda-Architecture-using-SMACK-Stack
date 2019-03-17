import com.datastax.spark.connector._
import com.datastax.spark.connector.cql.CassandraConnector
import org.apache.spark.sql.SaveMode
import org.apache.spark.sql.cassandra._

object TestCode {
  def main(args: Array[String]): Unit = {

    //Create a Spark session which connect to Cassandra
    val spark = org.apache.spark.sql.SparkSession
      .builder()
      .master("local[*]")
      .config("spark.cassandra.connection.host", "localhost")
      .appName("Spark Cassandra Connector Example")
      .getOrCreate()

    //Implicit methods available in Scala for converting common Scala objects into DataFrames
    import spark.implicits._

    //Get Spark Context from Spark session
    val sparkContext = spark.sparkContext

    //Set the Log file level
    sparkContext.setLogLevel("WARN")


    //Read Cassandra data using DataFrame
    val df = spark.read
      .format("org.apache.spark.sql.cassandra")
      .options(Map( "table" -> "emp", "keyspace" -> "testkeyspace"))
      .load()

    //Display all row of the emp table
    println("Details of all employees: ")
    df.show()


    //Read Cassandra data using DataFrame
    var df_highSal = spark.read.cassandraFormat("highsalary", "testkeyspace").load()

    //Display all high salary employees
    println("All high salary employees: ")
    df_highSal.show()

    var highSal=df.select("emp_name","emp_city").filter($"emp_sal"<50000)
    highSal.show()

    //Connect Spark to Cassandra and execute CQL statements from Spark applications
    val connector = CassandraConnector(sparkContext.getConf)

    //Delete the hashtag_batchview of the previous batch processing
    connector.withSessionDo(session => session.execute("TRUNCATE testkeyspace.highsalary"))


    highSal.write.format("org.apache.spark.sql.cassandra")
      .options(Map("keyspace"->"testkeyspace","table"->"highsalary"))
      .mode(SaveMode.Append)
      .save()

    df_highSal = spark.read.cassandraFormat("highsalary", "testkeyspace").load()

    //Display all high salary employees
    println("All high salary employees: ")
    df_highSal.show()
  }
}