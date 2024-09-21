import java.util.HashSet;
import java.util.Set;
import com.synthbot.jasiohost.*;

public class AudioHw implements AsioDriverListener {
	private static final int SAMPLING_TIME_MS = 10000; //sampling time 10s
	private static final int totalPoint = (SAMPLING_TIME_MS / 1000) * Config.PHY_TX_SAMPLING_RATE;  // sampling freq 48kHz
	private AsioDriver asioDriver;
	private Set<AsioChannel> activeChannels;
	private AsioChannel inputChannel;
	private AsioChannel outputChannel;
	private float[] Buffer;
	private boolean isRecording=false;
	private boolean isPlaying=false;
	private int totalSamplesRecorded = 0;
	private int samplesPlayed = 0; 

	// private float phase = 0;
	// private float freq = 5000f;  // Hz
	// private float sampleRate = 48000f;
	// private float dphase = (2 * (float)Math.PI * freq) / sampleRate;
	
	// private float[] output;
    
	public void init() {
		activeChannels = new HashSet<AsioChannel>();  // create a Set of AsioChannels

		if (asioDriver == null) {
			asioDriver = AsioDriver.getDriver("ASIO4ALL v2");
			asioDriver.addAsioDriverListener(this); // add an AsioDriverListener in order to receive callbacks from the driver
			
			asioDriver.setSampleRate(Config.PHY_TX_SAMPLING_RATE);
			Buffer = new float[totalPoint];

			outputChannel = asioDriver.getChannelOutput(0);
			inputChannel = asioDriver.getChannelInput(0);
			activeChannels.add(outputChannel);
			activeChannels.add(inputChannel);
			
			asioDriver.createBuffers(activeChannels); // create the audio buffers and prepare the driver to run

			System.out.println("ASIO buffer created, size: " + asioDriver.getBufferPreferredSize());
			System.out.println("ASIO Driver State: " + asioDriver.getCurrentState());
			
			System.out.println("------------------");
			System.out.println("Input Channels");
			// input channel 
			for (int i = 0; i < asioDriver.getNumChannelsInput(); i++) {
				System.out.println(asioDriver.getChannelInput(i));
			}
			
			System.out.println("------------------");
			System.out.println("Output Channels");
			// output channel
			for (int i = 0; i < asioDriver.getNumChannelsOutput(); i++) {
				System.out.println(asioDriver.getChannelOutput(i));
			}
			// activated channel print
			System.out.println("------------------");
			System.out.println("Active Channels");
			for (AsioChannel channel : activeChannels) {
				System.out.println(channel);
			}
			
			int numChannels = asioDriver.getNumChannelsInput();
			// System.out.println(numChannels);
			// no channel activated, error
			if (numChannels == 0) {
				System.out.println("No input channels found. Try restarting the ASIO driver.");
				asioDriver.returnToState(AsioDriverState.INITIALIZED);
				asioDriver.shutdownAndUnloadDriver(); 
				System.exit(0);
			}
		}
		/*
			* buffer size should be set either by modifying the JAsioHost source code or
			* configuring the preferred value in ASIO native window. We choose 128 i.e.,
			* asioDriver.getBufferPreferredSize() should be equal to Config.HW_BUFFER_SIZE = 128;
			* 
		*/
	}

	public void startRecording() {
		if (asioDriver != null) {
			System.out.println(asioDriver.getCurrentState());
			isRecording = true;
			asioDriver.start(); // start the driver
		}
	}

	public void stopRecording() {
		System.out.println("Recording stopped");
		isRecording = false;
		// test print  
		// if (totalSamplesRecorded > 0) {
		// 	System.out.println("Total samples recorded: " + totalSamplesRecorded);
		// 	for (int i = 0; i < 10; i++) { // print first 10 samples 
		// 		System.out.println("Buffer[" + i + "] = " + Buffer[i]);
		// 	}
		// } else {
		// 	System.out.println("No samples recorded.");
		// }
	}

	public void startPlayback() {
		if (asioDriver != null && totalSamplesRecorded > 0) {
			System.out.println("Playback started");
			isPlaying = true;

		}
	}

	public void stopPlayback() {
		isPlaying = false;
		asioDriver.returnToState(AsioDriverState.INITIALIZED);
		asioDriver.shutdownAndUnloadDriver(); 
		System.out.println("Playback stopped");
	}
	
	@Override
	public void bufferSwitch(final long systemTime, final long samplePosition, final Set<AsioChannel> channels) {
		// reading
		if (isRecording && totalSamplesRecorded < totalPoint) {
			for (AsioChannel channelInfo : channels) {
				// input 
				if (channelInfo.equals(inputChannel)) {

					float[] tempBuffer = new float[Config.HW_BUFFER_SIZE];
					channelInfo.read(tempBuffer);
					int samplesToCopy = Math.min(Config.HW_BUFFER_SIZE, totalPoint - totalSamplesRecorded);
					//normally samples2copy = 128 
					// but the last clip to read may < HW_BUFFER_SIZE = 128
					System.arraycopy(tempBuffer, 0, Buffer, totalSamplesRecorded, samplesToCopy);
					// head of copy index
					totalSamplesRecorded += samplesToCopy;
					//print test
					// for (int i = 0; i < Math.min(3, samplesToCopy); i++) {
					// 	System.out.println("tempBuffer[" + i + "] = " + tempBuffer[i]);
					// }
					// 读取数据
				}
			}
		}
		// writing
		else if (isPlaying && samplesPlayed <= totalPoint) { // 
			for (AsioChannel channelInfo : channels) {
				//output
				//System.out.println("111");
				if (channelInfo.equals(outputChannel)) {
					int samples2write = Math.min(Config.HW_BUFFER_SIZE, totalPoint - samplesPlayed);
					float[] tempBuffer = new float[samples2write];
					System.arraycopy(Buffer, samplesPlayed, tempBuffer, 0, samples2write);
					channelInfo.write(tempBuffer);
					samplesPlayed += samples2write;
					////test
					// for (int i = 0; i < Math.min(3, tempBuffer.length); i++) {
					// 	System.out.println("tempBuffer[" + i + "] = " + tempBuffer[i]);
					// }

				}
			}
		}
	}

	@Override
	public void latenciesChanged(final int inputLatency, final int outputLatency) {
		System.out.println("latenciesChanged() callback received.");
	}

	@Override
	public void bufferSizeChanged(final int bufferSize) {
		System.out.println("bufferSizeChanged() callback received.");
	}

	@Override
	public void resetRequest() {
		/*
		 * This thread will attempt to shut down the ASIO driver. However, it will block
		 * on the AsioDriver object at least until the current method has returned.
		 */
		new Thread() {
			@Override
			public void run() {
				System.out.println("resetRequest() callback received. Returning driver to INITIALIZED state.");
				asioDriver.returnToState(AsioDriverState.INITIALIZED);
			}
		}.start();
	}

	@Override
	public void resyncRequest() {
		System.out.println("resyncRequest() callback received.");
	}

	@Override
	public void sampleRateDidChange(final double sampleRate) {
		System.out.println("sampleRateDidChange() callback received.");
	}

  	public static void main(final String[] args) {
		
		final AudioHw audiohw = new AudioHw();
		audiohw.init();
		audiohw.startRecording();
		try {
			Thread.sleep(SAMPLING_TIME_MS); // =delay, recording
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
		
		audiohw.stopRecording();

		audiohw.startPlayback();

		try {
			Thread.sleep(SAMPLING_TIME_MS);
			// playing 
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
		audiohw.stopPlayback();
	}
}


