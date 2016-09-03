FOR /L %%G IN (110,10,200) DO type NUL > %%G.txt
FOR /L %%G IN (110,10,200) DO START java -jar dist/DisCaCloud.jar %%G 0.00 100000