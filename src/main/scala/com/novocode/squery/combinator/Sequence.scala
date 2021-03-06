package com.novocode.squery.combinator

import com.novocode.squery.combinator.basic.BasicProfile

class Sequence[T : TypeMapper] private[Sequence] (val name: String,
    val _minValue: Option[T],
    val _maxValue: Option[T],
    val _increment: Option[T],
    val _start: Option[T],
    val _cycle: Boolean) { seq =>

  def min(v: T) = new Sequence[T](name, Some(v), _maxValue, _increment, _start, _cycle)
  def max(v: T) = new Sequence[T](name, _minValue, Some(v), _increment, _start, _cycle)
  def inc(v: T) = new Sequence[T](name, _minValue, _maxValue, Some(v), _start, _cycle)
  def start(v: T) = new Sequence[T](name, _minValue, _maxValue, _increment, Some(v), _cycle)
  def cycle = new Sequence[T](name, _minValue, _maxValue, _increment, _start, true)

  final object next extends OperatorColumn[T] with SimpleFunction with UnaryNode {
    val name = "nextval"
    val child = ConstColumn(seq.name)(TypeMapper.StringTypeMapper)
  }

  final object curr extends OperatorColumn[T] with SimpleFunction with UnaryNode {
    val name = "currval"
    val child = ConstColumn(seq.name)(TypeMapper.StringTypeMapper)
  }

  def ddl(implicit profile: BasicProfile): DDL = profile.buildSequenceDDL(this)
}

object Sequence {
  def apply[T : TypeMapper](name: String) = new Sequence[T](name, None, None, None, None, false)
}
