package data_collector

import com.datastax.driver.core.Cluster


object CassandraDB {

  def main(args: Array[String]): Unit = {
    //creating Cluster object
    val cluster = Cluster.builder.addContactPoint("127.0.0.1").build
    //Creating Session object
    var session = cluster.connect

    //Query to create lambda_architecture keyspace
    var query = "CREATE KEYSPACE IF NOT EXISTS lambda_architecture WITH replication = {'class':'SimpleStrategy', 'replication_factor':3};"
    session.execute(query)

    //Connect to the lambda_architecture keyspace
    session = cluster.connect("lambda_architecture")

    //Create master_dataset table
    query = "CREATE TABLE master_dataset(tweet_id bigint PRIMARY KEY, created_date bigint,content text);"
    session.execute(query)

  }

}
