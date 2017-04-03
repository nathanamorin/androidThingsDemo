package com.nathanmorin.androidthingsdemotemp;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManagerService;
import com.google.android.things.pio.UartDevice;
import com.google.android.things.pio.UartDeviceCallback;

import java.io.IOException;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "Main Activity";
    private static final String GPIO_PORT_1 = "BCM4";
    private static final String UART_DEVICE_NAME = "UART0";
    PeripheralManagerService manager;
    private UartDevice serial;
    int BUFFER_LENGTH = 30;
    byte[] buffer = new byte[BUFFER_LENGTH];
    Gpio gpio;
    String test_buffer = "";
    double current_temp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        manager = new PeripheralManagerService();
        try {
            gpio = manager.openGpio(GPIO_PORT_1);
            gpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            serial = manager.openUartDevice(UART_DEVICE_NAME);
            serial.setBaudrate(115200);
            serial.setDataSize(8);
            serial.setStopBits(1);
            serial.registerUartDeviceCallback(mUartCallback);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void btn_Click(View view) {
        try {
            gpio.setValue(!gpio.getValue());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    private UartDeviceCallback mUartCallback = new UartDeviceCallback() {
        @Override
        public boolean onUartDeviceDataAvailable(UartDevice uart) {
            // Read available data from the UART device
            try {
                readUartBuffer(uart);
            } catch (IOException e) {
                Log.w(TAG, "Unable to access UART device", e);
            }

            // Continue listening for more interrupts
            return true;
        }

        @Override
        public void onUartDeviceError(UartDevice uart, int error) {
            Log.w(TAG, uart + ": Error event " + error);
        }
    };


    public void readUartBuffer(UartDevice uart) throws IOException {
        // Maximum amount of data to read at one time
        final int maxCount = 30;
        byte[] temp_buffer = new byte[maxCount];

        int count;
        while ((count = uart.read(temp_buffer, temp_buffer.length)) > 0) {
//            Log.d(TAG, Arrays.toString(temp_buffer));

            for (byte b : temp_buffer){

                if (b == 0) continue;

                if ((char)b == '^' || (char)b == ':'){

                    if (!test_buffer.equals("")){
                        int count_periods = test_buffer.length() - test_buffer.replace(".", "").length();
                        if (count_periods == 1 &&
                                !(test_buffer.charAt(0) == '.') &&
                                !(test_buffer.charAt(test_buffer.length()-1) == '.')){

                            try{
//                                Log.d(TAG,test_buffer);
                                double tmp = Double.parseDouble(test_buffer);

                                if (current_temp == 0 || Math.abs(tmp - current_temp) < 10){
                                    current_temp = tmp;
                                    ((TextView)findViewById(R.id.tmpOut)).setText(Double.toString(current_temp));
                                    Log.d(TAG,Double.toString(current_temp));
                                }

//                                Log.e(TAG,Double.toString(current_temp));

                            }catch (Exception ex){
                                ex.printStackTrace();
                            }
                        }

                        test_buffer = "";
                    }

                } else{
                    test_buffer += (char)b;
                }



            }
            temp_buffer = new byte[maxCount];
        }
    }

    private String getStr(byte[] bytes){
        Log.d(TAG, Arrays.toString(bytes));
        String re = "";
        for (byte b : bytes){
            if (b == 0) continue;
            re += (char)b;
        }
        return re;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        serial.unregisterUartDeviceCallback(mUartCallback);
    }
}
