@echo off
echo "loop args by batch file."
:loop
if "%1"=="" goto :done
echo %1
shift
goto :loop

:done
echo Done.
