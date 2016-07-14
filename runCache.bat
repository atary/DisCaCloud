FOR /L %%G IN (50,50,400) DO type NUL > %%G.txt
FOR /L %%G IN (50,50,400) DO START java -jar dist/DisCaCloud.jar %%G 0.00 100000