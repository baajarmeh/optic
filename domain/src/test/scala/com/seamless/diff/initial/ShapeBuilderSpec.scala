package com.seamless.diff.initial

import com.seamless.contexts.rfc.{RfcService, RfcServiceJSFacade}
import com.seamless.diff.JsonFileFixture
import com.seamless.serialization.CommandSerialization
import org.scalatest.FunSpec

class ShapeBuilderSpec extends FunSpec with JsonFileFixture {

  it("can learn a basic concept with 3 string keys") {
    val basic = fromFile("basic-concept")
    val result = new ShapeBuilder(basic, "basic").run.asConceptNamed("Basic")
    assert(result.commands == commandsFrom("basic-concept"))
  }

  it("can learn a nested concept") {
    val basic = fromFile("nested-concept")
    val result = new ShapeBuilder(basic, "nested").run.asConceptNamed("Nested")
    assert(result.nameRequests.size == 3)
    assert(result.commands == commandsFrom("nested-concept"))
  }

  it("can learn with array of primitives") {
    val basic = fromFile("primitive-array")
    val result = new ShapeBuilder(basic, "pa").run.asConceptNamed("Array")
    assert(result.nameRequests.size == 1)
    assert(result.commands == commandsFrom("primitive-array"))
  }

  it("works with todo example") {
    val basic = fromFile("todo-body")
    val result = new ShapeBuilder(basic, "pa").run

    val eventStore = RfcServiceJSFacade.makeEventStore()
    val rfcService: RfcService = new RfcService(eventStore)
    println(CommandSerialization.toJson(result.commands))
    rfcService.handleCommandSequence("id", result.commands)
  }

}
