/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package util;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import parsing.*;

/**
 *
 * @author Bhanu Pratap Gupta
 */

public class TableMap implements Serializable {

	private static Logger logger = Logger.getLogger(TableMap.class.getName());
	private static final long serialVersionUID = 4802766539372778676L;
	private Map<String, Table> tables = null;
	private Map<String, Table> subquerytables = null;
	private static Map<Integer, TableMap> schemaTableMap = null;
	private static Map<String, Table> indexMap = new HashMap<String, Table>();
	// added by deeksha
	private static boolean usingCnt = false;

	// end
	public Map<String, Table> getTables() {
		return tables;
	}

	public void setTables(Map<String, Table> tables) {
		this.tables = tables;
	}

	/**
	 * @author parismita ----start
	 * @param tables
	 */
	public void putTables(Map<String, Table> tables) {
		this.tables.putAll(tables);
	}

	public Map<String, Table> getSQTables() {
		return this.subquerytables;
	}

	public void setSQTables(Map<String, Table> tables) {
		this.subquerytables = tables;
	}

	public Table getSQTableByName(String tableName) {
		return this.subquerytables.get(tableName);
	}

	/**
	 * @author parismita ----end
	 * @param subquerytables
	 */
	public void putSQTables(Map<String, Table> tables) {
		this.subquerytables.putAll(tables);
	}

	private Vector<Table> topSortedTables = null;
	private String database;
	private String schema;

	private transient Connection conn = null;
	public Graph<Table, ForeignKey> foreignKeyGraph = null;

	public static void clearAllInstances() {
		if (schemaTableMap != null)
			schemaTableMap.clear();
	}

	public static TableMap getInstances(Connection dbConn, int schemaId) {
		TableMap instance = null;
		if (schemaTableMap == null) {
			instance = new TableMap(dbConn);
			instance.createTableMap();
			schemaTableMap = new HashMap<Integer, TableMap>();
			schemaTableMap.put(schemaId, instance);
		} else {
			TableMap t = schemaTableMap.get(schemaId);

			if (t != null) {
				return t;
			} else {
				instance = new TableMap(dbConn);
				instance.createTableMap();
				schemaTableMap.put(schemaId, instance);
			}
		}
		return instance;
	}

	private TableMap(Connection dbConn) {
		tables = new LinkedHashMap<String, Table>();
		conn = dbConn;

		try {

			if (Configuration.getProperty("tempDatabaseType").equalsIgnoreCase("sqlite")) {

				String dbPath = conn.getMetaData().getURL().split(":")[2];
				database = new java.io.File(dbPath).getName();
			} else {
				database = dbConn.getCatalog();
			}
		} catch (SQLException e1) {
			logger.log(Level.SEVERE, e1.getMessage(), e1);
			e1.printStackTrace();
		}

		try {
			schema = dbConn.getMetaData().getUserName();
		} catch (SQLException e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
			e.printStackTrace();
		}
		;
	}

