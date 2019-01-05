package db;

import db.mysql.MySQLConnection;

public class DBConnectionFactory {
	// This should change based on the pipeline.
	private static final String DEFAULT_DB = "mysql"; // "mongodb"
	// static class	
	public static DBConnection getConnection(String db) { 
		switch (db) {
		case "mysql":
			return new MySQLConnection();
		case "mongodb":
			// return new MongoDBConnection();
			return null;
		default:
			throw new IllegalArgumentException("Invalid db:" + db);
		}

	}

	public static DBConnection getConnection() { //默认构造函数
		return getConnection(DEFAULT_DB);
	}

}
