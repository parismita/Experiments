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
INSERT INTO customer (c_custkey, c_name, c_address, c_nationkey, c_phone, c_acctbal, c_mktsegment, c_comment) VALUES (5, 'Customer000000005', 'KvpyuHCplrB84WgAiGV6sYpZq7Tj', 3, '137509426364', 794.47, 'HOUSEHOLD ', 'accounts');
INSERT INTO customer (c_custkey, c_name, c_address, c_nationkey, c_phone, c_acctbal, c_mktsegment, c_comment) VALUES (6, 'Customer000000006', 'sKZz0CsnMD7mp4Xd0YrBvx,LREYKUWAh yVn', 20, '301149684951', 7638.57, 'AUTOMOBILE', 'foxes');
INSERT INTO customer (c_custkey, c_name, c_address, c_nationkey, c_phone, c_acctbal, c_mktsegment, c_comment) VALUES (7, 'Customer000000007', 'TcGe5gaZNgVePxU5kRrvXBfkasDTea', 18, '2819989759', 9561.95, 'AUTOMOBILE', 'ainst');
INSERT INTO customer (c_custkey, c_name, c_address, c_nationkey, c_phone, c_acctbal, c_mktsegment, c_comment) VALUES (8, 'Customer000000008', 'I0B10bB0AymmC, 0PrRYBCP1yGJ8xcBPmWhl5', 17, '271475749335', 6819.74, 'BUILDING', 'among');
INSERT INTO customer (c_custkey, c_name, c_address, c_nationkey, c_phone, c_acctbal, c_mktsegment, c_comment) VALUES (9, 'Customer000000009', 'xKiAFTjUsCuxfeleNqefumTrjS', 8, '183389063675', 8324.07, 'FURNITURE ', 'apple');
INSERT INTO customer (c_custkey, c_name, c_address, c_nationkey, c_phone, c_acctbal, c_mktsegment, c_comment) VALUES (10, 'Customer000000010', '6LrEaV6KR6PLVcgl2ArL Q3rqzLzcT1 v2', 5, '157413469870', 2753.54, 'HOUSEHOLD ', 'fur');
INSERT INTO customer (c_custkey, c_name, c_address, c_nationkey, c_phone, c_acctbal, c_mktsegment, c_comment) VALUES (11, 'Customer000000011', 'PkWS 3HlXqwTuzrKg633BEi', 23, '334641513439', -272.60, 'BUILDING', 'requests');
INSERT INTO customer (c_custkey, c_name, c_address, c_nationkey, c_phone, c_acctbal, c_mktsegment, c_comment) VALUES (12, 'Customer000000012', '9PWKuhzT4Zr1Q', 13, '237912761263', 3396.49, 'HOUSEHOLD ', 'along');
INSERT INTO customer (c_custkey, c_name, c_address, c_nationkey, c_phone, c_acctbal, c_mktsegment, c_comment) VALUES (13, 'Customer000000013', 'nsXQu0oVjD7PM659uC3SRSp', 3, '137615475974', 3857.34, 'BUILDING', 'blithely');
INSERT INTO customer (c_custkey, c_name, c_address, c_nationkey, c_phone, c_acctbal, c_mktsegment, c_comment) VALUES (14, 'Customer000000014', 'KXkletMlL2JQEA ', 1, '118451293851', 5266.30, 'FURNITURE ', 'ironic');
INSERT INTO customer (c_custkey, c_name, c_address, c_nationkey, c_phone, c_acctbal, c_mktsegment, c_comment) VALUES (15, 'Customer000000015', 'YtWggXoOLdwdo7b0y,BZaGUQMLJMX1Y,EC,6Dn', 23, '336875427601', 2788.52, 'HOUSEHOLD ', 'fluf');
INSERT INTO customer (c_custkey, c_name, c_address, c_nationkey, c_phone, c_acctbal, c_mktsegment, c_comment) VALUES (16, 'Customer000000016', 'cYiaeMLZSMAOQ2 d0W,', 10, '207816093107', 4681.03, 'FURNITURE ', 'fluffily');
INSERT INTO customer (c_custkey, c_name, c_address, c_nationkey, c_phone, c_acctbal, c_mktsegment, c_comment) VALUES (17, 'Customer000000017', 'izrh 6jdqtp2eqdtbkswDD8SG4SzXruMfIXyR7', 2, '129706823487', 6.34, 'AUTOMOBILE', 'packages');
INSERT INTO customer (c_custkey, c_name, c_address, c_nationkey, c_phone, c_acctbal, c_mktsegment, c_comment) VALUES (18, 'Customer000000018', '3txGO AiuFux3zT0Z9NYaFRnZt', 6, '161552151315', 5494.43, 'BUILDING', 'sleep');
INSERT INTO customer (c_custkey, c_name, c_address, c_nationkey, c_phone, c_acctbal, c_mktsegment, c_comment) VALUES (19, 'Customer000000019', 'uc,3bHIx84H,wdrmLOjVsiqXCq2tr', 18, '283965265053', 8914.71, 'HOUSEHOLD ', 'nag');





