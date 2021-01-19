package com.virtuslab.intellij.freezes

import java.time.LocalDate
import scala.concurrent.duration.Duration

trait Show[-A] {
  def show(a: A): String
}

object Show {
  implicit class ShowOps[A: Show](a: A) {
    def show: String = implicitly[Show[A]].show(a)
  }

  implicit val showInt: Show[Int] = (a: Int) => "%1$5d".format(a)

  implicit val showFiniteDuration: Show[Duration] = (a: Duration) => {
    val duration = java.time.Duration.ofNanos(a.toNanos)
    val s = duration.getSeconds
    "%02d:%02d:%02d".format(s / 3600, (s % 3600) / 60, s % 60)
  }

  implicit val showLocalDate: Show[LocalDate] = _.format(dateFormat)

}
