import jpype
import os
import shutil
import configparser

JARS_DIR = "/home/xdata/jars"
import sys

def prepare_input(basefilepath, datasetPath, queriesPath, bmname):
    path = os.path.join(basefilepath, bmname) 
    if not os.path.exists(path):
        os.mkdir(path) 
        
    path = os.path.join(basefilepath+"/"+bmname, "datasets") 
    os.makedirs(path, exist_ok=True)

    path = os.path.join(basefilepath+"/"+bmname, "queries") 
    os.makedirs(path, exist_ok=True)

    path = os.path.join(basefilepath+"/"+bmname, "mutants") 
    os.makedirs(path, exist_ok=True)

    path = os.path.join(basefilepath+"/"+bmname, "sample") 
    os.makedirs(path, exist_ok=True)

   
    shutil.copy(datasetPath, basefilepath+"/"+bmname + "/datasets/"+bmname+".sql") 
    shutil.copy(queriesPath, basefilepath+"/"+bmname + "/queries/"+bmname+".txt") 
    
    with open(basefilepath+"/"+bmname + "/mutants/"+bmname+".txt", 'w') as empty_file:
        pass
    
    import json

    with open(basefilepath+"/"+bmname + "/sample/" + bmname +".json", 'w') as f:
        json.dump({}, f)

    return basefilepath

def run_tests(basefilepath):
    
    LIST_OF_JARS = [os.path.abspath(os.path.join(JARS_DIR, jar_file_name))
                    for jar_file_name in os.listdir(JARS_DIR)
                    if jar_file_name.endswith(".jar")]

    LIST_OF_JARS.append(os.path.abspath(os.getcwd()))

    if not jpype.isJVMStarted():
        jpype.startJVM(classpath=LIST_OF_JARS)


    regression_tests = jpype.JClass('test.RegressionTests')

    # basefilepath = prepare_input()
    outputPath = "/home/xdata/output"

    
    args = jpype.JArray(jpype.JString)([basefilepath, "datasets","", "queries", "mutants","",outputPath])

    regression_tests.main(args)

    jpype.shutdownJVM()


if __name__ == "__main__":
    
    config = configparser.ConfigParser()
    config.read('query_config.cfg')

    # Access the schema_path and query_path
    datasetPath = config['paths']['datasetPath']
    queriesPath = config['paths']['queriesPath']

    basefilepath = "/home/xdata/inputs"
    prepare_input(basefilepath, datasetPath, queriesPath, "custom")
    basefilepath = basefilepath + "/custom" 
    run_tests(basefilepath)