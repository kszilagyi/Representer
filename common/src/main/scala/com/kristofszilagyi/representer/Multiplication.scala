package com.kristofszilagyi.representer

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

import com.kristofszilagyi.representer.Common.{hiddenLayerSizes, modelPath, scale}
import com.kristofszilagyi.representer.DataGenerator.{Data, testData, trainingData}
import com.kristofszilagyi.representer.Warts.discard
import org.log4s.getLogger
import smile.classification.NeuralNetwork
import smile.classification.NeuralNetwork.{ActivationFunction, ErrorFunction}
import smile.{classification, validation, write}

object Multiplication {
  private val logger = getLogger

  trait Features {
    def doubles: Array[Double]
  }
  final case class FeaturesWithResults(features: Features, result: Boolean)

  private final case class Metrics(trainingF1: Double, testF1: Double) {
    def csv: String = s"$trainingF1,$testF1"
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


  private def trainAndMeasureMetrics(training: Data, test: Data, hiddenLayerSize: Int): (NeuralNetwork, Metrics) = {
    logger.info(s"Train $hiddenLayerSize")
    val scaledAll = scale(training, test)
    val classifier = generateClassifier(scaledAll.training, hiddenLayerSize = hiddenLayerSize)
    (classifier, measureMetrics(classifier, scaledAll))
  }

  final case class Input(a: Double, b: Double) extends Features {
    override def doubles: Array[Double] = Array(a, b)
  }

  def main(args: Array[String]): Unit = {
    val sampleSize = 100000
    val training = trainingData(sampleSize)
    val test = testData(sampleSize)
    val results = hiddenLayerSizes.map { hiddenLayerSize =>
      val (nn, metrics) = trainAndMeasureMetrics(training, test, hiddenLayerSize = hiddenLayerSize)
      hiddenLayerSize -> ((nn, metrics))
    }
    val resultString = results.map { case (hiddenLayerSize, (_, metrics)) =>
        s"$hiddenLayerSize,${metrics.csv}"
    }.mkString("\n")

    discard(Files.write(Paths.get("results.txt"), resultString.getBytes(StandardCharsets.UTF_8)))

    results.foreach { case (hiddenLayerSize, (nn, _)) =>
      write.xstream(nn, modelPath(hiddenLayerSize).toFile.toString)
    }
  }
}
