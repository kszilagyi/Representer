package com.kristofszilagyi.representer.cases

import com.kristofszilagyi.representer.Representer.{FeaturesWithResults, InputN}
import com.kristofszilagyi.representer.TypeSafeEqualsOps._
import com.kristofszilagyi.representer.cases.Csv.{readCsv, toData, toY}
import com.kristofszilagyi.representer.{BiasParams, Data, TestCase}

import scala.collection.immutable
import scala.language.reflectiveCalls
import scala.util.Random


object Csv {

  private def readCsv(path: String) = {
    using(io.Source.fromFile(path)) { source =>
      source.getLines.map { line =>
        line.split(",").map(_.trim).map(_.toDouble)
      }.toArray
    }
  }

  def using[A <: { def close(): Unit }, B](resource: A)(f: A => B): B = {
    try {
      f(resource)
    } finally {
      resource.close()
    }
  }

  private def toY(csv: Array[Array[Double]]) = {
    csv.map { line =>
      assert(line.length ==== 1)
      if (line(0) ==== 1.0) true
      else if(line(0) ==== 0.0) false
      else throw new AssertionError(s"$line")
    }
  }
  private def toData(xs: Array[Array[Double]], ys: Array[Boolean]) = {
    val featuresWithResults = xs.zip(ys).map { case (x, y) =>
      FeaturesWithResults(InputN(x), y)
    }
    Data(featuresWithResults.toIndexedSeq)
  }
}
final class Csv extends TestCase {

  private val trainingX = readCsv("training-features.csv")
  private val trainingY = toY(readCsv("training-labels.csv"))
  private val testX = readCsv("test-features.csv")
  private val testY = toY(readCsv("test-labels.csv"))
  assert(trainingX.length ==== trainingY.length)
  assert(testX.length ==== testY.length)
  assert(trainingX(0).length ==== testX(0).length)

  def generateData(random: Random, size: Int, biasParams: BiasParams): immutable.IndexedSeq[FeaturesWithResults] = {
    throw new NotImplementedError("dont use this")
  }

  override def trainingData(size: Int, biased: BiasParams): Data = {
    assert(size ==== trainingX.length)
    assert(biased.ratio ==== 0.0)
    toData(trainingX, trainingY)
  }

  override def testData(size: Int): Data = {
    assert(size ==== testX.length)
    toData(testX, testY)
  }

  override def trainingSampleSize: Int = trainingX.length
  override def testSampleSize: Int = testX.length

  override def biasParamPairs: Seq[BiasParams] = List(
    BiasParams(ratio = 0.0, radius = 1000000),
  )

}
