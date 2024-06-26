package org.cytosm.cypher2sql.lowering;

import org.cytosm.cypher2sql.PassAvailables;
import org.cytosm.cypher2sql.lowering.sqltree.ScopeSelect;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 */
public class UnwrapAliasVarTests extends BaseLDBCTests {

    @Test
    public void testAliasVarIsKeptOnReturn() throws Exception {
        String cypher = "MATCH (a:Person) RETURN a.firstName AS a";
        ScopeSelect tree = PassAvailables.cypher2sqlOnExpandedPaths(getGTopInterface(), cypher);
        Assertions.assertEquals(1, tree.ret.exportedItems.size());
    }
}
