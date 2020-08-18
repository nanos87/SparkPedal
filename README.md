# SparkPedal

Project to create a simple footswitch for positive grids' spark amplifier.
Influenced by https://github.com/jrnelson90/tinderboxpedal

## Used components:
### Hardware:
- micro controller (like arduino nano)
- electrical stuff (foot switches, cabels, usb adapter)
- case made of wood, steel and leather

### Software:
- Android (>9) app in Kotlin (with Android Studio)
- Arduino C++ in Arduino IDE


## Arduino nano
Different types of micro controllers will work.
At least they should support this great USB serial lib https://github.com/felHR85/UsbSerial

### Schematic
![alt text](https://github.com/nanos87/SparkPedal/blob/master/Arduino/SparkPedal_Schematic.png "schematic")

### Pedalboard
![alt text](https://github.com/nanos87/SparkPedal/blob/master/Pedalboard/pb_top1.jpg "pedalboard top")
![alt text](https://github.com/nanos87/SparkPedal/blob/master/Pedalboard/pb_back.jpg "pedalboard back")
![alt text](https://github.com/nanos87/SparkPedal/blob/master/Pedalboard/pb_left.jpg "pedalboard left")
![alt text](https://github.com/nanos87/SparkPedal/blob/master/Pedalboard/pb_right.jpg "pedalboard right")
![alt text](https://github.com/nanos87/SparkPedal/blob/master/Pedalboard/Pedalboard.png "draft")


## Android App
### Dependencies
https://github.com/felHR85/UsbSerial

### Connect Activity
![alt text](https://github.com/nanos87/SparkPedal/blob/master/App/Drafts/app_connect.png "connect")

### Control Activity
![alt text](https://github.com/nanos87/SparkPedal/blob/master/App/Drafts/app_control.png "control")

### Settings Activity
![alt text](https://github.com/nanos87/SparkPedal/blob/master/App/Drafts/app_setting.png "setting")