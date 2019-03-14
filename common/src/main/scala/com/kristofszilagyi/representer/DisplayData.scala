//package com.kristofszilagyi.representer
//
//import java.awt.{Color, GridLayout}
//
//import com.kristofszilagyi.representer.Common._
//import com.kristofszilagyi.representer.TypeSafeEqualsOps._
//import javax.swing.JPanel
//import smile.classification.NeuralNetwork
//import smile.plot.{PlotCanvas, ScatterPlot, Window}
//import smile.read
//
//object DisplayData {
//
//  private def canvas(data: Array[Array[Double]], label: Array[Int], title: String) = {
//    val canvas = ScatterPlot.plot(data, label, 'x', Array(Color.BLACK, Color.GRAY))
//    canvas.setAxisLabels("a", "b")
//    canvas.setTitle(title)
//    canvas
//  }
//
//  private def successCanvas(data: Array[Array[Double]], label: Array[Int], title: String) = {
//    val canvas = ScatterPlot.plot(data, label, 'x', Array(Color.RED, Color.GREEN))
//    canvas.setAxisLabels("a", "b")
//    canvas.setTitle(title)
//  }
//
//  private def multiWindow(title: String, canvases: Seq[PlotCanvas]): Unit = {
//    val panel = new JPanel(new GridLayout(2, canvases.size / 2))
//    canvases.foreach(panel.add)
//    val frame = Window.frame(title)
//    frame.add(panel)
//    canvases.foreach(_.repaint())
//  }
//
//  def main(args: Array[String]): Unit = {
//    hiddenLayerSizes.take(3).foreach { hiddenLayerSize =>
//      val model = read.xstream(modelPath(hiddenLayerSize).toFile.toString).asInstanceOf[NeuralNetwork]
//      val dataSize = 100000
//      val constraint = (a: Double, b: Double) => math.abs(a * b - 50) < 20
//      val trainingUnconstrained = DataGenerator.trainingData(dataSize)
//      val scaler = teachScaler(trainingUnconstrained)
//      val ScaledAll(training, test) = scale(
//        DataGenerator.constrainedTrainingData(dataSize, constraint),
//        DataGenerator.constrainedTestData(dataSize, constraint),
//        scaler
//      )
//      val trainingPrediction = model.predict(training.x)
//      val testPrediction = model.predict(test.x)
//
//      val layerSizeTitle = s"Hidden layer size: $hiddenLayerSize"
//      val layerSizeString = s"(${layerSizeTitle.toLowerCase})"
//
//      val trainingSuccess = trainingPrediction.zip(training.y).map { case (pred, y) => if (pred ==== y) 1 else 0 }
//      val testSuccess = testPrediction.zip(test.y).map { case (pred, y) => if (pred ==== y) 1 else 0 }
//      multiWindow(layerSizeTitle,
//        List(
//          canvas(training.x, trainingPrediction, s"Training prediction $layerSizeString"),
//          canvas(training.x, training.y, s"Training real  $layerSizeString"),
//          canvas(test.x, testPrediction, s"Test prediction  $layerSizeString"),
//          canvas(test.x, test.y, s"Test real $layerSizeString"),
//
//          successCanvas(training.x, trainingSuccess, s"Success training layerSizeString"),
//          successCanvas(test.x, testSuccess, s"Success test layerSizeString")
//        )
//      )
//    }
//  }
//}
