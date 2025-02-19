package com.example.tugas8;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.tugas8.databinding.ActivityMapsBinding;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        Button go = findViewById(R.id.idGo);
        go.setOnClickListener(op);

        Button cari = findViewById(R.id.idCari);
        cari.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sembunyikanKeyBoard(v);
                goCari();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        LatLng ITS = new LatLng(-7.28, 112.79);
        mMap.addMarker(new MarkerOptions().position(ITS).title("ITS"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ITS, 8));
    }

    View.OnClickListener op = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (view.getId() == R.id.idGo) {
                sembunyikanKeyBoard(view);
                gotoLokasi();
            }
        }
    };

    private void gotoLokasi() {
        EditText lat = findViewById(R.id.idLokasiLat);
        EditText lng = findViewById(R.id.idLokasiLng);
        EditText zoom = findViewById(R.id.idZoom);

        try {
            Double dbllat = Double.parseDouble(lat.getText().toString());
            Double dbllng = Double.parseDouble(lng.getText().toString());
            Float dblzoom = Float.parseFloat(zoom.getText().toString());

            Toast.makeText(this, "Move to Lat:" + dbllat + " Long:" + dbllng, Toast.LENGTH_LONG).show();
            gotoPeta(dbllat, dbllng, dblzoom);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid input", Toast.LENGTH_SHORT).show();
        }
    }

    private void goCari() {
        EditText tempat = findViewById(R.id.idDaerah);
        Geocoder g = new Geocoder(getBaseContext());
        try {
            List<Address> daftar = g.getFromLocationName(tempat.getText().toString(), 1);
            if (daftar.isEmpty()) {
                Toast.makeText(this, "Location not found", Toast.LENGTH_LONG).show();
                return;
            }
            Address alamat = daftar.get(0);
            String nemuAlamat = alamat.getAddressLine(0);
            Double lintang = alamat.getLatitude();
            Double bujur = alamat.getLongitude();

            Toast.makeText(getBaseContext(), "Found: " + nemuAlamat, Toast.LENGTH_LONG).show();

            EditText zoom = findViewById(R.id.idZoom);
            Float dblzoom = Float.parseFloat(zoom.getText().toString());

            gotoPeta(lintang, bujur, dblzoom);
            hitungJarak(-7.2819705, 112.795323, lintang, bujur);

            EditText lat = findViewById(R.id.idLokasiLat);
            EditText lng = findViewById(R.id.idLokasiLng);

            lat.setText(lintang.toString());
            lng.setText(bujur.toString());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid zoom level.", Toast.LENGTH_SHORT).show();
        }
    }

    private void gotoPeta(Double lat, Double lng, float z) {
        LatLng Lokasibaru = new LatLng(lat, lng);
        mMap.addMarker(new MarkerOptions().position(Lokasibaru).title("Marker in " + lat + ":" + lng));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(Lokasibaru, z));
    }

    private void sembunyikanKeyBoard(View v) {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }

    private void hitungJarak(double lat1, double lon1, double lat2, double lon2) {
        Location loc1 = new Location("asal");
        Location loc2 = new Location("tujuan");
        loc2.setLatitude(lat2);
        loc2.setLongitude(lon2);
        loc1.setLatitude(lat1);
        loc1.setLongitude(lon1);

        float jarak = loc1.distanceTo(loc2) / 1000;
        String jaraknya = String.valueOf(jarak);
        Toast.makeText(this, "Jarak: " + jaraknya + " km", Toast.LENGTH_SHORT).show();

        drawRoute(lat1, lon1, lat2, lon2);
    }

    private void drawRoute(double lat1, double lon1, double lat2, double lon2) {
        String apiKey = "YOUR_API_KEY";
        String url = "https://maps.googleapis.com/maps/api/directions/json?origin=" + lat1 + "," + lon1 +
                "&destination=" + lat2 + "," + lon2 + "&key=" + apiKey;

        new RouteTask().execute(url);
    }

    private class RouteTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            StringBuilder response = new StringBuilder();
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return response.toString();
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                JSONObject jsonObject = new JSONObject(result);
                JSONArray routes = jsonObject.getJSONArray("routes");
                if (routes.length() > 0) {
                    JSONObject route = routes.getJSONObject(0);
                    JSONObject polyline = route.getJSONObject("overview_polyline");
                    String points = polyline.getString("points");
                    List<LatLng> path = decodePoly(points);

                    PolylineOptions polylineOptions = new PolylineOptions()
                            .addAll(path)
                            .width(10)
                            .color(Color.BLUE);

                    mMap.addPolyline(polylineOptions);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private List<LatLng> decodePoly(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;
            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((lat / 1E5), (lng / 1E5));
            poly.add(p);
        }
        return poly;
    }

    // Menu for selecting map type
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.terserah, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.normal) {
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        } else if (itemId == R.id.terrain) {
            mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
        } else if (itemId == R.id.satellite) {
            mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        } else if (itemId == R.id.hibryd) {
            mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        } else if (itemId == R.id.none) {
            mMap.setMapType(GoogleMap.MAP_TYPE_NONE);
        }

        return super.onOptionsItemSelected(item);
    }

    private void showPassengerConditionDialog() {
        // Inisialisasi kondisi penumpang
        String[] conditions = {"Tunadaksa", "Tunarungu", "Tunanetra", "Tunawicara", "Tuna Grahita", "-"};
        boolean[] checkedConditions = new boolean[conditions.length];

        // Pastikan passengerConditionButton sudah diinisialisasi sebelumnya
        Button passengerConditionButton = findViewById(R.id.idPassengerCondition);

        new AlertDialog.Builder(this)
                .setTitle("Pilih Kondisi Penumpang")
                .setMultiChoiceItems(conditions, checkedConditions, (dialog, which, isChecked) -> {
                    // Set kondisi yang dipilih
                    checkedConditions[which] = isChecked;
                })
                .setPositiveButton("OK", (dialog, which) -> {
                    // Mengumpulkan kondisi yang dipilih
                    StringBuilder selectedConditions = new StringBuilder();
                    for (int i = 0; i < conditions.length; i++) {
                        if (checkedConditions[i]) {
                            if (selectedConditions.length() > 0) selectedConditions.append(", ");
                            selectedConditions.append(conditions[i]);
                        }
                    }
                    // Set text di passengerConditionButton
                    passengerConditionButton.setText(selectedConditions.length() > 0 ? selectedConditions.toString() : "Kondisi Penumpang");
                })
                .setNegativeButton("Batal", null)
                .show();

    }
}
