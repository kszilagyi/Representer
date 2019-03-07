package com.kristofszilagyi.representer

object BooleanOps {
  implicit class RichBoolean(b: Boolean) {
    def toInt: Int = if (b) 1 else 0
  }
}
