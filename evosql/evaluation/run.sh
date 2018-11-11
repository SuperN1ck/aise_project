java -agentlib:jdwp=transport=dt_socket,server=y,address=12000 -Dlog4j.configurationFile=log4j2.xml -Xmx768m -Xms256m -jar build/libs/evaluation-1.0-all.jar erpnext evosql
