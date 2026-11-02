@echo off
rem Double-click to open the autotest matrix GUI (works from any current directory).
where pythonw >nul 2>nul && (start "" pythonw "%~dp0matrix_gui.py" %* & exit /b)
where python >nul 2>nul && (start "" python "%~dp0matrix_gui.py" %* & exit /b)
echo Python 3.9+ was not found on PATH. Install it from https://www.python.org/ (with tkinter) and try again.
pause
