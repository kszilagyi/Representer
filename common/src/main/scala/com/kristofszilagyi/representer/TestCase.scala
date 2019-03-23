package com.kristofszilagyi.representer

import com.kristofszilagyi.representer.BooleanOps.RichBoolean
import com.kristofszilagyi.representer.Representer.FeaturesWithResults
import slick.jdbc.JdbcType
import slick.jdbc.PostgresProfile.api._

import scala.collection.immutable
import scala.util.Random

final case class Data(d: immutable.IndexedSeq[FeaturesWithResults]) {
  def unscaledX: Array[Array[Double]] = d.map(_.features.doubles.toArray).toArray
  def unscaledY: Array[Int] = d.map(_.result.toInt).toArray
}

object TestCaseName {
  implicit val jdbcType: JdbcType[TestCaseName] = MappedColumnType.base[TestCaseName, String](_.s, TestCaseName.apply)
}
final case class TestCaseName(s: String)

trait TestCase {
  def trainingData(size: Int): Data = {
    val random = new Random(1)
    Data(generateData(random, size))
  }

  def testData(size: Int): Data = {
    val random = new Random(2)
    Data(generateData(random, size))
  }

  def generateData(random: Random, size: Int): immutable.IndexedSeq[FeaturesWithResults]

  def name: TestCaseName
}
