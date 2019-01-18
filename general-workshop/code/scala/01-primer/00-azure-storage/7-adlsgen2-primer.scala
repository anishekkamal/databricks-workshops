// Databricks notebook source
// MAGIC %md
// MAGIC # ADLS gen 2 - primer
// MAGIC Azure Data Lake Storage Gen2 combines the capabilities of two existing storage services: Azure Data Lake Storage Gen1 features, such as file system semantics, file-level security and scale are combined with low-cost, tiered storage, high availability/disaster recovery capabilities, and a large SDK/tooling ecosystem from Azure Blob Storage.<br><br>
// MAGIC 
// MAGIC ### What's in this exercise?
// MAGIC We will complete the following in batch operations on DBFS-Hierarchical Name Space enabled ADLS Gen2:<br>
// MAGIC 1.  Create a dataset and persist to ADLS Gen2, create external table and run queries<br>
// MAGIC Ensure you on DBR 5.1 or above, and you should have created the root file system in the mount section.<br>
// MAGIC 
// MAGIC References:<br>
// MAGIC ADLS Gen2 product page:https://docs.microsoft.com/en-us/azure/storage/data-lake-storage/using-databricks-spark<br>
// MAGIC Databricks ADLS Gen2 integration: https://docs.azuredatabricks.net/spark/latest/data-sources/azure/azure-datalake-gen2.html

// COMMAND ----------

// MAGIC %md
// MAGIC ### 1. Define configuration needed for Delta - ADLSGen2

// COMMAND ----------

//This section is needed for the purpose of Delta table definition only as mount point is not supported
// Credentials
val clientID = dbutils.secrets.get(scope = "gws-adlsgen2-storage", key = "client-id")
val clientSecret = dbutils.secrets.get(scope = "gws-adlsgen2-storage", key = "client-secret")
val tenantID = "https://login.microsoftonline.com/" + dbutils.secrets.get(scope = "gws-adlsgen2-storage", key = "tenant-id") + "/oauth2/token"

// Add ADLSGen2 cred to Spark conf
spark.conf.set("dfs.adls.oauth2.access.token.provider.type", "ClientCredential")
spark.conf.set("dfs.adls.oauth2.client.id", clientID)
spark.conf.set("dfs.adls.oauth2.credential", clientSecret)
spark.conf.set("dfs.adls.oauth2.refresh.url", tenantID)

// COMMAND ----------

// MAGIC %md
// MAGIC ### 2.0. Create dataset

// COMMAND ----------

val booksDF = Seq(
   ("b00001", "Arthur Conan Doyle", "A study in scarlet", 1887),
   ("b00023", "Arthur Conan Doyle", "A sign of four", 1890),
   ("b01001", "Arthur Conan Doyle", "The adventures of Sherlock Holmes", 1892),
   ("b00501", "Arthur Conan Doyle", "The memoirs of Sherlock Holmes", 1893),
   ("b00300", "Arthur Conan Doyle", "The hounds of Baskerville", 1901)
).toDF("book_id", "book_author", "book_name", "book_pub_year")

booksDF.printSchema
booksDF.show

// COMMAND ----------

// MAGIC %md
// MAGIC ### 3.0a. Persist as Parquet to ADLSGen2, create external table, run queries on the dataset

// COMMAND ----------

val destDirectoryRoot = "/mnt/workshop-adlsgen2/gwsroot/books-prq/"
dbutils.fs.rm(destDirectoryRoot, recurse=true)

// COMMAND ----------

//Persist dataframe to delta format after coalescing
booksDF.coalesce(1).write.save(destDirectoryRoot)

// COMMAND ----------

//List
display(dbutils.fs.ls(destDirectoryRoot))

// COMMAND ----------

// MAGIC %sql
// MAGIC CREATE DATABASE IF NOT EXISTS books_db_adlsgen2;
// MAGIC USE books_db_adlsgen2;
// MAGIC 
// MAGIC DROP TABLE IF EXISTS books_prq;
// MAGIC CREATE TABLE books_prq
// MAGIC USING parquet
// MAGIC LOCATION "/mnt/workshop-adlsgen2/gwsroot/books-prq/";

// COMMAND ----------

// MAGIC %sql
// MAGIC select * from books_db_adlsgen2.books_prq;

// COMMAND ----------

// MAGIC %md
// MAGIC ### 3.0b. Persist as Delta to ADLSGen2, create external table, run queries on the dataset

// COMMAND ----------

val deltaTableDirectory = "/mnt/workshop-adlsgen2/gwsroot/books-delta/"
dbutils.fs.rm(deltaTableDirectory, recurse=true)

// COMMAND ----------

// MAGIC %sql
// MAGIC CREATE DATABASE IF NOT EXISTS books_db_adlsgen2;
// MAGIC USE books_db_adlsgen2;
// MAGIC 
// MAGIC DROP TABLE IF EXISTS books_delta;

// COMMAND ----------

//Persist dataframe to delta format after coalescing using mountpoint
booksDF.coalesce(1).write.format("delta").save("/mnt/workshop-adlsgen2/gwsroot/books-delta/")

// COMMAND ----------

//Persist dataframe to delta format after coalescing without mountpoint
booksDF.coalesce(1).write.format("delta").save("abfss://gwsroot@gwsadlsgen2sa.dfs.core.windows.net/books-delta/")

// COMMAND ----------

//List
display(dbutils.fs.ls(deltaTableDirectory))

// COMMAND ----------

// MAGIC %sql
// MAGIC USE books_db_adlsgen2;
// MAGIC CREATE TABLE books_delta
// MAGIC USING DELTA
// MAGIC LOCATION "/mnt/workshop-adlsgen2/gwsroot/books-delta/";

// COMMAND ----------

// MAGIC %sql
// MAGIC USE books_db_adlsgen2;
// MAGIC CREATE TABLE books_delta
// MAGIC USING DELTA
// MAGIC LOCATION "abfss://gwsroot@gwsadlsgen2sa.dfs.core.windows.net/books-delta/";

// COMMAND ----------

// MAGIC %sql
// MAGIC select * from books_db_adlsgen2.books_delta;