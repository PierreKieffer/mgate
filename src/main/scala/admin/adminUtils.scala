package admin

import java.io.{BufferedWriter, File, FileWriter}

object adminUtils {
  def fileWriter(inputData : String, outputPath : String, filename : String, extension : String): Unit = {
    try {
//      val processSchema = inputData.replaceAll(" ","").replaceAll("\n","").replaceAll(",\"required\":\\[\\]", "")
      val file = new File(outputPath + filename + "." + extension)
      val bw = new BufferedWriter(new FileWriter(file))
      bw.write(inputData)
      bw.close()
    } catch {
      case exception: Exception => println(exception)
    }
  }
}
