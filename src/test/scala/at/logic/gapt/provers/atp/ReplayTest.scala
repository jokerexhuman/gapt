/*
 * Tests for the prover9 interface.
**/

package at.logic.gapt.provers.atp

import at.logic.gapt.expr._
import at.logic.gapt.formats.prover9.Prover9TermParser.parseFormula
import at.logic.gapt.formats.readers.StringReader
import at.logic.gapt.formats.simple.SimpleFOLParser
import at.logic.gapt.proofs.lk.base.RichOccSequent
import at.logic.gapt.proofs.occurrences.factory
import at.logic.gapt.proofs.resolutionOld.robinson.RobinsonResolutionProof
import at.logic.gapt.proofs.resolutionOld.{ OccClause, ResolutionProof }
import at.logic.gapt.proofs.{ FOLClause, HOLSequent }
import at.logic.gapt.provers.atp.commands.Prover9InitCommand
import at.logic.gapt.provers.atp.commands.base.SetStreamCommand
import at.logic.gapt.provers.atp.commands.sequents.SetTargetClause
import at.logic.gapt.provers.prover9.Prover9Prover
import org.specs2.mutable._

class ReplayTest extends Specification {
  def parse( str: String ): FOLFormula = ( new StringReader( str ) with SimpleFOLParser getTerm ).asInstanceOf[FOLFormula]

  implicit def fo2occ( f: FOLFormula ) = factory.createFormulaOccurrence( f, Nil )

  private object MyProver extends Prover[OccClause]

  def getRefutation( ls: Iterable[HOLSequent] ): Boolean = MyProver.refute( Stream( SetTargetClause( HOLSequent( List(), List() ) ), Prover9InitCommand( ls ), SetStreamCommand() ) ).next must beLike {
    case Some( a ) if a.asInstanceOf[ResolutionProof[OccClause]].root syntacticMultisetEquals ( HOLSequent( List(), List() ) ) => ok
    case _ => ko
  }

  def getRefutation2( ls: Iterable[HOLSequent] ) = MyProver.refute( Stream( SetTargetClause( HOLSequent( List(), List() ) ), Prover9InitCommand( ls ), SetStreamCommand() ) ).next

