package com.kristofszilagyi.representer

import slick.jdbc.JdbcType
import slick.jdbc.PostgresProfile.api._

object LearningRateDecayStrategyName {
  implicit val jdbcType: JdbcType[LearningRateDecayStrategyName] =
    MappedColumnType.base[LearningRateDecayStrategyName, String](_.s, LearningRateDecayStrategyName.apply)
}
final case class LearningRateDecayStrategyName(s: String)
sealed trait LearningRateDecayStrategy {
  def name: LearningRateDecayStrategyName
  def decayRate: Double
}
object LearningRateDecayStrategy extends {
  final case object Constant extends LearningRateDecayStrategy {
    def name: LearningRateDecayStrategyName = LearningRateDecayStrategyName("constant")
    def decayRate: Double = 1.0
  }
  final case class NaiveDecay(decayRate: Double) extends LearningRateDecayStrategy {
    def name: LearningRateDecayStrategyName = LearningRateDecayStrategyName("NaiveDecay")

  }

  def learningStrategies: Seq[LearningRateDecayStrategy] = List(Constant, NaiveDecay(0.9), NaiveDecay(0.99), NaiveDecay(0.999))

  def maxEpochs = List(25, 500, 10000)
}
