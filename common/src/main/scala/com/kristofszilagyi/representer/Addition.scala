package com.kristofszilagyi.representer

import com.kristofszilagyi.representer.Representer.{FeaturesWithResults, Input}
import com.kristofszilagyi.representer.TypeSafeEqualsOps._
import com.kristofszilagyi.representer.Warts.Var

import scala.collection.immutable
import scala.util.Random
//todo test bias with plotting

object Addition extends TestCase {
  def generateData(random: Random, size: Int, biasParams: BiasParams): immutable.IndexedSeq[FeaturesWithResults] = {
    val unbiasedPercentage = 1 - biasParams.ratio
    val cutTarget = 10
    val unbiased = (1 to Math.round(size.toDouble * unbiasedPercentage).toInt).map { _ =>
      val a = random.nextDouble() * 10
      val b = random.nextDouble() * 10
      val input = Input(a, b)
      FeaturesWithResults(input, a + b > cutTarget)
    }
    @SuppressWarnings(Array(Var))
    var biased = List.empty[FeaturesWithResults]
    while (biased.size + unbiased.size < size) {
      val a = random.nextDouble() * 10
      val b = random.nextDouble() * 10
      val input = Input(a, b)
      val result = a + b
      val distanceFromTarget = math.abs(result - cutTarget)
      if (distanceFromTarget < biasParams.radius) {
        biased = FeaturesWithResults(input, result > cutTarget) +: biased
      }
    }

    val all = unbiased ++ biased
    assert(all.size ==== size)
    all
  }

  override def name: TestCaseName = TestCaseName("Addition")
}
