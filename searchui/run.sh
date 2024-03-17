PACKAGE=search-ui-1.0.0
TOMCAT=/home/devep/tomcat/tomcat_search_ui_9310/
APP=search-ui
sh ${TOMCAT}/bin/shutdown.sh
/bin/rm -rf ${TOMCAT}/logs/*
/bin/rm -rf  ${TOMCAT}/webapps/*
/bin/rm -rf *.zip

svn up
mvn package
/bin/rm -rf target/${PACKAGE}//META-INF
cp arch/${APP}.properties target/${PACKAGE}/WEB-INF/classes/
cp -r   target/${PACKAGE}/  ${TOMCAT}/webapps/${APP}
zip -r ${APP}.zip  target/${PACKAGE}/*  -q


sh ${TOMCAT}/bin/startup.sh
/bin/rm -rf target
