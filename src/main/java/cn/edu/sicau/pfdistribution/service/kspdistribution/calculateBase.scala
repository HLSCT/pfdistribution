package cn.edu.sicau.pfdistribution.service.kspdistribution


import cn.edu.sicau.pfdistribution.service.kspcalculation.{KSPUtil, ReadExcel}
import org.apache.spark.{SparkConf, SparkContext}
import org.springframework.beans.factory.annotation.Autowired

import scala.collection.JavaConverters._
import scala.collection.mutable
import util.control.Breaks._
import scala.collection.mutable.Map


abstract class calculateBase extends calculateInterface{

  @Autowired
  val kspUtil:KSPUtil
  val readExcel:ReadExcel

  override def odDistributionResult(targetOd: String):mutable.Map[Array[String],Double] = {
    val aList = targetOd.split(" ")
    val sou = aList(0)
    val tar = aList(1)
//    val readExcel = new ReadExcel()
    val graph = readExcel.buildGrapgh("data/stationLine.xls", "data/edge.xls")
//    val kspUtil = new KSPUtil()
    kspUtil.setGraph(graph)
    val ksp = kspUtil.computeODPath(sou,tar,2)
    val iter = ksp.iterator()
    val passenger = 1000 //OD对的总人数，暂为所有OD设置为1000
    var text:mutable.Map[Iterator[String],Double] = mutable.Map()
    var text1:mutable.Map[Array[String],Double] = mutable.Map()
    while(iter.hasNext) {
      val p = iter.next()
      //      一条路径的站点构成
      val nodesIter = p.getNodes.iterator()
      //      println("费用:"  + p.getTotalCost)
      text += (nodesIter.asScala -> p.getTotalCost)   //静态费用
      //      println(text.toList)
    }
    for (key <- text.keys) {
      val myArray = key.toArray
      text1 += (myArray -> text.apply(key))
    }
    return distribution(text1, passenger)
  }

  override def distribution(map: mutable.Map[Array[String], Double], x: Int):mutable.Map[Array[String],Double] = {
    val e = Math.E
    val Q = -3.2
    var p = 0.0
    var fenMu = 0.0
    val probability_Passenger = new Array[Double](10)
    val costMin = map.values.min
    val kspMap = scala.collection.mutable.Map[Array[String], Double]()
    for (value <- map.values) {
      //分配概率
      fenMu = fenMu + Math.pow(e, (Q * value / costMin))
    }
    var count = 0
    for (value <- map.values) {
      p = Math.pow(e, (Q * value / costMin)) / fenMu
      val kspPassenger = x.asInstanceOf[Double] * p //计算人数
      probability_Passenger(count) = kspPassenger
      count = count + 1
    }
    val keys = map.keySet
    var count1 = 0
    for (key <- keys) {
      kspMap += (key -> probability_Passenger(count1))
      count1 = count1 + 1
    }
    return kspMap
  }

  override def odRegion(map: mutable.Map[Array[String], Double]):mutable.Map[String, Double] = {
    val odMap = scala.collection.mutable.Map[String, Double]()
    for (key <- map.keys) {
      for (i <- 0 to (key.length - 2)) {
        val str = key(i) + " " + key(i + 1)
        if (odMap.contains(str)) {
          odMap += (str -> (map(key) + odMap(str)))
        }
        else {
          odMap += (str -> map(key))
        }
      }
    }
    return odMap
  }

  override def odRegionWithTime(map: mutable.Map[Array[String], Double]):mutable.Map[String, Double] = {
    val odMap = scala.collection.mutable.Map[String, Double]()
    val intervalTime = getTime()
    for (key <- map.keys) {
      var count = 0
      var i = 0
      //        for (i <- 0 to (key.length - 2)) {
      while (i < key.length - 2) {
        if (count <= intervalTime) {
          count += getTwoSiteTime(key(i), key(i + 1))
          val str = key(i) + " " + key(i + 1)
          if (odMap.contains(str)) {
            odMap += (str -> (map(key) + odMap(str)))
          }
          else {
            odMap += (str -> map(key))
          }
          i += 1
        } else
          break()
      }
    }
    return odMap
  }

  def getTime():Int={   //返回定义的区间粒度时间
    val int_Time: Int = 15
    return int_Time
  }
  def getTwoSiteTime(siteId1:String,siteId2:String):Int={  //返回两个站点的运行时间
    val twoSiteTime:Int =5
    return twoSiteTime
  }
  def getSiteStopTime(siteId:String):Int={
    val siteStopTime:Int =2
    return siteStopTime
  }
  override def intervalResult(odList: List[String]): mutable.Map[String, Double] = {
    val conf = new SparkConf().setAppName("IntervalDistribution").setMaster("local[4]")
    val sc = new SparkContext(conf)
    val rdd = sc.makeRDD(odList)
    //od对，起点与终点与用空格连接
    val rdd1 = rdd.map(String => odDistributionResult(String))
    val rdd2 = rdd1.reduce((x, y) => x ++ y)
    val regionMap = odRegion(rdd2)
    return regionMap
  }
}
