package parsing;

import java.io.Serializable;
import java.util.*;

import parsing.Column;
import parsing.ForeignKey;
/**
 * 
 * @author Bhanu Pratap Gupta
 * modified by Sandeep Patidar
 *
 */
public class Table implements Serializable, Cloneable{
	
	private static final long serialVersionUID = -280379039351408487L;
	private String queryId;
	private String tableName;
	private String aliasName;
	private Map<String,ForeignKey> fKeys;
	private Vector<Column> primaryKey;
	private Vector<Column> nonNullKey;
	private String sqTableType;
	private Boolean isExist;
	//private Map<String,Table> referenceTableMap;

	//private Vector<Column> columns= null;
	private HashMap<String,Column> columns = null;
	private Vector<String> columnsIndex = null;
	private boolean isExportedTable = false;

	/////// Vector for getting values from database
	private HashMap<String,String> hm;

	public void intializeCtidRowHashMap(){
		this.hm = new HashMap<String,String>();
	}

	public void addCtidRow(String ctid, String ctidRow){
		this.hm.put(ctid, ctidRow);
	}

	public HashMap getCtidRow(){
		return this.hm;
	}

	public void addColumnInPrimaryKey(Column col){
		this.primaryKey.add(col);
	}

	public Vector<Column> getPrimaryKey(){
		return this.primaryKey;
	}
	public Vector<Column> getNonNullKey(){
		return this.nonNullKey;
	}
	public void addColumnInNonNullKey(Column col){
		this.nonNullKey.add(col);
	}

	public String getTableName() {
		return tableName;
	}
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}
	public String getAliasName() {
		return aliasName;
	}
	public void setAliasName(String aliasName) {
		this.aliasName = aliasName;
	}
	//parismita
	public String getSQType() {
		return this.sqTableType;
	}
	public void setSQType(String type) {
		this.sqTableType = type;
	}
	public Boolean getIsExist() {
		return this.isExist;
	}
	public void setIsExist(Boolean type) {
		this.isExist = type;
	}
	//end
	
	public String getQueryId() {
		return queryId;
	}
	public void setQueryId(String queryId) {
		this.queryId = queryId;
	}
	public String getLastColumnKey() {
		Map.Entry<String, Column> lastEntry = null;
		for (Map.Entry<String, Column> entry : columns.entrySet()) {
			lastEntry = entry;
		}
	
		return lastEntry.getKey();
	}
	
	public Column getLastColumnValue() {
	Map.Entry<String, Column> lastEntry = null;
	for (Map.Entry<String, Column> entry : columns.entrySet()) {
		lastEntry = entry;
	}
	
	return lastEntry.getValue();
	}
	

	public Table(String tableName){
		//tableName = tableName.toUpperCase();
		this.tableName = tableName;
		//columns = new Vector<Column>();
		columns = new LinkedHashMap<String, Column>();
		primaryKey = new Vector<Column>();
		nonNullKey = new Vector<Column>();
		columnsIndex = new Vector<String>();
	}

	/**
	 * Copy constructor. Added by Mahesh
	 * @param table
	 */
	public Table(Table table){
		if(table == null)
			return;
		if(table.getQueryId() == null)
			this.queryId = null;
		else
			this.queryId = new String(table.getQueryId());
		if(table.getTableName() == null)
			this.tableName = null;
		else
			this.tableName = new String(table.getTableName());
		if(table.getAliasName() == null)
			this.aliasName = null;
		else
			this.aliasName = new String(table.getAliasName());
		if(table.getForeignKeys() == null)
			this.fKeys = null;
		else
			this.fKeys = new HashMap<String, ForeignKey>(table.getForeignKeys());
		if(table.getPrimaryKey() == null)
			this.primaryKey = null;
		else
			this.primaryKey = new Vector<Column>(table.getPrimaryKey());
		if(table.getNonNullKey() == null)
			this.nonNullKey = null;
		else
			this.nonNullKey = new Vector<Column>(table.getNonNullKey());
		
		if(table.getColumns() == null)
			this.columns = null;
		else
			this.columns = new LinkedHashMap<String, Column>(table.getColumns());
		if(table.getColumnIndexList() == null)
			this.columnsIndex =null;
		else
			this.columnsIndex = new Vector<String>(table.getColumnIndexList());
		this.isExportedTable = table.isExportedTable();
		if(table.hm == null)
			this.hm =null;
		else
			this.hm = new HashMap<String, String>(table.getHm());
	}

	public Vector<String> getColumnIndexList(){
		return this.columnsIndex;
	}

	public HashMap<String, String> getHm(){
		return this.hm;
	}
	public void addColumn(Column column){
		//columns.add(column);
		columns.put(column.getColumnName(),column);
		columnsIndex.add(column.getColumnName());
	}  
	//deeksha testcode to remove column
	public void removeColumn(Column column){
		//columns.add(column);
		columns.remove(column);
//		columns.put(column.getColumnName(),column);
		columnsIndex.remove(column.getColumnName());
	} 
	//testcode ends
	/* public void deleteColumn(int index){
        String columnName = columns.get(index).getColumnName();
        columns.remove(index);
        columnMap.remove(columnName);                        
    }*/

	public HashMap<String,Column> getColumns(){
		return columns;
	}

	public int getNoOfColumn(){
		return columns.size();        
	}

	public Column getColumn(int index){
		return columns.get(columnsIndex.get(index));
	}

	public Column getColumn(String columnName){           
		return columns.get(columnName);
	}

	public int getColumnIndex(String columnName){
		return columnsIndex.indexOf(columnName);
	}
	/*
    public int getColumnIndex(Column column){
    	return 0;
    }*/

	public boolean isExportedTable(){
		return isExportedTable;
	}

	public void setIsExportedTable(boolean isExportedTable){
		this.isExportedTable = isExportedTable;
	}

	public void addForeignKey(ForeignKey key){
		fKeys.put(key.getKeyName(),key);
	}

	/*public void addReferenceTable(Table table){
    	if(referenceTableMap==null)
    		referenceTableMap = new HashMap<String, Table>();
    	if(!referenceTableMap.containsKey(table.getTableName()))
    	referenceTableMap.put(table.getTableName(), table);
    }*/

	/*public Map<String,Table> getReferenceTables(){
    	return referenceTableMap;
    }*/

	public ForeignKey getForeignKey(String keyName){
		if(fKeys==null)
			fKeys = new HashMap<String, ForeignKey>();

		ForeignKey fKey = fKeys.get(keyName);
		if(fKey==null){
			fKey = new ForeignKey(keyName);
		}
		return fKey;
	}

	public Map<String,ForeignKey> getForeignKeys(){
		return fKeys;
	}

	public Map<String,ForeignKey> getForeignKeys(String pkTableName){
		Map<String,ForeignKey> fKeys = null;
		for(String keyName : getForeignKeys().keySet()){
			ForeignKey fKey = getForeignKeys().get(keyName);
			if(fKey.getReferenceTable().getTableName().equalsIgnoreCase(pkTableName)){
				if(fKeys == null)
					fKeys = new HashMap<String, ForeignKey>();
				fKeys.put(keyName,getForeignKeys().get(keyName) );
			}
		}
		return fKeys;
	}

	public boolean hasForeignKey(){
		if(getForeignKeys() != null){
			return true;
		}
		return false;
	}


	@Override
	public String toString(){
		return tableName;
	}

	public boolean allColsNullable() {
		Iterator<String> cols = columns.keySet().iterator();
		while(cols.hasNext()){
			Column col = columns.get(cols.next());
			if(!col.isNullable()){
				return false;
			}
		}
		return true;
	}

	//Added by Mahesh
	//@Override
