
#log文件默认放在当前目录下.
#项目端口在application.yml中修改.
#验证码训练集也在application.yml中修改.
nohup java -jar api-0.0.1.jar > file.log 2>&1 &