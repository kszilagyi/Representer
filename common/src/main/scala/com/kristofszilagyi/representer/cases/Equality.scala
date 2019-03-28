package com.kristofszilagyi.representer.cases

import com.kristofszilagyi.representer.Representer.{FeaturesWithResults, Input}
import com.kristofszilagyi.representer.TypeSafeEqualsOps._
import com.kristofszilagyi.representer.Warts.Var
import com.kristofszilagyi.representer.{BiasParams, TestCase, TestCaseName}

import scala.collection.immutable
import scala.util.Random

object Equality extends TestCase {
  def generateData(random: Random, size: Int, biasParams: BiasParams): immutable.IndexedSeq[FeaturesWithResults] = {
    val unbiasedPercentage = 1 - biasParams.ratio
    val epsilon = 1
    val unbiased = (1 to Math.round(size.toDouble * unbiasedPercentage).toInt).map { _ =>
      val a = random.nextDouble() * 10
      val b = random.nextDouble() * 10
      val input = Input(a, b)
      FeaturesWithResults(input, math.abs(a - b) < epsilon)
    }
    @SuppressWarnings(Array(Var))
    var biased = List.empty[FeaturesWithResults]
    while (biased.size + unbiased.size < size) {
      val a = random.nextDouble() * 10
      val b = random.nextDouble() * 10
      val input = Input(a, b)
      val result = math.abs(a - b)
      if (result < biasParams.radius) {
        biased = FeaturesWithResults(input, result < epsilon) +: biased
      }
    }

    val all = unbiased ++ biased
    assert(all.size ==== size)
    all
  }

  override def name: TestCaseName = TestCaseName("Equality")
}
