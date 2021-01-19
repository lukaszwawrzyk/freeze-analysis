package com.virtuslab.intellij.freezes.analysis

import java.time.LocalDate
import scala.concurrent.duration.FiniteDuration

case class Freeze(
  date: LocalDate,
  intelliJBuild: String,
  duration: FiniteDuration,
  fullStackTrace: Seq[String],
  stackTrace: Seq[String])
