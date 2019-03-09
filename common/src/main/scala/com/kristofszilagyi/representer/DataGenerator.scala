package com.kristofszilagyi.representer

import com.kristofszilagyi.representer.BooleanOps.RichBoolean
import com.kristofszilagyi.representer.Multiplication.{FeaturesWithResults, Input}

import scala.collection.immutable
import scala.util.Random

object DataGenerator {
  private def generateData(random: Random, size: Int): immutable.IndexedSeq[FeaturesWithResults] = {
    (1 to size).map { _ =>
      val a = random.nextDouble() * 10
      val b = random.nextDouble() * 10
      val input = Input(a, b)
      FeaturesWithResults(input, a * b > 50)
    }
  }
  final case class Data(d: immutable.IndexedSeq[FeaturesWithResults]) {
    def unscaledX: Array[Array[Double]] = d.map(_.features.doubles.toArray).toArray
    def unscaledY: Array[Int] = d.map(_.result.toInt).toArray
  }

  def trainingData(size: Int): Data = {
    val random = new Random(1)
    Data(generateData(random, size))
  }

  def testData(size: Int): Data = {
    val random = new Random(2)
    Data(generateData(random, size))
  }
}
