In this Repo, XXXX-2-BM corresponds to MutBM and XXXX-2 corresponds to AnonM

# View benchmarks used
- We present results for 3 benchmarks: XData-BM, LargeJoin, TPCH.
- `/benchamrks/<benchmark-name>` directory contains following files.
  - Schema file (.sql). (eg. DDL.sql)
  - Queries file (.txt). Format to write queries is "queryID|queryType|query"
  - Mutants file (.txt). Format to write queries is "queryID|mutantType|mutant". Note mutants corresponding to each queryID are given.

# View precomputed results for benchmarks
- Precomputed `<benchmark>-mutant-result.xslx` files are present in /results folder

# Set Up - XData
Note: Currently supported for Linux and Windows

- Clone this repo using `git clone` command
  - `git clone https://gitlab.com/xdata/xdatapublic.git`

- Install Docker to setup the container.
  - Follow the steps here: https://docs.docker.com/engine/install/

- To build container for the first time
  - Run `chmod +x xdata.sh` then
  - Run `./xdata.sh build` (run with sudo if user does not have permission)
  - This will take approximately 5 mins

- To run the container
  - Run `./xdata.sh run` (run with sudo if user does not have permission)


# Run XData
- Run `source vrtuenv/bin/activate` to start virtual env
- Run `python3 execute_tests.py <benchmark> | tee <benchmark>-datagen.log`
  - `<benchmark>` options:
    - `XData-BM`: Run`python3 execute_tests.py XData-BM | tee XData-BM-datagen.log`
      - avg time to run 30 mins
    - `LargeJoin`: Run `python3 execute_tests.py LargeJoin | tee LargeJoin-datagen.log`
      - avg time to run 5 mins
    - `TPCH`: Run `python3 execute_tests.py TPCH | tee TPCH-datagen.log`
      - avg time to run 10 mins
    - `Other`: Run `python3 execute_tests.py Other | tee Other-datagen.log`
  - First three benchmarks to be used to replicate results
  - To run XData on any other set of query mutant pairs
    - Modify `benchmarks/Other/DDL.sql' with postgreSQL compatible schema
    - Modify `benchmarks/Other/queries.txt'
    - Modify `benchmarks/Other/mutants.txt'
    - Then rebuild the setup and run execute_tests.py

- To run xdata three input files are required. Place all the three files in benchmarks folder
    - Schema file (.sql). (eg. DDL.sql)
    - Queries file (.txt). Format to write queries is "queryID|queryType|query"
    - Mutants file (.txt). Format to write queries is "queryID|mutantType|mutant". Note mutants corresponding to each queryID are given.
    - This version does not require sample data as input

- Run `python3 execute_query.py` to run a single query. The schema and query paths are to be mentioned at `query_config.cfg`

# Reading Output after running XData

- `/home/xdata/output/<benchmark>-mutant-results.out` file contains output state(Killed/NotKilled/Unsupported) for each mutant-query pair. 
  - Note: Precomputed .xslx files are present in /results folder (outside of the container)
- Directories `/home/xdata/tmp/temp_smt/<query_id>` contains the datasets (.sql files) which are generated for each original query
