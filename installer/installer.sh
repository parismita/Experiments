#!/bin/bash


# echo "Copying z3 libraries..."
# cp /home/xdata/z3/z3  /usr/bin/z3
# cp /home/xdata/z3/libz3java.so  /usr/lib/libz3java.so
# cp /home/xdata/z3/libz3.so /usr/lib/libz3.so
# cp /home/xdata/z3/libz3.a /usr/lib/libz3.a
# chmod 777 /usr/bin/z3
# echo "...done."

# cat /home/installer/config.txt > /etc/postgresql/14/main/pg_hba.conf
service postgresql start

su postgres -c 'psql postgres < /home/installer/xdata_db.sql' 

echo "Finalizing container......"
