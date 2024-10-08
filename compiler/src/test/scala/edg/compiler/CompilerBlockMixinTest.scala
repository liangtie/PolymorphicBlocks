package edg.compiler

import edg.{CompilerTestUtil, EdgirUtils}
import edg.ElemBuilder._
import edg.ExprBuilder.Ref
import edg.wir.ProtoUtil.BlockProtoToSeqMap
import edg.wir.{DesignPath, EdgirLibrary, IndirectDesignPath, IndirectStep, Refinements}
import org.scalatest._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers._

import scala.collection.SeqMap

/** Test for block expansion with mixins
  */
class CompilerBlockMixinTest extends AnyFlatSpec with CompilerTestUtil {
  val library = Library(
    base = CompilerExpansionTest.library,
    blocks = Seq(
      Block.Block(
        "baseBlock",
        isAbstract = true,
        ports = SeqMap(
          "port" -> Port.Library("sinkPort"),
        )
      ),
      Block.Block(
        "mixin",
        superclasses = Seq("baseBlock"),
        isAbstract = true,
        ports = SeqMap(
          "mixinPort" -> Port.Library("sinkPort"),
        )
      ),
      Block.Block( // subclass mixin that adds a new port
        "mixinSub",
        superclasses = Seq("mixin"),
        isAbstract = true,
        ports = SeqMap(
          "mixinPort" -> Port.Library("sinkPort"),
          "mixinSubPort" -> Port.Library("sinkPort"),
        )
      ),
      Block.Block(
        "concreteBaseBlock",
        superclasses = Seq("baseBlock"),
        ports = SeqMap(
          "port" -> Port.Library("sinkPort"),
        )
      ),
      Block.Block(
        "concreteMixinBlock",
        superclasses = Seq("baseBlock", "mixin"),
        ports = SeqMap(
          "port" -> Port.Library("sinkPort"),
          "mixinPort" -> Port.Library("sinkPort"),
        )
      ),
      Block.Block(
        "concreteMixinSubBlock",
        superclasses = Seq("baseBlock", "mixinSub"),
        superSuperclasses = Seq("mixin"),
        ports = SeqMap(
          "port" -> Port.Library("sinkPort"),
          "mixinPort" -> Port.Library("sinkPort"),
          "mixinSubPort" -> Port.Library("sinkPort"),
        )
      ),
    ),
  )

  val inputDesign = Design(Block.Block(
    "topDesign",
    blocks = SeqMap(
      "source" -> Block.Library("sourceBlock"),
      "mixinSource" -> Block.Library("sourceBlock"),
      "block" -> Block.Library("baseBlock", mixins = Seq("mixin")),
    ),
    links = SeqMap(
      "link" -> Link.Library("link"),
      "mixinLink" -> Link.Library("link"),
    ),
    constraints = SeqMap(
      "sourceConnect" -> Constraint.Connected(Ref("source", "port"), Ref("link", "source")),
      "sinkConnect" -> Constraint.Connected(Ref("block", "port"), Ref("link", "sink")),
      "mixinSourceConnect" -> Constraint.Connected(Ref("mixinSource", "port"), Ref("mixinLink", "source")),
      "mixinSinkConnect" -> Constraint.Connected(Ref("block", "mixinPort"), Ref("mixinLink", "sink")),
    )
  ))

