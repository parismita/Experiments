import jpype
import os
import shutil

JARS_DIR = "/home/xdata/jars"
import sys

def prepare_input(basefilepath, datasetPath, queriesPath, mutantsPath, bmname):
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
    shutil.copy(mutantsPath, basefilepath+"/"+bmname + "/mutants/"+bmname+".txt") 
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
    if(len(sys.argv) != 2):
        print("Usage: python3 execute_tests.py <benchmark>")
        exit()
    benchmark = sys.argv[1]
    if(benchmark == "XData-BM" or benchmark == "TPCH" or benchmark == "LargeJoin" or benchmark == "Other"):
        
        basefilepath = "/home/xdata/inputs"
        datasetPath = "/home/benchmarks/" + benchmark+ "/DDL.sql"
        queriesPath = "/home/benchmarks/" + benchmark+ "/queries.txt"
        mutantsPath = "/home/benchmarks/" + benchmark+ "/mutants.txt"
        prepare_input(basefilepath, datasetPath, queriesPath, mutantsPath, benchmark)
        basefilepath = basefilepath + "/" + benchmark
        run_tests(basefilepath)
    else:
        print("Incorrect Usage:\nUse one of the follwoing arguments: XData-BM, TPCH, LargeJoin, Other")


