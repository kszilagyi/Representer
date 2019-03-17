package com.kristofszilagyi.representer

import java.util.concurrent.TimeUnit

import com.kristofszilagyi.representer.NaiveDecayStrategyTable.naiveDecayStrategyQuery
import com.kristofszilagyi.representer.ResultTable.resultQuery
import com.kristofszilagyi.representer.implicits._
import com.thoughtworks.xstream.XStream
import slick.jdbc.JdbcType
import slick.jdbc.PostgresProfile.api._
import slick.lifted.ProvenShape

import scala.concurrent.duration.DurationConversions.Classifier
import scala.concurrent.duration.FiniteDuration

object implicits {
  @SuppressWarnings(Array(Warts.AsInstanceOf))
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


final case class OORun(model: Classifier[Array[Double]], firstHiddenLayerSize: Int, initialLearningRate: Double,
                       epochsTaken: Int, finalResult: OOResult, timeTaken: FiniteDuration, naiveDecayStrategy: Option[OONaiveDecayStrategy],
                       private val intermediateResultsUnordered: Traversable[OOResult]) {
  def intermediateResults: Seq[OOResult] = intermediateResultsUnordered.toSeq.sortBy(_.epoch)
}

object RunId {
  implicit val jdbcType: JdbcType[RunId] = MappedColumnType.base[RunId, Int](_.i, RunId.apply)
}
final case class RunId(i: Int)
final case class Run(id: RunId, model: Classifier[Array[Double]], firstHiddenLayerSize: Int, initialLearningRate: Double,
                     epochsTaken: Int, finalResultId: ResultId, timeTaken: FiniteDuration, naiveDecayStrategy: Option[NaiveDecayStrategyId])

object RunsTable {
  def runsQuery = TableQuery[RunsTable]
}

final class RunsTable(tag: Tag) extends Table[Run](tag, "runs") {
  def id: Rep[RunId] = column[RunId]("id")
  def model: Rep[Classifier[Array[Double]]] = column[Classifier[Array[Double]]]("model")
  def firstHiddenLayerSize: Rep[Int] = column[Int]("firstHiddenLayerSize")
  def initialLearningRate: Rep[Double] = column[Double]("initialLearningRate")
  def epochsTaken: Rep[Int] = column[Int]("epochsTaken")
  def finalResultId: Rep[ResultId] = column[ResultId]("finalResultId")
  def finalResult = foreignKey("finalResultFK", finalResultId, resultQuery)(_.id)
  def timeTaken: Rep[FiniteDuration] = column[FiniteDuration]("timeTaken")
  def naiveDecayStrategyId: Rep[Option[NaiveDecayStrategyId]] = column[Option[NaiveDecayStrategyId]]("naiveDecayStrategy")
  def naiveDecayStrategy = foreignKey("naiveDecayStrategyFK", naiveDecayStrategyId,
    naiveDecayStrategyQuery)(_.id.?)

  def * : ProvenShape[Run] = (id, model, firstHiddenLayerSize, initialLearningRate, epochsTaken, finalResultId,
    timeTaken, naiveDecayStrategyId).shaped <> (Run.tupled.apply, Run.unapply)
}