  args( skipAll = !new Prover9Prover().isInstalled )
  "replay" should {
    /*"prove (with para) SKKx = Ix : { :- f(a,x) = x; :- f(f(f(b,x),y),z) = f(f(x,z), f(y,z)); :- f(f(c,x),y) = x; f(f(f(b,c),c),x) = f(a,x) :- }" in {

      //checks, if the execution of prover9 works, o.w. skip test
      new Prover9Prover().getRobinsonProof(box ) must not(throwA[IOException]).orSkip


      val i = parse("=(f(a,x),x)")
      val s = parse("=(f(f(f(b,x),y),z), f(f(x,z), f(y,z)))")
      val k = parse("=(f(f(c,x),y), x)")
      val skk_i = parse("=(f(f(f(b,c),c),x), f(a,x))")

      val s1 = (Nil, List(i))
      val s2 = (Nil, List(k))
      val s3 = (Nil, List(s))
      val t1 = (List(skk_i),Nil)
      getRefutation(List(s1,s2,s3,t1)) must beTrue
    }

    "prove 0) :- p(a) , (1) p(x) :- p(f(x)) , (2) p(f(f(a))) :- " in {
      val pa = parse("P(a)")
      val px = parse("P(x)")
      val pfx = parse("P(f(x))")
      val pffa = parse("P(f(f(a)))")
      val s1 = (Nil, List(pa))
      val s2 = (List(px),List(pfx))
      val s3 = (List(pffa),Nil)
      getRefutation(List(s1,s2,s3)) must beTrue
    }

    "prove 0) :- p(a) , (1) p(x), p(y) :- p(f(x)) , (2) p(f(f(a))) :- " in {
      val pa = parse("P(a)")
      val px = parse("P(x)")
      val py = parse("P(y)")
      val pfx = parse("P(f(x))")
      val pffa = parse("P(f(f(a)))")
      val s1 = (Nil, List(pa))
      val s2 = (List(px,py),List(pfx))
      val s3 = (List(pffa),Nil)
      getRefutation(List(s1,s2,s3)) must beTrue
    }

    "prove (with factor and copy/merge) 0) :- p(a) , (1) p(x), p(y) :- p(f(x)), p(f(y)) , (2) p(f(f(a))) :- " in {
      val pa = parse("P(a)")
      val px = parse("P(x)")
      val py = parse("P(y)")
      val pfx = parse("P(f(x))")
      val pfy = parse("P(f(y))")
      val pffa = parse("P(f(f(a)))")
      val s1 = (Nil, List(pa))
      val s2 = (List(px,py),List(pfx,pfy))
      val s3 = (List(pffa),Nil)
      getRefutation(List(s1,s2,s3)) must beTrue
    }
    "prove (with xx - 1) 0) :- p(a) , (1) z = z, p(x), p(y) :- p(f(x)), p(f(y)) , (2) p(f(f(a))) :- " in {
      val pa = parse("P(a)")
      val px = parse("P(x)")
      val py = parse("P(y)")
      val zz = parse("=(z,z)")
      val pfx = parse("P(f(x))")
      val pfy = parse("P(f(y))")
      val pffa = parse("P(f(f(a)))")
      val s1 = (Nil, List(pa))
      val s2 = (List(zz,px,py),List(pfx,pfy))
      val s3 = (List(pffa),Nil)
      getRefutation(List(s1,s2,s3)) must beTrue
    }
    "prove (with xx - 2) P(f(a)). -=(z,z) | -P(x) | -P(y) | P(f(x)) | P(f(y)). -P(f(f(a)))." in {
      val pfa = parse("P(f(a))")
      val px = parse("P(x)")
      val py = parse("P(y)")
      val zz = parse("=(z,z)")
      val pfx = parse("P(f(x))")
      val pfy = parse("P(f(y))")
      val pffa = parse("P(f(f(a)))")
      val s1 = (Nil, List(pfa))
      val s2 = (List(zz,px,py),List(pfx,pfy))
      val s3 = (List(pffa),Nil)
      (getRefutation2(List(s1,s2,s3)) match {
        case Some(a) if a.asInstanceOf[ResolutionProof[Clause]].toTreeProof.root syntacticMultisetEquals (List(),List()) => true
        case _ => false
      }) must beTrue
    } */

    "work on the tape-in clause set" in {
      val formulas = List(
        "f(X+Y)=0",
        "f(Y+X)=1",
        "f(X + Z0) = 0",
        "f(((X + Z0) + 1) + Z1) = 0",
        "f(X + Z0) = 1",
        "f(((X + Z0) + 1) + Z1) = 1"
      ).map( parseFormula )

      val c1 = HOLSequent( Nil, List( formulas( 0 ), formulas( 1 ) ) )
      val c2 = HOLSequent( List( formulas( 2 ), formulas( 3 ) ), Nil )
      val c3 = HOLSequent( List( formulas( 4 ), formulas( 5 ) ), Nil )

      val ls = List( c1, c2, c3 )

      val prover = new Prover[OccClause] {}

      prover.refute( Stream(
        SetTargetClause( HOLSequent( List(), List() ) ),
        Prover9InitCommand( ls ),
        SetStreamCommand()
      ) ).next must beLike {
        case Some( a ) if a.asInstanceOf[ResolutionProof[OccClause]].root syntacticMultisetEquals ( HOLSequent( List(), List() ) ) =>
          ok
        case _ =>
          ko
      }
    }

    "prove (with xx - 3) -=(a,a) | -=(a,a)." in {
      val eaa = parse( "=(a,a)" )
      val s = HOLSequent( List( eaa, eaa ), Nil )
      ( getRefutation2( List( s ) ) match {
        case Some( a ) if a.asInstanceOf[ResolutionProof[OccClause]].root.toHOLSequent multiSetEquals ( HOLSequent( List(), List() ) ) => true
        case _ => false
      } ) must beTrue
    }

    "prove an example from the automated deduction exercises" in {
      skipped( "never worked" )

      /* loops at derivation of clause 7:
        <clause id="7">
          <literal>
          ladr3(ladr2,A) = ladr3(B,ladr3(B,A))
          </literal>
          <justification jstring="[para(4(a,1),2(a,1,1))].">
          <j1 parents="4 2" rule="para"></j1>
          </justification>
        </clause>
       */

      //println("=======AD Example: =======")
      val assoc = parse( "=(*(x,*(y,z)), *(*(x,y),z) )" )
      val neutr = parse( "=(*(x,e), x)" )
      val idem = parse( "=(*(x,x), e)" )
      val comm = parse( "=(*(x,y), *(y,x))" )
      val ncomm = parse( "=(*(c1,c2), *(c2,c1))" )
      val s1 = HOLSequent( Nil, List( assoc ) )
      val s2 = HOLSequent( Nil, List( neutr ) )
      val s3 = HOLSequent( Nil, List( idem ) )
      val s4 = HOLSequent( List( ncomm ), Nil )
      ( getRefutation2( List( s1, s2, s3, s4 ) ) match {
        case Some( a ) if a.asInstanceOf[RobinsonResolutionProof].root.toHOLSequent multiSetEquals ( HOLSequent( List(), List() ) ) =>
          //println(Formatter.asHumanReadableString(a)   )
          //println("======= GraphViz output: =======" + sys.props("line.separator") + Formatter.asGraphViz(a)   )
          true
        case _ => false
      } ) must beTrue
    }

    "refute { :- P; P :- }" in {
      val p = FOLAtom( "P", Nil )
      val s1 = FOLClause( Nil, p :: Nil )
      val s2 = FOLClause( p :: Nil, Nil )
      val result = new Prover9Prover().getRobinsonProof( s1 :: s2 :: Nil )
      result match {
        case Some( proof ) =>
          //println(Formatter.asHumanReadableString(proof))
          true must beEqualTo( true )
        case None => "" must beEqualTo( "Refutation failed!" )
      }

    }

    "find a refutation for a simple clause set " in {
      //println("==== SIMPLE EXAMPLE ====")
      val f_eq_g = parse( "=(f(x),g(x))" )
      val px = parse( "P(x)" )
      val pfx = parse( "P(f(x))" )
      val pa = parse( "P(a)" )
      val goal = parse( "P(g(a))" )

      val s1 = HOLSequent( Nil, List( f_eq_g ) )
      val s2 = HOLSequent( List( px ), List( pfx ) )
      val s3 = HOLSequent( Nil, List( pa ) )
      val t1 = HOLSequent( List( goal ), Nil )
      //println(TPTPFOLExporter.tptp_problem(List(s1,s2,s3,t1)))
      //println()
      val Some( result ) = getRefutation2( List( s1, s2, s3, t1 ) )
      //println(result)

      //println(Formatter.asTex(result))

      true must beEqualTo( true )
    }
  }

}