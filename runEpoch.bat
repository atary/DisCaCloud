FOR /L %%G IN (4100,100,5000) DO type NUL > %%G.txt
FOR /L %%G IN (4100,100,5000) DO START java -jar dist/DisCaCloud.jar %%G 0.01 100000