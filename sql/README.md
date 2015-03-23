

Instrucciones de instalación y ejecución de la API de la librería

Este es el proceso que hay que seguir para hacer funcionar la API de la librería: 

1- Descargar proyecto de GitHub 

2- Construir proyecto Maven desde terminal:

Acceder al directorio donde se encuentra el proyecto
>mvn clean
>mvn package 

3- Copiar el archivo .war generado (./target/libreria-api.war) en la carpeta /webapps del directorio donde tengamos instalado Tomcat

4- Construcción de la BD en MySQL Acceder a MySQL como root y llamar (source) al archivo libreriadb-user.sql Acceder a MySQL con los datos del nuevo usuario (libreria, libreria) y llamar al archivo libreriadb-schema.sql

5- Ejecutar Tomcat y servidor MySQL

6- Realizar peticiones HTTP desde Postman