INSERT INTO lineitem (l_orderkey, l_partkey, l_suppkey, l_linenumber, l_quantity, l_extendedprice, l_discount, l_tax, l_returnflag, l_linestatus, l_shipdate, l_commitdate, l_receiptdate, l_shipinstruct, l_shipmode, l_comment) VALUES (2, 1062, 33, 1, 38.00, 36596.28, 0.00, 0.05, 'N', 'O', '1997-01-28', '1997-01-14', '1997-02-02', 'TAKE BACK RETURN ', 'RAIL', 'requests');
INSERT INTO lineitem (l_orderkey, l_partkey, l_suppkey, l_linenumber, l_quantity, l_extendedprice, l_discount, l_tax, l_returnflag, l_linestatus, l_shipdate, l_commitdate, l_receiptdate, l_shipinstruct, l_shipmode, l_comment) VALUES (3, 43, 19, 1, 45.00, 42436.80, 0.06, 0.00, 'R', 'F', '1994-02-02', '1994-01-04', '1994-02-23', 'NONE ', 'AIR ', 'requests');
INSERT INTO lineitem (l_orderkey, l_partkey, l_suppkey, l_linenumber, l_quantity, l_extendedprice, l_discount, l_tax, l_returnflag, l_linestatus, l_shipdate, l_commitdate, l_receiptdate, l_shipinstruct, l_shipmode, l_comment) VALUES (3, 1285, 60, 3, 27.00, 32029.56, 0.06, 0.07, 'A', 'F', '1994-01-16', '1993-11-22', '1994-01-23', 'DELIVER IN PERSON', 'SHIP', 'requests');
INSERT INTO lineitem (l_orderkey, l_partkey, l_suppkey, l_linenumber, l_quantity, l_extendedprice, l_discount, l_tax, l_returnflag, l_linestatus, l_shipdate, l_commitdate, l_receiptdate, l_shipinstruct, l_shipmode, l_comment) VALUES (3, 294, 22, 4, 2.00, 2388.58, 0.01, 0.06, 'A', 'F', '1993-12-04', '1994-01-07', '1994-01-01', 'NONE ', 'TRUCK ', 'requests');
INSERT INTO lineitem (l_orderkey, l_partkey, l_suppkey, l_linenumber, l_quantity, l_extendedprice, l_discount, l_tax, l_returnflag, l_linestatus, l_shipdate, l_commitdate, l_receiptdate, l_shipinstruct, l_shipmode, l_comment) VALUES (3, 1831, 61, 5, 28.00, 48519.24, 0.04, 0.00, 'R', 'F', '1993-12-14', '1994-01-10', '1994-01-01', 'TAKE BACK RETURN ', 'FOB ', 'requests');
INSERT INTO lineitem (l_orderkey, l_partkey, l_suppkey, l_linenumber, l_quantity, l_extendedprice, l_discount, l_tax, l_returnflag, l_linestatus, l_shipdate, l_commitdate, l_receiptdate, l_shipinstruct, l_shipmode, l_comment) VALUES (3, 622, 16, 6, 26.00, 39588.12, 0.10, 0.02, 'A', 'F', '1993-10-29', '1993-12-18', '1993-11-04', 'TAKE BACK RETURN ', 'RAIL', 'requests');
INSERT INTO lineitem (l_orderkey, l_partkey, l_suppkey, l_linenumber, l_quantity, l_extendedprice, l_discount, l_tax, l_returnflag, l_linestatus, l_shipdate, l_commitdate, l_receiptdate, l_shipinstruct, l_shipmode, l_comment) VALUES (4, 881, 81, 1, 30.00, 53456.40, 0.03, 0.08, 'N', 'O', '1996-01-10', '1995-12-14', '1996-01-18', 'DELIVER IN PERSON', 'REG AIR ', 'idly');
INSERT INTO lineitem (l_orderkey, l_partkey, l_suppkey, l_linenumber, l_quantity, l_extendedprice, l_discount, l_tax, l_returnflag, l_linestatus, l_shipdate, l_commitdate, l_receiptdate, l_shipinstruct, l_shipmode, l_comment) VALUES (1, 674, 75, 2, 36.00, 56688.12, 0.09, 0.06, 'N', 'O', '1996-04-12', '1996-02-28', '2014-12-12', 'TAKE BACK RETURN ', 'MAIL', 'bold');
INSERT INTO lineitem (l_orderkey, l_partkey, l_suppkey, l_linenumber, l_quantity, l_extendedprice, l_discount, l_tax, l_returnflag, l_linestatus, l_shipdate, l_commitdate, l_receiptdate, l_shipinstruct, l_shipmode, l_comment) VALUES (1, 637, 38, 3, 8.00, 12301.04, 0.10, 0.02, 'N', 'O', '1996-01-29', '1996-03-05', '2014-12-12', 'TAKE BACK RETURN ', 'REG AIR ', 'requests');
INSERT INTO lineitem (l_orderkey, l_partkey, l_suppkey, l_linenumber, l_quantity, l_extendedprice, l_discount, l_tax, l_returnflag, l_linestatus, l_shipdate, l_commitdate, l_receiptdate, l_shipinstruct, l_shipmode, l_comment) VALUES (1, 22, 48, 4, 28.00, 25816.56, 0.09, 0.06, 'N', 'O', '1996-04-21', '1996-03-30', '2014-12-12', 'NONE ', 'AIR ', 'requests');
INSERT INTO lineitem (l_orderkey, l_partkey, l_suppkey, l_linenumber, l_quantity, l_extendedprice, l_discount, l_tax, l_returnflag, l_linestatus, l_shipdate, l_commitdate, l_receiptdate, l_shipinstruct, l_shipmode, l_comment) VALUES (1, 241, 23, 5, 24.00, 27389.76, 0.10, 0.04, 'N', 'O', '1996-03-30', '1996-03-14', '2014-12-12', 'NONE ', 'FOB ', 'requests');
INSERT INTO lineitem (l_orderkey, l_partkey, l_suppkey, l_linenumber, l_quantity, l_extendedprice, l_discount, l_tax, l_returnflag, l_linestatus, l_shipdate, l_commitdate, l_receiptdate, l_shipinstruct, l_shipmode, l_comment) VALUES (1, 157, 10, 6, 32.00, 33828.80, 0.07, 0.02, 'N', 'O', '2014-12-12', '1996-02-07', '2014-12-12', 'DELIVER IN PERSON', 'MAIL', 'requests');
INSERT INTO lineitem (l_orderkey, l_partkey, l_suppkey, l_linenumber, l_quantity, l_extendedprice, l_discount, l_tax, l_returnflag, l_linestatus, l_shipdate, l_commitdate, l_receiptdate, l_shipinstruct, l_shipmode, l_comment) VALUES (3, 191, 70, 2, 250.00, 53468.31, 0.10, 0.00, 'R', 'F', '1993-11-09', '1993-12-20', '1993-11-24', 'TAKE BACK RETURN ', 'RAIL', 'unusual');



