package com.kristofszilagyi.representer

import com.kristofszilagyi.representer.BooleanOps.RichBoolean
import Representer.FeaturesWithResults

import scala.collection.immutable
import scala.util.Random

final case class Data(d: immutable.IndexedSeq[FeaturesWithResults]) {
  def unscaledX: Array[Array[Double]] = d.map(_.features.doubles.toArray).toArray
  def unscaledY: Array[Int] = d.map(_.result.toInt).toArray
}

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

}
