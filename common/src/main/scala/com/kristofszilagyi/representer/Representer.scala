package com.kristofszilagyi.representer

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import slick.jdbc.PostgresProfile.api._

import com.kristofszilagyi.representer.Common.{autoScale, hiddenLayerSizes, modelPath}
import com.kristofszilagyi.representer.Warts.discard
import org.log4s.getLogger
import smile.classification.NeuralNetwork
import smile.classification.NeuralNetwork.{ActivationFunction, ErrorFunction}
import smile.{classification, validation, write}

object Representer {
  private val logger = getLogger

  trait Features {
    def doubles: Array[Double]
  }
  final case class FeaturesWithResults(features: Features, result: Boolean)

  private final case class Metrics(trainingF1: Double, testF1: Double) {
    def csv: String = s"$trainingF1,$testF1"
  }

  sealed trait LearningRateStrategy
  final case object Constant extends LearningRateStrategy
  final case object Adaptive extends LearningRateStrategy


  @SuppressWarnings(Array(Warts.Var))
  private def adaptiveLearning(training: ScaledData, hiddenLayerSize: Int) = {
    val numOfAttributes = training.x.head.length
    var learningRate = 0.1
    val model = classification.mlp(training.x, training.y, Array(numOfAttributes, hiddenLayerSize, 1),
      ErrorFunction.CROSS_ENTROPY, ActivationFunction.LOGISTIC_SIGMOID, eta = learningRate, epochs = 1)
    var f1s = Vector.empty[Double]
    val initialF1 = measureMetrics(model, training)
    logger.info(s"Current f1 is (epoch 0): $initialF1")

    f1s :+= initialF1

    var epoch = 1 //above is 0
    var failedToImproveCount = 0
    while(failedToImproveCount < 100) {
      model.learn(training.x, training.y)
      val f1 = measureMetrics(model, training)
      f1s :+= f1
      logger.info(s"Current f1 is (epoch $epoch): $f1")
      if (f1s.size > 6 && f1s(epoch - 6) >= f1s(epoch)) {
        learningRate *= 0.9995
        model.setLearningRate(learningRate)
        logger.info(s"Reducing learning rate: $learningRate")
      }

      if(f1s.size > 20 && f1s(epoch - 20) >= f1s(epoch)) {
        failedToImproveCount += 1
        logger.info(s"Failed to improve count now is $failedToImproveCount")
      } else failedToImproveCount = 0

      epoch += 1
    }


    model
  }
  private def generateClassifier(training: ScaledData, hiddenLayerSize: Int, learningRateStrategy: LearningRateStrategy): NeuralNetwork = {
    val numOfAttributes = training.x.head.length
    learningRateStrategy match {
      case Constant =>
        classification.mlp(training.x, training.y, Array(numOfAttributes, hiddenLayerSize, 1), ErrorFunction.CROSS_ENTROPY, ActivationFunction.LOGISTIC_SIGMOID)
      case Adaptive =>
        adaptiveLearning(training, hiddenLayerSize)
    }
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
    val scaledAll = autoScale(training, test)
    val classifier = generateClassifier(scaledAll.training, hiddenLayerSize = hiddenLayerSize, Adaptive)
    (classifier, measureMetrics(classifier, scaledAll))
  }

  final case class Input(a: Double, b: Double) extends Features {
    override def doubles: Array[Double] = Array(a, b)
  }

  def main(args: Array[String]): Unit = {
    val db = Database.forConfig("representer")

    val cases: Traversable[TestCase] = Traversable(Multiplication)
    val sampleSize = 10000
    cases.foreach { testCase =>
      val training = testCase.trainingData(sampleSize)
      val test = testCase.testData(sampleSize)
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
}
