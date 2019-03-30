package com.kristofszilagyi.representer

import java.nio.file.{Path, Paths}

import smile.feature.Scaler

import scala.collection.immutable
final class ScaledData(val x: Array[Array[Double]], val y: Array[Int])
final case class ScaledAll(training: ScaledData, test: ScaledData)
final case class BiasParams(ratio: Double, radius: Double)
object Common {
  def modelPath(hiddenLayerSize: Int): Path = {
    Paths.get(s"$hiddenLayerSize.xml")
  }

  def hiddenLayerSizes: immutable.IndexedSeq[Int] = {
    (0 to 6).map { hiddenLayerExponent =>
      math.round(math.pow(2, hiddenLayerExponent.toDouble)).toInt
    }.reverse
  }

  def initialLearningRates: immutable.Seq[Double] = List(0.01, 0.1, 1)

  def teachScaler(training: Data): Scaler = {
    val trainingX = training.unscaledX
    val scaler = new Scaler(true)
    scaler.learn(trainingX)
    scaler
  }

  def scale(training: Data, test: Data, scaler: Scaler): ScaledAll = {
    val trainingX = training.unscaledX
    val trainingY = training.unscaledY
    val testX = scaler.transform(test.unscaledX)
    val testY = test.unscaledY
    val scaledTrainingX = scaler.transform(trainingX)
    val scaledTrainingData = new ScaledData(scaledTrainingX, trainingY)
    val scaledTestData = new ScaledData(testX, testY)
    ScaledAll(scaledTrainingData, scaledTestData)
  }

  def autoScale(training: Data, test: Data): ScaledAll = {
    val scaler = teachScaler(training)
    scale(training, test, scaler)
  }
}
