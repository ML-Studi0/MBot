rmdir /S /Q .\.gradle
rmdir /S /Q .\.idea
rmdir /S /Q .\app\build
rmdir /S /Q .\build
rmdir /S /Q .\app\.idea
rmdir /S /Q .\gradle
del   .\app\*.iml
del   .\app\*.bak

del   *.iml
del   *.bak

del local.properties
del gradle.properties
del gradlew
del gradlew.bat
