package com.kristofszilagyi.representer

import com.kristofszilagyi.representer.Common.{autoScale, biasParamPairs, hiddenLayerSizes, initialLearningRates}
import com.kristofszilagyi.representer.LearningRateDecayStrategy._
import com.kristofszilagyi.representer.TypeSafeEqualsOps._
import com.kristofszilagyi.representer.Warts.{IsInstanceOf, Var, discard}
import com.kristofszilagyi.representer.tables.RunsTable._
import com.kristofszilagyi.representer.tables._
import org.log4s.getLogger
import slick.jdbc.PostgresProfile.api._
import smile.classification.NeuralNetwork
import smile.classification.NeuralNetwork.{ActivationFunction, ErrorFunction}
import smile.{classification, validation}

import scala.concurrent.duration.{Duration, DurationInt}
import scala.concurrent.{Await, ExecutionContext, Future}

object Representer {
  private val logger = getLogger

  trait Features {
    def doubles: Array[Double]
  }
  final case class FeaturesWithResults(features: Features, result: Boolean)
  final case class ClassifierWithLastEpoch(classifier: NeuralNetwork, lastEpoch: Epoch)

  final case class Metrics(tp: Int, fp: Int, tn: Int, fn: Int) {
    def f1: Double = {
      val precision = tp.toDouble / (tp.toDouble + fp.toDouble)
      val recall = tp.toDouble / (fn.toDouble + tp.toDouble)
      2 * (precision * recall) / (precision + recall)
    }
  }

  private final case class AllMetrics(training: Metrics, test: Metrics)

