echo off

echo Generate POM dependency graphs using depgraph-maven-plugin & plantuml (including graphviz dot) - excluding mastercard libraries

set DEPENDENCY_GRAPH_HOME=%~dp0
set BASE_PROJECT_PATH="C:\\src_mpe"
set GRAPHVIZ_DOT="C:\Programs\graphviz-2.38\bin\dot.exe"

set GROUP_EXCLUSION_LIST=
REM org.springframework.boot:*,org.springframework.cloud:*,org.springframework:*
set EXCLUSION_LIST=springframework zipkin fasterxml

echo  ---------------------------------------------------------------------------       
echo  -                         Install depgraph-maven-plugin                   -       
echo  ---------------------------------------------------------------------------       

echo call mvn install:install-file -Dfile=./depgraph-plugin/depgraph-maven-plugin-4.0.1.jar -DpomFile=./depgraph-plugin/depgraph-maven-plugin-4.0.1.pom -Dsources=./depgraph-plugin/depgraph-maven-plugin-4.0.1-sources.jar -Djavadoc=./depgraph-plugin/depgraph-maven-plugin-4.0.1-javadoc.jar -DgroupId=com.github.ferstl -DartifactId=depgraph-maven-plugin -Dversion=4.0.1 -Dpackaging=maven-plugin

mkdir "%DEPENDENCY_GRAPH_HOME%\dependency-graphs"

rem del /f /s /q "%DEPENDENCY_GRAPH_HOME%\dependency-graphs\*.*"

cd %BASE_PROJECT_PATH%
echo .
		
REM	if "%%i" NEQ ".settings" if "%%i" NEQ "batch-data-upload" if "%%i" NEQ "swagger-redoc" (

for /D %%i in (gpse*) do (
rem	if "%%i" == "gpse-processing-core" (
			cd %%i
		if exist pom.xml ( 
			echo " "
			echo "Generate module-dependency-graph for *** %%i *** modular service"
			rem dir %%i
	 
			del /f /s /q target\*.puml
	 
			REM --offline
			call mvn -e com.github.ferstl:depgraph-maven-plugin:4.0.1:aggregate -DgraphFormat=puml -Dexcludes=%GROUP_EXCLUSION_LIST% -DtransitiveExcludes=%GROUP_EXCLUSION_LIST% -DfollowDependencies=false -DshowGroupIds=false -DshowDuplicates=false -DshowConflicts=true -Dscope=compile -DoutputFileName=%%i-dependencies.puml
			
			rem mvn -e com.github.ferstl:depgraph-maven-plugin:4.0.1:graph 
			REM mvn depgraph:help -Ddetail=true
			
			echo "clean"
			rem for %%t in (%EXCLUSION_LIST%) do (
			rem 	echo "Exclude [%%t*] dependencies"
			rem 	find /V "%%t" "target\%%i-dependencies.puml" > target\temp.puml
			rem 	rem pause
			rem 	copy /Y target\temp.puml "target\%%i-dependencies.puml"
			rem 	powershell -NoProfile -Command "Get-Content -Path target\temp.puml | Select-Object -Skip 2 > "target\%%i-dependencies.puml"
			rem 	del /f /s /q target\temp.puml
			rem )
						
			copy /Y target\%%i-dependencies.puml "%DEPENDENCY_GRAPH_HOME%\dependency-graphs\%%i-dependencies.puml"

			rem pause		
			echo .
		)
		cd ..
rem	)
)


echo Go to dependency-graph home directory


cd %DEPENDENCY_GRAPH_HOME%

echo 'List down all the components in [dependency-graphs\all_components.txt] file.'
echo 'List down all the components in [dependency-graphs\all_components.txt] file.'
find "rectangle" dependency-graphs\*.puml | find "as" | find /V "mastercard" > dependency-graphs\all_components.txt

echo 'Write all the unique components in [dependency-graphs\01-unique-components.puml] file.'
rem for /f "tokens=2" %%a in (dependency-graphs\all_components.txt) do set "_%%a=."
rem for /f "delims=_=" %%a in ('set _') do echo %%a >> dependency-graphs\01-unique-components.puml
echo @startuml > dependency-graphs\01-unique-components.puml
powershell type "dependency-graphs\all_components.txt" | sort /unique >> dependency-graphs\01-unique-components.puml
echo @enduml >> dependency-graphs\01-unique-components.puml

java -jar plantuml-1.2022.6.jar -stdlib
echo 'Generate png files based on the *.puml files...'
java -jar plantuml-1.2022.6.jar -duration dependency-graphs\*.puml
rem java -jar plantuml-1.2022.6.jar -encodesprite 16z -duration dependency-graphs\*.puml

REM java -jar plantuml-8059.jar -testdot
REM for %%f in (dependency-graphs\*.puml) do (
REM 	echo java -jar plantuml-8059.jar -duration %%f
REM 	java -jar plantuml-8059.jar -duration %%f
REM )

echo del /f /s /q dependency-graphs\*.puml

pause
