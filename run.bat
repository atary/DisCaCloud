set agg=%1
FOR /L %%G IN (3000,1000,12000) DO START /B /WAIT java -jar dist/DisCaCloud.jar %%G %agg% 100000