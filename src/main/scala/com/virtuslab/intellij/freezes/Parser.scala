package com.virtuslab.intellij.freezes

import java.nio.file.{Files, Path}
import java.time.LocalDate
import org.apache.commons.csv.CSVFormat
import scala.concurrent.duration.{DurationDouble, FiniteDuration}
import scala.jdk.CollectionConverters._

// data specific
object Parser {

  case class Freeze(
    date: LocalDate,
    intelliJBuild: String,
    duration: FiniteDuration,
    fullStackTrace: Seq[String])

  def parse(path: Path): Seq[Freeze] = {
    val reader = Files.newBufferedReader(path)
    val records = CSVFormat.DEFAULT.parse(reader).getRecords.asScala
    records.map(record => parseLine(record.iterator().asScala.toList)).toVector
  }

  private def parseLine(line: List[String]): Freeze = {
    val List(dateStr, durationStr, stackTraceStr, intelliJBuild) = line
    val date = LocalDate.parse(dateStr, dateFormat)
    val duration = durationStr.toDouble.seconds
    val stackTrace = {
      if (stackTraceStr.linesIterator.size > 1) {
        stackTraceStr.split("\n\tat ").toList
      } else {
        stackTraceStr.stripSuffix(")").split(')').map(_ + ")").toList
      }
    }
    Freeze(date, intelliJBuild, duration, stackTrace)
  }

}
