package com.kristofszilagyi.representer

import java.awt.{Color, GridLayout}

import com.kristofszilagyi.representer.Common.{hiddenLayerSizes, modelPath, scale}
import com.kristofszilagyi.representer.TypeSafeEqualsOps._
import javax.swing.JPanel
import smile.classification.NeuralNetwork
import smile.plot.{PlotCanvas, ScatterPlot, Window}
import smile.read

object DisplayData {

  private def canvas(data: Array[Array[Double]], label: Array[Int], title: String) = {
    val canvas = ScatterPlot.plot(data, label, 'x', Array(Color.BLACK, Color.WHITE))
    canvas.setBackground(Color.GRAY)
    canvas.setAxisLabels("a", "b")
    canvas.setTitle(title)
    canvas
  }

  private def successCanvas(data: Array[Array[Double]], label: Array[Int], title: String) = {
    val canvas = ScatterPlot.plot(data, label, 'x', Array(Color.RED, Color.GREEN))
    canvas.setAxisLabels("a", "b")
    canvas.setTitle(title)
  }

  private def multiWindow(title: String, canvases: Seq[PlotCanvas]) = {
    val panel = new JPanel(new GridLayout(2, canvases.size / 2))
    panel.setBackground(Color.white)
    canvases.foreach(panel.add)
    val frame = Window.frame(title)
    frame.add(panel)
    canvases.foreach(_.repaint())
  }

  def main(args: Array[String]): Unit = {
    hiddenLayerSizes.foreach { hiddenLayerSize =>
      val model = read.xstream(modelPath(hiddenLayerSize).toFile.toString).asInstanceOf[NeuralNetwork]
      val dataSize = 1000
      val ScaledAll(training, test) = scale(DataGenerator.trainingData(dataSize), DataGenerator.testData(dataSize))
      val trainingPrediction = model.predict(training.x)
      val testPrediction = model.predict(test.x)

      val layerSizeTitle = s"Hidden layer size: $hiddenLayerSize"
      val layerSizeString = s"(${layerSizeTitle.toLowerCase})"

      val trainingSuccess = trainingPrediction.zip(training.y).map { case (pred, y) => if (pred ==== y) 1 else 0 }
      val testSuccess = testPrediction.zip(test.y).map { case (pred, y) => if (pred ==== y) 1 else 0 }
      multiWindow(layerSizeTitle,
        List(
          canvas(training.x, trainingPrediction, s"Training prediction $layerSizeString"),
          canvas(training.x, training.y, s"Training real  $layerSizeString"),
          canvas(test.x, testPrediction, s"Test prediction  $layerSizeString"),
          canvas(test.x, test.y, s"Test real $layerSizeString"),

          successCanvas(training.x, trainingSuccess, s"Success training layerSizeString"),
          successCanvas(test.x, testSuccess, s"Success test layerSizeString")
        )
      )
    }
  }
}
