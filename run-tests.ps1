$ErrorActionPreference = "Stop"
Write-Host "java -version:"
java -version
mvn clean test
