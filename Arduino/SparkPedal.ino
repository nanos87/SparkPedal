const bool DEBUG = false;

int activeChannel = 0; // active Channel (Pin)
bool state = false; // says if device confirmed the channel change

const int ARRAY_LENGTH = 4;
int pinsCh[ARRAY_LENGTH] = {2,3,4,5};
int pinsLED[ARRAY_LENGTH] = {9,10,11,12};

byte channelCommands[4][27] = {
  {0x01,0xfe,0x00,0x00,0x53,0xfe,0x1a,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0xf0,0x01,0x24,0x00,0x01,0x38,0x00,0x00,0x00,0xf7,0x79},
  {0x01,0xfe,0x00,0x00,0x53,0xfe,0x1a,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0xf0,0x01,0x24,0x00,0x01,0x38,0x00,0x00,0x01,0xf7,0x79},
  {0x01,0xfe,0x00,0x00,0x53,0xfe,0x1a,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0xf0,0x01,0x24,0x00,0x01,0x38,0x00,0x00,0x02,0xf7,0x79},
  {0x01,0xfe,0x00,0x00,0x53,0xfe,0x1a,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0xf0,0x01,0x24,0x00,0x01,0x38,0x00,0x00,0x03,0xf7,0x79}
};

byte deviceAnswerOK[23] = {0x01,0xfe,0x00,0x00,0x41,0xff,0x17,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0xf0,0x01,0x24,0x00,0x04,0x38,0xf7};

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

  // set Channel 1 on startup
  activeChannel = activateChannel(pinsCh[0]);
}

void loop() { 
  if (state) { //only check signals if last transmission was successful
    checkSignals();
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


int activateChannel(int channel) {
  for (int i = 0; i < ARRAY_LENGTH; i++) {
    if (channel == pinsCh[i]) {
      sendData(channelCommands[i]);
      digitalWrite(pinsLED[i], HIGH);
    } else {
      digitalWrite(pinsLED[i], LOW);
    }
  }
  return channel;
}

void sendData(byte data[27]) {
  state = false;
  while (state == false) {
    Serial.write(data,27);
    //Serial.println(data, HEX);
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
      ledsSwitch(pinsLED, true, 150, false, false);
      ledsSwitch(pinsLED, false, 150, false, false);
    }
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
