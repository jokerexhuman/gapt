package gapt.examples

import gapt.expr._
import gapt.proofs.Sequent
import gapt.proofs.context.Context
import gapt.proofs.context.update.InductiveType
import gapt.proofs.context.update.{PrimitiveRecursiveFunction => PrimRecFun}
import gapt.proofs.context.update.ProofDefinitionDeclaration
import gapt.proofs.context.update.ProofNameDeclaration
import gapt.proofs.context.update.Sort
import gapt.proofs.gaptic._

object VeryWeakLexicoPHPSchema extends TacticsProof {
  ctx += InductiveType("nat", hoc"0 : nat", hoc"s : nat>nat")
  ctx += Sort("i")
  ctx += hoc"f:i>i>nat"
  ctx += hoc"suc:i>i"
  ctx += hoc"z:i"
  ctx += hoc"E: nat>nat>o"
  ctx += hoc"LE: nat>nat>o"

  ctx += hoc"omega: nat>nat"
  ctx += hoc"phi: nat>nat"
  ctx += PrimRecFun(hoc"POR:nat>i>i>o", "POR 0 x y = E 0 (f x y) ", "POR (s n) x y = (E (s n) (f x y) ∨ POR n x y)")
  ctx += "LEDefinition" -> hos"POR(n,a,b) :- LE(f(a,b), s(n))"
  ctx += "LEDefinition2" -> hos"POR(n,a,b) :- LE(f(a,suc(b)), s(n))"
  ctx += "LEDefinition3" -> hos"POR(n,a,b) :- LE(f(suc(a),b), s(n))"
  ctx += "LEDefinitionb" -> hos"POR(n,suc(a),suc(b)) :- LE(f(a,b), s(n))"
  ctx += "LEDefinition2b" -> hos"POR(n,suc(a),suc(b)) :- LE(f(a,suc(b)), s(n))"
  ctx += "LEDefinition3b" -> hos"POR(n,suc(a),suc(b)) :- LE(f(suc(a),b), s(n))"
  ctx += "NumericTransitivity" -> hos"E(n,f(a,b)),E(n,f(suc(a),suc(b))) :- E(f(a,b), f(suc(a),suc(b)))"
  ctx += "NumericTransitivity2" -> hos"E(n,f(a,suc(b))),E(n,f(suc(a),suc(b))) :- E(f(a,suc(b)), f(suc(a),suc(b)))"
  ctx += "NumericTransitivity3" -> hos"E(n,f(suc(a),b)),E(n,f(suc(a),suc(b))) :- E(f(suc(a),b), f(suc(a),suc(b)))"

  ctx += "minimalElement" -> hos"LE(f(z,z),0) :- "

  ctx += "ordcon" -> hos"LE(f(a,b),s(n)) :- E(n,f(suc(a),b)),E(n,f(a,suc(b))),LE(f(a,b),n)"
  ctx += "ordcon2" -> hos"LE(f(a,suc(b)),s(n)) :- E(n,f(suc(a),b)),E(n,f(a,b)), LE(f(a,suc(b)),n)"
  ctx += "ordcon3" -> hos"LE(f(suc(a),b),s(n)) :- E(n,f(a,b)),E(n,f(a,suc(b))), LE(f(suc(a),b),n)"
  ctx += "ordcon4" -> hos"LE(f(a,b),s(n)) :- E(n,f(suc(a),suc(b))), LE(f(a,b),n)"
  ctx += "ordcon5" -> hos"LE(f(a,suc(b)),s(n)) :- E(n,f(suc(a),suc(b))), LE(f(a,suc(b)),n)"
  ctx += "ordcon6" -> hos"LE(f(suc(a),b),s(n)) :- E(n,f(suc(a),suc(b))), LE(f(suc(a),b),n)"

