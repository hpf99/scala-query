package com.novocode.squery

import java.sql.{PreparedStatement, ResultSet}
import com.novocode.squery.session._

/**
 * An invoker which calls a function to retrieve a ResultSet. This can be used
 * for reading information from a java.sql.DatabaseMetaData object which has
 * many methods that return ResultSets.
 */
abstract class ResultSetInvoker[+R] extends UnitInvokerMixin[R] { self =>

  protected def createResultSet(session: Session): ResultSet

  protected def extractValue(rs: PositionedResult): R

  def foreach(param: Unit, f: R => Unit, maxRows: Int)(implicit session: Session) {
    val rs = createPR(session)
    try {
      var count = 0
      while(rs.next && (maxRows == 0 || count < maxRows)) {
        f(extractValue(rs))
        count += 1
      }
    } finally { rs.close() }
  }

  def elements(param: Unit)(implicit session: Session): CloseableIterator[R] = {
    val rs = createPR(session)
    new ReadAheadIterator[R] with CloseableIterator[R] {
      def close() = rs.close()
      protected def fetchNext() = {
        if(rs.next) Some(extractValue(rs))
        else { close(); None }
      }
    }
  }

  private[this] def createPR(session: Session) =
    new PositionedResult(createResultSet(session)) { def close() = rs.close() }
}

object ResultSetInvoker {
  def apply[R](f: Session => ResultSet)(implicit conv: PositionedResult => R): UnitInvoker[R] = new ResultSetInvoker[R] {
    def createResultSet(session: Session) = f(session)
    def extractValue(pr: PositionedResult) = conv (pr)
  }
}
