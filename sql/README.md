

Instrucciones de instalación y ejecución de la API de beeter-project

1- Descargar proyecto de GitHub 

2- Construir proyecto Maven desde terminal:

Acceder al directorio donde se encuentra el proyecto
>mvn clean
>mvn package 


3- Copiar el archivo .war generado (./target/beeter-api.war) en la carpeta /webapps del directorio donde tengamos instalado Tomcat

4- Construcción de la BD en MySQL Acceder a MySQL como root y llamar (source) al archivo beeterdb-user.sql Acceder a MySQL con los datos del nuevo usuario (beeter, beeter) y llamar al archivo beeteradb-schema.sql

5- Ejecutar Tomcat y servidor MySQL

6- Realizar peticiones HTTP desde Postman
