package com.virtuslab.intellij.freezes.analysis

import com.virtuslab.intellij.freezes.Parser
import java.nio.charset.StandardCharsets
import org.apache.commons.io.IOUtils
import scala.concurrent.duration._

object Analysis {

  def groupFreezes(parsedFreezes: Seq[Parser.Freeze]): Seq[FreezeGroup] = {
    val freezes = prepare(parsedFreezes)
    val indicators = prepareIndicatorsList(freezes, stripMethod = false)
    val minGroupSize = 30

    freezes
      .groupBy { freeze =>
        indicators
          .find(indicator => freeze.stackTrace.exists(_.contains(indicator)))
          .getOrElse("contains only ignored frames")
      }
      .map { case (indicator, freezes) => FreezeGroup(indicator, freezes) }
      .filter(_.freezes.size >= minGroupSize)
      .toSeq
  }

  private def prepare(parsedFreezes: Seq[Parser.Freeze]) = {
    val irrelevantFrameIndicators = listFromResources("irrelevant-frames-indicators.txt")

    val freezes = parsedFreezes.map { freeze =>
      val stackTrace = freeze.fullStackTrace
        .filterNot(frame => irrelevantFrameIndicators.exists(i => frame.contains(i)))
      Freeze(freeze.date, freeze.intelliJBuild, freeze.duration, freeze.fullStackTrace, stackTrace)
    }

    val maxDuration = 30.minutes
    freezes
      .filter(_.duration < maxDuration) // ignore badly measured freezes that take hours
      .filter(_.stackTrace.nonEmpty) // ignore freezes with only irrelevant frames
  }

  private def prepareIndicatorsList(freezes: Seq[Freeze], stripMethod: Boolean): Seq[String] = {
    val predefinedIndicators = listFromResources("meaningful-frame-indicators.txt")

    def toGroupingKey(frame: String): String = {
      def byPredefinedGroup = {
        predefinedIndicators.find(frame.contains)
      }

      def byClass = {
        val withoutCodeLocation = frame.takeWhile(_ != '(').stripSuffix("$")
        if (stripMethod) {
          withoutCodeLocation.split('.').init.mkString(".")
        } else withoutCodeLocation
      }

      byPredefinedGroup.getOrElse(byClass)
    }

    // priority from 0 (lowest) to 1 (highest)
    case class Frame(value: String, priority: Double)

    val frames = freezes.flatMap { freeze =>
      freeze.stackTrace.zipWithIndex.map {
        case (frame, index) =>
          val isPredefined = predefinedIndicators.exists(indicator => frame.contains(indicator))
          // prioritize frames that are predefined and then prioritize by depth (deepest first)
          val rank = if (isPredefined) {
            1
          } else {
            val normalizedDepth = index / (freeze.stackTrace.size - 1).toDouble
            1 - normalizedDepth
          }
          Frame(frame, rank)
      }
    }

    frames
      .groupBy(frame => toGroupingKey(frame.value))
      .toList
      .sortBy {
        case (indicator, group) =>
          val totalPriority = group.map(_.priority).sum
          -totalPriority
      }.map { case (indicator, group) => indicator }
  }

  private def listFromResources(name: String) = {
    val path = s"/com/virtuslab/intellij/freezes/$name"
    IOUtils.resourceToString(path, StandardCharsets.UTF_8).linesIterator.toList
  }
}
