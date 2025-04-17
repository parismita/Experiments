package testDataGen;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import generateConstraints.ConstraintGenerator;
import parsing.AppTest_Parameters;
import util.Configuration;
import util.Result;
import util.TableMap;
import util.Utilities;

public class GenerateDataSet {
	/*
	 * This function generates test datasets for a query
	 * 
	 * @param conn Database connection to use
	 * 
	 * @param queryId The query id
	 * 
	 * @param query The query for which dataset needs to be generated
	 * 
	 * @param schemaFile File containing the schema against which the query has been
	 * written
	 * 
	 * @param sampleDataFile File containing sample data to generate realistic
	 * values
	 * 
	 * @param orderDependent Whether the order of tuples in the result matter. Set
	 * this to true for queries that have ORDER BY
	 * 
	 * @param tempFilePath File path to create temporary files and datasets
	 * 
	 * @return List of dataset ids that have been generated
	 * 
	 * @throws Exception
	 */
	public Result generateDatasetForQuery(Connection conn, int queryId, String query, File schemaFile,
			File sampleDataFile, String sampleDataJsonFilePath, boolean orderDependent, String tempFilePath,
			AppTest_Parameters obj) throws Exception {
		String line, schema = "", sampleData = "";

		schema = Utilities.readFile(schemaFile);

		sampleData = Utilities.readFile(sampleDataFile);

		return generateDatasetForQuery(conn, queryId, query, schema, sampleData, sampleDataJsonFilePath, orderDependent,
				tempFilePath, obj);
	}

	/**
	 * This function generates test datasets for a query
	 * 
	 * @param conn           Database connection to use
	 * @param queryId        The query id
	 * @param query          The query for which dataset needs to be generated
	 * @param schema         The schema against which the query has been written
	 * @param sampleData     Sample data to generate realistic values
	 * @param orderDependent Whether the order of tuples in the result matter. Set
	 *                       this to true for queries that have ORDER BY
	 * @param tempFilePath   File path to create temporary files and datasets
	 * @return List of dataset ids that have been generated
	 * @throws Exception
	 */
	public Result generateDatasetForQuery(Connection conn, int queryId, String query, String schema,
			String sampleData, String sampleDataJsonFilePath, boolean orderDependent, String tempFilePath,
			AppTest_Parameters appTestParams) throws Exception {

		if (tempFilePath == null | tempFilePath.equals("")) {
			tempFilePath = "/tmp/" + queryId;
		}

		GenerateCVC1 cvc = new GenerateCVC1();

		if(query.contains("INTERSECT ALL") || query.contains("intersect all")|| query.contains("EXCEPT ALL") || query.contains("except all"))
			{
				cvc.isAll=1;
			}
		cvc.setFilePath(tempFilePath);
		cvc.setFne(false);
		cvc.setIpdb(false);
		cvc.setOrderindependent(orderDependent);
		cvc.setQueryId(queryId);

		loadSchema(conn, schema);
		if (!Configuration.getProperty("sampleDataJson").equalsIgnoreCase("true"))
			loadSampleData(conn, sampleData);

		cvc.setSchemaFile(schema);
		cvc.setDataFileName(sampleData);
		cvc.setSampleDataJsonFilePath(sampleDataJsonFilePath);

		TableMap.clearAllInstances();
		cvc.setTableMap(TableMap.getInstances(conn, 1));
		cvc.setConnection(conn);

		deletePreviousDatasets(cvc);
		// Application Testing
		if (appTestParams == null)
			appTestParams = new AppTest_Parameters();
		cvc.setDBAppparams(appTestParams);
		// end
		FileWriter fw = new FileWriter(Configuration.homeDir + "/temp_smt" + cvc.getFilePath() + "/queries.txt");
		fw.write(query);
		fw.close();

		if (Configuration.getProperty("printSQL").equalsIgnoreCase("true")) {
			File directory = new File(Configuration.getProperty("printDir") + "/" + cvc.getQueryId());
			if (directory.exists())
				deleteDirectory(directory);
			directory.mkdir();
		}

		PreProcessingActivity.preProcessingActivity(cvc);
		ConstraintGenerator cg = new ConstraintGenerator();
		cg.clearContext();
		Result res = new Result(listOfDatasets(cvc), cvc.getTupleCount());
		return res;
	}

