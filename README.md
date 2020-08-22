# SparkPedal
![alt text](https://github.com/nanos87/SparkPedal/blob/master/App/Android/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png "logo")

Project to create a simple footswitch for positive grids' spark amplifier.

Demo (functional): https://youtu.be/c4bS8wjsf9A
Demo (song): https://youtu.be/IBmtbeGdhAU

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
At least they should be supported by this USB serial library: https://github.com/felHR85/UsbSerial

### Schematic
![alt text](https://github.com/nanos87/SparkPedal/blob/master/Arduino/SparkPedal_Schematic.png "schematic")

### Pedalboard
![alt text](https://github.com/nanos87/SparkPedal/blob/master/Pedalboard/pb_front.png "pedalboard front")
![alt text](https://github.com/nanos87/SparkPedal/blob/master/Pedalboard/pb_back.png "pedalboard back")
![alt text](https://github.com/nanos87/SparkPedal/blob/master/Pedalboard/pb_left.png "pedalboard left")
![alt text](https://github.com/nanos87/SparkPedal/blob/master/Pedalboard/pb_right.png "pedalboard right")
![alt text](https://github.com/nanos87/SparkPedal/blob/master/Pedalboard/pb_front_led.png "pedalboard led")
![alt text](https://github.com/nanos87/SparkPedal/blob/master/Pedalboard/pb_front_1.png "pedalboard top")
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


This project is inspired by https://github.com/jrnelson90/tinderboxpedal