  @SuppressWarnings(Array(Var))
  private def adaptiveLearning(training: ScaledData, hiddenLayerSize: Int, initialLearningRate: Double, decayRate: Double, maxEpochs: Int) = {
    val numOfAttributes = training.x.head.length
    var learningRate = initialLearningRate
    val model = classification.mlp(training.x, training.y, Array(numOfAttributes, hiddenLayerSize, 1),
      ErrorFunction.CROSS_ENTROPY, ActivationFunction.LOGISTIC_SIGMOID, eta = learningRate, epochs = 1)
    var f1s = Vector.empty[Double]
    val initialMetrics = measureMetrics(model, training)
    logger.debug(s"Current f1 is (epoch 0): $initialMetrics")

    f1s :+= initialMetrics.f1

    var epoch = 1 //above is 0
    var failedToImproveCount = 0
    while(failedToImproveCount < 100 && epoch < maxEpochs) {
      model.learn(training.x, training.y)
      val metrics = measureMetrics(model, training)
      f1s :+= metrics.f1
      logger.debug(s"Current f1 is (epoch $epoch): ${metrics.f1}")
      if (f1s.size > 6 && f1s(epoch - 6) >= f1s(epoch)) {
        learningRate *= decayRate
        model.setLearningRate(learningRate)
        logger.debug(s"Reducing learning rate: $learningRate")
      }

      if(f1s.size > 20 && f1s(epoch - 20) >= f1s(epoch)) {
        failedToImproveCount += 1
        logger.debug(s"Failed to improve count now is $failedToImproveCount")
      } else failedToImproveCount = 0

      epoch += 1
    }


    ClassifierWithLastEpoch(model, Epoch(epoch - 1))
  }
  private def generateClassifier(training: ScaledData, hiddenLayerSize: Int, learningRateStrategy: LearningRateDecayStrategy,
                                 initialLearningRate: Double, maxEpochs: Int): ClassifierWithLastEpoch = {
    val numOfAttributes = training.x.head.length
    learningRateStrategy match {
      case Constant =>
        ClassifierWithLastEpoch(classification.mlp(training.x, training.y, Array(numOfAttributes, hiddenLayerSize, 1), ErrorFunction.CROSS_ENTROPY,
          ActivationFunction.LOGISTIC_SIGMOID, eta = initialLearningRate, epochs = maxEpochs), Epoch(maxEpochs - 1))
      case NaiveDecay(decayRate) =>
        adaptiveLearning(training, hiddenLayerSize, initialLearningRate, decayRate, maxEpochs)
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

  private def trainAndMeasureMetrics(training: Data, test: Data, hiddenLayerSize: Int, learningRateStrategy: LearningRateDecayStrategy,
                                     initialLearningRate: Double, maxEpochs: Int) = {
    val scaledAll = autoScale(training, test)
    val start = System.nanoTime()
    val classifierWithLastEpoch = generateClassifier(scaledAll.training, hiddenLayerSize = hiddenLayerSize, learningRateStrategy, initialLearningRate, maxEpochs)
    val end = System.nanoTime()
    (classifierWithLastEpoch,
      measureMetrics(classifierWithLastEpoch.classifier, scaledAll),
      Duration.fromNanos(end - start)
    )
  }

  final case class Input(a: Double, b: Double) extends Features {
    override def doubles: Array[Double] = Array(a, b)
  }

  @SuppressWarnings(Array(IsInstanceOf))
  def main(args: Array[String]): Unit = {
    val db = Database.forConfig("representer")
    implicit val ec: ExecutionContext = new SyncEc() // this makes sense because the db has it's own thread pool which makes the whole thing parallel
    val cases: Traversable[TestCase] = Traversable(Multiplication)
    val sampleSizes = List(10, 1000)

    val futures = cases.flatMap { testCase =>
      biasParamPairs.flatMap { biasParam =>
        sampleSizes.flatMap{ sampleSize =>
          val training = testCase.trainingData(sampleSize, biasParam)
          val test = testCase.testData(sampleSize)
          hiddenLayerSizes.flatMap { hiddenLayerSize =>
            initialLearningRates.flatMap { initialLearningRate =>
              learningStrategies.flatMap { learningRateDecayStrategy =>
                maxEpochs.map { maxEpochs =>
                  val checkIfDone = db.run(runsQuery.filter { r =>
                    r.testCaseName === testCase.name &&
                      r.sampleSize === sampleSize &&
                      r.firstHiddenLayerSize === hiddenLayerSize &&
                      r.initialLearningRate === initialLearningRate &&
                      r.decayStrategy === learningRateDecayStrategy.name &&
                      r.learningRateDecayRate === learningRateDecayStrategy.decayRate &&
                      r.maxEpochs === maxEpochs &&
                      r.trainingBiasRatio === biasParam.ratio &&
                      r.trainingBiasRadius === biasParam.radius
                  }.result)
                  val computeAndWrite = checkIfDone.flatMap { matchingRuns =>
                    val paramsString = s"${testCase.name.s}: hiddenLayerSize=$hiddenLayerSize, sampleSize=$sampleSize," +
                      s" initialLearningRate=$initialLearningRate, learningRateStrategy=${learningRateDecayStrategy.name.s}," +
                      s" learningDecayRate:${learningRateDecayStrategy.decayRate}, maxEpochs: $maxEpochs, trainingBias: $biasParam"
                    if (matchingRuns.isEmpty) {
                      logger.info(s"Training $paramsString")
                      val (nnWithLastEpoch, metrics, timeTook) = trainAndMeasureMetrics(training, test,
                        hiddenLayerSize = hiddenLayerSize,
                        learningRateStrategy = learningRateDecayStrategy, initialLearningRate = initialLearningRate, maxEpochs = maxEpochs
                      )
                      val run = Run(id = RunId.ignored, testCaseName = testCase.name, model = nnWithLastEpoch.classifier,
                        sampleSize = sampleSize, firstHiddenLayerSize = hiddenLayerSize, initialLearningRate = initialLearningRate,
                        timeTaken = timeTook, decayStrategy = learningRateDecayStrategy.name,
                        learningRateDecayRate = learningRateDecayStrategy.decayRate, maxEpoch = maxEpochs,
                        trainingBiasRatio = biasParam.ratio, trainingBiasRadius = biasParam.radius,
                        tpTrain = metrics.training.tp,
                        fpTrain = metrics.training.fp,
                        tnTrain = metrics.training.tn,
                        fnTrain = metrics.training.fn,
                        tpTest = metrics.test.tp,
                        fpTest = metrics.test.fp,
                        tnTest = metrics.test.tn,
                        fnTest = metrics.test.fn,
                        lastEpoch = nnWithLastEpoch.lastEpoch
                      )
                      db.run(runsQuery += run).andThen { case _ => logger.info(s"Written $paramsString") }
                    } else if (matchingRuns.size ==== 1) {
                      logger.info(s"Skipping $paramsString")
                      Future.successful(())
                    } else {
                      Future.failed(new AssertionError(s"multiple matching runs: $matchingRuns"))
                    }
                  }
                  computeAndWrite
                }
              }
            }
          }
        }
      }
    }
    discard(Await.result(Future.sequence(futures), 356.days))
  }
}
