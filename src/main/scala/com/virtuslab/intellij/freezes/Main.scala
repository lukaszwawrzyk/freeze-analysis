package com.virtuslab.intellij.freezes

import com.virtuslab.intellij.freezes.Show._
import com.virtuslab.intellij.freezes.analysis.Analysis
import java.nio.file.{Files, Path, Paths}
import java.time.LocalDate
import org.apache.commons.io.FileUtils
import org.rogach.scallop.{ScallopConf, singleArgConverter}

class Options(args: Seq[String]) extends ScallopConf(args) {
  implicit val localDateConvert = singleArgConverter[LocalDate](LocalDate.parse(_, dateFormat))
  private val tempDir = Paths.get(System.getProperty("java.io.tmpdir"))

  val top = opt[Int](
    short = 't',
    descr = "Number of top items to show",
    default = Some(20)
  )

  val since = opt[LocalDate](
    short = 'd',
    descr = "Dump freezes since this date",
    default = Some(LocalDate.now().minusMonths(1))
  )

  val remoteServer = opt[String](
    short = 'r',
    descr = "Server to run presto query on",
    default = Some("nest7.smfc.virtuslab.com")
  )

  val ideaBuild = opt[String](
    short = 'b',
    descr = "Substring of idea build"
  )

  val csvPath = opt[Path](
    short = 'i',
    descr = "Location for csv file dumo",
    default = Some {
      val sinceDate = since().show
      val tillDate = LocalDate.now().show
      tempDir.resolve(s"intellij-freezes-since-$sinceDate-till-$tillDate.csv")
    }
  )

  val overwrite = opt[Boolean](
    short = 'f',
    descr = "Overwrite dump if it exists"
  )

  val sorting = opt[List[String]](
    short = 's',
    descr = "How to sort the results",
    default = Some(List(SummaryStrategy.NumberOfOccurrences.name, SummaryStrategy.Median.name))
  ).map(_.map(SummaryStrategy.fromName))

  val output = opt[Path](
    short = 'o',
    descr = "Output directory for report",
    default = Some(
      tempDir.resolve(
        s"intellij-freezes-report-since-${since().show}-till-${LocalDate.now().show}"))
  )

  val dump = opt[Boolean](
    short = 'u',
    descr = "Enable to dump the detailed report",
    default = Some(false)
  )

  verify()
}

object Main {

  def main(args: Array[String]): Unit = {
    val options = new Options(args)
    val freezesDump = options.csvPath()

    if (Files.notExists(freezesDump) || options.overwrite()) {
      println("Fetching data from database...")
      Database.fetchFreezesAsCsv(
        freezesDump,
        options.since(),
        options.remoteServer(),
        options.ideaBuild.toOption)
    }

    println("Parsing data...")
    val parsedFreezes = Parser.parse(freezesDump)
    println(s"Grouping ${parsedFreezes.size} freezes...")
    val groups = Analysis.groupFreezes(parsedFreezes)
    println("Reporting...")
    Summary.showGroups(groups, options.sorting(), options.top())

    if (options.dump()) {
      println("Dumping data...")
      val output = options.output()
      if (Files.exists(output)) {
        if (options.overwrite()) {
          FileUtils.deleteDirectory(output.toFile)
        } else {
          println(
            s"Directory $output already exists. Rerun with -f to overwrite or specify different output directory with -o.")
          sys.exit(1)
        }
      }
      Summary.dump(output, groups, options.sorting(), options.top())
      println(s"Dumped to $output")
    }
  }

}