//	public int hashCode() {
//		final int prime = 31;
//		int result = 1;
//		result = prime * result
//				+ ((aliasName == null) ? 0 : aliasName.hashCode());
//		result = prime * result + ((columns == null) ? 0 : columns.hashCode());
//		result = prime * result
//				+ ((columnsIndex == null) ? 0 : columnsIndex.hashCode());
//		result = prime * result + ((fKeys == null) ? 0 : fKeys.hashCode());
//		result = prime * result + ((hm == null) ? 0 : hm.hashCode());
//		result = prime * result + (isExportedTable ? 1231 : 1237);
//		result = prime * result
//				+ ((primaryKey == null) ? 0 : primaryKey.hashCode());
//		result = prime * result
//				+ ((tableName == null) ? 0 : tableName.hashCode());
//		return new Integer(result).hashCode();
//	}
//
//	@Override
//	public boolean equals(Object obj) {
//		if (this == obj)
//			return true;
//		if (obj == null)
//			return false;
//		if (getClass() != obj.getClass())
//			return false;
//		Table other = (Table) obj;
//		if (aliasName == null) {
//			if (other.aliasName != null)
//				return false;
//		} else if (!aliasName.equals(other.aliasName))
//			return false;
//		
//		if (tableName == null) {
//			if (other.tableName != null)
//				return false;
//		} else if (!tableName.equals(other.tableName))
//			return false;
//		return true;
//	}

@Override
	public Table clone() throws CloneNotSupportedException{	
			
				// Create a new Table object
				Table clonedTable = (Table) super.clone();
	
				// Clone or copy all the non-primitive fields
				if(this.fKeys != null)
					clonedTable.fKeys = new HashMap<>(this.fKeys);
				if(this.primaryKey != null)
					clonedTable.primaryKey = new Vector<>(this.primaryKey);
				if(this.nonNullKey != null)
					clonedTable.nonNullKey = new Vector<>(this.nonNullKey);
				if(this.columns != null)
					clonedTable.columns = new HashMap<>(this.columns);
				if(this.columnsIndex != null)
					clonedTable.columnsIndex = new Vector<>(this.columnsIndex);
	
				// If you have other non-primitive fields, clone or copy them here
	
				return clonedTable;
			

		}

}

