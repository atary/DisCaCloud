type NUL > 0.txt
FOR /L %%G IN (0,3,27) DO type NUL > %%G.txt
START run.bat 0.000
START run.bat 0.003
START run.bat 0.006
START run.bat 0.009
START run.bat 0.012
START run.bat 0.015
START run.bat 0.018
START run.bat 0.021
START run.bat 0.024
START run.bat 0.027