INSERT INTO nation (n_nationkey, n_name, n_regionkey, n_comment) VALUES (0, 'ALGERIA', 0, 'haggle');
INSERT INTO nation (n_nationkey, n_name, n_regionkey, n_comment) VALUES (1, 'ARGENTINA', 1, 'haggle');
INSERT INTO nation (n_nationkey, n_name, n_regionkey, n_comment) VALUES (2, 'BRAZIL ', 1, 'haggle');
INSERT INTO nation (n_nationkey, n_name, n_regionkey, n_comment) VALUES (3, 'CANADA ', 1, 'haggle');
INSERT INTO nation (n_nationkey, n_name, n_regionkey, n_comment) VALUES (4, 'EGYPT', 4, 'haggle');
INSERT INTO nation (n_nationkey, n_name, n_regionkey, n_comment) VALUES (5, 'ETHIOPIA ', 0, 'haggle');
INSERT INTO nation (n_nationkey, n_name, n_regionkey, n_comment) VALUES (6, 'FRANCE ', 3, 'haggle');
INSERT INTO nation (n_nationkey, n_name, n_regionkey, n_comment) VALUES (7, 'GERMANY', 3, 'haggle');
INSERT INTO nation (n_nationkey, n_name, n_regionkey, n_comment) VALUES (8, 'INDIA', 2, 'haggle');
INSERT INTO nation (n_nationkey, n_name, n_regionkey, n_comment) VALUES (9, 'INDONESIA', 2, 'slylyy');
INSERT INTO nation (n_nationkey, n_name, n_regionkey, n_comment) VALUES (10, 'IRAN ', 4, 'efully');
INSERT INTO nation (n_nationkey, n_name, n_regionkey, n_comment) VALUES (11, 'IRAQ ', 4, 'nic');
INSERT INTO nation (n_nationkey, n_name, n_regionkey, n_comment) VALUES (12, 'JAPAN', 2, 'ously');
INSERT INTO nation (n_nationkey, n_name, n_regionkey, n_comment) VALUES (13, 'JORDAN ', 4, 'ic');
INSERT INTO nation (n_nationkey, n_name, n_regionkey, n_comment) VALUES (14, 'KENYA', 0, ' pending');
INSERT INTO nation (n_nationkey, n_name, n_regionkey, n_comment) VALUES (15, 'MOROCCO', 0, 'riousl');
INSERT INTO nation (n_nationkey, n_name, n_regionkey, n_comment) VALUES (16, 'MOZAMBIQUE ', 0, 'elyr');
INSERT INTO nation (n_nationkey, n_name, n_regionkey, n_comment) VALUES (17, 'PERU ', 1, 'into');
INSERT INTO nation (n_nationkey, n_name, n_regionkey, n_comment) VALUES (18, 'CHINA', 2, 'accounts');
INSERT INTO nation (n_nationkey, n_name, n_regionkey, n_comment) VALUES (19, 'ROMANIA', 3, 'account');
INSERT INTO nation (n_nationkey, n_name, n_regionkey, n_comment) VALUES (20, 'SAUDI ARABIA ', 4, 'blithely');
INSERT INTO nation (n_nationkey, n_name, n_regionkey, n_comment) VALUES (21, 'VIETNAM', 2, 'hely');
INSERT INTO nation (n_nationkey, n_name, n_regionkey, n_comment) VALUES (22, 'RUSSIA ', 3, ' requests');
INSERT INTO nation (n_nationkey, n_name, n_regionkey, n_comment) VALUES (23, 'UNITED KINGDOM ', 3, 'boost');
INSERT INTO nation (n_nationkey, n_name, n_regionkey, n_comment) VALUES (24, 'UNITED STATES', 1, 'slow');




