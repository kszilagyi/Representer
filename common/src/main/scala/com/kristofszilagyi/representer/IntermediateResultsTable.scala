package com.kristofszilagyi.representer
import com.kristofszilagyi.representer.ResultTable.resultQuery
import com.kristofszilagyi.representer.RunsTable.runsQuery
import slick.jdbc.PostgresProfile.api._
import slick.lifted.{ForeignKeyQuery, ProvenShape}

final case class IntermediateResults(run: RunId, result: ResultId)

final class IntermediateResultsTable (tag: Tag) extends Table[IntermediateResults](tag, "intermediateResults") {
  def runId: Rep[RunId] = column[RunId]("run")
  def run: ForeignKeyQuery[RunsTable, Run] = foreignKey("runFK", runId, runsQuery)(_.id)

  def resultId: Rep[ResultId] = column[ResultId]("resultId")
  def result: ForeignKeyQuery[ResultTable, Result] = foreignKey("resultFk", resultId, resultQuery)(_.id)

  def * : ProvenShape[IntermediateResults] = (runId, resultId).shaped <> (IntermediateResults.tupled.apply, IntermediateResults.unapply)
}