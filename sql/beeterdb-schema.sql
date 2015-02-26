drop database if exists beeterdb;
create database beeterdb;
 
use beeterdb;
 
create table users (
username varchar(20) not null primary key,
userpass char(32) not null,
name varchar(70) not null,
email varchar(255) not null
);
 
create table user_roles (
username varchar(20) not null,
rolename varchar(20) not null,
foreign key(username) references users(username) on delete cascade,
primary key (username, rolename)
);
 
create table stings (
stingid int not null auto_increment primary key,
username varchar(20) not null,
subject varchar(100) not null,
content varchar(500) not null,
last_modified timestamp default current_timestamp ON UPDATE CURRENT_TIMESTAMP,
creation_timestamp datetime not null default current_timestamp,
foreign key(username) references users(username)
); 