package com.virtuslab.intellij.freezes

import com.virtuslab.intellij.freezes.analysis.FreezeGroup
import com.virtuslab.intellij.freezes.Show._

sealed abstract class SummaryStrategy(
  val name: String,
  val description: String) {
  def sortGroups(groups: Seq[FreezeGroup]): Seq[FreezeGroup]
  def showGroup(group: FreezeGroup): String
}

object SummaryStrategy {
  private var all = Seq.empty[SummaryStrategy]

  val NumberOfOccurrences = create("count", "number of occurrences", _.size)
  val AverageDuration = create("avg", "average time spent", _.average)
  val Median = create("median", "median time spent", _.median)
  val Percentile90 = create("perc90", "90th percentile time spent", _.percentile90)
  val TotalDuration = create("duration", "total time spent", _.totalDuration)

  private def create[A: Show: Ordering](
    name: String,
    description: String,
    extract: FreezeGroup => A
  ): SummaryStrategy = {
    val strategy = new SummaryStrategy(name, description) {
      def showGroup(group: FreezeGroup): String = {
        group.show(extract(group))
      }

      def sortGroups(groups: Seq[FreezeGroup]): Seq[FreezeGroup] = {
        groups.sortBy(extract)(implicitly[Ordering[A]].reverse)
      }
    }
    all :+= strategy
    strategy
  }

  def fromName(name: String): SummaryStrategy = {
    all
      .find(_.name.equalsIgnoreCase(name))
      .getOrElse {
        val available = all.map(_.name).mkString(", ")
        throw new RuntimeException(
          s"Could not find summary strategy '$name', try one of: $available")
      }
  }
}
