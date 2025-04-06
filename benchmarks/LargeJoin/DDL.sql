CREATE TABLE RA (
    A1 varchar(5) ,
    B varchar(5) ,
    primary key (A1)
);

CREATE TABLE RB (
    A2 varchar(5) ,
    B varchar(5) ,
    primary key (A2) ,
    foreign key (B) references RA (A1)

);

CREATE TABLE RC (
    A3 varchar(5) ,
    B varchar(5) ,
    primary key (A3) ,
    foreign key (B) references RB (A2)
);

CREATE TABLE RD (
    A4 varchar(5) ,
    B varchar(5) ,
    primary key (A4) ,
    foreign key (B) references RC (A3)
);

CREATE TABLE RE (
    A5 varchar(5) ,
    B varchar(5) ,
    primary key (A5) ,
    foreign key (B) references RD (A4)
);

CREATE TABLE RF (
    A6 varchar(5) ,
    B varchar(5) ,
    primary key (A6) ,
    foreign key (B) references RE (A5)
);

CREATE TABLE RG (
    A7 varchar(5) ,
    B varchar(5) ,
    primary key (A7) ,
    foreign key (B) references RF (A6)
);

CREATE TABLE RH (
    A8 varchar(5) ,
    B varchar(5) ,
    primary key (A8) ,
    foreign key (B) references RG (A7)
);

CREATE TABLE RI (
    A9 varchar(5) ,
    B varchar(5) ,
    primary key (A9) ,
    foreign key (B) references RH (A8)
);

CREATE TABLE RJ (
    A10 varchar(5) ,
    B varchar(5) , 
    primary key (A10) ,
    foreign key (B) references RI (A9)
);