const bool DEBUG = false;

int activeChannel = 0; // active Channel (Pin)
bool state = false; // says if device confirmed the channel change

const int ARRAY_LENGTH = 4;
int pinsCh[ARRAY_LENGTH] = {2,3,4,5};
int pinsLED[ARRAY_LENGTH] = {9,10,11,12};

byte channelCommands[4][26] = {
  {0x01,0xfe,0x00,0x00,0x53,0xfe,0x1a,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0xf0,0x01,0x24,0x00,0x01,0x38,0x00,0x00,0x00,0xf7},
  {0x01,0xfe,0x00,0x00,0x53,0xfe,0x1a,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0xf0,0x01,0x24,0x00,0x01,0x38,0x00,0x00,0x01,0xf7},
  {0x01,0xfe,0x00,0x00,0x53,0xfe,0x1a,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0xf0,0x01,0x24,0x00,0x01,0x38,0x00,0x00,0x02,0xf7},
  {0x01,0xfe,0x00,0x00,0x53,0xfe,0x1a,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0xf0,0x01,0x24,0x00,0x01,0x38,0x00,0x00,0x03,0xf7}
};

byte deviceAnswerOK[23] = {0x01,0xfe,0x00,0x00,0x41,0xff,0x17,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0xf0,0x01,0x24,0x00,0x04,0x38,0xf7};
//check only the last 5 bytes here
byte deviceCommands[4][5] = {
  {0x38,0x00,0x00,0x00,0xf7},
  {0x38,0x00,0x00,0x01,0xf7},
  {0x38,0x00,0x00,0x02,0xf7},
  {0x38,0x00,0x00,0x03,0xf7}
};

void setup() {
  // initialize serial:
  Serial.begin(9600);

  // initiale INPUT and OUTPUT Pins
  for (int i = 0; i < ARRAY_LENGTH; i++) {
    pinMode(pinsCh[i], INPUT_PULLUP);  //switches
    pinMode(pinsLED[i], OUTPUT);  // LEDs
  }

  // amazing led lightshow on startup
  startLightShow(pinsLED);
  delay(1500);

  // set Channel 1 on startup
  activeChannel = activateChannel(pinsCh[0]);
}

void loop() { 
  if (state) { //only check signals if last transmission was successful
    checkSignals();
  }
  if (Serial.available() == 26) {
    int i = 0;
    int j = 0;
    int matchChannel1 = 0;
    int matchChannel2 = 0;
    int matchChannel3 = 0;
    int matchChannel4 = 0;
    //Serial.write("Got Data with 26");
    while(Serial.available()) {
      byte input = Serial.read();
      if(i >= 21) {
        if (deviceCommands[0][j] == input) {
          matchChannel1++;
        }
        if (deviceCommands[1][j] == input) {
          matchChannel2++;
        }
        if (deviceCommands[2][j] == input) {
          matchChannel3++;
        }
        if (deviceCommands[3][j] == input) {
          matchChannel4++;
        }
        j++;
      } 
      i++;
      }
      //Serial.flush();
      if (matchChannel1 == j) {
        digitalWrite(pinsLED[0], HIGH);
        digitalWrite(pinsLED[1], LOW);
        digitalWrite(pinsLED[2], LOW);
        digitalWrite(pinsLED[3], LOW);
        //Serial.write("USB-Channel1");
        activeChannel = 1;
      }
      else if (matchChannel2 == j) {
        digitalWrite(pinsLED[0], LOW);
        digitalWrite(pinsLED[1], HIGH);
        digitalWrite(pinsLED[2], LOW);
        digitalWrite(pinsLED[3], LOW);
        //Serial.write("USB-Channel2");
        activeChannel = 2;
      }
      else if (matchChannel3 == j) {
        digitalWrite(pinsLED[0], LOW);
        digitalWrite(pinsLED[1], LOW);
        digitalWrite(pinsLED[2], HIGH);
        digitalWrite(pinsLED[3], LOW);
        //Serial.write("USB-Channel3");
        activeChannel = 3;
      }
      else if (matchChannel4 == j) {
        digitalWrite(pinsLED[0], LOW);
        digitalWrite(pinsLED[1], LOW);
        digitalWrite(pinsLED[2], LOW);
        digitalWrite(pinsLED[3], HIGH);
        //Serial.write("USB-Channel4");
        activeChannel = 4;
      }
  }
}

void startLightShow(int pins[]) {
  
  ledsSwitch(pins, true, 150, true, true);
  ledsSwitch(pins, false, 150, true, false);
  ledsSwitch(pins, true, 150, true, true);

  for (int i = 0; i < 10; i++) {
    ledsSwitch(pins, i%2, 150, false, true);
  }
}

void ledsSwitch (int pins[], bool turnOn, int msDelay, bool steps, bool forward) {
  /*
   * pins[]: array of led pin numbers
   * turnOn: leds on or off 
   * msDelay: blinking delay in ms
   * steps: blinking one by one or all together
   * forward: loop over led array forward or reverse
  */
  if (forward) {
    for (int i = 0; i < ARRAY_LENGTH; i++) {
      if (turnOn) 
      {
        digitalWrite(pins[i], HIGH);
      }
      else
      {
        digitalWrite(pins[i], LOW);
      }
      if (steps) {
        delay(msDelay);
      }
    } 
  } 
  else {
    for (int i = ARRAY_LENGTH -1 ; i >= 0; i--) {
      if (turnOn) 
      {
        digitalWrite(pins[i], HIGH);
      }
      else
      {
        digitalWrite(pins[i], LOW);
      }
      if (steps) {
        delay(msDelay);
      }
    } 
  }

  if (!steps) {
    delay(msDelay);
  }
}

void ledSwitch (int pin, bool turnOn, int msDelay, bool steps, bool forward) {
  int pins[1] = {pin};
  ledsSwitch (pins,turnOn,msDelay,steps,forward);
}
int activateChannel(int channel) {
  for (int i = 0; i < ARRAY_LENGTH; i++) {
    if (channel == pinsCh[i]) {
      sendData(channelCommands[i], pinsLED[i]);
      digitalWrite(pinsLED[i], HIGH);
    } else {
      digitalWrite(pinsLED[i], LOW);
    }
  }
  return channel;
}

void sendData(byte data[26], int ledPin) {
  state = false;
  int countLoops = 1;
  Serial.write(data,26);
  delay(30);
  while (state == false) {
    if (countLoops % 8 == 0) {
      //Retry signal send
      Serial.write(data,26);
    }
    if (Serial.available()) {
      int i = 0;
      while(Serial.available()) {
        state = deviceAnswerOK[i] == Serial.read();
        i++;
        if (DEBUG) {
          state = true;
        }
      }
    } else { //visual feedback if state is not OK
     
      ledSwitch(ledPin, true, 200, false, false);
      ledSwitch(ledPin, false, 200, false, false);
    }
    countLoops++;
  }
}

void checkSignals() {
  for (int i = 0; i < ARRAY_LENGTH; i++) {  // check all Inputs
    if (digitalRead(pinsCh[i]) == LOW && activeChannel != pinsCh[i]) {
      activeChannel = activateChannel(pinsCh[i]);
      delay(500);  //wait until checking next signal
    }
  }
}
