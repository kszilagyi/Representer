package com.kristofszilagyi.representer.tables

import java.util.concurrent.TimeUnit

import com.kristofszilagyi.representer.Warts.AsInstanceOf
import com.kristofszilagyi.representer.tables.implicits._
import com.kristofszilagyi.representer.{LearningRateDecayStrategyName, TestCaseName}
import com.thoughtworks.xstream.XStream
import slick.jdbc.JdbcType
import slick.jdbc.PostgresProfile.api._
import slick.lifted.ProvenShape
import smile.classification.Classifier

import scala.concurrent.duration.FiniteDuration

object implicits {
  @SuppressWarnings(Array(AsInstanceOf))
  implicit val classifierColumnType: JdbcType[Classifier[Array[Double]]] = MappedColumnType.base[Classifier[Array[Double]], String](
    { classifier =>
      val xstream = new XStream
      xstream.toXML(classifier)
    },
    { xml =>
      val xstream = new XStream
      xstream.fromXML(xml).asInstanceOf[Classifier[Array[Double]]]
    }
  )
  implicit val finiteDurationColumnType: JdbcType[FiniteDuration] =
    MappedColumnType.base[FiniteDuration, Long](_.toNanos, FiniteDuration.apply(_, TimeUnit.NANOSECONDS)
  )
}
object Epoch {
  implicit val jdbcType: JdbcType[Epoch] = MappedColumnType.base[Epoch, Int](_.i, Epoch.apply)
}
final case class Epoch(i: Int)


object RunId {
  implicit val jdbcType: JdbcType[RunId] = MappedColumnType.base[RunId, Int](_.i, RunId.apply)

  val ignored = RunId(-1)
}
final case class RunId(i: Int)

final case class Run(id: RunId, testCaseName: TestCaseName, model: Classifier[Array[Double]], trainingSampleSize: Int,
                     testSampleSize: Int, firstHiddenLayerSize: Int,
                     initialLearningRate: Double, timeTaken: FiniteDuration, decayStrategy: LearningRateDecayStrategyName,
                     learningRateDecayRate: Double, maxEpoch: Int, trainingBiasRatio: Double, trainingBiasRadius: Double,
                     tpTrain: Int, fpTrain: Int, tnTrain: Int,
                     fnTrain: Int, tpTest: Int, fpTest: Int, tnTest: Int,
                     fnTest: Int, lastEpoch: Epoch)

object RunsTable {
  def runsQuery = TableQuery[RunsTable]
}

final class RunsTable(tag: Tag) extends Table[Run](tag, "runs") {
  def id: Rep[RunId] = column[RunId]("id", O.PrimaryKey, O.AutoInc)
  def model: Rep[Classifier[Array[Double]]] = column[Classifier[Array[Double]]]("model")
  def testCaseName: Rep[TestCaseName] = column[TestCaseName]("testCaseName")
  def trainingSampleSize: Rep[Int] = column[Int]("trainingSampleSize")
  def testSampleSize: Rep[Int] = column[Int]("testSampleSize")
  def firstHiddenLayerSize: Rep[Int] = column[Int]("firstHiddenLayerSize")
  def initialLearningRate: Rep[Double] = column[Double]("initialLearningRate")
  def timeTaken: Rep[FiniteDuration] = column[FiniteDuration]("trainingTimeNs")
  def decayStrategy: Rep[LearningRateDecayStrategyName] = column[LearningRateDecayStrategyName]("learningRateDecayStrategy")
  def learningRateDecayRate: Rep[Double] = column[Double]("learningRateDecayRate")
  def maxEpochs: Rep[Int] = column[Int]("maxEpochs")
  def trainingBiasRatio: Rep[Double] = column[Double]("trainingBiasRatio")
  def trainingBiasRadius: Rep[Double] = column[Double]("trainingBiasRadius")
  def tpTrain: Rep[Int] = column[Int]("tpTrain")
  def fpTrain: Rep[Int] = column[Int]("fpTrain")
  def tnTrain: Rep[Int] = column[Int]("tnTrain")
  def fnTrain: Rep[Int] = column[Int]("fnTrain")
  def tpTest: Rep[Int] = column[Int]("tpTest")
  def fpTest: Rep[Int] = column[Int]("fpTest")
  def tnTest: Rep[Int] = column[Int]("tnTest")
  def fnTest: Rep[Int] = column[Int]("fnTest")
  def lastEpoch: Rep[Epoch] = column[Epoch]("lastEpoch")

  def * : ProvenShape[Run] = (id, testCaseName, model, trainingSampleSize, testSampleSize, firstHiddenLayerSize, initialLearningRate, timeTaken,
    decayStrategy, learningRateDecayRate, maxEpochs, trainingBiasRatio, trainingBiasRadius, tpTrain, fpTrain, tnTrain, fnTrain,
    tpTest, fpTest, tnTest, fnTest,
    lastEpoch).shaped <> (Run.tupled.apply, Run.unapply)
}
