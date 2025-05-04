//package com.example.a3aaaa;

/*import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import java.io.InputStream;
import java.util.UUID;

public class BluetoothService {
    private BluetoothDevice device;
    private Context context;
    private BluetoothSocket socket;
    private InputStream inputStream;
    private Thread readThread;
    private OnDataReceivedListener listener;

    public BluetoothService(BluetoothDevice device, Context context) {
        this.device = device;
        this.context = context;
    }

    public void connect() {
        try {
            socket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            socket.connect();
            inputStream = socket.getInputStream();

            readThread = new Thread(() -> {
                byte[] buffer = new byte[1024];
                int bytes;
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        bytes = inputStream.read(buffer);
                        if (bytes > 0) {
                            String data = new String(buffer, 0, bytes);
                            if (listener != null) {
                                listener.onDataReceived(data.trim());
                            }
                        }
                    } catch (Exception e) {
                        Log.e("BluetoothService", "Ошибка при чтении", e);
                        break;
                    }
                }
            });
            readThread.start();
        } catch (Exception e) {
            Log.e("BluetoothService", "Ошибка подключения", e);
        }
    }

    public void setOnDataReceivedListener(OnDataReceivedListener listener) {
        this.listener = listener;
    }

    public interface OnDataReceivedListener {
        void onDataReceived(String data);
    }
}

 */




/* package com.example.a3aaaa;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static final String BASE_URL = "http://10.0.2.2:5000";
    private static Retrofit retrofit;

    public static ApiService getApiService() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(ApiService.class);
    }
}


 */




