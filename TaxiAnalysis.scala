import org.apache.spark.sql.SparkSession

object TaxiAnalysis {
  def main(args: Array[String]): Unit = {
    // 1. 创建 Spark 入口
    val spark = SparkSession.builder()
      .appName("TaxiDataStat")
      .master("local[*]") // 本地运行，课程设计足够
      .getOrCreate()
    import spark.implicits._

    // ======================
    // 1. 读取 HDFS 数据集
    // ======================
    val df = spark.read.parquet("hdfs://localhost:9000/taxi/raw_data/yellow_tripdata_2024-01.parquet")

    println("==================== 原始数据预览 ====================")
    df.show(10)

    // ======================
    // 2. 数据清洗（核心步骤）
    // ======================
    val cleanDF = df.filter(
      "passenger_count > 0" +
        " AND trip_distance > 0" +
        " AND fare_amount > 0" +
        " AND tpep_pickup_datetime IS NOT NULL"
    )

    println("==================== 清洗后数据 ====================")
    cleanDF.show(10)

    // 保存清洗后的数据到 HDFS
    cleanDF.write
      .mode("overwrite")
      .parquet("hdfs://localhost:9000/taxi/clean_data")

    // ======================
    // 3. 5 大统计分析
    // ======================

    // 1. 各服务商订单量 & 总收入
    val stat1 = cleanDF.groupBy("VendorID")
      .count().withColumnRenamed("count", "订单量")
      .agg(Map("fare_amount" -> "sum"))
      .withColumnRenamed("sum(fare_amount)", "总收入")

    // 2. 每日订单量统计
    val stat2 = cleanDF.selectExpr("to_date(tpep_pickup_datetime) as date")
      .groupBy("date").count().orderBy("date")

    // 3. 乘车人数分布
    val stat3 = cleanDF.groupBy("passenger_count")
      .count().orderBy("passenger_count")

    // 4. 乘车里程 TOP10
    val stat4 = cleanDF.orderBy($"trip_distance".desc)
      .select("PULocationID", "DOLocationID", "trip_distance")
      .limit(10)

    // 5. 车费区间统计
    val stat5 = cleanDF.select(
      when($"fare_amount" < 5, "0-5美元")
        .when($"fare_amount" < 10, "5-10美元")
        .when($"fare_amount" < 20, "10-20美元")
        .otherwise("20美元以上")
        .as("车费区间")
    ).groupBy("车费区间").count()

    // ======================
    // 打印结果
    // ======================
    println("\n==================== 统计1：各服务商订单量 & 收入 ====================")
    stat1.show()

    println("\n==================== 统计2：每日订单量 ====================")
    stat2.show(10)

    println("\n==================== 统计3：乘车人数分布 ====================")
    stat3.show()

    println("\n==================== 统计4：里程最长TOP10 ====================")
    stat4.show()

    println("\n==================== 统计5：车费区间分布 ====================")
    stat5.show()

    // ======================
    // 保存所有统计结果到 HDFS
    // ======================
    stat1.write.mode("overwrite").csv("hdfs://localhost:9000/taxi/result/vendor")
    stat2.write.mode("overwrite").csv("hdfs://localhost:9000/taxi/result/dayorder")
    stat3.write.mode("overwrite").csv("hdfs://localhost:9000/taxi/result/passenger")
    stat4.write.mode("overwrite").csv("hdfs://localhost:9000/taxi/result/distance")
    stat5.write.mode("overwrite").csv("hdfs://localhost:9000/taxi/result/fare")

    println("数据统计完成！结果已保存到 HDFS /taxi/result/")

    spark.stop()
  }
}
