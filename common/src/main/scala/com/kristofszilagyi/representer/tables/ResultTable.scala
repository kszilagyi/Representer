package com.kristofszilagyi.representer.tables

import slick.jdbc.JdbcType
import slick.jdbc.PostgresProfile.api._
import slick.lifted.ProvenShape
object ResultId {
  implicit val jdbcType: JdbcType[ResultId] = MappedColumnType.base[ResultId, Int](_.i, ResultId.apply)
  def ignored = ResultId(-1)
}
final case class ResultId(i: Int)

object Epoch {
  implicit val ordering: Ordering[Epoch] = Ordering.by[Epoch, Int](_.i)
  implicit val jdbcType: JdbcType[Epoch] = MappedColumnType.base[Epoch, Int](_.i, Epoch.apply)
}

final case class Epoch(i: Int)

final case class OOResult(tpTrain: Int, fpTrain: Int, tnTrain: Int,
                          fnTrain: Int, tpTest: Int, fpTest: Int, tnTest: Int,
                          fnTest: Int, epoch: Epoch) {
  def toRelational(id: ResultId): Result = {
    Result(id, tpTrain = tpTrain, fpTrain = fpTrain, tnTrain = tnTrain,
      fnTrain = fnTrain, tpTest = tpTest, fpTest = fpTest, tnTest = tnTest,
      fnTest = fnTest, epoch = epoch)
  }
}

final case class Result(id: ResultId, tpTrain: Int, fpTrain: Int, tnTrain: Int,
                        fnTrain: Int, tpTest: Int, fpTest: Int, tnTest: Int,
                        fnTest: Int, epoch: Epoch) {
  def toOO: OOResult = OOResult(tpTrain = tpTrain, fpTrain = fpTrain, tnTrain = tnTrain,
    fnTrain = fnTrain, tpTest = tpTest, fpTest = fpTest, tnTest = tnTest,
    fnTest = fnTest, epoch = epoch)
}

object ResultTable {
  val resultQuery =  TableQuery[ResultTable]
}
final class ResultTable(tag: Tag) extends Table[Result](tag, "result") {
  def id: Rep[ResultId] = column[ResultId]("id", O.PrimaryKey, O.AutoInc)
  def tpTrain: Rep[Int] = column[Int]("tpTrain")
  def fpTrain: Rep[Int] = column[Int]("fpTrain")
  def tnTrain: Rep[Int] = column[Int]("tnTrain")
  def fnTrain: Rep[Int] = column[Int]("fnTrain")
  def tpTest: Rep[Int] = column[Int]("tpTest")
  def fpTest: Rep[Int] = column[Int]("fpTest")
  def tnTest: Rep[Int] = column[Int]("tnTest")
  def fnTest: Rep[Int] = column[Int]("fnTest")
  def epoch: Rep[Epoch] = column[Epoch]("epoch")
  def * : ProvenShape[Result] = (id, tpTrain, fpTrain, tnTrain, fnTrain, tpTest, fpTest, tnTest,
    fnTest, epoch).shaped <> (Result.tupled.apply, Result.unapply)
}
