@IF EXIST "%~dp0\node.exe" (
  "%~dp0\node.exe"  "%~dp0\dump_args.js" %*
) ELSE (
  @SETLOCAL
  @SET PATHEXT=%PATHEXT:;.JS;=;%
  node  "%~dp0\dump_args.js" %*
)
