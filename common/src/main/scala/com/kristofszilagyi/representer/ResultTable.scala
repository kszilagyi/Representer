package com.kristofszilagyi.representer

import slick.jdbc.JdbcType
import slick.jdbc.PostgresProfile.api._
import slick.lifted.ProvenShape
object ResultId {
  implicit val jdbcType: JdbcType[ResultId] = MappedColumnType.base[ResultId, Int](_.i, ResultId.apply)
}
final case class ResultId(i: Int)

final case class Result(id: ResultId, truePositiveCount: Int, falsePositiveCount: Int, trueNegativeCount: Int,
                        falseNegativeCount: Int, epoch: Int)

object ResultTable {
  val resultQuery =  TableQuery[ResultTable]
}
final class ResultTable(tag: Tag) extends Table[Result](tag, "result") {
  def id: Rep[ResultId] = column[ResultId]("id")
  def truePositiveCount: Rep[Int] = column[Int]("truePositiveCount")
  def falsePositiveCount: Rep[Int] = column[Int]("falsePositiveCount")
  def trueNegativeCount: Rep[Int] = column[Int]("trueNegativeCount")
  def falseNegativeCount: Rep[Int] = column[Int]("falseNegativeCount")
  def epoch: Rep[Int] = column[Int]("epoch")
  def * : ProvenShape[Result] = (id, truePositiveCount, falsePositiveCount, trueNegativeCount,
    falseNegativeCount, epoch).shaped <> (Result.tupled.apply, Result.unapply)
}
