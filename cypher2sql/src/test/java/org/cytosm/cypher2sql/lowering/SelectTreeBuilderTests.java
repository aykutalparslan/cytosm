package org.cytosm.cypher2sql.lowering;

import org.cytosm.cypher2sql.PassAvailables;
import org.cytosm.cypher2sql.lowering.sqltree.SimpleSelect;
import org.cytosm.cypher2sql.lowering.sqltree.SimpleSelectWithInnerJoins;
import org.cytosm.cypher2sql.lowering.sqltree.SimpleSelectWithLeftJoins;
import org.cytosm.cypher2sql.lowering.sqltree.ScopeSelect;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 */
public class SelectTreeBuilderTests {

    @Test
    public void testStructure() {
        String cypher = """
                MATCH (a)
                MATCH (a)
                RETURN a.firstName;""";
        ScopeSelect tree = PassAvailables.buildQueryTree(cypher);
        Assertions.assertEquals(tree.ret.limit, -1);
        Assertions.assertEquals(tree.ret.skip, -1);
        Assertions.assertTrue(tree.ret.orderBy.isEmpty());
        Assertions.assertNull(tree.ret.whereCondition);
        Assertions.assertEquals(tree.withQueries.size(), 2);
        Assertions.assertEquals(tree.withQueries.get(0).varId, tree.withQueries.get(0).subquery.varId);
        Assertions.assertEquals(tree.withQueries.get(1).varId, tree.withQueries.get(1).subquery.varId);
        Assertions.assertNotNull(tree.withQueries.get(0).subquery.varId);
        Assertions.assertNotNull(tree.withQueries.get(1).subquery.varId);
        Assertions.assertNotNull(tree.ret.varId);
    }

    @Test
    public void testSkipRet() {
        String cypher = "MATCH (a) RETURN a.firstName SKIP 2 + 4*10";
        ScopeSelect tree = PassAvailables.buildQueryTree(cypher);
        Assertions.assertEquals(tree.ret.skip, 42);
    }

    @Test
    public void testSkipWith() {
        String cypher = "MATCH (a) WITH a SKIP 2 + 4*10 RETURN a";
        ScopeSelect tree = PassAvailables.buildQueryTree(cypher);
        Assertions.assertEquals(((SimpleSelect) tree.withQueries.get(1).subquery).skip, 42);
    }

    @Test
    public void testLimitRet() {
        String cypher = "MATCH (a) RETURN a.firstName LIMIT 2 + 4*10";
        ScopeSelect tree = PassAvailables.buildQueryTree(cypher);
        Assertions.assertEquals(tree.ret.limit, 42);
    }

    @Test
    public void testLimitWith() {
        String cypher = "MATCH (a) WITH a.firstName AS afirstName LIMIT 2 + 4*10 RETURN 50";
        ScopeSelect tree = PassAvailables.buildQueryTree(cypher);
        Assertions.assertEquals(((SimpleSelect) tree.withQueries.get(1).subquery).limit, 42);
    }

    @Test
    public void testOrderByASC() {
        String cypher = "MATCH (a) RETURN a.firstName ORDER BY a.firstName ASC";
        ScopeSelect tree = PassAvailables.buildQueryTree(cypher);
        Assertions.assertFalse(tree.ret.orderBy.get(0).descending);
    }

    @Test
    public void testOrderByDESC() {
        String cypher = "MATCH (a) RETURN a.firstName ORDER BY a.firstName DESC";
        ScopeSelect tree = PassAvailables.buildQueryTree(cypher);
        Assertions.assertTrue(tree.ret.orderBy.get(0).descending);
    }

    @Test
    public void testOptionalMatchNormalMatch() {
        String cypher = """
                MATCH (a:Person {id: 0})
                OPTIONAL MATCH (a)-[:KNOWS]-(b:Person)
                RETURN a.firstName, b.firstName""";
        ScopeSelect tree = PassAvailables.buildQueryTree(cypher);
        Assertions.assertEquals(tree.withQueries.size(), 2);
        Assertions.assertInstanceOf(SimpleSelectWithInnerJoins.class, tree.withQueries.get(0).subquery);
        Assertions.assertInstanceOf(SimpleSelectWithLeftJoins.class, tree.withQueries.get(1).subquery);
    }
}
