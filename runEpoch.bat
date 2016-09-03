FOR /L %%G IN (11000,1000,20000) DO type NUL > %%G.txt
FOR /L %%G IN (11000,1000,20000) DO START java -jar dist/DisCaCloud.jar %%G 0.016 100000