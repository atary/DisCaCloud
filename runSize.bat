FOR /L %%G IN (100000,100000,800000) DO type NUL > %%G.txt
FOR /L %%G IN (100000,100000,800000) DO START java -jar dist/DisCaCloud.jar 1400 0.00 %%G