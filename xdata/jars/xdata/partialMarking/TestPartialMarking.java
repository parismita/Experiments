
/**
 * 
 */
package partialMarking;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.logging.Logger;

import parsing.AppTest_Parameters;
import parsing.QueryStructure;
import testDataGen.GenerateCVC1;
import testDataGen.GenerateDataSet;
import testDataGen.PreProcessingActivity;
import util.Configuration;
import util.TableMap;
import util.Utilities;

/**
 * @author mathew
 *
 */
public class TestPartialMarking {
	
	private static Logger logger = Logger.getLogger(TestPartialMarking.class.getName());
	
	static int assignNo=0;//for University Schema
//	static int assignNo=4; //for TPCH Schema
//	static int assignNo=13; //for Amol sirs Schema
		
	// Maximum marks
	int maxMarks=100;
	
	public void setMaxMarks(int marks){
		maxMarks=marks;
	}

	public static void printSQL(QueryStructure qs) throws Exception{
		String str = "select * from student;";
		FileWriter op = new FileWriter(Configuration.homeDir + "/a.txt");

		op.write(str);
		op.close();
	}
	
	public static GenerateCVC1 processCVC(GenerateCVC1 cvc, String strQuery) throws Exception{
		Connection conn = null;
				
		Class.forName("org.postgresql.Driver");
		String loginUrl = "jdbc:postgresql://" + Configuration.getProperty("databaseIP") + ":" + Configuration.getProperty("databasePort") + "/" + Configuration.getProperty("databaseName");
		conn = DriverManager.getConnection(loginUrl, Configuration.getProperty("testDatabaseUser"), Configuration.getProperty("testDatabaseUserPasswd"));;
	
		File schemaFile=new File(Configuration.testDir+"/universityTest/DDL.sql");
		File sampleDataFile=new File(Configuration.testDir+"/universityTest/sampleData.sql");
		String sampleDataJsonFilePath = ""; 
		if(Configuration.sampleDataJson.equalsIgnoreCase("true"))
			sampleDataJsonFilePath = Configuration.testDir+"/universityTest/sample.json";	// Put json path here - sunanda
		String schema="",sampleData="";			
		schema=Utilities.readFile(schemaFile);
		sampleData=Utilities.readFile(sampleDataFile);

		boolean orderDependent=false;
		long startTime = System.currentTimeMillis();
		String tempFilePath=File.separator +1;
			
		GenerateDataSet d=new GenerateDataSet();
		AppTest_Parameters obj = new AppTest_Parameters();
		if(tempFilePath==null | tempFilePath.equals("")){
				tempFilePath="/tmp/"+1;
			}
			
		cvc.setFilePath(tempFilePath);
		cvc.setFne(false); 
		cvc.setIpdb(false);
		cvc.setOrderindependent(orderDependent);	
		
		
		d.loadSchema(conn,schema);
		if(!Configuration.getProperty("sampleDataJson").equalsIgnoreCase("true"))
			d.loadSampleData(conn,sampleData);
		
		cvc.setSchemaFile(schema);
		cvc.setDataFileName(sampleData);
		cvc.setSampleDataJsonFilePath(sampleDataJsonFilePath);
		
		TableMap.clearAllInstances();
		cvc.setTableMap(TableMap.getInstances(conn, 1));
		cvc.setConnection(conn);
		
		d.deletePreviousDatasets(cvc);
		//Application Testing
		cvc.setDBAppparams(obj);
		QueryStructure qStructure=new QueryStructure(cvc.getTableMap());
		qStructure.buildQueryStructure("q1", strQuery.toString(),cvc.getDBAppparams());
		qStructure.buildSQLevel(); //parismita
		cvc.setqStructure(qStructure);
		
		cvc.initializeQueryDetailsQStructure(cvc.getqStructure() );
				
		return cvc;
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		TestPartialMarking testObj=new TestPartialMarking();
		try{
//			String instructorQuery = "";//"SELECT DISTINCT course_id, title FROM course NATURAL JOIN section WHERE semester = 'Spring' AND year = 2010 AND course_id NOT IN (SELECT course_id FROM prereq)";
			//"SELECT course_id, title FROM course NATURAL JOIN takes WHERE semester = 'Spring' AND year = '2010' AND course_id NOT IN (SELECT course_id FROM prereq)";
			String instructorQuery="SELECT name FROM instructor order by name limit 5;";
			String studentQuery="SELECT id FROM instructor order by name limit 5;";
			//studentQuery="select distinct id from takes, course where takes.course_id=course.course_id";
			//readQueriesFromFileParseAndTest();
			//readQueriesFromDBParseAndTest();			
			//processStudentQueryFromKeyboard(testObj);
			
			GenerateCVC1 cvc1 = new GenerateCVC1(), cvc2 = new GenerateCVC1();
			cvc1 = processCVC(cvc1, studentQuery);
			cvc2 = processCVC(cvc2, instructorQuery);
			CanonicalizeQuery.Canonicalize(cvc1.getqStructure());
			CanonicalizeQuery.Canonicalize(cvc2.getqStructure());

			printSQL(cvc1.getqStructure());
			Float normalMarks=PartialMarker.calculateScore(cvc2.getqStructure(), cvc2.getqStructure(), 0).Marks;
			Float studentMarks=PartialMarker.calculateScore(cvc2.getqStructure(), cvc1.getqStructure(), 0).Marks;
			//System.out.println("normal Marks"+normalMarks+ " studentMarks "+studentMarks+ " partial marks"+studentMarks*100/normalMarks);
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	

}