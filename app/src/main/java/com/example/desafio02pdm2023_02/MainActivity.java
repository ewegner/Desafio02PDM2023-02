package com.example.desafio02pdm2023_02;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.Timestamp;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private Spinner fromCurrency;
    private Spinner toCurrency;
    private TextView txtPrice;
    private TextView txtTime;
    private String moeda1, moeda2;
    private int moeda1pos, moeda2pos;

    String [] currencies = {"BRL", "BTC", "EUR", "USD", "GBR"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fromCurrency = (Spinner) findViewById(R.id.fromCurrency);
        toCurrency   = (Spinner) findViewById(R.id.toCurrency);
        txtPrice     = (TextView) findViewById(R.id.txtPrice);
        txtTime      = (TextView) findViewById(R.id.txtTime);

        SharedPreferences cotacao = getSharedPreferences("UltimaCotacao", MODE_PRIVATE);
        String price = cotacao.getString("price", "");
        String datetime = cotacao.getString("datetime", "");
        int moeda1saved = cotacao.getInt("moeda1", 0);
        int moeda2saved = cotacao.getInt("moeda2", 0);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, Arrays.asList(currencies));
        fromCurrency.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                moeda1 = currencies[i];
                moeda1pos = i;
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        toCurrency.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                moeda2 = currencies[i];
                moeda2pos = i;
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        fromCurrency.setAdapter(adapter);
        toCurrency.setAdapter(adapter);

        if (!price.isEmpty() && !datetime.isEmpty()) {
            fromCurrency.setSelection(moeda1saved);
            toCurrency.setSelection(moeda2saved);

            txtPrice.setText("Última cotação: " + price);
            txtTime.setText("Data: " + datetime);
        }
    }

    public void convert(View view) {
        new MainActivity.HttpAsyncTask().execute();
    }

    private class HttpAsyncTask extends AsyncTask<String, Void, String> {
        ProgressDialog dialog;

        protected String doInBackground(String... params) {
            try {

                URL url = new URL("https://economia.awesomeapi.com.br/json/last/" + moeda1 + "-" + moeda2);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                int status = urlConnection.getResponseCode();

                if (status == 200) {
                    InputStream stream = new BufferedInputStream(urlConnection.getInputStream());
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stream));
                    StringBuilder builder = new StringBuilder();

                    String inputString;

                    while ((inputString = bufferedReader.readLine()) != null) {
                        builder.append(inputString);
                    }

                    urlConnection.disconnect();
                    return builder.toString();
                }
            } catch (Exception ex) {
                Toast.makeText(MainActivity.this, ex.getMessage() + "", Toast.LENGTH_SHORT).show();
            }
            return null;
        }
        public void onPostExecute(String result) {
            dialog.dismiss();

            if(result != null) {
                try {
                    JSONObject obj = new JSONObject(result);
                    JSONObject arrayCotacao = obj.getJSONObject(moeda1+moeda2);

                    String price = arrayCotacao.get("bid").toString() + " " + arrayCotacao.get("codein").toString();

                    SimpleDateFormat oldFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                    Date date = oldFormat.parse(arrayCotacao.get("create_date").toString());
                    SimpleDateFormat newFormat = new SimpleDateFormat("dd/MM/yyyy H:mm");
                    String datetime = newFormat.format(date);

                    txtPrice.setText("Última cotação: " + price);
                    txtTime.setText("Data: " + datetime);

                    SharedPreferences cotacao = getSharedPreferences("UltimaCotacao", MODE_PRIVATE);
                    SharedPreferences.Editor editor = cotacao.edit();
                    editor.putString("price", price);
                    editor.putString("datetime", datetime);
                    editor.putInt("moeda1", moeda1pos);
                    editor.putInt("moeda2", moeda2pos);
                    editor.apply();
                    editor.commit();

                } catch (Exception ex) {
                    Toast.makeText(MainActivity.this, ex.getMessage() + "", Toast.LENGTH_SHORT).show();
                }
            } else {
                txtPrice.setText("Não foi possível encontrar cotação para essa combinação");
            }
        }

        protected void onPreExecute() {
            dialog = new ProgressDialog(MainActivity.this);
            dialog.show();
        }
    }
}