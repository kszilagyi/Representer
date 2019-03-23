package com.kristofszilagyi.representer
import com.kristofszilagyi.representer.Representer.{FeaturesWithResults, Input}

import scala.collection.immutable
import scala.util.Random

object Multiplication extends TestCase {
  override def generateData(random: Random, size: Int): immutable.IndexedSeq[Representer.FeaturesWithResults] = {
    (1 to size).map { _ =>
      val a = random.nextDouble() * 10
      val b = random.nextDouble() * 10
      val input = Input(a, b)
      FeaturesWithResults(input, a * b > 50)
    }
  }

  override def name: TestCaseName = TestCaseName("Multiplication")
}
