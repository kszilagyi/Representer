package com.kristofszilagyi.representer.tables

import slick.jdbc.JdbcType
import slick.jdbc.PostgresProfile.api._
import slick.lifted.ProvenShape
object ResultId {
  implicit val jdbcType: JdbcType[ResultId] = MappedColumnType.base[ResultId, Int](_.i, ResultId.apply)
  def ignored = ResultId(-1)
}
final case class ResultId(i: Int)

final case class OOResult(truePositiveCount: Int, falsePositiveCount: Int, trueNegativeCount: Int,
                          falseNegativeCount: Int, epoch: Int) {
  // main reason for this is to check if the fields are the same
  def toRelational(id: ResultId): Result = {
    Result(id, truePositiveCount = truePositiveCount, falsePositiveCount = falsePositiveCount,
      trueNegativeCount = trueNegativeCount,
      falseNegativeCount = falseNegativeCount, epoch = epoch)
  }
}

final case class Result(id: ResultId, truePositiveCount: Int, falsePositiveCount: Int, trueNegativeCount: Int,
                        falseNegativeCount: Int, epoch: Int) {
  def toOO: OOResult = OOResult(truePositiveCount = truePositiveCount, falsePositiveCount = falsePositiveCount,
    trueNegativeCount = trueNegativeCount, falseNegativeCount = falseNegativeCount, epoch = epoch)
}

object ResultTable {
  val resultQuery =  TableQuery[ResultTable]
}
final class ResultTable(tag: Tag) extends Table[Result](tag, "result") {
  def id: Rep[ResultId] = column[ResultId]("id", O.PrimaryKey, O.AutoInc)
  def truePositiveCount: Rep[Int] = column[Int]("truePositiveCount")
  def falsePositiveCount: Rep[Int] = column[Int]("falsePositiveCount")
  def trueNegativeCount: Rep[Int] = column[Int]("trueNegativeCount")
  def falseNegativeCount: Rep[Int] = column[Int]("falseNegativeCount")
  def epoch: Rep[Int] = column[Int]("epoch")
  def * : ProvenShape[Result] = (id, truePositiveCount, falsePositiveCount, trueNegativeCount,
    falseNegativeCount, epoch).shaped <> (Result.tupled.apply, Result.unapply)
}
