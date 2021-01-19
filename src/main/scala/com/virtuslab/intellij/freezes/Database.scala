package com.virtuslab.intellij.freezes

import com.virtuslab.intellij.freezes.Show._
import java.nio.file.Path
import java.time.LocalDate
import scala.sys.process._

object Database {
  def fetchFreezesAsCsv(
    path: Path,
    since: LocalDate,
    remoteServer: String,
    ideaBuild: Option[String]
  ): Unit = {
    ???
  }

}
