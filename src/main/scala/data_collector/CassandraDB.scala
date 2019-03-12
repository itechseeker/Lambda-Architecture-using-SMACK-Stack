package data_collector

import com.datastax.driver.core.{Cluster, ResultSet, Row}


object CassandraDB {

  def runDB(): Unit = {
    //creating Cluster object
    val cluster = Cluster.builder.addContactPoint("127.0.0.1").build
    //Creating Session object
    var session = cluster.connect

    //Query to create lambda_architecture keyspace
    // Using 'replication_factor':1 if only run on local machine
    var query = "CREATE KEYSPACE IF NOT EXISTS lambda_architecture WITH replication = {'class':'SimpleStrategy', 'replication_factor':1};"
    session.execute(query)

    //Connect to the lambda_architecture keyspace
    session = cluster.connect("lambda_architecture")

    //Enable this to delete master_dataset table
    query = "DROP TABLE master_dataset;"
    session.execute(query)

    //Create master_dataset table
    query = "CREATE TABLE IF NOT EXISTS master_dataset(tweet_id bigint PRIMARY KEY, user_id bigint, user_name text, user_loc text, content text,hashtag text, created_date bigint);"
    session.execute(query)

    //Create batch_view for hashtag
    query = "CREATE TABLE IF NOT EXISTS hashtag_batchView(hashtag text PRIMARY KEY, count int);"
    session.execute(query)

    //Create realtime_view for hashtag
    query = "CREATE TABLE IF NOT EXISTS hashtag_realtimeView(hashtag text PRIMARY KEY, count int);"
    session.execute(query)

    //Stop the connection
    println("Keyspace and tables were created successfully.")
    cluster.close()

  }

}
