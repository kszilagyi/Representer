package com.kristofszilagyi.representer

import org.log4s.getLogger

import scala.concurrent.ExecutionContext

final class SyncEc extends ExecutionContext{
  private val logger = getLogger

  def execute(runnable: Runnable): Unit = {
    runnable.run()
  }

  def reportFailure(cause: Throwable): Unit = {
    logger.info(s"EC exception: $cause")
  }
}
