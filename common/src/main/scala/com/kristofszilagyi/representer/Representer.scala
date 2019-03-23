package com.kristofszilagyi.representer

import com.kristofszilagyi.representer.Common.{autoScale, hiddenLayerSizes}
import com.kristofszilagyi.representer.tables._
import org.log4s.getLogger
import slick.jdbc.PostgresProfile.api._
import smile.classification.NeuralNetwork
import smile.classification.NeuralNetwork.{ActivationFunction, ErrorFunction}
import smile.{classification, validation}
import TypeSafeEqualsOps._
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContextExecutor}

object Representer {
  private val logger = getLogger

  trait Features {
    def doubles: Array[Double]
  }
  final case class FeaturesWithResults(features: Features, result: Boolean)


  final case class Metrics(tp: Int, fp: Int, tn: Int, fn: Int) {
    def f1: Double = {
      val precision = tp.toDouble / (tp.toDouble + fp.toDouble)
      val recall = tp.toDouble / (fn.toDouble + tp.toDouble)
      2 * (precision * recall) / (precision + recall)
    }
  }

  private final case class AllMetrics(training: Metrics, test: Metrics) {
    def toResult(epoch: Epoch): OOResult = OOResult(
      tpTrain = training.tp, fpTrain = training.fp, tnTrain = training.tn, fnTrain = training.fn,
      tpTest = test.tp, fpTest = test.fp, tnTest = test.tn, fnTest = test.fn,
      epoch = epoch)
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
    val initialMetrics = measureMetrics(model, training)
    logger.debug(s"Current f1 is (epoch 0): $initialMetrics")

    f1s :+= initialMetrics.f1

    var epoch = 1 //above is 0
    var failedToImproveCount = 0
    while(failedToImproveCount < 100) {
      model.learn(training.x, training.y)
      val metrics = measureMetrics(model, training)
      f1s :+= metrics.f1
      logger.debug(s"Current f1 is (epoch $epoch): ${metrics.f1}")
      if (f1s.size > 6 && f1s(epoch - 6) >= f1s(epoch)) {
        learningRate *= 0.9995
        model.setLearningRate(learningRate)
        logger.debug(s"Reducing learning rate: $learningRate")
      }

      if(f1s.size > 20 && f1s(epoch - 20) >= f1s(epoch)) {
        failedToImproveCount += 1
        logger.debug(s"Failed to improve count now is $failedToImproveCount")
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

  def countFP(truth: Array[Int], prediction: Array[Int]): Int = {
    truth.zip(prediction).count{ case (t, p) => t ==== 0 && p ==== 1 }
  }

  def countFN(truth: Array[Int], prediction: Array[Int]): Int = {
    truth.zip(prediction).count{ case (t, p) => t ==== 1 && p ==== 0 }
  }

  def countTP(truth: Array[Int], prediction: Array[Int]): Int = {
    truth.zip(prediction).count{ case (t, p) => t ==== 1 && p ==== 1 }
  }

  def countTN(truth: Array[Int], prediction: Array[Int]): Int = {
    truth.zip(prediction).count{ case (t, p) => t ==== 0 && p ==== 0 }
  }

  private def measureMetrics(classifier: NeuralNetwork, scaledData: ScaledData): Metrics = {
    val prediction = classifier.predict(scaledData.x)
    val truth = scaledData.y
    val f1 = validation.f1(truth, prediction)
    val tp = countTP(truth, prediction)
    val fp = countFP(truth, prediction)
    val tn = countTN(truth, prediction)
    val fn = countFN(truth, prediction)
    val m = Metrics(tp = tp, fp = fp, tn = tn, fn = fn)
    assert(f1.isNaN || m.f1 ==== f1)
    m
  }
  private def measureMetrics(classifier: NeuralNetwork, scaledAll: ScaledAll): AllMetrics = {
    val training = measureMetrics(classifier, scaledAll.training)
    val test = measureMetrics(classifier, scaledAll.test)
    AllMetrics(training = training, test = test)
  }

  private def trainAndMeasureMetrics(training: Data, test: Data, hiddenLayerSize: Int): (NeuralNetwork, AllMetrics) = {
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
    implicit val ec: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global
    val cases: Traversable[TestCase] = Traversable(Multiplication)
    val sampleSize = 500

    cases.foreach { testCase =>
      val training = testCase.trainingData(sampleSize)
      val test = testCase.testData(sampleSize)
      hiddenLayerSizes.map { hiddenLayerSize =>
        val (nn, metrics) = trainAndMeasureMetrics(training, test, hiddenLayerSize = hiddenLayerSize)
        val run = OORun(testCase.name, nn, sampleSize = sampleSize, firstHiddenLayerSize = hiddenLayerSize, initialLearningRate = 10,
          metrics.toResult(Epoch(10)), 10.seconds,
          Some(OONaiveDecayStrategy(10)), Traversable(OOResult(10, 10, 10, 10, 10, 10, 10, 10, Epoch(10))))
        Await.result(run.write(db), 10.seconds)
      }

    }
  }
}