INSERT INTO orders (o_orderkey, o_custkey, o_orderstatus, o_totalprice, o_orderdate, o_orderpriority, o_clerk, o_shippriority, o_comment) VALUES (1, 370, 'O', 172799.49, '1996-01-02', '5LOW', 'Clerk000000951', 0, 'nstructions');
INSERT INTO orders (o_orderkey, o_custkey, o_orderstatus, o_totalprice, o_orderdate, o_orderpriority, o_clerk, o_shippriority, o_comment) VALUES (2, 781, 'O', 38426.09, '1996-12-01', '1URGENT ', 'Clerk000000880', 0, 'asymptot');
INSERT INTO orders (o_orderkey, o_custkey, o_orderstatus, o_totalprice, o_orderdate, o_orderpriority, o_clerk, o_shippriority, o_comment) VALUES (3, 1234, 'F', 205654.30, '1993-10-14', '5LOW', 'Clerk000000955', 0, 'sly');
INSERT INTO orders (o_orderkey, o_custkey, o_orderstatus, o_totalprice, o_orderdate, o_orderpriority, o_clerk, o_shippriority, o_comment) VALUES (4, 1369, 'O', 56000.91, '1995-10-11', '5LOW', 'Clerk000000124', 0, 'sits');
INSERT INTO orders (o_orderkey, o_custkey, o_orderstatus, o_totalprice, o_orderdate, o_orderpriority, o_clerk, o_shippriority, o_comment) VALUES (5, 445, 'F', 105367.67, '1994-07-30', '5LOW', 'Clerk000000925', 0, 'quickly');
INSERT INTO orders (o_orderkey, o_custkey, o_orderstatus, o_totalprice, o_orderdate, o_orderpriority, o_clerk, o_shippriority, o_comment) VALUES (6, 557, 'F', 45523.10, '1992-02-21', '4NOT SPECIFIED', 'Clerk000000058', 0, 'a');
INSERT INTO orders (o_orderkey, o_custkey, o_orderstatus, o_totalprice, o_orderdate, o_orderpriority, o_clerk, o_shippriority, o_comment) VALUES (7, 392, 'O', 271885.66, '1996-01-10', '2HIGH ', 'Clerk000000470', 0, 'ly special requests ');
INSERT INTO orders (o_orderkey, o_custkey, o_orderstatus, o_totalprice, o_orderdate, o_orderpriority, o_clerk, o_shippriority, o_comment) VALUES (32, 1301, 'O', 198665.57, '1995-07-16', '2HIGH ', 'Clerk000000616', 0, 'ise');
INSERT INTO orders (o_orderkey, o_custkey, o_orderstatus, o_totalprice, o_orderdate, o_orderpriority, o_clerk, o_shippriority, o_comment) VALUES (33, 670, 'F', 146567.24, '1993-10-27', '3MEDIUM ', 'Clerk000000409', 0, 'uriously');
INSERT INTO orders (o_orderkey, o_custkey, o_orderstatus, o_totalprice, o_orderdate, o_orderpriority, o_clerk, o_shippriority, o_comment) VALUES (66, 1292, 'F', 104190.66, '1994-01-20', '5LOW', 'Clerk000000743', 0, 'integrate');
INSERT INTO orders (o_orderkey, o_custkey, o_orderstatus, o_totalprice, o_orderdate, o_orderpriority, o_clerk, o_shippriority, o_comment) VALUES (67, 568, 'O', 182481.16, '1996-12-19', '4NOT SPECIFIED', 'Clerk000000547', 0, 'iron');
INSERT INTO orders (o_orderkey, o_custkey, o_orderstatus, o_totalprice, o_orderdate, o_orderpriority, o_clerk, o_shippriority, o_comment) VALUES (68, 286, 'O', 301968.79, '1998-04-18', '3MEDIUM ', 'Clerk000000440', 0, 'acro');



