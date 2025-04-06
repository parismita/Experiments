delete from customer;
delete from lineitem;
delete from nation;
delete from orders;
delete from part;
delete from partsupp;
delete from region;
delete from supplier;


INSERT INTO customer (c_custkey, c_name, c_address, c_nationkey, c_phone, c_acctbal, c_mktsegment, c_comment) VALUES (2, 'Customer000000002', 'XSTf4,NCwDVaWNe6tEgvwfmRchLXak', 13, '237686873665', 121.65, 'AUTOMOBILE', 'l accounts');
INSERT INTO customer (c_custkey, c_name, c_address, c_nationkey, c_phone, c_acctbal, c_mktsegment, c_comment) VALUES (3, 'Customer000000003', 'MG9kdTD2WBHm', 1, '11719748364', 7498.12, 'AUTOMOBILE', ' deposits');
INSERT INTO customer (c_custkey, c_name, c_address, c_nationkey, c_phone, c_acctbal, c_mktsegment, c_comment) VALUES (4, 'Customer000000004', 'XxVSJsLAGtn', 4, '141281905944', 2866.83, 'MACHINERY ', ' requests');



INSERT INTO lineitem (l_orderkey, l_partkey, l_suppkey, l_linenumber, l_quantity, l_extendedprice, l_discount, l_tax, l_returnflag, l_linestatus, l_shipdate, l_commitdate, l_receiptdate, l_shipinstruct, l_shipmode, l_comment) VALUES (2, 1062, 33, 1, 38.00, 396.28, 0.00, 0.05, 'N', 'O', '1997-01-28', '1997-01-14', '1997-02-02', 'TAKE BACK RETURN ', 'RAIL', 'requests');
INSERT INTO lineitem (l_orderkey, l_partkey, l_suppkey, l_linenumber, l_quantity, l_extendedprice, l_discount, l_tax, l_returnflag, l_linestatus, l_shipdate, l_commitdate, l_receiptdate, l_shipinstruct, l_shipmode, l_comment) VALUES (3, 43, 19, 1, 45.00, 4246.80, 0.06, 0.00, 'R', 'F', '1994-02-02', '1994-01-04', '1994-02-23', 'NONE ', 'AIR ', 'requests');
INSERT INTO lineitem (l_orderkey, l_partkey, l_suppkey, l_linenumber, l_quantity, l_extendedprice, l_discount, l_tax, l_returnflag, l_linestatus, l_shipdate, l_commitdate, l_receiptdate, l_shipinstruct, l_shipmode, l_comment) VALUES (3, 1285, 60, 3, 27.00, 329.56, 0.06, 0.07, 'A', 'F', '1994-01-16', '1993-11-22', '1994-01-23', 'DELIVER IN PERSON', 'SHIP', 'requests');
INSERT INTO lineitem (l_orderkey, l_partkey, l_suppkey, l_linenumber, l_quantity, l_extendedprice, l_discount, l_tax, l_returnflag, l_linestatus, l_shipdate, l_commitdate, l_receiptdate, l_shipinstruct, l_shipmode, l_comment) VALUES (3, 294, 22, 4, 2.00, 238.58, 0.01, 0.06, 'A', 'F', '1993-12-04', '1994-01-07', '1994-01-01', 'NONE ', 'TRUCK ', 'requests');


INSERT INTO nation (n_nationkey, n_name, n_regionkey, n_comment) VALUES (0, 'ALGERIA', 0, 'haggle');
INSERT INTO nation (n_nationkey, n_name, n_regionkey, n_comment) VALUES (1, 'ARGENTINA', 1, 'haggle');
INSERT INTO nation (n_nationkey, n_name, n_regionkey, n_comment) VALUES (2, 'BRAZIL ', 1, 'haggle');




INSERT INTO orders (o_orderkey, o_custkey, o_orderstatus, o_totalprice, o_orderdate, o_orderpriority, o_clerk, o_shippriority, o_comment) VALUES (1, 370, 'O', 1729.49, '1996-01-02', '5LOW', 'Clerk000000951', 0, 'nstructions');
INSERT INTO orders (o_orderkey, o_custkey, o_orderstatus, o_totalprice, o_orderdate, o_orderpriority, o_clerk, o_shippriority, o_comment) VALUES (2, 781, 'O', 386.09, '1996-12-01', '1URGENT ', 'Clerk000000880', 0, 'asymptot');
INSERT INTO orders (o_orderkey, o_custkey, o_orderstatus, o_totalprice, o_orderdate, o_orderpriority, o_clerk, o_shippriority, o_comment) VALUES (3, 1234, 'F', 2054.30, '1993-10-14', '5LOW', 'Clerk000000955', 0, 'sly');