	private boolean deleteDirectory(File directory) {
		if (directory.isDirectory()) {
			File[] files = directory.listFiles();
			if (files != null) {
				for (File file : files) {
					deleteDirectory(file);
				}
			}
		}
		return directory.delete();
	}

	/**
	 * Creates tables provided in the schema for the given connection
	 * 
	 * @param conn
	 * @param schema
	 * @throws Exception
	 */
	public static void loadSchema(Connection conn, String schema) throws Exception {

		byte[] dataBytes = null;
		String tempFile = "";
		FileOutputStream fos = null;
		ArrayList<String> listOfQueries = null;
		ArrayList<String> listOfDDLQueries = new ArrayList<String>();
		String[] inst = null;

		dataBytes = schema.getBytes();
		tempFile = "/tmp/dummy";

		fos = new FileOutputStream(tempFile);
		fos.write(dataBytes);
		fos.close();
		listOfQueries = Utilities.createQueries(tempFile);
		inst = listOfQueries.toArray(new String[listOfQueries.size()]);
		listOfDDLQueries.addAll(listOfQueries);
		deleteAllTablesFromTestUser(conn);

		for (int i = 0; i < inst.length; i++) {

			if (!inst[i].trim().equals("") && !inst[i].trim().contains("drop table")) {
				DatabaseMetaData dbmd = conn.getMetaData();
				String dbType = dbmd.getDatabaseProductName();
				String temp = "";
				if (dbType.equalsIgnoreCase("MySql")) {
					temp = inst[i].trim();
				} else if (dbType.equalsIgnoreCase("PostgreSQL")) {
					temp = inst[i].trim().replaceAll("(?i)^\\s*create\\s+table\\s+", "create temporary table ");
				} else if (dbType.equalsIgnoreCase("sqlite")) {
					// temp = inst[i].trim();
					if (!inst[i].toUpperCase().contains("IF NOT EXISTS"))
						temp = inst[i].trim().replaceAll("(?i)^\\s*create\\s+table\\s+", "create table IF NOT EXISTS ");
					else
						temp = inst[i];
					String temp2 = temp.toUpperCase();
					// if(temp2.contains("DATABASE") || temp2.contains("USE"))
					// continue;
				}

				PreparedStatement stmt2 = conn.prepareStatement(temp);
				stmt2.executeUpdate();
				stmt2.close();
			}

		}
	}

	/**
	 * Loads datasets for the given connection
	 * 
	 * @param conn
	 * @param sampleData
	 * @throws Exception
	 */
	public static void loadSampleData(Connection conn, String sampleData) throws Exception {

		byte[] dataBytes = null;
		String tempFile = "/tmp/dummy";
		FileOutputStream fos = null;
		ArrayList<String> listOfQueries = null;
		String[] inst = null;

		dataBytes = sampleData.getBytes();
		fos = new FileOutputStream(tempFile);
		fos.write(dataBytes);
		fos.close();

		listOfQueries = Utilities.createQueries(tempFile);
		inst = listOfQueries.toArray(new String[listOfQueries.size()]);

		for (int i = 0; i < inst.length; i++) {
			//if (!inst[i].trim().equals("") && !inst[i].contains("drop table") && !inst[i].contains("delete from")) {
			if (!inst[i].trim().equals("") && !inst[i].contains("drop table")) {

				PreparedStatement stmt = conn.prepareStatement(inst[i]);
				stmt.executeUpdate();
				stmt.close();
			}
		}
	}

	private List<String> listOfDatasets(GenerateCVC1 cvc) {
		ArrayList<String> fileListVector = new ArrayList<String>();
		ArrayList<String> datasets = new ArrayList<String>();
		String fileList[] = new File(Configuration.homeDir + "/temp_smt" + cvc.getFilePath()).list();
		for (int k = 0; k < fileList.length; k++) {
			fileListVector.add(fileList[k]);
		}
		Collections.sort(fileListVector);
		for (int i = 0; i < fileList.length; i++) {
			File f1 = new File(Configuration.homeDir + "/temp_smt" + cvc.getFilePath() + "/" + fileListVector.get(i));

			if (f1.isFile() && fileListVector.get(i).startsWith("DS")) {
				datasets.add(fileListVector.get(i));
			}
		}
		return datasets;
	}

