# PROJECT REPORT

This report aims to record some difficulties encountered in project, and solutions.

## PROJECT 0

### TASK0and1

- writing 没有进入判断相当于：isPlaying一直是false  // writing: elseif (isPlaying&&totalSamplesRecorded<=totalPoint)
- attention: totalSamplesRecorded 变量不要弄错
- Envionment: Download stable JDK edition, not JRE. Do NOT use the latest but unstable JDK, or you will spend whole night(like poor me), on fixing the unmatch, cannot find the package and so on.
- tempbuffer: store instant data, to verify whether the computer receives the input voice; need to vary in sequence. The problem is tempbuffer kept printing 0.0
- Driver setting in ASIO4ALL, firstly print the active channel names and tick(brighten) the coresponding name of sound card driver in the menu.

### TASK2

#### Method

ADC - Discrete waveform -> sound card driver's buffer -(copy) -> sound card hardware buffer - DAC