  "Compiler on design with abstract mixin" should "expand blocks and error" in {
    val referenceElaborated = Design(Block.Block(
      "topDesign",
      blocks = SeqMap(
        "source" -> Block.Block(
          selfClass = "sourceBlock",
          ports = SeqMap(
            "port" -> Port.Port(selfClass = "sourcePort"),
          )
        ),
        "mixinSource" -> Block.Block(
          selfClass = "sourceBlock",
          ports = SeqMap(
            "port" -> Port.Port(selfClass = "sourcePort"),
          )
        ),
        "block" -> Block.Block(
          selfClass = "baseBlock",
          prerefineMixins = Seq("mixin"),
          isAbstract = true,
          ports = SeqMap(
            "port" -> Port.Port(selfClass = "sinkPort"),
            "mixinPort" -> Port.Port(selfClass = "sinkPort"),
          )
        ),
      ),
      links = SeqMap(
        "link" -> Link.Link(
          selfClass = "link",
          ports = SeqMap(
            "source" -> Port.Port(selfClass = "sourcePort"),
            "sink" -> Port.Port(selfClass = "sinkPort"),
          )
        ),
        "mixinLink" -> Link.Link(
          selfClass = "link",
          ports = SeqMap(
            "source" -> Port.Port(selfClass = "sourcePort"),
            "sink" -> Port.Port(selfClass = "sinkPort"),
          )
        ),
      ),
      constraints = SeqMap(
        "sourceConnect" -> Constraint.Connected(Ref("source", "port"), Ref("link", "source")),
        "sinkConnect" -> Constraint.Connected(Ref("block", "port"), Ref("link", "sink")),
        "mixinSourceConnect" -> Constraint.Connected(Ref("mixinSource", "port"), Ref("mixinLink", "source")),
        "mixinSinkConnect" -> Constraint.Connected(Ref("block", "mixinPort"), Ref("mixinLink", "sink")),
      )
    ))
    val compiler = new Compiler(inputDesign, new EdgirLibrary(library))
    val compiled = compiler.compile()
    compiler.getErrors() shouldBe empty
    new DesignStructuralValidate().map(compiled) should equal(Seq(
      CompilerError.AbstractBlock(DesignPath() + "block", LibraryPath("baseBlock")),
      CompilerError.RefinementSubclassError( // technically not a refinement error, but still a subclass error
        DesignPath() + "block",
        LibraryPath("baseBlock"),
        LibraryPath("mixin")
      )
    ))
    compiled.toProtoString should equal(referenceElaborated.toProtoString)
  }

  "Compiler on design with invalid mixin refinement" should "error" in {
    // don't care about the output here, it's invalid
    val compiler = new Compiler(
      inputDesign,
      new EdgirLibrary(library),
      refinements = Refinements(
        instanceRefinements = Map(DesignPath() + "block" -> LibraryPath("concreteBaseBlock")),
      )
    )
    val compiled = compiler.compile()
    compiler.getErrors() shouldBe empty
    new DesignStructuralValidate().map(compiled) should equal(Seq(
      CompilerError.RefinementSubclassError(
        DesignPath() + "block",
        LibraryPath("concreteBaseBlock"),
        LibraryPath("mixin")
      )
    ))
    new DesignRefsValidate().validate(compiled) should not be empty // bad connection to mixinPort
    new DesignAssertionCheck(compiler).map(compiled) shouldBe empty
  }

  "Compiler on design with refined mixin" should "expand blocks" in {
    val referenceElaborated = Design(Block.Block(
      "topDesign",
      blocks = SeqMap(
        "source" -> Block.Block(
          selfClass = "sourceBlock",
          ports = SeqMap(
            "port" -> Port.Port(selfClass = "sourcePort"),
          )
        ),
        "mixinSource" -> Block.Block(
          selfClass = "sourceBlock",
          ports = SeqMap(
            "port" -> Port.Port(selfClass = "sourcePort"),
          )
        ),
        "block" -> Block.Block(
          superclasses = Seq("baseBlock", "mixin"),
          prerefine = "baseBlock",
          prerefineMixins = Seq("mixin"),
          selfClass = "concreteMixinBlock",
          ports = SeqMap(
            "port" -> Port.Port(selfClass = "sinkPort"),
            "mixinPort" -> Port.Port(selfClass = "sinkPort"),
          ),
          meta = Some(Map(
            "refinedNewPorts" -> EdgirUtils.strSeqToMeta(Seq()),
            "refinedNewParams" -> EdgirUtils.strSeqToMeta(Seq()),
          ))
        ),
      ),
      links = SeqMap(
        "link" -> Link.Link(
          selfClass = "link",
          ports = SeqMap(
            "source" -> Port.Port(selfClass = "sourcePort"),
            "sink" -> Port.Port(selfClass = "sinkPort"),
          )
        ),
        "mixinLink" -> Link.Link(
          selfClass = "link",
          ports = SeqMap(
            "source" -> Port.Port(selfClass = "sourcePort"),
            "sink" -> Port.Port(selfClass = "sinkPort"),
          )
        ),
      ),
      constraints = SeqMap(
        "sourceConnect" -> Constraint.Connected(Ref("source", "port"), Ref("link", "source")),
        "sinkConnect" -> Constraint.Connected(Ref("block", "port"), Ref("link", "sink")),
        "mixinSourceConnect" -> Constraint.Connected(Ref("mixinSource", "port"), Ref("mixinLink", "source")),
        "mixinSinkConnect" -> Constraint.Connected(Ref("block", "mixinPort"), Ref("mixinLink", "sink")),
      )
    ))

    val (compiler, compiled) = testCompile(
      inputDesign,
      library,
      refinements = Refinements(
        instanceRefinements = Map(DesignPath() + "block" -> LibraryPath("concreteMixinBlock")),
      ),
      expectedDesign = Some(referenceElaborated)
    )
    compiler.getValue(IndirectDesignPath() + "block" + "port" + IndirectStep.IsConnected) should
      equal(Some(BooleanValue(true)))
    compiler.getValue(IndirectDesignPath() + "block" + "mixinPort" + IndirectStep.IsConnected) should
      equal(Some(BooleanValue(true)))
  }

