package edg.compiler

import edg.util.{Errorable, StreamUtils}
import edgrpc.compiler.{compiler => edgcompiler}
import edgrpc.compiler.compiler.{CompilerRequest, CompilerResult}
import edgrpc.hdl.{hdl => edgrpc}
import edg.wir.{DesignPath, IndirectDesignPath, Refinements}
import edgir.elem.elem
import edgir.ref.ref
import edgir.schema.schema

import java.io.{File, PrintWriter, StringWriter}

// a PythonInterface that uses the on-event hooks to forward stderr and stdout
// without this, the compiler can freeze on large stdout/stderr data, possibly because of queue sizing
class ForwardingPythonInterface(pythonPaths: Seq[String] = Seq())
    extends PythonInterface(pythonPaths = pythonPaths) {
  def forwardProcessOutput(): Unit = {
    StreamUtils.forAvailable(processOutputStream) { data =>
      System.out.print(new String(data))
      System.out.flush()
    }
    StreamUtils.forAvailable(processErrorStream) { data =>
      System.err.print(new String(data))
      System.err.flush()
    }
  }

  override protected def onLibraryRequestComplete(
      element: ref.LibraryPath,
      result: Errorable[(schema.Library.NS.Val, Option[edgrpc.Refinements])]
  ): Unit = {
    forwardProcessOutput()
  }

  override protected def onElaborateGeneratorRequestComplete(
      element: ref.LibraryPath,
      values: Map[ref.LocalPath, ExprValue],
      result: Errorable[elem.HierarchyBlock]
  ): Unit = {
    forwardProcessOutput()
  }

  override protected def onRunBackendComplete(
      backend: ref.LibraryPath,
      result: Errorable[Map[DesignPath, String]]
  ): Unit = {
    forwardProcessOutput()
  }
}

/** A Scala-based EDG compiler interface as a server. Because this uses a persistent HdlInterfaceService, this should
  * not be used where HDL changes are expected during its lifetime. This is suitable for unit tests, but not for
  * long-term user interaction.
  */
object CompilerServerMain {
  final val kHdlVersionMismatchDelayMs = 2000

  private def constPropToSolved(vals: Map[IndirectDesignPath, ExprValue]): Seq[edgcompiler.CompilerResult.Value] = {
    vals.map { case (path, value) =>
      edgcompiler.CompilerResult.Value(
        path = Some(path.toLocalPath),
        value = Some(value.toLit)
      )
    }.toSeq
  }

  def compile(request: CompilerRequest, library: PythonInterfaceLibrary): CompilerResult = {
    try {
      val refinements = Refinements(request.getRefinements)
      val compiler = new Compiler(request.getDesign, library, refinements)
      val compiled = compiler.compile()
      val errors = compiler.getErrors() ++ new DesignAssertionCheck(compiler).map(compiled) ++
        new DesignStructuralValidate().map(compiled) ++ new DesignRefsValidate().validate(compiled)
      val result = edgcompiler.CompilerResult(
        design = Some(compiled),
        errors = errors.map(_.toIr),
        solvedValues = constPropToSolved(compiler.getAllSolved)
      )
      result
    } catch {
      case e: Throwable =>
        val sw = new StringWriter()
        e.printStackTrace(new PrintWriter(sw))
        edgcompiler.CompilerResult(errors =
          Seq(edgcompiler.ErrorRecord(
            path = Some(DesignPath().asIndirect.toLocalPath),
            kind = "Internal error",
            name = "",
            details = sw.toString
          ))
        )
    }
  }

  def main(args: Array[String]): Unit = {
    val pyIf = new ForwardingPythonInterface()
    (pyIf.getProtoVersion() match {
      case Errorable.Success(pyVersion) if pyVersion == Compiler.kExpectedProtoVersion => None
      case Errorable.Success(pyMismatchVersion) => Some(pyMismatchVersion.toString)
      case Errorable.Error(errMsg) => Some(s"error $errMsg")
    }).foreach { pyMismatchVersion =>
      System.err.println(f"WARNING: Python / compiler version mismatch, Python reported $pyMismatchVersion, " +
        f"expected ${Compiler.kExpectedProtoVersion}.")
      System.err.println(f"If you get unexpected errors or results, consider updating the Python library or compiler.")
      Thread.sleep(kHdlVersionMismatchDelayMs)
    }

    val pyLib = new PythonInterfaceLibrary()
    pyLib.withPythonInterface(pyIf) {
      while (true) {
        val expectedMagicByte = System.in.read()
        require(expectedMagicByte == ProtobufStdioSubprocess.kHeaderMagicByte || expectedMagicByte < 0)

        val request = edgcompiler.CompilerRequest.parseDelimitedFrom(System.in)
        val result = request match {
          case Some(request) =>
            compile(request, pyLib)
          case None => // end of stream
            System.exit(0)
            throw new NotImplementedError() // provides a return type, shouldn't ever happen
        }

        pyIf.forwardProcessOutput() // in case the hooks didn't catch everything

        System.out.write(ProtobufStdioSubprocess.kHeaderMagicByte)
        result.writeDelimitedTo(System.out)
        System.out.flush()
      }
    }
  }
}
