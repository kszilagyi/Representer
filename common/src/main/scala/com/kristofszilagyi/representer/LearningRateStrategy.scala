package com.kristofszilagyi.representer

import com.kristofszilagyi.representer.tables.OONaiveDecayStrategy

sealed trait LearningRateStrategy {
  def name: String
  def toRelational: Option[OONaiveDecayStrategy]
}
object LearningRateStrategy extends {
  final case object Constant extends LearningRateStrategy {
    def name: String = "constant"

    def toRelational: Option[OONaiveDecayStrategy] = None
  }
  final case class NaiveDecay(decayRate: Double) extends LearningRateStrategy {
    def name: String = "NaiveDecay"

    def toRelational: Option[OONaiveDecayStrategy] = Some(OONaiveDecayStrategy(decayRate))
  }

  def learningStrategies: Seq[LearningRateStrategy] = List(Constant, NaiveDecay(0.9), NaiveDecay(0.99), NaiveDecay(0.999))
}
