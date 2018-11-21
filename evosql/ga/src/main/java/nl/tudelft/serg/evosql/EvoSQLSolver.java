package nl.tudelft.serg.evosql;

import nl.tudelft.serg.evosql.db.ISchemaExtractor;
import nl.tudelft.serg.evosql.db.SchemaExtractor;
import nl.tudelft.serg.evosql.path.PathExtractor;


public abstract class EvoSQLSolver {
    	
	protected ISchemaExtractor schemaExtractor;
	protected PathExtractor pathExtractor;

    public EvoSQLSolver(String jdbcString, String dbDatabase, String dbUser, String dbPwd) {
		this(new SchemaExtractor(jdbcString, dbDatabase, dbUser, dbPwd));
    }

    public EvoSQLSolver(ISchemaExtractor se) {
		this.schemaExtractor = se;
		pathExtractor = new PathExtractor(schemaExtractor);
	}
    
    public void setPathExtractor(PathExtractor pe) {
		this.pathExtractor = pe;
    }
    
    public abstract Result execute(String sqlToBestedString);
}