INSERT INTO part (p_partkey, p_name, p_mfgr, p_brand, p_type, p_size, p_container, p_retailprice, p_comment) VALUES (1, 'goldenrod', 'Manufacturer1 ', 'Brand13', 'COPPER', 7, 'JUMBO PKG ', 901.00, 'ly');
INSERT INTO part (p_partkey, p_name, p_mfgr, p_brand, p_type, p_size, p_container, p_retailprice, p_comment) VALUES (2, 'blush', 'Manufacturer1 ', 'Brand13', 'BRASS', 1, 'LG CASE ', 902.00, 'lar');
INSERT INTO part (p_partkey, p_name, p_mfgr, p_brand, p_type, p_size, p_container, p_retailprice, p_comment) VALUES (3, 'sprin', 'Manufacturer4 ', 'Brand42', 'POLISHED BRASS', 21, 'WRAP CASE ', 903.00, 'egularg');
INSERT INTO part (p_partkey, p_name, p_mfgr, p_brand, p_type, p_size, p_container, p_retailprice, p_comment) VALUES (4, 'cornflower', 'Manufacturer3 ', 'Brand34', 'BRASS', 14, 'MED DRUM', 904.00, 'furious');
INSERT INTO part (p_partkey, p_name, p_mfgr, p_brand, p_type, p_size, p_container, p_retailprice, p_comment) VALUES (5, 'forest', 'Manufacturer3 ', 'Brand32', 'TIN', 15, 'SM PKG', 905.00, ' wake');
INSERT INTO part (p_partkey, p_name, p_mfgr, p_brand, p_type, p_size, p_container, p_retailprice, p_comment) VALUES (6, 'bisque', 'Manufacturer2 ', 'Brand24', 'STEEL', 4, 'MED BAG ', 906.00, 'sual');
INSERT INTO part (p_partkey, p_name, p_mfgr, p_brand, p_type, p_size, p_container, p_retailprice, p_comment) VALUES (7, 'moccasin', 'Manufacturer1 ', 'Brand11', 'COPPER', 45, 'SM BAG', 907.00, 'lylyex');
INSERT INTO part (p_partkey, p_name, p_mfgr, p_brand, p_type, p_size, p_container, p_retailprice, p_comment) VALUES (8, 'misty', 'Manufacturer4 ', 'Brand44', 'TIN', 41, 'LG DRUM ', 908.00, 'eposi');
INSERT INTO part (p_partkey, p_name, p_mfgr, p_brand, p_type, p_size, p_container, p_retailprice, p_comment) VALUES (9, 'thistle', 'Manufacturer4 ', 'Brand43', 'BURNISHED STEEL', 12, 'WRAP CASE ', 909.00, 'ironicfoxe');
INSERT INTO part (p_partkey, p_name, p_mfgr, p_brand, p_type, p_size, p_container, p_retailprice, p_comment) VALUES (10, 'linen', 'Manufacturer5 ', 'Brand54', 'STEEL', 44, 'LG CAN', 910.01, 'finaldeposit');
INSERT INTO part (p_partkey, p_name, p_mfgr, p_brand, p_type, p_size, p_container, p_retailprice, p_comment) VALUES (11, 'spring', 'Manufacturer2 ', 'Brand25', 'NICKEL', 43, 'WRAP BOX', 911.01, 'nggr');
INSERT INTO part (p_partkey, p_name, p_mfgr, p_brand, p_type, p_size, p_container, p_retailprice, p_comment) VALUES (12, 'cornflower', 'Manufacturer3 ', 'Brand33', 'STEEL', 25, 'JUMBO CASE', 912.01, 'quickly');
INSERT INTO part (p_partkey, p_name, p_mfgr, p_brand, p_type, p_size, p_container, p_retailprice, p_comment) VALUES (13, 'ghost', 'Manufacturer5 ', 'Brand55', 'NICKEL', 1, 'JUMBO PACK', 913.01, 'osits');
INSERT INTO part (p_partkey, p_name, p_mfgr, p_brand, p_type, p_size, p_container, p_retailprice, p_comment) VALUES (14, 'khaki', 'Manufacturer1 ', 'Brand13', 'STEEL', 28, 'JUMBO BOX ', 914.01, 'kagesc');
INSERT INTO part (p_partkey, p_name, p_mfgr, p_brand, p_type, p_size, p_container, p_retailprice, p_comment) VALUES (15, 'blanched', 'Manufacturer1 ', 'Brand15', 'BRASS', 45, 'LG CASE ', 915.01, 'usualac');
INSERT INTO part (p_partkey, p_name, p_mfgr, p_brand, p_type, p_size, p_container, p_retailprice, p_comment) VALUES (16, 'deep', 'Manufacturer3 ', 'Brand32', 'TIN', 2, 'MED PACK', 916.01, 'untsa');
INSERT INTO part (p_partkey, p_name, p_mfgr, p_brand, p_type, p_size, p_container, p_retailprice, p_comment) VALUES (17, 'indian', 'Manufacturer4 ', 'Brand43', 'STEEL', 16, 'LG BOX', 917.01, 'regularaccounts');
INSERT INTO part (p_partkey, p_name, p_mfgr, p_brand, p_type, p_size, p_container, p_retailprice, p_comment) VALUES (18, 'turquoise', 'Manufacturer1 ', 'Brand11', 'STEEL', 42, 'JUMBO PACK', 918.01, 's');
INSERT INTO part (p_partkey, p_name, p_mfgr, p_brand, p_type, p_size, p_container, p_retailprice, p_comment) VALUES (19, 'chocolate', 'Manufacturer2 ', 'Brand23', 'NICKEL', 33, 'WRAP BOX', 919.01, 'pending acc');
INSERT INTO part (p_partkey, p_name, p_mfgr, p_brand, p_type, p_size, p_container, p_retailprice, p_comment) VALUES (20, 'ivory', 'Manufacturer1 ', 'Brand12', 'NICKEL', 48, 'MED BAG ', 920.02, 'are');




