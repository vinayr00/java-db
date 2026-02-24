@echo off
echo Compiling StudentBackend...
javac -cp ".;sqlite-jdbc-3.36.0.3.jar" StudentBackend.java
if %errorlevel% neq 0 (
    echo Compilation failed!
    pause
    exit /b %errorlevel%
)
echo Starting Web Server at http://localhost:8080...
echo Keep this window open to access the application.
java -cp ".;sqlite-jdbc-3.36.0.3.jar" StudentBackend
pause
