type NUL > 0.txt
FOR /L %%G IN (1,2,17) DO type NUL > %%G.txt
START run.bat 0.000
START run.bat 0.001
START run.bat 0.003
START run.bat 0.005
START run.bat 0.007
START run.bat 0.009
START run.bat 0.011
START run.bat 0.013
START run.bat 0.015
START run.bat 0.017