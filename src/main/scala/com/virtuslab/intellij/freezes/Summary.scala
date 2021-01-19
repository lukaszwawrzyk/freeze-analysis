package com.virtuslab.intellij.freezes

import com.virtuslab.intellij.freezes.Show._
import com.virtuslab.intellij.freezes.analysis.FreezeGroup
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import org.apache.commons.io.FileUtils

object Summary {
  def dump(
    output: Path,
    groups: Seq[FreezeGroup],
    sorting: Seq[SummaryStrategy],
    top: Int
  ): Unit = {
    sorting.foreach { sorting =>
      dump(output.resolve(sorting.name), groups, top, sorting)
    }
  }

  def showGroups(groups: Seq[FreezeGroup], sorting: Seq[SummaryStrategy], top: Int): Unit = {
    sorting.foreach { sorting =>
      showSorted(groups, top, sorting)
    }
  }

  private def showSorted(
    groups: Seq[FreezeGroup],
    top: Int,
    summaryStrategy: SummaryStrategy
  ): Unit = {
    val sortedGroups = sortGroups(groups, top, summaryStrategy)
    val summary = renderSummary(top, summaryStrategy, sortedGroups)
    println(summary)
  }

  private def dump(
    output: Path,
    groups: Seq[FreezeGroup],
    top: Int,
    summaryStrategy: SummaryStrategy
  ): Unit = {
    val sortedGroups = sortGroups(groups, top, summaryStrategy)
    val summary = renderSummary(top, summaryStrategy, sortedGroups)
    write(output.resolve("summary.txt"), summary)

    sortedGroups.foreach { group =>
      val file = output.resolve(s"${group.name}.txt")
      val freezes = group.freezes.map { freeze =>
        s"""Date: ${freeze.date.show}
           |Duration: ${freeze.duration.show}
           |IntelliJ Build: ${freeze.intelliJBuild}
           |Stack trace:
           |${freeze.fullStackTrace.mkString("\n")}
           |
           |""".stripMargin
      }
      val content = freezes.mkString("\n")
      write(file, content)
    }
  }

  private def renderSummary(
    top: Int,
    summaryStrategy: SummaryStrategy,
    sortedGroups: Seq[FreezeGroup]
  ) = {
    val lines = Seq(s"Results for: ${summaryStrategy.description} (top $top)") ++
      sortedGroups.map(summaryStrategy.showGroup) ++ Seq("", "")
    lines.mkString("\n")
  }

  private def sortGroups(
    groups: Seq[FreezeGroup],
    top: Int,
    summaryStrategy: SummaryStrategy
  ) = {
    summaryStrategy.sortGroups(groups).take(top)
  }

  private def write(path: Path, string: String): Unit = {
    FileUtils.write(path.toFile, string, StandardCharsets.UTF_8)
  }
}
