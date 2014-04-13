maven-jsondoc-plugin
====================

maven-jsondoc-plugin


<plugin>
	<groupId>org.jsondoc</groupId>
	<artifactId>maven-jsondoc-plugin</artifactId>
	<version>1.0.2-SNAPSHOT</version>
	<configuration>
			<apiVersion>${project.version}</apiVersion>
			<apiBasePath>/rest/api/v1</apiBasePath>
			<packageToScan>org.exmaple.services.rest.v1</packageToScan>
			<outputFile>rest-api-v1.json</outputFile>
			<outputFormat>json</outputFormat>
		</configuration>
</plugin>
