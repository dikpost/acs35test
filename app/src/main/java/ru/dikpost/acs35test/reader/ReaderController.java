package ru.dikpost.acs35test.reader;

import android.content.Context;
import android.media.AudioManager;
import android.util.Log;
import android.widget.Toast;

import com.acs.audiojack.AudioJackReader;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ru.dikpost.acs35test.activity.MainActivity;

public class ReaderController {

	public interface ReaderControllerListener {
		
		void onOutputStrings( List<String> out );
		void onAtrString(String atr);
	}

	ReaderControllerListener listener;
	
	public static String TAG = "ACS35Test";

	MainActivity context;
	AudioManager audioManager;
	AudioJackReader reader;
	int waitTimeout = 10;
	int cardType = 0x8F;

	byte[] blockNum = new byte[1];
	
	byte[] authKey = null;
	byte[] keyType = null;
	
//	byte[] commandGetId = {(byte)0xFF,(byte)0xCA,(byte)0x00,(byte)0x00,(byte)0x00};
	byte[] commandLoadKeys = {(byte)0xFF,(byte)0x82,(byte)0x00,(byte)0x00,(byte)0x06,
			(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF};
	byte[] commandAutorize = {(byte)0xFF,(byte)0x86,(byte)0x00,(byte)0x00,(byte)0x05,
			(byte)0x01,(byte)0x00,(byte)0x04/*04*/,(byte)0x60,(byte)0x00};
	byte[] commandReadBlock = {(byte)0xFF,(byte)0xB0,(byte)0x00,(byte)0x04/*04*/,(byte)0x30};
	
	List<Command> commands = null;
	List<String> out = null;
	static boolean process = false;
	static int cnt = 0;
	
	public ReaderController( MainActivity c ){
		context = c;
		listener = (MainActivity) context;
		
	    audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        reader = new AudioJackReader(audioManager, true);
        
        reader.setOnPiccAtrAvailableListener( new AtrAvailableListener());
        reader.setOnPiccResponseApduAvailableListener( new ResponseApduAvailableListener());
        reader.setOnDeviceIdAvailableListener( new DeviceIdAvailableListener());
        reader.setOnFirmwareVersionAvailableListener( new FirmwareVersionAvailableListener());
        
        commands = new ArrayList<>();
    	out = new ArrayList<>();
	}

	
	
	public AudioJackReader getReader(){
		return reader;
	}
	
	public void reset( String key, String kType ){

		if(!checkResetVolume()) return;
		
		byte[] tmp = null;
//		String key = "D3 F7 D3 F7 D3 F7";
		authKey = hexStringToBinary(key);
		keyType = new byte[1];
		keyType[0] = (byte)(Integer.decode(kType) & 0xFF);
		
		blockNum[0] = (byte)0x04;

		System.arraycopy(authKey, 0, commandLoadKeys, 5, authKey.length);
        System.arraycopy(blockNum, 0, commandAutorize, 7, 1);
        System.arraycopy(keyType, 0, commandAutorize, 8, 1);
        System.arraycopy(blockNum, 0, commandReadBlock, 3, 1);

        out.clear();
    	commands.clear();
    	Command cmd = new Command(0,0);
    	cmd.data = commandLoadKeys;
    	commands.add( cmd );

		cnt = 0;
    	int i = 4;

		blockNum[0] = (byte)i;
		tmp = commandAutorize.clone();
		cmd = new Command(0,i);
		cmd.data = tmp;
		commands.add(cmd);
		tmp = commandReadBlock.clone();
		cmd = new Command(1,i);
		cmd.data = tmp;
		commands.add(cmd);

      	reader.reset( new ResetCompleteListener());
//        Log.d(TAG, "onReaderReset()");
	}
	
	class ResetCompleteListener implements AudioJackReader.OnResetCompleteListener {

		@Override
		public void onResetComplete(AudioJackReader arg0) {
			
			Log.d(TAG, "onReaderResetComplete()");
			reader.piccPowerOn(waitTimeout, cardType);
        }
	}
	
	class AtrAvailableListener implements AudioJackReader.OnPiccAtrAvailableListener {

		@Override
		public void onPiccAtrAvailable(AudioJackReader arg0, byte[] arg1) {
		
			final String atr = toHexString(arg1);
//	        Log.d(ReaderController.TAG, "Reader.atrAvailable(): " + atr);
	        reader.piccTransmit(waitTimeout, commands.get(0).getData());

	        context.runOnUiThread(new Runnable() {

	            @Override
	            public void run() {

	        		listener.onAtrString("ATR: " + atr);
                }
	        });
		}
	}
	
	class ResponseApduAvailableListener implements AudioJackReader.OnPiccResponseApduAvailableListener {

		@Override
		public void onPiccResponseApduAvailable(AudioJackReader arg0, byte[] arg1) {
			
			boolean state = isSuccessfulResponse(arg1);
			if(!state){
				readDone( false );
				return;
			}
			int block = commands.get(cnt).getBlock();
			int type = commands.get(cnt).getType();
			byte[] str = null;
			
			if(type == 1){ // read block
					out.add(String.format("DATA: start block = %d, length = %d bytes\n", block, arg1.length-2));
					str = new byte[ arg1.length - 2];
					System.arraycopy(arg1, 0, str, 0, arg1.length-2);
					out.add(new String(toHexString( str )));
				    //Log.d(ReaderController.TAG, "Reader.apduAvailable().read: " + toHexString(str));
			}
			
	        cnt++;
	        if( cnt < commands.size() ) {
	        	
	        	reader.piccTransmit(waitTimeout, commands.get(cnt).getData());
	        }else{
	        	
	        	readDone( true );
	        }
		}
	}
	
	class DeviceIdAvailableListener implements AudioJackReader.OnDeviceIdAvailableListener {

		@Override
		public void onDeviceIdAvailable(AudioJackReader arg0, byte[] arg1) {
			
	        Log.d(ReaderController.TAG, "Reader.deviceIdAvailable(): " + arg1);
        }
	}
	
	class FirmwareVersionAvailableListener implements AudioJackReader.OnFirmwareVersionAvailableListener {

		@Override
		public void onFirmwareVersionAvailable(AudioJackReader arg0, String arg1) {
		
			
		}
	}
	
	private void readDone( boolean success ){
		
    	reader.piccPowerOff();
    	if(!success){
    		out.clear();
    		out.add(new String("Status: READ ERROR"));
    	}
    	
        context.runOnUiThread(new Runnable() {

            @Override
            public void run() {

        		listener.onOutputStrings(out);

            }
        });
    }
	
    private String toHexString(byte[] buffer) {

        String bufferString = "";

        if (buffer != null) {

            for (int i = 0; i < buffer.length; i++) {

                String hexChar = Integer.toHexString(buffer[i] & 0xFF);
                if (hexChar.length() == 1) {
                    hexChar = "0" + hexChar;
                }

                bufferString += hexChar.toUpperCase(Locale.US) + " ";
            }
        }

        return bufferString;
    }


    private byte[] hexStringToBinary( String str ) {

    	byte[] binary = null;
    	
        if ( !"".equals(str)) {

        	String[] strBytes = str.split(" "); 
        	binary = new byte[strBytes.length];
            for (int i = 0; i < strBytes.length; i++) {

            	binary[i] = (byte)(Integer.decode("0x" + strBytes[i]) & 0xFF);
            }
        }

        return binary;
    }

    
    private boolean isSuccessfulResponse( byte[] r ){
    	boolean ret = false;
    	if(r.length < 2 ) return false;
    	if((r[r.length - 1] == (byte)0x00) && (r[r.length - 2] == (byte)0x90)) ret = true;
    	
    	return ret;
    }
    
    /**
     * Checks the reset volume.
     * 
     * @return true if current volume is equal to maximum volume.
     */
    private boolean checkResetVolume() {

        boolean ret = true;

        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        if (currentVolume < maxVolume) {

        	Toast.makeText(context, "Поставьте максимальную громкость", Toast.LENGTH_LONG).show();
            ret = false;
        }

        return ret;
    }

}
