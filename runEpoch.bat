FOR /L %%G IN (3100,100,4000) DO type NUL > %%G.txt
FOR /L %%G IN (3100,100,4000) DO START java -jar dist/DisCaCloud.jar %%G 0.01 100000