  val esOmega = Sequent(
    Seq(hof"!x !y POR(n,x,y)"),
    Seq(hof"?x ?y ( E(f(x,y), f(suc(x),suc(y))) | E(f(x,suc(y)), f(suc(x),suc(y))) | E(f(suc(x),y), f(suc(x),suc(y))))")
  )
  ctx += ProofNameDeclaration(le"omega n", esOmega)
  val esPhi = Sequent(
    Seq(hof"?x ?y ( (E(n,f(x,y)) & E(n,f(suc(x),suc(y)))) | (E(n,f(x,suc(y))) & E(n,f(suc(x),suc(y)))) | (E(n,f(suc(x),y)) & E(n,f(suc(x),suc(y)))) ) | !x !y (LE(f(x,y),n) & LE(f(x,suc(y)),n) & LE(f(suc(x),y),n))"),
    Seq(hof"?x ?y ( E(f(x,y), f(suc(x),suc(y))) | E(f(x,suc(y)), f(suc(x),suc(y))) | E(f(suc(x),y), f(suc(x),suc(y))))")
  )
  ctx += ProofNameDeclaration(le"phi n", esPhi)
  // The base case of  omega
  val esOmegaBc =
    Sequent(
      Seq("Ant_0" -> hof"!x !y POR(0,x,y)"),
      Seq("Suc_0" -> hof"?x ?y ( E(f(x,y), f(suc(x),suc(y))) | E(f(x,suc(y)), f(suc(x),suc(y))) | E(f(suc(x),y), f(suc(x),suc(y))))")
    )
  val omegaBc = Lemma(esOmegaBc) {
    cut(
      "cut",
      hof"?x ?y ( (E(0,f(x,y)) & E(0,f(suc(x),suc(y)))) | (E(0,f(x,suc(y))) & E(0,f(suc(x),suc(y)))) | (E(0,f(suc(x),y)) & E(0,f(suc(x),suc(y)))) ) | !x !y (LE(f(x,y),0) & LE(f(x,suc(y)),0) & LE(f(suc(x),y),0))"
    )
    forget("Suc_0")
    orR
    allR(fov"a")
    allR(fov"b")
    exR("cut_0", fov"a")
    exR("cut_0_0", fov"b")
    orR
    orR
    allL(le"(suc a)")
    allL("Ant_0_0", le"(suc b)")
    allL("Ant_0_0", fov"b")
    unfold("POR") atMost 1 in "Ant_0_0_0"
    unfold("POR") atMost 1 in "Ant_0_0_1"
    andR("cut_0_0_0_1")
    trivial
    trivial
    orL
    exL(fov"a")
    exL(fov"b")
    exR(fov"a")
    exR("Suc_0_0", fov"b")
    orR
    orR
    orL
    orL
    andL
    ref("NumericTransitivity")
    andL
    ref("NumericTransitivity2")
    andL
    ref("NumericTransitivity3")
    allL("cut", le"z")
    allL("cut_0", le"z")
    andL
    andL
    ref("minimalElement")
  }
  ctx += ProofDefinitionDeclaration(le"omega 0", omegaBc)

  // The step case of  omega
  val esOmegaSc =
    Sequent(
      Seq("Ant_0" -> hof"!x !y POR(s(n),x,y)"),
      Seq("Suc_0" -> hof"?x ?y ( E(f(x,y), f(suc(x),suc(y))) | E(f(x,suc(y)), f(suc(x),suc(y))) | E(f(suc(x),y), f(suc(x),suc(y))))")
    )
  val omegaSc = Lemma(esOmegaSc) {
    cut(
      "cut",
      hof"?x ?y ( (E(s(n),f(x,y)) & E(s(n),f(suc(x),suc(y)))) | (E(s(n),f(x,suc(y))) & E(s(n),f(suc(x),suc(y)))) | (E(s(n),f(suc(x),y)) & E(s(n),f(suc(x),suc(y)))) ) | !x !y (LE(f(x,y),s(n)) & LE(f(x,suc(y)),s(n)) & LE(f(suc(x),y),s(n)))"
    )
    forget("Suc_0")
    orR
    allR(fov"a")
    allR(fov"b")
    exR("cut_0", fov"a")
    exR("cut_0_0", fov"b")
    orR
    orR
    allL(le"(suc a)")
    allL("Ant_0_0", le"(suc b)")
    allL("Ant_0", fov"a")
    allL("Ant_0_1", fov"b")

    unfold("POR") atMost 1 in "Ant_0_0_0"
    unfold("POR") atMost 1 in "Ant_0_1_0"
    andR("cut_0_0_0_0_0")
    orL("Ant_0_1_0")
    trivial
    andR("cut_1")
    andR("cut_1")
    ref("LEDefinition")
    ref("LEDefinition2")
    ref("LEDefinition3")
    orL("Ant_0_0_0")
    trivial
    andR("cut_1")
    andR("cut_1")
    ref("LEDefinitionb")
    ref("LEDefinition2b")
    ref("LEDefinition3b")
    ref("phi")
  }
  ctx += ProofDefinitionDeclaration(le"omega (s n)", omegaSc)

