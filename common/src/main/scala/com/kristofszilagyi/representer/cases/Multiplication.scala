package com.kristofszilagyi.representer.cases

import com.kristofszilagyi.representer.Representer.{FeaturesWithResults, Input2}
import com.kristofszilagyi.representer.TypeSafeEqualsOps._
import com.kristofszilagyi.representer.Warts.Var
import com.kristofszilagyi.representer.{BiasParams, TestCase}

import scala.collection.immutable
import scala.util.Random

object Multiplication extends TestCase {
  def generateData(random: Random, size: Int, biasParams: BiasParams): immutable.IndexedSeq[FeaturesWithResults] = {
    val unbiasedPercentage = 1 - biasParams.ratio
    val cutTarget = 50
    val unbiased = (1 to Math.round(size.toDouble * unbiasedPercentage).toInt).map { _ =>
      val a = random.nextDouble() * 10
      val b = random.nextDouble() * 10
      val input = Input2(a, b)
      FeaturesWithResults(input, a * b > cutTarget)
    }
    @SuppressWarnings(Array(Var))
    var biased = List.empty[FeaturesWithResults]
    while (biased.size + unbiased.size < size) {
      val a = random.nextDouble() * 10
      val b = random.nextDouble() * 10
      val input = Input2(a, b)
      val result = a * b
      val distanceFromTarget = math.abs(result - cutTarget)
      if (distanceFromTarget < biasParams.radius) {
        biased = FeaturesWithResults(input, result > cutTarget) +: biased
      }
    }

    val all = unbiased ++ biased
    assert(all.size ==== size)
    all
  }

}
