package com.kristofszilagyi.representer

import com.kristofszilagyi.representer.BooleanOps.RichBoolean
import com.kristofszilagyi.representer.Multiplication.{FeaturesWithResults, Input}

import scala.collection.immutable
import scala.util.Random
//try generating biased data: a lot more from the hard
object DataGenerator {
  private def generateData(random: Random, size: Int, includeFilter: (Double, Double) => Boolean): immutable.IndexedSeq[FeaturesWithResults] = {
    (1 to size).map { _ =>
      var a = random.nextDouble() * 10
      var b = random.nextDouble() * 10
      while(!includeFilter(a, b)) {
        a = random.nextDouble() * 10
        b = random.nextDouble() * 10
      }
      val input = Input(a, b)
      FeaturesWithResults(input, a * b > 50)
    }
  }
  final case class Data(d: immutable.IndexedSeq[FeaturesWithResults]) {
    def unscaledX: Array[Array[Double]] = d.map(_.features.doubles.toArray).toArray
    def unscaledY: Array[Int] = d.map(_.result.toInt).toArray
  }

  def constrainedTrainingData(size: Int, includeFilter: (Double, Double) => Boolean): Data = {
    val random = new Random(1)
    Data(generateData(random, size, includeFilter))
  }

  def constrainedTestData(size: Int, includeFilter: (Double, Double) => Boolean): Data = {
    val random = new Random(2)
    Data(generateData(random, size, includeFilter))
  }

  def trainingData(size: Int): Data = {
    constrainedTrainingData(size, (_, _) => true)
  }

  def testData(size: Int): Data = {
    constrainedTestData(size, (_, _) => true)

  }
}