	public static void deletePreviousDatasets(GenerateCVC1 cvc) throws IOException, InterruptedException {

		File f = new File(Configuration.homeDir + "/temp_smt" + cvc.getFilePath() + "/");

		if (f.exists()) {
			File f2[] = f.listFiles();
			if (f2 != null)
				for (int i = 0; i < f2.length; i++) {
					if (f2[i].isDirectory() && f2[i].getName().startsWith("DS")) {

						Utilities.deletePath(
								Configuration.homeDir + "/temp_smt" + cvc.getFilePath() + "/" + f2[i].getName());
					}
				}
		}

		File dir = new File(Configuration.homeDir + "/temp_smt" + cvc.getFilePath());
		if (dir.exists()) {
			for (File file : dir.listFiles()) {
				file.delete();
			}
		} else {
			dir.mkdirs();
		}
	}

	public static void deleteAllTablesFromTestUser(Connection conn) throws Exception {
		try {

			String loginUrl = "jdbc:sqlite" + ":" + Configuration.getProperty("homeDir")
					+ Configuration.getProperty("databaseName");

			conn = DriverManager.getConnection(loginUrl, Configuration.getProperty("testDatabaseUser"),
					Configuration.getProperty("testDatabaseUserPasswd"));

			DatabaseMetaData dbm = conn.getMetaData();

			// added by rambabu
			String dbType = dbm.getDatabaseProductName();

			if (dbType.equalsIgnoreCase("MySql")) {
				String[] types = { "TABLE" };
				ResultSet rs = dbm.getTables(conn.getCatalog(), null, "%", types);

				// ResultSet rs = dbm.getTables(null, null, "%", types);
				// while (rs.next()) {
				// System.out.println(rs.getString(3));
				// }
				String query = "SET FOREIGN_KEY_CHECKS = 0";
				if (dbType.equalsIgnoreCase("sqlite"))
					query = "PRAGMA foreign_keys=off;";
				PreparedStatement pstmt = conn.prepareStatement(query);
				pstmt.executeUpdate();
				pstmt.close();

				while (rs.next()) {
					String table = rs.getString("TABLE_NAME");
					if (!table.equalsIgnoreCase("dataset")
							&& !table.equalsIgnoreCase("xdata_temp1")
							&& !table.equalsIgnoreCase("xdata_temp2")) {
						System.out.println("drop table if exists " + table + " cascade");
						// PreparedStatement pstmt = conn.prepareStatement("delete from "+table);
						// PreparedStatement pstmt = conn.prepareStatement("drop table if exists "+table
						// +" cascade");
						// pstmt.executeUpdate();
						// pstmt.close();

						query = "drop table if exists " + table;
						PreparedStatement pstmt1 = conn.prepareStatement(query);
						pstmt1.executeUpdate();
						pstmt1.close();
					}

				}

				query = "SET FOREIGN_KEY_CHECKS = 1";
				PreparedStatement pstmt2 = conn.prepareStatement(query);
				pstmt2.executeUpdate();
				pstmt2.close();

				rs.close();

			} else if (dbType.equalsIgnoreCase("sqlite")) {
				String[] types = { "TABLE" };
				ResultSet rs = dbm.getTables(conn.getCatalog(), null, "%", types);

				rs = dbm.getTables(null, null, "%", types);
				Vector<String> tables = getTablesToDrop(conn);

				String query = "PRAGMA foreign_keys = OFF;";
				Statement stmt = conn.createStatement();
				stmt.executeUpdate(query);
				stmt.close();

				for (String table : tables) {
					// String table=rs.getString("TABLE_NAME");
					if (!table.equalsIgnoreCase("dataset")
							&& !table.equalsIgnoreCase("xdata_temp1")
							&& !table.equalsIgnoreCase("xdata_temp2")) {
						// System.out.println("drop table if exists "+table +" cascade");
						// PreparedStatement pstmt = conn.prepareStatement("delete from "+table);
						// PreparedStatement pstmt = conn.prepareStatement("drop table if exists "+table
						// +" cascade");
						// pstmt.executeUpdate();
						// pstmt.close();
						// query = "select * from "+ table;
						// stmt.executeUpdate(query);

						// query = "select * from " + table;
						// query = "drop table "+table;
						query = "DELETE FROM " + table;
						stmt.setQueryTimeout(30); // set timeout to 30 sec.
						PreparedStatement pstmt1 = conn.prepareStatement(query);
						pstmt1.executeUpdate();
						pstmt1.close();
					}

				}

				query = "PRAGMA foreign_keys=on;";
				PreparedStatement pstmt2 = conn.prepareStatement(query);
				pstmt2.executeUpdate();
				pstmt2.close();

				rs.close();

			} else if (dbType.equalsIgnoreCase("postgreSQL")) {
				String[] types = { "TEMPORARY TABLE" };
				ResultSet rs = dbm.getTables(conn.getCatalog(), null, "%", types);

				while (rs.next()) {
					String table = rs.getString("TABLE_NAME");
					if (!table.equalsIgnoreCase("dataset")
							&& !table.equalsIgnoreCase("xdata_temp1")
							&& !table.equalsIgnoreCase("xdata_temp2")) {
						// PreparedStatement pstmt = conn.prepareStatement("delete from "+table);
						PreparedStatement pstmt = conn.prepareStatement("Truncate table " + table + " cascade");
						pstmt.executeUpdate();
						pstmt.close();
					}

				}

				rs.close();
			}

		} catch (Exception e) {
			System.out.println(e);
		}

	}

