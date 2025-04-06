CREATE TABLE customer (
    c_custkey integer NOT NULL,
    c_name varchar(25) NOT NULL,
    c_address varchar(40) NOT NULL,
    c_nationkey integer NOT NULL,
    c_phone varchar(15) NOT NULL,
    c_acctbal numeric(15,2) NOT NULL,
    c_mktsegment varchar(10) NOT NULL,
    c_comment varchar(117) NOT NULL
);


CREATE TABLE lineitem (
    l_orderkey integer NOT NULL,
    l_partkey integer NOT NULL,
    l_suppkey integer NOT NULL,
    l_linenumber integer NOT NULL,
    l_quantity numeric(15,2) NOT NULL,
    l_extendedprice numeric(15,2) NOT NULL,
    l_discount numeric(15,2) NOT NULL,
    l_tax numeric(15,2) NOT NULL,
    l_returnflag character(1) NOT NULL,
    l_linestatus character(1) NOT NULL,
    l_shipdate date NOT NULL,
    l_commitdate date NOT NULL,
    l_receiptdate date NOT NULL,
    l_shipinstruct varchar(25) NOT NULL,
    l_shipmode varchar(10) NOT NULL,
    l_comment varchar(44) NOT NULL
);

CREATE TABLE nation (
    n_nationkey integer NOT NULL,
    n_name varchar(25) NOT NULL,
    n_regionkey integer NOT NULL,
    n_comment varchar(152)
);



CREATE TABLE orders (
    o_orderkey integer NOT NULL,
    o_custkey integer NOT NULL,
    o_orderstatus character(1) NOT NULL,
    o_totalprice numeric(15,2) NOT NULL,
    o_orderdate date NOT NULL,
    o_orderpriority varchar(15) NOT NULL,
    o_clerk varchar(15) NOT NULL,
    o_shippriority integer NOT NULL,
    o_comment varchar(79) NOT NULL
);



CREATE TABLE part (
    p_partkey integer NOT NULL,
    p_name varchar(55) NOT NULL,
    p_mfgr varchar(25) NOT NULL,
    p_brand varchar(10) NOT NULL,
    p_type varchar(25) NOT NULL,
    p_size integer NOT NULL,
    p_container varchar(10) NOT NULL,
    p_retailprice numeric(15,2) NOT NULL,
    p_comment varchar(23) NOT NULL
);


CREATE TABLE partsupp (
    ps_partkey integer NOT NULL,
    ps_suppkey integer NOT NULL,
    ps_availqty integer NOT NULL,
    ps_supplycost numeric(15,2) NOT NULL,
    ps_comment varchar(199) NOT NULL
);


CREATE TABLE region (
    r_regionkey integer NOT NULL,
    r_name varchar(25) NOT NULL,
    r_comment varchar(152)
);


CREATE TABLE supplier (
    s_suppkey integer NOT NULL,
    s_name varchar(25) NOT NULL,
    s_address varchar(40) NOT NULL,
    s_nationkey integer NOT NULL,
    s_phone varchar(15) NOT NULL,
    s_acctbal numeric(15,2) NOT NULL,
    s_comment varchar(101) NOT NULL
);

ALTER TABLE ONLY customer
    ADD CONSTRAINT customer_pkey PRIMARY KEY (c_custkey);


--
-- Name: lineitem_pkey; Type: CONSTRAINT; Schema: tpch1; Owner: tpch1; Tablespace: 
--

ALTER TABLE ONLY lineitem
    ADD CONSTRAINT lineitem_pkey PRIMARY KEY (l_orderkey, l_linenumber);


--
-- Name: nation_pkey; Type: CONSTRAINT; Schema: tpch1; Owner: tpch1; Tablespace: 
--

ALTER TABLE ONLY nation
    ADD CONSTRAINT nation_pkey PRIMARY KEY (n_nationkey);


--
-- Name: orders_pkey; Type: CONSTRAINT; Schema: tpch1; Owner: tpch1; Tablespace: 
--

ALTER TABLE ONLY orders
    ADD CONSTRAINT orders_pkey PRIMARY KEY (o_orderkey);


--
-- Name: part_pkey; Type: CONSTRAINT; Schema: tpch1; Owner: tpch1; Tablespace: 
--

ALTER TABLE ONLY part
    ADD CONSTRAINT part_pkey PRIMARY KEY (p_partkey);


--
-- Name: partsupp_pkey; Type: CONSTRAINT; Schema: tpch1; Owner: tpch1; Tablespace: 
--

ALTER TABLE ONLY partsupp
    ADD CONSTRAINT partsupp_pkey PRIMARY KEY (ps_partkey, ps_suppkey);



--
-- Name: region_pkey; Type: CONSTRAINT; Schema: tpch1; Owner: tpch1; Tablespace: 
--

ALTER TABLE ONLY region
    ADD CONSTRAINT region_pkey PRIMARY KEY (r_regionkey);


--
-- Name: supplier_pkey; Type: CONSTRAINT; Schema: tpch1; Owner: tpch1; Tablespace: 
--

ALTER TABLE ONLY supplier
    ADD CONSTRAINT supplier_pkey PRIMARY KEY (s_suppkey);

