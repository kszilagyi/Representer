package com.kristofszilagyi.representer.cases

import com.kristofszilagyi.representer.Representer.{FeaturesWithResults, Input6}
import com.kristofszilagyi.representer.TypeSafeEqualsOps._
import com.kristofszilagyi.representer.{BiasParams, TestCase}

import scala.collection.immutable
import scala.util.Random

object Indentation extends TestCase {
  def generateData(random: Random, size: Int, biasParams: BiasParams): immutable.IndexedSeq[FeaturesWithResults] = {
    assert(biasParams.ratio ==== 0.0)
    (1 to size).map { _ =>
      val line1IndentLeft  = random.nextInt(5) * 2
      val line2IndentLeft  = random.nextInt(5) * 2
      val line3IndentLeft  = random.nextInt(5) * 2
      val line1IndentRight = random.nextInt(5) * 2
      val line2IndentRight = random.nextInt(5) * 2
      val line3IndentRight = random.nextInt(5) * 2

      val input = Input6(line1IndentLeft.toDouble, line2IndentLeft.toDouble, line3IndentLeft.toDouble, line1IndentRight.toDouble,
        line2IndentRight.toDouble, line3IndentRight.toDouble)
      val line1Distance = line1IndentLeft - line1IndentRight
      val line2Distance = line2IndentLeft - line2IndentRight
      val line3Distance = line3IndentLeft - line3IndentRight
      val epsilon = 0.001
      val output = math.abs(line1Distance - line2Distance) < epsilon && math.abs(line2Distance - line3Distance) < epsilon

      FeaturesWithResults(input, output)
    }
  }

  override def biasParamPairs: Seq[BiasParams] = List(
    BiasParams(ratio = 0.0, radius = 1000000),
  )

}