  "Compiler on design without mixin in base" should "expand blocks" in {
    val inputDesign = Design(Block.Block(
      "topDesign",
      blocks = SeqMap(
        "source" -> Block.Library("sourceBlock"),
        "block" -> Block.Library("baseBlock"), // no mixin, mixinPort is disconnected
      ),
      links = SeqMap(
        "link" -> Link.Library("link"),
      ),
      constraints = SeqMap(
        "sourceConnect" -> Constraint.Connected(Ref("source", "port"), Ref("link", "source")),
        "sinkConnect" -> Constraint.Connected(Ref("block", "port"), Ref("link", "sink")),
      )
    ))

    val referenceElaborated = Design(Block.Block(
      "topDesign",
      blocks = SeqMap(
        "source" -> Block.Block(
          selfClass = "sourceBlock",
          ports = SeqMap(
            "port" -> Port.Port(selfClass = "sourcePort"),
          )
        ),
        "block" -> Block.Block(
          superclasses = Seq("baseBlock", "mixin"),
          prerefine = "baseBlock",
          selfClass = "concreteMixinBlock",
          ports = SeqMap(
            "port" -> Port.Port(selfClass = "sinkPort"),
            "mixinPort" -> Port.Port(selfClass = "sinkPort"),
          ),
          meta = Some(Map(
            "refinedNewPorts" -> EdgirUtils.strSeqToMeta(Seq("mixinPort")),
            "refinedNewParams" -> EdgirUtils.strSeqToMeta(Seq()),
          ))
        ),
      ),
      links = SeqMap(
        "link" -> Link.Link(
          selfClass = "link",
          ports = SeqMap(
            "source" -> Port.Port(selfClass = "sourcePort"),
            "sink" -> Port.Port(selfClass = "sinkPort"),
          )
        ),
      ),
      constraints = SeqMap(
        "sourceConnect" -> Constraint.Connected(Ref("source", "port"), Ref("link", "source")),
        "sinkConnect" -> Constraint.Connected(Ref("block", "port"), Ref("link", "sink")),
      )
    ))

    val (compiler, compiled) = testCompile(
      inputDesign,
      library,
      refinements = Refinements(
        instanceRefinements = Map(DesignPath() + "block" -> LibraryPath("concreteMixinBlock")),
      ),
      expectedDesign = Some(referenceElaborated)
    )
    compiler.getValue(IndirectDesignPath() + "block" + "port" + IndirectStep.IsConnected) should
      equal(Some(BooleanValue(true)))
    compiler.getValue(IndirectDesignPath() + "block" + "mixinPort" + IndirectStep.IsConnected) should
      equal(Some(BooleanValue(false)))
  }

  "Compiler on design with multiple overlapping mixin and refined" should "expand blocks" in {
    // the abstract block has both mixin and mixinSub which have overlapping port names
    // this checks that IsConnected is properly defined and there are no errors
    val inputDesign = Design(Block.Block(
      "topDesign",
      blocks = SeqMap(
        "source" -> Block.Library("sourceBlock"),
        "mixinSource" -> Block.Library("sourceBlock"),
        "block" -> Block.Library("baseBlock", mixins = Seq("mixin", "mixinSub")),
      ),
    ))

    val (compiler, compiled) = testCompile(
      inputDesign,
      library,
      refinements = Refinements(
        instanceRefinements = Map(DesignPath() + "block" -> LibraryPath("concreteMixinSubBlock")),
      ),
    )
    compiler.getValue(IndirectDesignPath() + "block" + "port" + IndirectStep.IsConnected) should
      equal(Some(BooleanValue(false)))
    compiler.getValue(IndirectDesignPath() + "block" + "mixinPort" + IndirectStep.IsConnected) should
      equal(Some(BooleanValue(false)))
    compiler.getValue(IndirectDesignPath() + "block" + "mixinSubPort" + IndirectStep.IsConnected) should
      equal(Some(BooleanValue(false)))
  }
}