INSERT INTO partsupp (ps_partkey, ps_suppkey, ps_availqty, ps_supplycost, ps_comment) VALUES (1, 2, 3325, 771.64, 'even');
INSERT INTO partsupp (ps_partkey, ps_suppkey, ps_availqty, ps_supplycost, ps_comment) VALUES (2, 3, 8895, 378.49, 'nic');
INSERT INTO partsupp (ps_partkey, ps_suppkey, ps_availqty, ps_supplycost, ps_comment) VALUES (3, 4, 4651, 920.92, 'ilent');
INSERT INTO partsupp (ps_partkey, ps_suppkey, ps_availqty, ps_supplycost, ps_comment) VALUES (3, 29, 4093, 498.13, 'ending');
INSERT INTO partsupp (ps_partkey, ps_suppkey, ps_availqty, ps_supplycost, ps_comment) VALUES (3, 54, 3917, 645.40, 'final');
INSERT INTO partsupp (ps_partkey, ps_suppkey, ps_availqty, ps_supplycost, ps_comment) VALUES (3, 79, 9942, 191.92, 'unusual');
INSERT INTO partsupp (ps_partkey, ps_suppkey, ps_availqty, ps_supplycost, ps_comment) VALUES (4, 5, 1339, 113.97, 'carefully');
INSERT INTO partsupp (ps_partkey, ps_suppkey, ps_availqty, ps_supplycost, ps_comment) VALUES (4, 30, 6377, 591.18, 'ly');



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
INSERT INTO supplier (s_suppkey, s_name, s_address, s_nationkey, s_phone, s_acctbal, s_comment) VALUES (8, 'Supplier000000008 ', '9Sq4bBH2FQEmaFOocY45sRTxo6yuoG', 17, '274987423860', 7627.85, 'slylyn');
INSERT INTO supplier (s_suppkey, s_name, s_address, s_nationkey, s_phone, s_acctbal, s_comment) VALUES (9, 'Supplier000000009 ', '1KhUgZegwM3ua7dsYmekYBsK', 10, '204033988662', 5302.37, 'slylyn');
INSERT INTO supplier (s_suppkey, s_name, s_address, s_nationkey, s_phone, s_acctbal, s_comment) VALUES (10, 'Supplier000000010 ', 'Saygah3gYWMp72i PY', 24, '348524898585', 3891.91, 'slylyn');
INSERT INTO supplier (s_suppkey, s_name, s_address, s_nationkey, s_phone, s_acctbal, s_comment) VALUES (14, 'Supplier000000014 ', 'EXsnO5pTNj4iZRm', 15, '256562475058', 9189.82,'slylyn');
INSERT INTO supplier (s_suppkey, s_name, s_address, s_nationkey, s_phone, s_acctbal, s_comment) VALUES (15, 'Supplier000000015 ', 'olXVbNBfVzRqgokr1T,Ie', 8, '184533576394', 308.56, 'slylyn');
INSERT INTO supplier (s_suppkey, s_name, s_address, s_nationkey, s_phone, s_acctbal, s_comment) VALUES (16, 'Supplier000000016 ', 'YjP5C55zHDXL7LalK27zfQnwejdpin4AMpvh', 22, '328225024215', 2972.26, 'slylyn');
INSERT INTO supplier (s_suppkey, s_name, s_address, s_nationkey, s_phone, s_acctbal, s_comment) VALUES (17, 'Supplier000000017 ', 'c2d,ESHRSkK3WYnxpgw6aOqN0q', 19, '296018849219', 1687.81, 'slylyn');
INSERT INTO supplier (s_suppkey, s_name, s_address, s_nationkey, s_phone, s_acctbal, s_comment) VALUES (18, 'Supplier000000018 ', 'PGGVE5PWAMwKDZw ', 16, '267295511115', 7040.82, 'slylyn');
INSERT INTO supplier (s_suppkey, s_name, s_address, s_nationkey, s_phone, s_acctbal, s_comment) VALUES (19, 'Supplier000000019 ', 'edZT3es,nBFD8lBXTGeTl', 24, '342783102731', 6150.38, 'slylyn');
INSERT INTO supplier (s_suppkey, s_name, s_address, s_nationkey, s_phone, s_acctbal, s_comment) VALUES (20, 'Supplier000000020 ', 'iybAE,RmTymrZVYaFZva2SH,j', 3, '137159456730', 530.82, 'slylyn');
INSERT INTO supplier (s_suppkey, s_name, s_address, s_nationkey, s_phone, s_acctbal, s_comment) VALUES (29, 'John ', 'New York', 10, '987654 ', 100.12, 'comment1');
INSERT INTO supplier (s_suppkey, s_name, s_address, s_nationkey, s_phone, s_acctbal, s_comment) VALUES (54, 'John ', 'New York', 10, '987654 ', 100.12, 'comment1');
INSERT INTO supplier (s_suppkey, s_name, s_address, s_nationkey, s_phone, s_acctbal, s_comment) VALUES (79, 'John ', 'New York', 10, '987654 ', 100.12, 'comment1');
INSERT INTO supplier (s_suppkey, s_name, s_address, s_nationkey, s_phone, s_acctbal, s_comment) VALUES (30, 'John ', 'New York', 10, '987654 ', 100.12, 'comment1');
INSERT INTO supplier (s_suppkey, s_name, s_address, s_nationkey, s_phone, s_acctbal, s_comment) VALUES (55, 'John ', 'New York', 10, '987654 ', 100.12, 'comment1');
INSERT INTO supplier (s_suppkey, s_name, s_address, s_nationkey, s_phone, s_acctbal, s_comment) VALUES (84, 'John ', 'New York', 10, '987654 ', 100.12, 'comment1');
INSERT INTO supplier (s_suppkey, s_name, s_address, s_nationkey, s_phone, s_acctbal, s_comment) VALUES (80, 'John ', 'New York', 10, '987654 ', 100.12, 'comment1');
INSERT INTO supplier (s_suppkey, s_name, s_address, s_nationkey, s_phone, s_acctbal, s_comment) VALUES (31, 'John ', 'New York', 10, '987654 ', 100.12, 'comment1');
INSERT INTO supplier (s_suppkey, s_name, s_address, s_nationkey, s_phone, s_acctbal, s_comment) VALUES (56, 'John ', 'New York', 10, '987654 ', 100.12, 'comment1');
INSERT INTO supplier (s_suppkey, s_name, s_address, s_nationkey, s_phone, s_acctbal, s_comment) VALUES (81, 'John ', 'New York', 10, '987654 ', 100.12, 'comment1');
INSERT INTO supplier (s_suppkey, s_name, s_address, s_nationkey, s_phone, s_acctbal, s_comment) VALUES (75, 'John ', 'New York', 10, '987654 ', 100.12, 'comment1');
INSERT INTO supplier (s_suppkey, s_name, s_address, s_nationkey, s_phone, s_acctbal, s_comment) VALUES (33, 'John ', 'New York', 10, '987654 ', 100.12, 'comment1');