  val esPhiBc =
    Sequent(
      Seq("Ant_0" -> hof"?x ?y ( (E(0,f(x,y)) & E(0,f(suc(x),suc(y)))) | (E(0,f(x,suc(y))) & E(0,f(suc(x),suc(y)))) | (E(0,f(suc(x),y)) & E(0,f(suc(x),suc(y)))) ) | !x !y (LE(f(x,y),0) & LE(f(x,suc(y)),0) & LE(f(suc(x),y),0))"),
      Seq("Suc_0" -> hof"?x ?y ( E(f(x,y), f(suc(x),suc(y))) | E(f(x,suc(y)), f(suc(x),suc(y))) | E(f(suc(x),y), f(suc(x),suc(y))))")
    )
  val phiBc = Lemma(esPhiBc) {
    orL
    exL(fov"a")
    exL(fov"b")
    exR("Suc_0", fov"a")
    exR("Suc_0_0", fov"b")
    orR
    orR
    orL
    orL
    andL
    ref("NumericTransitivity")
    andL
    ref("NumericTransitivity2")
    andL
    ref("NumericTransitivity3")
    allL(le"z")
    allL("Ant_0_0", le"z")
    andL
    andL
    ref("minimalElement")
  }
  ctx += ProofDefinitionDeclaration(le"phi 0", phiBc)

  val esPhiSc =
    Sequent(
      Seq("Ant_0" -> hof"?x ?y ( (E(s(n),f(x,y)) & E(s(n),f(suc(x),suc(y)))) | (E(s(n),f(x,suc(y))) & E(s(n),f(suc(x),suc(y)))) | (E(s(n),f(suc(x),y)) & E(s(n),f(suc(x),suc(y)))) ) | !x !y (LE(f(x,y),s(n)) & LE(f(x,suc(y)),s(n)) & LE(f(suc(x),y),s(n)))"),
      Seq("Suc_0" -> hof"?x ?y ( E(f(x,y), f(suc(x),suc(y))) | E(f(x,suc(y)), f(suc(x),suc(y))) | E(f(suc(x),y), f(suc(x),suc(y))))")
    )
  val phiSc = Lemma(esPhiSc) {
    cut(
      "cut",
      hof"?x ?y ( (E(n,f(x,y)) & E(n,f(suc(x),suc(y)))) | (E(n,f(x,suc(y))) & E(n,f(suc(x),suc(y)))) | (E(n,f(suc(x),y)) & E(n,f(suc(x),suc(y)))) ) | !x !y (LE(f(x,y),n) & LE(f(x,suc(y)),n) & LE(f(suc(x),y),n))"
    )
    orR
    orL
    exL(fov"a")
    exL(fov"b")
    exR("Suc_0", fov"a")
    exR("Suc_0_0", fov"b")
    orR
    orR
    orL
    orL
    andL
    ref("NumericTransitivity")
    andL
    ref("NumericTransitivity2")
    andL
    ref("NumericTransitivity3")
    allR(fov"a")
    allR(fov"b")
    allL("Ant_0", fov"a")
    allL("Ant_0_0", fov"b")
    andL
    andL
    exR("cut_0", fov"a")
    exR("cut_0_0", fov"b")
    orR
    orR
    forget("cut_0_0")
    forget("cut_0")
    forget("Suc_0")
    forget("Ant_0")
    forget("Ant_0_0")
    andR("cut_0_0_0_1")
    andR("cut_0_0_0_0_0")
    andR("cut_0_0_0_0_1")
    andR("cut_1")
    andR("cut_1")
    ref("ordcon")
    ref("ordcon2")
    ref("ordcon3")
    andR("cut_1")
    andR("cut_1")
    ref("ordcon4")
    ref("ordcon5")
    ref("ordcon6")
    andR("cut_1")
    andR("cut_1")
    ref("ordcon4")
    ref("ordcon5")
    ref("ordcon6")
    andR("cut_1")
    andR("cut_1")
    ref("ordcon4")
    ref("ordcon5")
    ref("ordcon6")
    ref("phi")
  }
  ctx += ProofDefinitionDeclaration(le"phi (s n)", phiSc)
}
