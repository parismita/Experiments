import json
import random
import string
import jsonlines

exist = {}
addtype = set()
additional = ""
dic = {}
def getOperator(op):
    if op == "gte":
        return " >= "
    if op == "lte":
        return " <= "
    if op == "lt":
        return " < "
    if op == "gt":
        return " > "
    if op == "neq":
        return " <> "
    
def generate_sql_add_foreign_keys(constraints_dict):
        # Add foreign key constraints
        schemaval = ""
        fk_set = set()
        un_set = set()
        if(constraints_dict == None):
            constraints_dict = []
        
        for const in constraints_dict:
            # print(const.keys())
            for val in const.keys() :
                dic[val] = 0
            fk_list = const.get('foreign', {})
            # print(fk_list)
            fk_child_table = ""
            fk_column = ""
            fk_parent_table = ""
            fk_pk_column = ""
            if(fk_list):
                for i in range(0, len(fk_list)):
                    # print(fk_list[i])
                    fk = fk_list[i]['value']
                    if(i == 0) :
                        fk_child_table = fk.split("__")[0]
                        fk_column = fk.split("__")[1]
                    if(i == 1):
                        fk_parent_table = fk.split("__")[0]
                        fk_pk_column = fk.split("__")[1]
      
                fk_constraint_name = f"FK_{fk_child_table}_{fk_column}"
                #
                if(fk_parent_table not in un_set and (fk_parent_table not in exist or exist[fk_parent_table]!=fk_pk_column)):
                    schemaval = schemaval + f"ALTER TABLE {fk_parent_table} ADD CONSTRAINT un_{fk_parent_table} UNIQUE ({fk_pk_column});\n\n"
                    un_set.add(fk_parent_table)
                if(fk_constraint_name not in fk_set):
                    schemaval = schemaval + "ALTER TABLE "+ fk_child_table + "\n" + "ADD CONSTRAINT " + fk_constraint_name +" FOREIGN KEY (" + fk_column + ")\n" + "REFERENCES " + fk_parent_table + "( " + fk_pk_column + ");\n\n"
                    fk_set.add(fk_constraint_name)
                
                # sql_file.write(f"ALTER TABLE {fk_child_table}\n")
                # sql_file.write(f"ADD CONSTRAINT {fk_constraint_name} FOREIGN KEY ({fk_column})\n")
                # sql_file.write(f"REFERENCES {fk_parent_table}({fk_pk_column});\n\n")
        
        return schemaval
def generate_check_constraints(constraints_dict):
    if(constraints_dict == None):
            constraints_dict = []
    inlist = {}
    additional = ""
    for const in constraints_dict:
            # print(const.keys())    
        inlist = const.get('between', {})
        if(inlist):
            column_name = inlist[0].get("value").split("__")[1]
            table_name = inlist[0].get("value").split("__")[0]
            lb = inlist[1]
            ub = inlist[2]
            additional = additional + "ALTER TABLE " + table_name + " ADD CHECK " + "(" + column_name + " >= " + str(lb) + " and "+ column_name + " <= " + str(ub) + ");\n"
        
        operators = ["gte", "lte", "lt", "gt", "neq"]
        for op in operators :
            inlist = const.get(op, {})
            if(inlist):
                column_name = inlist[0].get("value").split("__")[1]
                table_name = inlist[0].get("value").split("__")[0]
                lb = inlist[1]
                # ub = inlist[2]
                if(type(lb) == int or type(lb) == float):
                    additional = additional + "ALTER TABLE " + table_name + " ADD CHECK " + "(" + column_name + getOperator(op) + str(lb) + ");\n"
        
    return additional
                      
def generate_sql_from_schema(schema_dict, constraints_dict):
    # with open(output_file, 'w') as sql_file:
        schemaval = ""
        additional = ""
        for table_name, attributes in schema_dict.items():
            # Create SQL CREATE TABLE statement
            schemaval = schemaval + "CREATE TABLE " + table_name + " (\n"
            # sql_file.write(f"CREATE TABLE {table_name} (\n")
            columns = []
            pk_all = ""
            for column_name, column_info in attributes.items():
                column_type = column_info
                if(column_info=='None' or column_info=='' or column_info==None):
                	column_type='INT'
                elif(column_info.upper() not in ['INT', 'TEXT', 'VARCHAR', 'CHAR', 'DATE', 'TIME', 'TIMESTAMP', 'FLOAT', 'DOUBLE']):
                	if column_info.upper()[:4] == 'ENUM':
                	    enum = column_info.split(",")[1:]
                	    enum = "'"+"','".join(enum)+"'"
                	    a=f"type_{table_name}_{column_name}"
                	    if f"type_{table_name}_{column_name}" not in addtype:
                             additional = additional + "ALTER TABLE " + table_name + " ADD CHECK " + "(" + column_name + " in (" + enum +"))\n"
                          # additional = additional + f"CREATE TYPE {a} AS ENUM('{enum}');\n"
                        #   +  "ALTER TABLE " + table_name + " ADD CHECK " + "(" + column_name + " in " + enum ")" 
                	    addtype.add(a)
                	    column_type = a
                	    column_type = 'VARCHAR'
                columns.append(f"    {column_name} {column_type}")
                pk_all = pk_all + column_name + ","

            # Add primary key constraint
            if(constraints_dict == None):
                constraints_dict = []

            pk_columns = set()
            for const in constraints_dict:
                pklist = {}
                for pk in const.get('primary', {}):
                    pk = pk['value']
                    
                    table_namepk = pk.split("__")[0]
                    pk_col = pk.split("__")[1]
                    
                    if(table_namepk == table_name):
                        pk_columns.add(pk_col)
            pk_columns_str=""
            for i in pk_columns:
                pk_columns_str = pk_columns_str + i + ","  
                         
            if pk_columns_str != "":
                columns.append(f"    CONSTRAINT PK_{table_name} PRIMARY KEY ({pk_columns_str[:-1]})")
            else: 
                columns.append(f"    CONSTRAINT PK_{table_name} PRIMARY KEY ({pk_all[:-1]})")
            
            schemaval = schemaval +   ",\n".join(columns) + "\n);\n\n"
            # sql_file.write(",\n".join(columns))
            # sql_file.write("\n);\n\n")
        schemaval += additional
        return schemaval
            
          
