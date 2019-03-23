package com.kristofszilagyi.representer.tables

import java.util.concurrent.TimeUnit

import com.kristofszilagyi.representer.TestCaseName
import com.kristofszilagyi.representer.tables.IntermediateResultsTable.intermediateResultsQuery
import com.kristofszilagyi.representer.tables.NaiveDecayStrategyTable.naiveDecayStrategyQuery
import com.kristofszilagyi.representer.tables.ResultTable.resultQuery
import com.kristofszilagyi.representer.tables.RunsTable.runsQuery
import com.kristofszilagyi.representer.Warts.{AsInstanceOf, discard}
import com.kristofszilagyi.representer.tables.implicits._
import com.thoughtworks.xstream.XStream
import slick.jdbc.JdbcType
import slick.jdbc.PostgresProfile.api._
import slick.lifted.ProvenShape

import scala.concurrent.{ExecutionContext, Future}
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

final case class OORun(testCaseName: TestCaseName, model: Classifier[Array[Double]], firstHiddenLayerSize: Int, initialLearningRate: Double,
                       epochsTaken: Int, finalResult: OOResult, timeTaken: FiniteDuration, naiveDecayStrategy: Option[OONaiveDecayStrategy],
                       private val intermediateResultsUnordered: Traversable[OOResult]) {
  def intermediateResults: Seq[OOResult] = intermediateResultsUnordered.toSeq.sortBy(_.epoch)

  def maybeNaiveDecayStrategyRow: Option[NaiveDecayStrategy] = naiveDecayStrategy.map(_.toRelational(NaiveDecayStrategyId.ignored))
  def finalResultRow: Result = finalResult.toRelational(ResultId.ignored)

  def toRelational(id: RunId, finalResultId: ResultId, naiveDecayStrategyId: Option[NaiveDecayStrategyId]): Run =
    Run(id = id, model = model, testCaseName, firstHiddenLayerSize = firstHiddenLayerSize,
      initialLearningRate = initialLearningRate,
      epochsTaken = epochsTaken, finalResultId = finalResultId,
      timeTaken = timeTaken, naiveDecayStrategy = naiveDecayStrategyId
    )
  def write(db: Database)(implicit ec: ExecutionContext): Future[Unit] = {
    val maybeNaiveDecayStrategyId = maybeNaiveDecayStrategyRow.map((naiveDecayStrategyQuery returning naiveDecayStrategyQuery.map(_.id)) += _)
    val finalResultId = (resultQuery returning resultQuery.map(_.id)) += finalResult.toRelational(ResultId.ignored)
    val runId = finalResultId.flatMap { resultId =>
      maybeNaiveDecayStrategyId match {
        case Some(naiveStrategyId) =>
          naiveStrategyId.flatMap { strategyId =>
            (runsQuery returning runsQuery.map(_.id)) += this.toRelational(RunId.ignored, resultId, Some(strategyId))
          }
        case None =>
          (runsQuery returning runsQuery.map(_.id)) += this.toRelational(RunId.ignored, resultId, None)
      }
    }
    val intermediateResultIds = resultQuery returning resultQuery.map(_.id) ++= intermediateResults.map(_.toRelational(ResultId.ignored))

    val intermediateResultRows = intermediateResultIds.flatMap{interResult =>
      runId.map { r =>
        interResult.map { i =>
          IntermediateResults(r, i)
        }
      }
    }
    val insertAll = intermediateResultRows.map(rows => intermediateResultsQuery ++= rows)
    db.run(insertAll.transactionally).map(discard)
  }
}

object RunId {
  implicit val jdbcType: JdbcType[RunId] = MappedColumnType.base[RunId, Int](_.i, RunId.apply)

  val ignored = RunId(-1)
}
final case class RunId(i: Int)
final case class Run(id: RunId, model: Classifier[Array[Double]], testCaseName: TestCaseName, firstHiddenLayerSize: Int, initialLearningRate: Double,
                     epochsTaken: Int, finalResultId: ResultId, timeTaken: FiniteDuration, naiveDecayStrategy: Option[NaiveDecayStrategyId])

object RunsTable {
  def runsQuery = TableQuery[RunsTable]
}

final class RunsTable(tag: Tag) extends Table[Run](tag, "runs") {
  def id: Rep[RunId] = column[RunId]("id", O.PrimaryKey, O.AutoInc)
  def model: Rep[Classifier[Array[Double]]] = column[Classifier[Array[Double]]]("model")
  def testCaseName: Rep[TestCaseName] = column[TestCaseName]("testCaseName")
  def firstHiddenLayerSize: Rep[Int] = column[Int]("firstHiddenLayerSize")
  def initialLearningRate: Rep[Double] = column[Double]("initialLearningRate")
  def epochsTaken: Rep[Int] = column[Int]("epochsTaken")
  def finalResultId: Rep[ResultId] = column[ResultId]("finalResult")
  def finalResult = foreignKey("finalResultFK", finalResultId, resultQuery)(_.id)
  def timeTaken: Rep[FiniteDuration] = column[FiniteDuration]("trainingTimeNs")
  def naiveDecayStrategyId: Rep[Option[NaiveDecayStrategyId]] = column[Option[NaiveDecayStrategyId]]("naiveDecayStrategy")
  def naiveDecayStrategy = foreignKey("naiveDecayStrategyFK", naiveDecayStrategyId,
    naiveDecayStrategyQuery)(_.id.?)

  def * : ProvenShape[Run] = (id, model, testCaseName, firstHiddenLayerSize, initialLearningRate, epochsTaken, finalResultId,
    timeTaken, naiveDecayStrategyId).shaped <> (Run.tupled.apply, Run.unapply)
}
