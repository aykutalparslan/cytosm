package org.cytosm.cypher2sql.lowering;

import org.cytosm.cypher2sql.PassAvailables;
import org.cytosm.cypher2sql.cypher.ast.Statement;
import org.cytosm.cypher2sql.lowering.sqltree.ScopeSelect;
import org.cytosm.cypher2sql.lowering.sqltree.SimpleSelect;
import org.cytosm.cypher2sql.lowering.typeck.VarDependencies;
import org.cytosm.cypher2sql.lowering.typeck.var.NodeVar;
import org.cytosm.cypher2sql.lowering.typeck.var.TempVar;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 */
public class PopulateJoinsTests extends BaseLDBCTests {

    @Test
    public void testPopulateJoins() throws Exception {
        String cypher = """
                MATCH (a:Person {id: 0})
                OPTIONAL MATCH (a)<-[:KNOWS]-(b:Person)
                RETURN a.firstName, b.firstName""";

        ScopeSelect tree = fromBeginningUntilpopulateJoins(cypher);

        SimpleSelect optMatch = (SimpleSelect) tree.withQueries.get(1).subquery;
        Assertions.assertEquals(2, optMatch.joinList().size());
        Assertions.assertEquals(1, optMatch.fromItem.size());
        Assertions.assertEquals(1, optMatch.fromItem.get(0).variables.size());
        Assertions.assertEquals("person_knows_person", optMatch.joinList().get(0).joiningItem.sourceTableName);
        Assertions.assertEquals("Person", optMatch.joinList().get(1).joiningItem.sourceTableName);
        Assertions.assertEquals(1, optMatch.joinList().get(0).joiningItem.variables.size());
        Assertions.assertInstanceOf(TempVar.class, optMatch.joinList().get(0).joiningItem.variables.get(0));
        Assertions.assertEquals(1, optMatch.joinList().get(1).joiningItem.variables.size());
        Assertions.assertEquals("b", ((NodeVar) optMatch.joinList().get(1).joiningItem.variables.get(0)).name);
    }

    @Test
    public void testPopulateJoinsMultiRelations() throws Exception {
        String cypher = "" +
                "MATCH (a:Person)<-[:KNOWS]-(b:Person)<-[:KNOWS]-(c:Person)\n" +
                "RETURN a.firstName, b.firstName, c.firstName";
        ScopeSelect tree = fromBeginningUntilpopulateJoins(cypher);
        SimpleSelect match = (SimpleSelect) tree.withQueries.get(0).subquery;
        Assertions.assertEquals(4, match.joinList().size());
        Assertions.assertEquals(1, match.fromItem.size());
        Assertions.assertEquals(1, match.fromItem.get(0).variables.size());
        Assertions.assertEquals("person_knows_person", match.joinList().get(0).joiningItem.sourceTableName);
        Assertions.assertEquals("Person", match.joinList().get(1).joiningItem.sourceTableName);
        Assertions.assertEquals("person_knows_person", match.joinList().get(2).joiningItem.sourceTableName);
        Assertions.assertEquals("Person", match.joinList().get(3).joiningItem.sourceTableName);
    }

    @Test
    public void testPopulateJoinsReuseBothInFrom() throws Exception {
        String cypher = """
                MATCH (a:Person)<-[:KNOWS]-(b:Person)
                MATCH (a)-[:KNOWS]->(b)
                RETURN a.firstName, b.firstName""";

        ScopeSelect tree = fromBeginningUntilpopulateJoins(cypher);

        SimpleSelect match2 = (SimpleSelect) tree.withQueries.get(1).subquery;
        Assertions.assertEquals(1, match2.fromItem.size());
        Assertions.assertEquals(2, match2.fromItem.get(0).variables.size());
        Assertions.assertEquals(1, match2.joinList().size());
        Assertions.assertEquals("person_knows_person", match2.joinList().get(0).joiningItem.sourceTableName);
    }

    private ScopeSelect fromBeginningUntilpopulateJoins(String cypher) throws Exception {
        Statement st = PassAvailables.parseCypher(cypher);
        VarDependencies vars = new VarDependencies(st);
        ScopeSelect tree = SelectTreeBuilder.createQueryTree(vars, st);
        NameSubqueries.nameSubqueries(tree);
        ComputeFromItems.computeFromItems(tree, vars);
        MoveRestrictionInPattern.moveRestrictionInPatterns(tree, vars);
        tree = ExpandNodeVarWithGtop.computeTableNamesOnFromItems(tree, getGTopInterface());
        return PopulateJoins.populateJoins(tree, vars, getGTopInterface());
    }
}