	public void createTableMap() {

		try {

			// added by deeksha
			if (Configuration.getProperty("cntFlag").equalsIgnoreCase("true")) {
				usingCnt = true;
			} else {
				usingCnt = false;
			}
			// end

			DatabaseMetaData meta = this.conn.getMetaData();
			ResultSet rs = null, rs1 = null;

			// Note: Table types listed below are specific to PostgreSQL and could be
			// different for other databases: Oracle, MySql etc
			// One way to work around is to remove the filter whole together which would
			// mean all tables including the system tables will be returned.

			// added by rambabu for mysql
			String dbType = meta.getDatabaseProductName();

			String tableFilter[] = new String[1];
			if (dbType.equalsIgnoreCase("MySql")) {
				tableFilter[0] = "TABLE";
			} else if (dbType.equalsIgnoreCase("PostgreSQL")) {
				tableFilter[0] = "TEMPORARY TABLE";
			}
			if (Configuration.getProperty("tempDatabaseType").equalsIgnoreCase("sqlite")) {
				if (conn != null && !conn.isClosed()) {
					DatabaseMetaData metaData = conn.getMetaData();
					rs = metaData.getTables(database, "", "%", null);
				}
			} else
				rs = meta.getTables(database, "", "%", tableFilter);

			while (rs.next()) {
				// String tableName = rs.getString("TABLE_NAME").toUpperCase();
				String tableName = rs.getString("TABLE_NAME"); // added by rambabu
				// System.out.println(tableName); //added by rambabu
				if (tableName.contains("sqlite"))
					continue;
				if (tables.get(tableName.toUpperCase()) == null) { // modified by rambabu for mysql
					Table table = new Table(tableName);
					tables.put(tableName.toUpperCase(), table);
					indexMap.put(tableName.toUpperCase(), table);
				}
			}

			Vector<String> check_col = new Vector<String>();
			String constraint = null;
			String check_info = "SELECT conname FROM pg_constraint WHERE contype='c'";
			PreparedStatement s1 = this.conn.prepareStatement(check_info);
			ResultSet rset1 = s1.executeQuery();
			while (rset1.next()) {
				String name = rset1.getString("conname");
				check_col.add(name);
			}

			rs = null;
			Boolean sqlite = false;
			if (Configuration.getProperty("tempDatabaseType").equalsIgnoreCase("sqlite")) {
				if (conn != null && !conn.isClosed()) {
					DatabaseMetaData metaData = conn.getMetaData();
					rs = metaData.getColumns(database, "", "%", "%");
				}
				sqlite = true;
			} else
				rs = meta.getColumns(database, "", "%", "%");
			while (rs.next()) {
				// String tableName = rs.getString("TABLE_NAME").toUpperCase();
				String tableName = rs.getString("TABLE_NAME");
				Table table = getTable(tableName.toUpperCase());
				if (table == null)
					continue;
				String columnName = rs.getString("COLUMN_NAME").toUpperCase();
				String isNullable = rs.getString("IS_NULLABLE").toUpperCase();
				// System.out.println(columnName+" "+rs.getInt("DATA_TYPE")); //added by rambabu
				// if(sqlite){
				// columnName = "`"+columnName+"`";
				// }

				Column col = new Column(columnName, table);
				// logger.log(Level.INFO,"Table Values in TableMap = "+table+" and column =
				// "+columnName);

				String query1 = "SELECT check_clause FROM information_schema.check_constraints WHERE constraint_name ='"
						+ tableName.toLowerCase() + "_" + columnName.toLowerCase() + "_check'";
				if (check_col.contains(tableName.toLowerCase() + "_" + columnName.toLowerCase() + "_check")) {
					PreparedStatement s = this.conn.prepareStatement(query1);
					ResultSet rset = s.executeQuery();
					if (rset.next()) {
						constraint = rset.getString("check_clause");
						// System.out.println("check constraint: "+constraint);
					}
				}

				col.setDataType(rs.getInt("DATA_TYPE"));

				if (col.getDataType() == Types.NUMERIC || col.getDataType() == Types.DECIMAL
						|| col.getDataType() == Types.INTEGER ) { // modified by ram for
					// mysql

					// String query = "SELECT " + columnName + " FROM " + table;
					String query = "SELECT " + columnName + " FROM " + tableName; // added by rambabu for mysql
					// System.out.println(query); //added by rambabu
					PreparedStatement statement = this.conn.prepareStatement(query);

					ResultSet resultSet = statement.executeQuery();

					ResultSetMetaData metadata = resultSet.getMetaData();
					int precision = metadata.getPrecision(1);
					int scale = metadata.getScale(1);

					col.setPrecision(precision);
					col.setScale(scale);

					if (rs.getInt("DECIMAL_DIGITS") == 0)
						col.setDataType(Types.INTEGER);

					if (precision - scale == 10)
						col.setLimitDefined(false);
					else
						col.setLimitDefined(true);
					col.setMaxVal(Math.pow(10, precision - scale) < 99999 ? Math.pow(10, precision - scale) : 99999);
					col.setMinVal(
							-Math.pow(10, precision - scale) > -99999 ? -Math.pow(10, precision - scale) : -99999);
					String[] arrOfStr = {""};

					
					if (constraint != null) { // check constraint exists on the column
						if(constraint.contains("AND") || constraint.contains("and")){
							arrOfStr = constraint.split("AND");
						}
						col.setLimitDefined(true);

						if(arrOfStr.length > 1){
							for(int i=0; i<arrOfStr.length ; i++){
								String constr = arrOfStr[i];
								if (constr.contains("> ")) {
									// System.out.println(constraint.indexOf('>'));
									// System.out.println(constraint.substring(constraint.indexOf('>') + 3,
									// constraint.indexOf(')')));
		
									Pattern p = Pattern.compile("-?\\d+");
									Matcher m = p.matcher(constr);
									double min = -99999;
									
									while (m.find()) {
		
										min = Integer.parseInt(m.group());
									}
		
									// min = Integer.parseInt(
									// constraint.substring(constraint.indexOf('>') + 3, constraint.indexOf(')')));
									if(min != -99999)
										col.setMinVal(min + 1.0);
									// col.setMaxVal(Math.pow(10, precision - scale));
								}
								if (constr.contains("< ")) {
									int fromIndex = constr.indexOf('<');
									Pattern p = Pattern.compile("-?\\d+");
									Matcher m = p.matcher(constr);
									double max = 99999;
									while (m.find()) {
		
										max = Integer.parseInt(m.group());
									}
									if(max != 99999)
										col.setMaxVal(max - 1.0);
									// col.setMinVal(-999999.9);
									// col.setMinVal(-Math.pow(10, precision - scale));
								}
								if (constr.contains(">=")) {
									// double min = Integer.parseInt(
									// constraint.substring(constraint.indexOf('>') + 4, constraint.indexOf(')')));
									Pattern p = Pattern.compile("-?\\d+");
									Matcher m = p.matcher(constr);
									double min = -99999;
									while (m.find()) {
		
										min = Integer.parseInt(m.group());
									}
									if(min != -99999)
										col.setMinVal(min);
									// col.setMaxVal(Math.pow(10, precision - scale));
								}
								if (constr.contains("<=")) {
									int fromIndex = constr.indexOf('<');
									// double max = Integer.parseInt(constraint.substring(constraint.indexOf('<') +
									// 4,
									// constraint.indexOf(')', fromIndex)));
									Pattern p = Pattern.compile("-?\\d+");
									Matcher m = p.matcher(constr);
									double max = 99999;
									while (m.find()) {
		
										max = Integer.parseInt(m.group());
									}
									if(max != 99999)
										col.setMaxVal(max);
									// col.setMinVal(-999999.9);
									// col.setMinVal(-Math.pow(10, precision - scale));
								}
							}

						}
							
						else{
							if (constraint.contains("> ")) {
								// System.out.println(constraint.indexOf('>'));
								// System.out.println(constraint.substring(constraint.indexOf('>') + 3,
								// constraint.indexOf(')')));
	
								Pattern p = Pattern.compile("-?\\d+");
								Matcher m = p.matcher(constraint);
								double min = -99999;
								
								while (m.find()) {
	
									min = Integer.parseInt(m.group());
								}
	
								// min = Integer.parseInt(
								// constraint.substring(constraint.indexOf('>') + 3, constraint.indexOf(')')));
								if(min != -99999)
									col.setMinVal(min + 1.0);
								// col.setMaxVal(Math.pow(10, precision - scale));
							}
							if (constraint.contains("< ")) {
								int fromIndex = constraint.indexOf('<');
								Pattern p = Pattern.compile("-?\\d+");
								Matcher m = p.matcher(constraint);
								double max = 99999;
								while (m.find()) {
	
									max = Integer.parseInt(m.group());
								}
								if(max != 99999)
									col.setMaxVal(max - 1.0);
								// col.setMinVal(-999999.9);
								// col.setMinVal(-Math.pow(10, precision - scale));
							}
							if (constraint.contains(">=")) {
								// double min = Integer.parseInt(
								// constraint.substring(constraint.indexOf('>') + 4, constraint.indexOf(')')));
								Pattern p = Pattern.compile("-?\\d+");
								Matcher m = p.matcher(constraint);
								double min = -99999;
								while (m.find()) {
	
									min = Integer.parseInt(m.group());
								}
								if(min != -99999)
									col.setMinVal(min);
								// col.setMaxVal(Math.pow(10, precision - scale));
							}
							if (constraint.contains("<=")) {
								int fromIndex = constraint.indexOf('<');
								// double max = Integer.parseInt(constraint.substring(constraint.indexOf('<') +
								// 4,
								// constraint.indexOf(')', fromIndex)));
								Pattern p = Pattern.compile("-?\\d+");
								Matcher m = p.matcher(constraint);
								double max = 99999;
								while (m.find()) {
	
									max = Integer.parseInt(m.group());
								}
								if(max != 99999)
									col.setMaxVal(max);
								// col.setMinVal(-999999.9);
								// col.setMinVal(-Math.pow(10, precision - scale));
							}
						}
						
						
						constraint = null;
					}

				} else {
					if (col.getDataType() == Types.CHAR)
						col.setDataType(Types.VARCHAR);
					String regex = "'(.*?)'::character varying";

					// Compile the regular expression
					if (constraint != null) {
						// col.setLimitDefined(true);

						Pattern pattern = Pattern.compile(regex);
						Matcher matcher = pattern.matcher(constraint);

						// List to store the extracted values
						Vector<String> values = new Vector<>();

						// Find and add all matches to the list
						while (matcher.find()) {
							values.add(matcher.group(1));
						}
						col.setCheckValues(values);
					}

				}
				if (col.getDataType() == Types.CHAR)
					col.setDataType(Types.VARCHAR);
				col.setColumnSize(rs.getInt("COLUMN_SIZE"));
				col.setIsNullable(rs.getString("IS_NULLABLE").equals("YES"));
				if (rs.getString("COLUMN_DEF") != null && rs.getString("COLUMN_DEF").startsWith("nextval")) {
					col.setIsAutoIncement(true);
				}
				if (table.getColumnIndexList().contains(col.getColumnName())) // added by sunanda
					continue;
				table.addColumn(col);
				if (isNullable.equalsIgnoreCase("NO")) {
					table.addColumnInNonNullKey(col);
				}
			}

			rs = null;
			if (Configuration.getProperty("tempDatabaseType").equalsIgnoreCase("sqlite")) {
				if (conn != null && !conn.isClosed()) {
					DatabaseMetaData metaData = conn.getMetaData();
					rs = metaData.getTables(database, "", "%", null);
				}
			} else
				rs = meta.getTables(database, "", "%", tableFilter);

			while (rs.next()) {
				String tableName = rs.getString("TABLE_NAME");
				if (tableName.contains("sqlite"))
					continue;
				rs1 = null;
				rs1 = meta.getPrimaryKeys(database, "", tableName);
				// int size = rs1.getFetchSize();
				// added by rambabu as above statement was always returning 0
				int size = 0;
				while (rs1.next()) {
					size++;
				}
				rs1 = meta.getPrimaryKeys(database, "", tableName);

				while (rs1.next()) {
					Table table = getTable(tableName.toUpperCase());
					indexMap.remove(tableName.toUpperCase());

					String columnName = rs1.getString("COLUMN_NAME").toUpperCase();
					// if(sqlite){
					// columnName = "`"+columnName+"`";
					// }
					Column col = table.getColumn(columnName);
					table.addColumnInPrimaryKey(col);
					if (size == 1)
						col.setIsUnique(true);
				}
			}
			// For tables without primary keys, check if uniques indexes exists
			// We assume them as primary keys in the absence of primary key

			// Iterator it = indexMap.keySet().iterator();
			Iterator<String> it = indexMap.keySet().iterator(); // added by rambabu
			while (it.hasNext()) {
				// get tablename from keyset using iterator
				boolean listUniqueIndex = true;
				// String tname = (String)it.next();
				String tname = indexMap.get((String) it.next()).getTableName(); // added by rambabu
				// ResultSet rset = meta.getIndexInfo(database,"",
				// tname.toLowerCase(),listUniqueIndex, true);
				ResultSet rset = meta.getIndexInfo(database, "", tname, listUniqueIndex, true); // added by rambabu for
																								// mysql
				while (rset.next()) {
					String indexName = rset.getString("INDEX_NAME");
					// String table = rset.getString("TABLE_NAME");
					// String schema = rset.getString("TABLE_SCHEM");

					String columnName = rset.getString("COLUMN_NAME").toUpperCase();
					// if(sqlite)
					// columnName = "`"+columnName+"`";
					if (indexName == null) {
						continue;
					}
					Table table = getTable(tname.toUpperCase());
					Column col = table.getColumn(columnName);
					table.addColumnInPrimaryKey(col);

				}
			}

			foreignKeyGraph = new Graph<Table, ForeignKey>(true);
			// logger.log(Level.INFO,"Create Foreign Key Graph");
			if (Configuration.getProperty("tempDatabaseType").equalsIgnoreCase("sqlite")) {
				if (conn != null && !conn.isClosed()) {
					DatabaseMetaData metaData = conn.getMetaData();
					rs1 = metaData.getTables(database, "", "%", null);
				}
			} else
				rs1 = meta.getTables(database, "", "%", tableFilter);

			while (rs1.next()) {
				String tableName = rs1.getString("TABLE_NAME");
				if (tableName.contains("sqlite"))
					continue;
				if (Configuration.getProperty("tempDatabaseType").equalsIgnoreCase("sqlite")) {
					if (conn != null && !conn.isClosed()) {
						DatabaseMetaData metaData = conn.getMetaData();
						// rs = metaData.getExportedKeys(database, "", tableName);
						rs = metaData.getImportedKeys(database, "", tableName);
					}
				} else
					rs = meta.getExportedKeys(conn.getCatalog(), "", tableName);

				String fkName = "";
				// i++;
				int j = -1;
				while (rs.next()) {
					// if(!rs.getString("FK_NAME").equalsIgnoreCase(""))
					fkName = rs.getString("FK_NAME");

					String fkTableName = rs.getString("FKTABLE_NAME").toUpperCase();
					String fkColumnName = rs.getString("FKCOLUMN_NAME").toUpperCase();
					int seq_no = rs.getInt("KEY_SEQ");
					if (seq_no == 1) {
						j++;
					}
					if (Configuration.getProperty("tempDatabaseType").equalsIgnoreCase("sqlite")
							&& fkName.equalsIgnoreCase("")) {
						fkName = "fk" + j;
					}

					Table fkTable = getTable(fkTableName);
					Column fkColumn = fkTable.getColumn(fkColumnName);
					if (fkColumnName.equals(""))
						continue;

					String pkTableName = rs.getString("PKTABLE_NAME").toUpperCase();
					String pkColumnName = rs.getString("PKCOLUMN_NAME").toUpperCase();

					Table pkTable = getTable(pkTableName);
					pkTable.setIsExportedTable(true);
					Column pkColumn = pkTable.getColumn(pkColumnName);
					pkColumn.setIsUnique(true);
					fkColumn.setReferenceTableName(pkTableName);
					fkColumn.setReferenceColumn(pkColumn);

					ForeignKey fk = fkTable.getForeignKey(fkName);
					fk.addFKeyColumn(fkColumn, seq_no);
					fk.addReferenceKeyColumn(pkColumn, seq_no);
					fk.setReferenceTable(pkTable);
					fkTable.addForeignKey(fk);
				}
			}

			for (String tableName : tables.keySet()) {
				Table table = tables.get(tableName);
				if (table.hasForeignKey()) {
					for (String fKeyName : table.getForeignKeys().keySet()) {
						ForeignKey fKey = table.getForeignKeys().get(fKeyName);
						foreignKeyGraph.add(fKey.getReferenceTable(), table, fKey);
					}
				}
			}

			// deeksha testcode to add cnt attribute in all the relations/tables
			//
			//
			usingCnt = usingCnt;
			if (usingCnt) {
				// for(String tableName : tables.keySet())
				// {
				// Table table = tables.get(tableName);
				// Column cntCol = new Column("XDATA_CNT" , table);
				//
				// cntCol.setCvcDatatype("Int");
				// //cntCol.setColumnSize(rs.getInt("COLUMN_SIZE"));
				// table.addColumn(cntCol);
				// }
				rs = null;
				if (Configuration.getProperty("tempDatabaseType").equalsIgnoreCase("sqlite")) {
					if (conn != null && !conn.isClosed()) {
						DatabaseMetaData metaData = conn.getMetaData();
						rs = metaData.getTables(database, "", "%", null);
					}
				} else
					rs = meta.getTables(database, "", "%", tableFilter);
				while (rs.next()) {
					String tableName = rs.getString("TABLE_NAME");
					if (tables.get(tableName.toUpperCase()) == null)
						continue;

					Table table = getTable(tableName.toUpperCase());
					// if(table == null)
					// continue;
					Column cntCol = new Column("XDATA_CNT", table);
					cntCol.setDataType(Types.INTEGER);

					table.addColumn(cntCol);
				}
			}

			// testcode ends

			topSortedTables = foreignKeyGraph.topSort();
			for (String tableName : tables.keySet()) {
				Table table = tables.get(tableName);
				if (!topSortedTables.contains(table))
					topSortedTables.add(table);
			}

			// conn.close();
		} catch (Exception e) {

			logger.log(Level.SEVERE, "TableMap not created", e);

			e.printStackTrace();
		}
	}

	public Vector<Table> getAllTablesInTopSorted() {
		return topSortedTables;
	}

	public Table getTable(String tableName) {
		return (tables.get(tableName));
	}

	public Graph<Table, ForeignKey> getForeignKeyGraph() {
		return foreignKeyGraph;
	}

	public Connection getConnection() {
		return this.conn;
	}

	public static void main(String args[]) {
		try {
			TableMap tm = TableMap.getInstances(null, 0);
			logger.log(Level.INFO, "Top Sorted Tables : " + tm.getAllTablesInTopSorted());

			/*
			 * Table table = tm.getTable("ROLLHIST");
			 * for(String columnName : table.getColumns().keySet()){
			 * Column col = table.getColumns().get(columnName);
			 * logger.log(Level.INFO,col.getColumnName()+" is Nullable : "+col.isNullable())
			 * ;
			 * }
			 */
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Message", e);
			// e.printStackTrace();;
		}

	}

}