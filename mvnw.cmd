@echo off
set "MAVEN_EXE=C:\Users\sowmy\.m2\wrapper\dists\apache-maven-3.9.9\8e74001100ff70d6af083c5511fcc5ec49282d7017cde82c3698eee8fdf86698\bin\mvn.cmd"
if exist "%MAVEN_EXE%" (
    "%MAVEN_EXE%" %*
) else (
    mvn %*
)
