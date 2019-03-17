package com.kristofszilagyi.representer

import java.util.concurrent.TimeUnit

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
final case class Run(id: Int, model: Classifier[Array[Double]], firstHiddenLayerSize: Int, initialLearningRate: Double,
                     epochsTaken: Int, timeTaken: FiniteDuration, naiveDecayStrategy: Option[NaiveDecayStrategyId])
final class RunsTable(tag: Tag) extends Table[Run](tag, "runs") {
  def id: Rep[Int] = column[Int]("id")
  def model: Rep[Classifier[Array[Double]]] = column[Classifier[Array[Double]]]("model")
  def firstHiddenLayerSize: Rep[Int] = column[Int]("firstHiddenLayerSize")
  def initialLearningRate: Rep[Double] = column[Double]("initialLearningRate")
  def epochsTaken: Rep[Int] = column[Int]("epochsTaken")
  def timeTaken: Rep[FiniteDuration] = column[FiniteDuration]("timeTaken")
  def naiveDecayStrategy: Rep[Option[NaiveDecayStrategyId]] = column[Option[NaiveDecayStrategyId]]("naiveDecayStrategy")
  def * : ProvenShape[Run] = (id, model, firstHiddenLayerSize, initialLearningRate, epochsTaken,
    timeTaken, naiveDecayStrategy).shaped <> (Run.tupled.apply, Run.unapply)
}
