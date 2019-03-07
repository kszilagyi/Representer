package com.kristofszilagyi.representer

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

import com.kristofszilagyi.representer.BooleanOps.RichBoolean
import com.kristofszilagyi.representer.Warts.discard
import org.log4s.getLogger
import smile.classification.NeuralNetwork
import smile.classification.NeuralNetwork.{ActivationFunction, ErrorFunction}
import smile.feature.Scaler
import smile.{classification, validation}

import scala.collection.immutable
import scala.util.Random

object Multiplication {
  private val logger = getLogger

  trait Features {
    def doubles: Array[Double]
  }
  final case class FeaturesWithResults(features: Features, result: Boolean)

  private final case class ScaledData(x: Array[Array[Double]], y: Array[Int])
  private final case class ScaledAll(training: ScaledData, test: ScaledData)
  private final case class Metrics(trainingF1: Double, testF1: Double) {
    def csv: String = s"$trainingF1,$testF1"
  }

  private def scale(training: Traversable[FeaturesWithResults], test: Traversable[FeaturesWithResults]) = {
    val trainingX = training.map(_.features.doubles).toArray
    val trainingY = training.map(_.result.toInt).toArray
    val scaler = new Scaler(true)
    scaler.learn(trainingX)
    val testX = scaler.transform(test.map(_.features.doubles).toArray)
    val testY = test.map(_.result.toInt).toArray
    val scaledTrainingX = scaler.transform(trainingX)
    val scaledTrainingData = ScaledData(scaledTrainingX, trainingY)
    val scaledTestData = ScaledData(testX, testY)
    ScaledAll(scaledTrainingData, scaledTestData)
  }
  private def generateClassifier(training: ScaledData, hiddenLayerSize: Int): NeuralNetwork = {
    val numOfAttributes = training.x.head.length
    classification.mlp(training.x, training.y, Array(numOfAttributes, hiddenLayerSize, 1), ErrorFunction.CROSS_ENTROPY, ActivationFunction.LOGISTIC_SIGMOID)
  }
  private def measureMetrics(classifier: NeuralNetwork, scaledData: ScaledData): Double = {
    val prediction = classifier.predict(scaledData.x)
    val truth = scaledData.y
    validation.f1(truth, prediction)
  }
  private def measureMetrics(classifier: NeuralNetwork, scaledAll: ScaledAll): Metrics = {
    val training = measureMetrics(classifier, scaledAll.training)
    val test = measureMetrics(classifier, scaledAll.test)
    Metrics(trainingF1 = training, testF1 = test)
  }


  private def trainAndMeasureMetrics(training: Seq[FeaturesWithResults], test: Seq[FeaturesWithResults], hiddenLayerSize: Int): Metrics = {
    logger.info(s"Train $hiddenLayerSize")
    val scaledAll = scale(training, test)
    val classifier = generateClassifier(scaledAll.training, hiddenLayerSize = hiddenLayerSize)
    measureMetrics(classifier, scaledAll)
  }

  final case class Input(a: Double, b: Double) extends Features {
    override def doubles: Array[Double] = Array(a, b)
  }
  def generateData(random: Random, size: Int): immutable.IndexedSeq[FeaturesWithResults] = {
    (1 to size).map { _ =>
      val input = Input(random.nextDouble() * 10, random.nextDouble() * 10)
      FeaturesWithResults(input, input.a * input.b > 50)
    }
  }
  def main(args: Array[String]): Unit = {
    val random = new Random(0)
    val sampleSize = 100000
    val training = generateData(random, sampleSize)
    val test = generateData(random, sampleSize)
    val results = (0 to 6).map { hiddenLayerExponent =>
      val hiddenLayerSize = math.round(math.pow(2, hiddenLayerExponent.toDouble)).toInt
      val metrics = trainAndMeasureMetrics(training, test, hiddenLayerSize = hiddenLayerSize)
      hiddenLayerSize -> metrics
    }
    val resultString = results.map { case (hiddenLayerSize, metrics) =>
        s"$hiddenLayerSize,${metrics.csv}"
    }.mkString("\n")

    discard(Files.write(Paths.get("results.txt"), resultString.getBytes(StandardCharsets.UTF_8)))
  }
}
