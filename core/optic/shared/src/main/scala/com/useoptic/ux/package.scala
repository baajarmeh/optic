package com.useoptic

import com.useoptic.contexts.requests.{HttpRequest, HttpResponse}
import com.useoptic.contexts.rfc.RfcState
import com.useoptic.contexts.shapes.Commands.{FieldId, ShapeId}
import com.useoptic.diff.ChangeType.ChangeType
import com.useoptic.diff.{DiffResult, InteractiveDiffInterpretation}
import com.useoptic.diff.interactions.{InteractionDiffResult, UnmatchedRequestBodyContentType, UnmatchedRequestBodyShape, UnmatchedResponseBodyContentType}
import com.useoptic.diff.interactions.interpreters.DiffDescription
import com.useoptic.diff.shapes.ShapeDiffResult
import com.useoptic.types.capture.HttpInteraction
import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._

import scala.scalajs.js.annotation.JSExportAll
import scala.util.Try

package object ux {

  type DiffsToInteractionsMap = Map[InteractionDiffResult, Seq[HttpInteraction]]

  @JSExportAll
  case class Region(name: String, diffBlocks: Seq[DiffBlock]) {
    def isEmpty: Boolean = diffBlocks.isEmpty
    def nonEmpty: Boolean = diffBlocks.nonEmpty
  }
  @JSExportAll
  case class TopLevelRegions(newRegions: Region, requestRegions: Seq[Region], responseRegions: Seq[Region])

  type ToSuggestions = () => Seq[InteractiveDiffInterpretation]

  //Diff Types
  @JSExportAll
  trait DiffBlock {
    def inRequest: Boolean
    def inResponse: Boolean
    def interactions: Seq[HttpInteraction]
    def count = interactions.size
    def description: DiffDescription
    def suggestions: Seq[InteractiveDiffInterpretation]
    def firstSuggestion: InteractiveDiffInterpretation = suggestions.head
  }

  @JSExportAll
  case class NewRegionDiffBlock(diff: DiffResult,
                                interactions: Seq[HttpInteraction],
                                inRequest: Boolean,
                                inResponse: Boolean,
                                contentType: Option[String],
                                statusCode: Option[Int],
                                description: DiffDescription)
                               (implicit val toSuggestions: ToSuggestions) extends DiffBlock {
    def suggestions = toSuggestions()
  }
  @JSExportAll
  case class BodyShapeDiffBlock(diff: DiffResult,
                                shapeDiff: ShapeDiffResult,
                                interactions: Seq[HttpInteraction],
                                inRequest: Boolean,
                                inResponse: Boolean,
                                contentType: String,
                                description:  DiffDescription)
                               (implicit val toSuggestions: ToSuggestions,
                                _previewDiff: (HttpInteraction, Option[RfcState]) => RenderShapeRoot,
                                _previewRequest: (HttpInteraction, Option[RfcState]) => Option[RenderShapeRoot],
                                _previewResponse: (HttpInteraction, Option[RfcState]) => Option[RenderShapeRoot]) extends DiffBlock {

    def suggestions = toSuggestions()
    def previewRender(interaction: HttpInteraction = interactions.head, withRfcState: Option[RfcState] = None): RenderShapeRoot = _previewDiff(interaction, withRfcState)
    def previewRequest(interaction: HttpInteraction = interactions.head, withRfcState: Option[RfcState] = None): Option[RenderShapeRoot] = _previewRequest(interaction, withRfcState)
    def previewResponse(interaction: HttpInteraction = interactions.head,  withRfcState: Option[RfcState] = None): Option[RenderShapeRoot] = _previewResponse(interaction, withRfcState)
  }

  // Shape Renderer
  @JSExportAll
  case class RenderShapeRoot(rootId: ShapeId,
                             exampleFields: Map[FieldId, RenderField], exampleShapes: Map[ShapeId, RenderShape],
                             specFields: Map[FieldId, RenderField], specShapes: Map[ShapeId, RenderShape]) {

    def getUnifiedShape(shapeId: ShapeId): RenderShape = {
      val specShape = specShapes(shapeId)
      val exampleShape = exampleShapes.get(shapeId)
      val mergedFields = exampleShape.map(_.fields).getOrElse(Fields(Seq.empty, Seq.empty, Seq.empty, Seq.empty)).merge(specShape.fields)
      specShape.copy(fields = mergedFields, exampleValue = exampleShape.flatMap(_.exampleValue), diffs = specShape.diffs ++ exampleShape.map(_.diffs).getOrElse(Set.empty))
    }

    def getUnifiedField(fieldId: FieldId): Option[RenderField] = {
      val specField = specFields.get(fieldId)
      val exampleField = exampleFields.get(fieldId)

      if (specField.isEmpty && exampleField.isEmpty) {
        None
      } else {
        Some(RenderField(
          if (specField.isDefined) specField.get.fieldId else exampleField.get.fieldId,
          if (specField.isDefined) specField.get.fieldName else exampleField.get.fieldName,
          specField.flatMap(_.shapeId),
          exampleField.flatMap(_.exampleValue),
          specField.map(_.diffs).getOrElse(Set.empty) ++ exampleField.map(_.diffs).getOrElse(Set.empty)
        ))
      }
    }

    def resolveFields(fields: Fields): Seq[DisplayField] = {

      val unifiedMissing = fields.missing.flatMap(i => {
        val fieldOption = getUnifiedField(i)
        fieldOption.map(field => DisplayField(field.fieldName, field, "missing"))
      })

      val unifiedUnexpected = fields.unexpected.flatMap(i => {
        val fieldOption = getUnifiedField(i)
        fieldOption.map(field => DisplayField(field.fieldName, field, "unexpected"))
      })

      val unifiedExpected = fields.expected.flatMap(i => {
        val fieldOption = getUnifiedField(i)
        fieldOption.map(field => DisplayField(field.fieldName, field, "visible"))
      }).filterNot(i => unifiedMissing.exists(_.fieldName == i.fieldName))


      (unifiedExpected ++ unifiedMissing ++ unifiedUnexpected).sortBy(_.fieldName)
    }

    def resolveFieldShape(field: RenderField): Option[RenderShape] = Try(field.shapeId.map(getUnifiedShape)).toOption.flatten

  }

  @JSExportAll
  case class Fields(expected: Seq[FieldId], missing: Seq[FieldId], unexpected: Seq[FieldId], hidden: Seq[FieldId] = Seq.empty) {
    def merge(o: Fields) = {
      Fields(
        (expected ++ o.expected).distinct,
        (missing ++ o.missing).distinct,
        (unexpected ++ o.unexpected).distinct,
        (hidden ++ o.hidden).distinct
      )
    }
  }
  @JSExportAll
  case class Items(all: Seq[ShapeId], hidden: Seq[ShapeId])

  @JSExportAll
  case class DisplayField(fieldName: String, field: RenderField, display: String)

  @JSExportAll
  case class RenderField(fieldId: FieldId, fieldName: String, shapeId: Option[ShapeId], exampleValue: Option[Json], diffs: Set[DiffResult] = Set())
  @JSExportAll
  case class RenderShape(shapeId: FieldId,
                         baseShapeId: String,
                         fields: Fields = Fields(Seq.empty, Seq.empty, Seq.empty, Seq.empty),
                         items: Items = Items(Seq.empty, Seq.empty),
                         exampleValue: Option[Json] = None,
                         diffs: Set[DiffResult] = Set())


}