	public static String convertBackticksToSingleQuotes(String sql) {
		return sql.replace("`", "'");
	}

	private static Vector<String> getTablesToDrop(Connection conn) throws SQLException {
		// TODO Auto-generated method stub
		Vector<String> tables = new Vector<String>();

		String[] types = { "TABLE" };
		DatabaseMetaData dbm = conn.getMetaData();

		ResultSet rs = dbm.getTables(conn.getCatalog(), null, "%", types);

		while (rs.next()) {
			tables.add(rs.getString("TABLE_NAME"));
			// System.out.println(rs.getString("TABLE_NAME"));
		}
		rs.close();
		return tables;
		// throw new UnsupportedOperationException("Unimplemented method
		// 'getTablesToDrop'");
	}

	public static void main(String[] args) throws Exception {

		// TEMPCODE START : Rahul Sharma
		// REGRESSION TEST
		int regression_test = 0;
		if (regression_test == 1) {
			int start = 1;
			int end = 54;
			// end = start = 27;
			for (int i = start; i <= end; i++) {
				int queryId = i;
				String query = readQueryFromFile("test/universityTest/queries.txt", queryId + "");
				String tempDatabaseType = Configuration.getProperty("tempDatabaseType");
				String loginUrl = "";
				Connection conn = null;

				// choosing connection based on database type
				if (tempDatabaseType.equalsIgnoreCase("postgresql")) {
					Class.forName("org.postgresql.Driver");

					loginUrl = "jdbc:postgresql://" + Configuration.getProperty("databaseIP") + ":"
							+ Configuration.getProperty("databasePort") + "/"
							+ Configuration.getProperty("databaseName");
					conn = DriverManager.getConnection(loginUrl, Configuration.getProperty("testDatabaseUser"),
							Configuration.getProperty("testDatabaseUserPasswd"));
					;
				} else if (tempDatabaseType.equalsIgnoreCase("mysql")) {
					Class.forName("com.mysql.cj.jdbc.Driver");

					loginUrl = "jdbc:mysql://" + Configuration.getProperty("databaseIP") + ":"
							+ Configuration.getProperty("databasePort") + "/"
							+ Configuration.getProperty("databaseName");
					conn = DriverManager.getConnection(loginUrl, Configuration.getProperty("testDatabaseUser"),
							Configuration.getProperty("testDatabaseUserPasswd"));
					;
				}
				try {
					File schemaFile = new File("test/universityTest/DDL.sql");
					File sampleDataFile = new File("test/universityTest/sampleData.sql");
					String sampleDataJsonFilePath = ""; // Put json path here
					if (Configuration.sampleDataJson.equalsIgnoreCase("true"))
						sampleDataJsonFilePath = "test/universityTest/sample.json";
					boolean orderDependent = false;
					/* runtime analysis for regression test */
					long startTime = System.currentTimeMillis();
					String tempFilePath = File.separator + queryId;

					GenerateDataSet d = new GenerateDataSet();
					// Application Testing
					AppTest_Parameters obj = new AppTest_Parameters();

					// end
					Result res = d.generateDatasetForQuery(conn, queryId, query, schemaFile, sampleDataFile,
							sampleDataJsonFilePath, orderDependent, tempFilePath, obj);
					List<String> dataset = res.getDatasets();
					for (String s : dataset) {
						System.out.println(s);
					}

					long stopTime = System.currentTimeMillis();
					long elapsedTime = stopTime - startTime;
					System.out.println("Total time taken for data generation of the query " + queryId + " is : ");
					System.out.println(elapsedTime);
				} catch (Exception e) {
					e.printStackTrace();
					System.out.println("Query : " + query + "\n" + e);
				}
			}
			// TEMPCODE END : Rahul Sharma
		} else {

			String tempDatabaseType = Configuration.getProperty("tempDatabaseType");
			String loginUrl = "";
			Connection conn = null;

			// choosing connection based on database type
			if (tempDatabaseType.equalsIgnoreCase("postgresql")) {
				Class.forName("org.postgresql.Driver");

				loginUrl = "jdbc:postgresql://" + Configuration.getProperty("databaseIP") + ":"
						+ Configuration.getProperty("databasePort") + "/" + Configuration.getProperty("databaseName");
				conn = DriverManager.getConnection(loginUrl, Configuration.getProperty("testDatabaseUser"),
						Configuration.getProperty("testDatabaseUserPasswd"));
				;
			} else if (tempDatabaseType.equalsIgnoreCase("mysql")) {
				Class.forName("com.mysql.cj.jdbc.Driver");

				loginUrl = "jdbc:mysql://" + Configuration.getProperty("databaseIP") + ":"
						+ Configuration.getProperty("databasePort") + "/" + Configuration.getProperty("databaseName");
				conn = DriverManager.getConnection(loginUrl, Configuration.getProperty("testDatabaseUser"),
						Configuration.getProperty("testDatabaseUserPasswd"));
				;
			} else if (tempDatabaseType.equalsIgnoreCase("sqlite")) {
				Class.forName("org.sqlite.JDBC");

				File file = new File(Configuration.getProperty("homeDir")+ Configuration.getProperty("databaseName")); // Replace with your database path
				if (file.delete()) {
					System.out.println("Database file has been deleted successfully.");
				} 
				file = new File(Configuration.getProperty("homeDir")+ Configuration.getProperty("databaseName")+"-shm"); // Replace with your database path
				if (file.delete()) {
					System.out.println("Database file shm has been deleted successfully.");
				} 
				file = new File(Configuration.getProperty("homeDir")+ Configuration.getProperty("databaseName")+"-wal"); // Replace with your database path
				if (file.delete()) {
					System.out.println("Database file wal has been deleted successfully.");
				} 
				// else {
				// 	System.out.println("Failed to delete the database file.");
				// }
				loginUrl = "jdbc:sqlite" + ":" + Configuration.getProperty("homeDir")
						+ Configuration.getProperty("databaseName");

				conn = DriverManager.getConnection(loginUrl, Configuration.getProperty("testDatabaseUser"),
						Configuration.getProperty("testDatabaseUserPasswd"));
				String query = "PRAGMA journal_mode=WAL;";
				Statement stmt = conn.createStatement();
				stmt.executeUpdate(query);
				stmt.close();
			}

			int queryId = 99;

			
			String	query = "SELECT foo.name FROM (SELECT * FROM  (SELECT * FROM  student join takes on (student.ID = takes.ID)) as foo ) as foo";
			// query = "select distinct s.id, s.name from student s, takes t where s.id = t.id and  t.grade != 'F' ";
			// query = "SELECT * FROM (select course.dept_name, sum(credits) from course, department where (course.dept_name = department.dept_name) group by course.dept_name, credits) as foo" ;
								
			// // query = "select all dept_name from instructor";
			// query = "select dept_name,budget from department where (budget>40000 and budget<80000) or (salary>80000 and dept_name = 'cs')";	
			query = "SELECT takes.course_id FROM student INNER JOIN takes ON(student.id=takes.id) INNER JOIN course ON(course.course_id=takes.course_id) WHERE student.id = '12345'";	
			// query = "SELECT DISTINCT ID,NAME FROM (SELECT ID, TIME_SLOT_ID, YEAR FROM TAKES NATURAL JOIN SECTION GROUP BY ID, TIME_SLOT_ID, YEAR HAVING COUNT(TIME_SLOT_ID)>4) as FOO join STUDENT ON (FOO.ID=student.ID)";
			// query = "SELECT name FROM instructor WHERE EXISTS (SELECT * FROM teaches WHERE instructor.ID = teaches.ID)";
			// query = "select count(room_number) from classroom where exists (select d.building from department as d where dept_name='Comp. Sci.')";
			// query = "select building from classroom where capacity > 10 or capacity < 5";
			// query = "select * from takes natural join section";
			//query = "select dept_name from department where budget = (select min(budget) from department) or budget = 10";
			query = "select * from student, teaches where student.ID > teaches.COURSE_ID or student.ID = teaches.COURSE_ID";
			// query = "select count(distinct COURSE_ID) from teaches group by sec_id";
			query = "select building from classroom where capacity > 10 or capacity < 5";
			query = "(select course_id from section where semester = 'Fall' and year = 2009) union (select course_id from section where semester = 'Spring' and year = 2010)";
			query = "SELECT name FROM instructor WHERE EXISTS (SELECT * FROM teaches where year=2009)";
			query = "SELECT name FROM instructor WHERE EXISTS (SELECT * FROM teaches where year=2009)";

			File schemaFile=new File(Configuration.testDir+"/universityTest/DDL.sql");
				File sampleDataFile=new File(Configuration.testDir+"/universityTest/sampleData.sql");

			// testiing spider -- sunanda
			// schemaFile=new
			// File("/home/sunanda/Course-Files-IITB/Thesis/Text2SQL/spider/database/department_management/schemaup.sql");
			// sampleDataFile=new
			// File("/home/sunanda/Course-Files-IITB/Thesis/Text2SQL/spider/database/department_management/sampledata.sql");

			String sampleDataJsonFilePath = "";
			if (Configuration.sampleDataJson.equalsIgnoreCase("true"))
				sampleDataJsonFilePath = Configuration.testDir + "/universityTest/sample.json"; // Put json path here -
																								// sunanda

			boolean orderDependent = false;
			long startTime = System.currentTimeMillis();
			String tempFilePath = File.separator + queryId;

			GenerateDataSet d = new GenerateDataSet();
			// Application Testing
			AppTest_Parameters obj = new AppTest_Parameters();

			// end

			// if(Configuration.getProperty("tempDatabaseType").equalsIgnoreCase("sqlite")){
			// query = d.convertBackticksToSingleQuotes(query);
			// }

				if (args.length > 0)  query = args[0];
				System.out.println("\n"+query+"\n");
				Result res = d.generateDatasetForQuery(conn,queryId,query,  schemaFile,  sampleDataFile, sampleDataJsonFilePath,  orderDependent,  tempFilePath, obj);
				List<String> dataset = res.getDatasets();
				for(String s:dataset) {
					System.out.println(s);
				}
				System.out.println("\nNumber of datatsets: "+dataset.size()+"\n");
				long stopTime = System.currentTimeMillis();
				long elapsedTime = stopTime - startTime;
		        System.out.println("Total time taken for data generation of the query is : "+elapsedTime);
		        // System.out.print(elapsedTime);
			}
		}

	// TEMPCODE Rahul Sharma
	public static String readQueryFromFile(String fileName, String queryId) throws IOException {
		File file = new File(fileName);
		@SuppressWarnings("resource")
		BufferedReader br = new BufferedReader(new FileReader(file));
		String st;
		while ((st = br.readLine()) != null) {
			// System.out.println(st);
			if (st.length() > 0) {
				StringTokenizer stok = new StringTokenizer(st, "|");
				if (stok.nextToken().contentEquals(queryId))
					return stok.nextToken();
			}
		}
		return "";
	}

}