def generate_random_data(column_info):
    if "INT" in column_info:
        return 1
        #return random.randint(0, 100)
    elif "FLOAT" in column_info or "DOUBLE" in column_info:
        return 1
        #return round(random.uniform(0, 100), 2)
    elif "CHAR" in column_info or "TEXT" in column_info or "VARCHAR" in column_info:
        return 'a'
        #return ''.join(random.choice(string.ascii_letters) for i in range(3))
    elif "ENUM" in column_info:
        return column_info.split(",")[1]
    else:
        return ''

def save_as_json(data, output_path):
    with open(output_path, 'w') as json_file:
        json.dump(data, json_file, indent=4)

def generate_sample_data(schema_dict, output_path, num_samples=1):
    sample_data = {}
    # for table_name, attributes in schema_dict.items():
    #     for column_name, column_info in attributes.items():
    #         if(column_info=='None' or column_info=='' or column_info==None):
    #             sample_data[column_name]=[1]
    #         else:
    #             sample_data[column_name] = [generate_random_data(column_info) for _ in range(num_samples)]
    save_as_json(sample_data, output_path)

def count_unique_schemas(jsonlines_file):
    schema_count = {}
    schema_map = {}
    
    with jsonlines.open(jsonlines_file) as reader:
        for obj in reader:
            schema = obj.get('schema')
            constraints = obj.get('constraint')
            schemaval = ""
            
            # generate_sql_from_schema(schema, "/home/sunanda/Course-Files-IITB/xdata-paper-exp/benchmarks/leetcode.sql")
            
            schemaval = schemaval + generate_sql_from_schema(schema, constraints)
            schemaval = schemaval + generate_sql_add_foreign_keys(constraints)
            schemaval = schemaval + generate_check_constraints(constraints)
            if schemaval in schema_count:
                schema_count[schemaval] += 1
            else:
                schema_count[schemaval] = 1  
                schema_map[schemaval] = schema          
            
        
    return schema_count, schema_map

def get_schemawise_queries(jsonlines_file, schemapassed):
    schemawise_info = {}
    queries = {}
    with jsonlines.open(jsonlines_file) as reader:
        for obj in reader:
            pair = obj.get('pair')
            schema = obj.get('schema')
            constraints = obj.get('constraint')
            schemaval = ""
            schemaval = schemaval + generate_sql_from_schema(schema, constraints)
            schemaval = schemaval + generate_sql_add_foreign_keys(constraints)
            schemaval = schemaval + generate_check_constraints(constraints)
            if(schemapassed == schemaval):
                orignalQ = pair[0]
                mutantQ = pair[1]
                if(orignalQ in queries):
                    queries[orignalQ].append(mutantQ)
                else :
                    queries[orignalQ] = [mutantQ]
                    
        if schemapassed in schemawise_info:
            schemawise_info[schemapassed].append(queries)
        else :
            schemawise_info[schemapassed] = [queries]
    return schemawise_info
 

           
# Example usage:
if __name__ == "__main__":
    basepath = "/home/sunanda/xdata/XData-DataGen/XData/test/verieql/"
    datasett = "leetcode-uns"
    jsonlines_file = basepath+"bm-outputs/" +datasett +"/" + datasett +".jsonlines"  # Replace with your JSON Lines file path
    schema_count, schema_map = count_unique_schemas(jsonlines_file)
    
    # Print unique schemas and their counts
    cnt = 1
    qid = 1
    for schema, count in schema_count.items():
        schemaoutput_file = basepath+"/benchmarks/"+datasett+"/dataset/"+datasett + str(cnt) +".sql"
       
        schemawise_info = get_schemawise_queries(jsonlines_file, schema)
        with open(schemaoutput_file, 'w') as sql_file:
                if cnt==1:
                    sql_file.write(schema + additional)
                else:
                    sql_file.write(schema)  
        # print(f"SchemaId: {cnt},Schema: {schema}, Count: {count}")
        
        sampleoutput_file = basepath+"/benchmarks/"+datasett+"/sample/"+datasett + str(cnt) +".json"
        print(sampleoutput_file)

        sample = generate_sample_data(schema_map[schema], sampleoutput_file)

        queriesoutput_file = basepath+"/benchmarks/"+datasett+"/queries/"+datasett + str(cnt) +".txt"
        print(queriesoutput_file)

        mutantsoutput_file = basepath+"/benchmarks/"+datasett+"/mutants/"+datasett + str(cnt) +".txt"
        print(mutantsoutput_file)

        mutants = []
        queries = []
        
        for query in schemawise_info[schema] :   
            for key in query :
                
                updatedQ = str(qid) + "|"+"single|" + key 
                queries.append(updatedQ)
                
                for mutant in query[key]:
                    updatedM = str(qid) + "|" + "mutant|" + mutant 
                    mutants.append(updatedM)
                qid+=1
                    
        with open(queriesoutput_file, 'w') as sql_file:
            for item in queries :
                sql_file.write(item+"\n")
        
        with open(mutantsoutput_file, 'w') as sql_file:
            for item in mutants:
                sql_file.write(item+"\n")
                
        cnt+=1
    print(dic)
        
        
      
