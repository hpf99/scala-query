package com.novocode.squery.test

import org.junit.After
import org.junit.Test
import org.junit.Assert._
import com.novocode.squery.combinator._
import com.novocode.squery.combinator.TypeMapper._
import com.novocode.squery.combinator.extended.{ExtendedTable => Table, SQLiteDriver}
import com.novocode.squery.meta.MTable
import com.novocode.squery.session._
import com.novocode.squery.session.Database.threadLocalSession
import com.novocode.squery.test.util._
import com.novocode.squery.test.util.TestDB._

object ForeignKeyTest extends DBTestObject(H2Mem, Postgres, MySQL)

class ForeignKeyTest(tdb: TestDB) extends DBTest(tdb) {
  import tdb.driver.Implicit._

  @Test def test1(): Unit = db withSession {

    object Categories extends Table[(Int, String)]("categories") {
      def id = column[Int]("id", O PrimaryKey)
      def name = column[String]("name")
      def * = id ~ name
    }

    val Posts = new Table[(Int, String, Int)]("posts") {
      def id = column[Int]("id", O PrimaryKey, O AutoInc)
      def title = column[String]("title")
      def category = column[Int]("category")
      def * = id ~ title ~ category
      def categoryFK = foreignKey("category_fk", category, Categories)(_.id)
      def categoryJoin = Categories.where(_.id === category)
    }

    assertEquals(List(), tdb.getLocalTables)
    val ddl = Posts.ddl ++ Categories.ddl
    ddl.createStatements foreach println
    ddl create;
    assertEquals(List("categories", "posts"), tdb.getLocalTables)

    Categories insertAll (
      (1, "Scala"),
      (2, "ScalaQuery"),
      (3, "Windows"),
      (4, "Software")
    )
    Posts.title ~ Posts.category.? insertAll (
      ("Test Post", None),
      ("Formal Language Processing in Scala, Part 5", Some(1)),
      ("Efficient Parameterized Queries in ScalaQuery", Some(2)),
      ("Removing Libraries and HomeGroup icons from the Windows 7 desktop", Some(3)),
      ("A ScalaQuery Update", Some(2))
    )

    val q1 = for {
      p <- Posts
      c <- p.categoryJoin
      _ <- Query orderBy p.id
    } yield p.id ~ c.id ~ c.name ~ p.title
    q1.dump("Manual join: ")
    println("Manual join: "+q1.selectStatement)
    q1.foreach(x => println("  "+x))
    assertEquals(List((2,1), (3,2), (4,3), (5,2)), q1.map(p => p._1 ~ p._2).list)

    val q2 = for {
      p <- Posts
      c <- p.categoryFK
      _ <- Query orderBy p.id
    } yield p.id ~ c.id ~ c.name ~ p.title
    q2.dump("Foreign-key join: ")
    println("Foreign-key join: "+q2.selectStatement)
    q2.foreach(x => println("  "+x))
    assertEquals(List((2,1), (3,2), (4,3), (5,2)), q2.map(p => p._1 ~ p._2).list)

    val ddl2 = Categories.ddl ++ Posts.ddl
    ddl2.dropStatements foreach println
    ddl2 drop;
    assertEquals(List(), tdb.getLocalTables)
  }

  @Test def test2(): Unit = db withSession {

    object A extends Table[(Int, Int, String)]("a") {
      def k1 = column[Int]("k1")
      def k2 = column[Int]("k2")
      def s = column[String]("s")
      def * = k1 ~ k2 ~ s
      def bFK = foreignKey("b_fk", k1 ~ k2, B)(b => b.f1 ~ b.f2, onDelete = ForeignKeyAction.Cascade)
    }

    object B extends Table[(Int, Int, String)]("b") {
      def f1 = column[Int]("f1")
      def f2 = column[Int]("f2")
      def s = column[String]("s")
      def * = f1 ~ f2 ~ s
      def bIdx1 = index("b_idx1", f1 ~ f2, unique = true)
    }

    A.foreignKeys.foreach(println)
    assertEquals(Set("b_fk"), A.foreignKeys.map(_.name).toSet)

    (B.ddl ++ A.ddl) create

    B insertAll (
      (1, 2, "b12"),
      (3, 4, "b34"),
      (5, 6, "b56")
    )
    A insertAll (
      (1, 2, "a12"),
      (3, 4, "a34")
    )

    val q1 = for {
      a <- A
      b <- a.bFK
    } yield a.s ~ b.s
    println("Multiple rows: "+q1.selectStatement)
    q1.foreach(x => println("  "+x))
    assertEquals(Set(("a12","b12"), ("a34","b34")), q1.list.toSet)
  }
}
