package com.kristofszilagyi.representer
import slick.jdbc.JdbcType
import slick.jdbc.PostgresProfile.api._
import slick.lifted.ProvenShape

object NaiveDecayStrategyId{

  implicit val jdbcType: JdbcType[NaiveDecayStrategyId] = MappedColumnType.base[NaiveDecayStrategyId, Int](_.i, NaiveDecayStrategyId.apply)

}

final case class OONaiveDecayStrategy(multiplier: Double) {
  // compile time check for being the same
  def toRelational(id: NaiveDecayStrategyId): NaiveDecayStrategy = NaiveDecayStrategy(id, multiplier)
}


final case class NaiveDecayStrategyId(i: Int)
final case class NaiveDecayStrategy(id: NaiveDecayStrategyId, multiplier: Double) {
  def toOO: OONaiveDecayStrategy = OONaiveDecayStrategy(multiplier)
}
final class NaiveDecayStrategyTable(tag: Tag) extends Table[NaiveDecayStrategy](tag, "naiveDecayStrategy") {
  def id: Rep[NaiveDecayStrategyId] = column[NaiveDecayStrategyId]("id")
  def multiplier: Rep[Double] = column[Double]("multiplier")

  def * : ProvenShape[NaiveDecayStrategy] = (id, multiplier).shaped <> (NaiveDecayStrategy.tupled.apply, NaiveDecayStrategy.unapply)
}

object NaiveDecayStrategyTable {
  val naiveDecayStrategyQuery = TableQuery[NaiveDecayStrategyTable]
}