INSERT INTO part (p_partkey, p_name, p_mfgr, p_brand, p_type, p_size, p_container, p_retailprice, p_comment) VALUES (1, 'goldenrod', 'Manufacturer1 ', 'Brand13', 'COPPER', 7, 'JUMBO PKG ', 901.00, 'ly');
INSERT INTO part (p_partkey, p_name, p_mfgr, p_brand, p_type, p_size, p_container, p_retailprice, p_comment) VALUES (2, 'blush', 'Manufacturer1 ', 'Brand13', 'BRASS', 1, 'LG CASE ', 902.00, 'lar');
INSERT INTO part (p_partkey, p_name, p_mfgr, p_brand, p_type, p_size, p_container, p_retailprice, p_comment) VALUES (3, 'sprin', 'Manufacturer4 ', 'Brand42', 'POLISHED BRASS', 21, 'WRAP CASE ', 903.00, 'egularg');




INSERT INTO partsupp (ps_partkey, ps_suppkey, ps_availqty, ps_supplycost, ps_comment) VALUES (1, 2, 3325, 771.64, 'even');
INSERT INTO partsupp (ps_partkey, ps_suppkey, ps_availqty, ps_supplycost, ps_comment) VALUES (2, 3, 8895, 378.49, 'nic');
INSERT INTO partsupp (ps_partkey, ps_suppkey, ps_availqty, ps_supplycost, ps_comment) VALUES (3, 4, 4651, 920.92, 'ilent');



INSERT INTO region (r_regionkey, r_name, r_comment) VALUES (0, 'AFRICA ', 'lar');
INSERT INTO region (r_regionkey, r_name, r_comment) VALUES (1, 'AMERICA', 'haa');
INSERT INTO region (r_regionkey, r_name, r_comment) VALUES (2, 'ASIA ', 'ges');
INSERT INTO region (r_regionkey, r_name, r_comment) VALUES (3, 'EUROPE ', 'slylyn');
INSERT INTO region (r_regionkey, r_name, r_comment) VALUES (4, 'MIDDLE EAST', 'slylyn');




INSERT INTO supplier (s_suppkey, s_name, s_address, s_nationkey, s_phone, s_acctbal, s_comment) VALUES (1, 'Supplier000000001 ', ' N kD4on9OM Ipw3,gf0JBoQDd7tgrzrddZ', 17, '279183351736', 5755.94, 'each');
INSERT INTO supplier (s_suppkey, s_name, s_address, s_nationkey, s_phone, s_acctbal, s_comment) VALUES (2, 'Supplier000000002 ', '89eJ5ksX3ImxJQBvxObC,', 5, '156798612259', 4032.68, 'slylyn');
INSERT INTO supplier (s_suppkey, s_name, s_address, s_nationkey, s_phone, s_acctbal, s_comment) VALUES (3, 'Supplier000000003 ', 'q1,G3Pj6OjIuUYfUoH18BFTKP5aU9bEV3', 1, '113835161199', 4192.40, 'blithely');
INSERT INTO supplier (s_suppkey, s_name, s_address, s_nationkey, s_phone, s_acctbal, s_comment) VALUES (4, 'Supplier000000004 ', 'Bk7ah4CK8SYQTepEmvMkkgMwg', 15, '258437877479', 4641.08, 'riously');
INSERT INTO supplier (s_suppkey, s_name, s_address, s_nationkey, s_phone, s_acctbal, s_comment) VALUES (5, 'Supplier000000005 ', 'Gcdm2rJRzl5qlTVzc', 11, '211516903663', -283.84, '. slyly');
INSERT INTO supplier (s_suppkey, s_name, s_address, s_nationkey, s_phone, s_acctbal, s_comment) VALUES (6, 'Supplier000000006 ', 'tQxuVm7s7CnK', 14, '246969974969', 1365.79, 'slylyn');
INSERT INTO supplier (s_suppkey, s_name, s_address, s_nationkey, s_phone, s_acctbal, s_comment) VALUES (7, 'Supplier000000007 ', 's,4TicNGB4uO6PaSqNBUq', 23, '339909652201', 6820.35, 'slylyn');

