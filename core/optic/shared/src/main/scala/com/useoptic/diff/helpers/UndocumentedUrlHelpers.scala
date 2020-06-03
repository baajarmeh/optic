package com.useoptic.diff.helpers

import com.useoptic.contexts.requests.Utilities
import com.useoptic.contexts.rfc.RfcState
import com.useoptic.diff.helpers.UndocumentedUrlHelpers.{MethodAndPath, UrlCounter}
import com.useoptic.dsa.Counter
import com.useoptic.types.capture.HttpInteraction

import scala.scalajs.js.annotation.{JSExport, JSExportAll}

@JSExport
@JSExportAll
class UndocumentedUrlHelpers {
  def countUndocumentedUrls(spec: RfcState, interactions: Seq[HttpInteraction]): UrlCounter = {
    val grouping = Utilities.groupPathsByParentId(spec.requestsState.pathComponents)
    val urlCounter = UndocumentedUrlHelpers.newCounter
    interactions.foreach(interaction => {
      val pathId = Utilities.resolvePathByGrouping(interaction.request.path, grouping)
      if (pathId.isEmpty) {
        urlCounter.increment(MethodAndPath(interaction.request.method, interaction.request.path))
      }
    })
    urlCounter
  }
}

object UndocumentedUrlHelpers {
  type UrlCounter = Counter[MethodAndPath]

  case class MethodAndPath(method: String, path: String)

  def newCounter = new Counter[MethodAndPath]
}