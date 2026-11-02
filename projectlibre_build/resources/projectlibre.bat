@echo off
rem Small initial heap, up to half of RAM, G1 for short pauses (same as the macOS app).
rem Class data sharing: the first run writes an archive of the loaded classes, later runs start faster.
if not exist "%LOCALAPPDATA%\ProjectLibre" mkdir "%LOCALAPPDATA%\ProjectLibre"
java -Xms64m -XX:MaxRAMPercentage=50 -XX:+UseG1GC -XX:+AutoCreateSharedArchive "-XX:SharedArchiveFile=%LOCALAPPDATA%\ProjectLibre\projectlibre.jsa" -jar "%~dp0projectlibre.jar" %*
