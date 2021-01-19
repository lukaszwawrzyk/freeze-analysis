package com.virtuslab.intellij.freezes.analysis

import com.virtuslab.intellij.freezes.Show
import com.virtuslab.intellij.freezes.Show._
import scala.concurrent.duration.{Duration, FiniteDuration}

case class FreezeGroup(name: String, freezes: Seq[Freeze]) {
  def show[A: Show](parameter: A): String = {
    def show[B: Show](name: String, value: B): String = s"$name: ${value.show}"
    val commonProperties = Seq(
      show("size", size),
      show("total time", totalDuration),
      show("median", median),
      show("90th perc", percentile90),
    )
    commonProperties.mkString(s"${parameter.show} (", ", ", s") $name")
  }

  lazy val size: Int = freezes.size

  lazy val durations: Seq[FiniteDuration] = freezes.map(_.duration).sorted

  lazy val totalDuration: FiniteDuration = durations.fold(Duration.Zero)(_ + _)

  lazy val average: FiniteDuration = (totalDuration / size.toDouble).asInstanceOf[FiniteDuration]

  def durationPercentile(value: Double): FiniteDuration = durations((size * value).toInt)

  lazy val median = durationPercentile(0.5)

  lazy val percentile90 = durationPercentile(0.9)
}
