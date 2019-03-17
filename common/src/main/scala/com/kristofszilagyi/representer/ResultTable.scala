package com.kristofszilagyi.representer

import slick.jdbc.PostgresProfile.api._
import slick.lifted.ProvenShape


final case class Result(id: Int, truePositiveCount: Int, falsePositiveCount: Int, trueNegativeCount: Int,
                        falseNegativeCount: Int, epoch: Int)
final class ResultTable(tag: Tag) extends Table[Result](tag, "result") {
  def id: Rep[Int] = column[Int]("id")
  def truePositiveCount: Rep[Int] = column[Int]("truePositiveCount")
  def falsePositiveCount: Rep[Int] = column[Int]("falsePositiveCount")
  def trueNegativeCount: Rep[Int] = column[Int]("trueNegativeCount")
  def falseNegativeCount: Rep[Int] = column[Int]("falseNegativeCount")
  def epoch: Rep[Int] = column[Int]("epoch")
  def * : ProvenShape[Result] = (id, truePositiveCount, falsePositiveCount, trueNegativeCount,
    falseNegativeCount, epoch).shaped <> (Result.tupled.apply, Result.unapply)
}
