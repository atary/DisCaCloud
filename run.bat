set agg=%1
FOR /L %%G IN (500,500,5000) DO START /B /WAIT java -jar dist/DisCaCloud.jar %%G %agg% 100000