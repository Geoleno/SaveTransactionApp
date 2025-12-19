package com.example.savetransactionapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import android.widget.Button;
import android.widget.EditText;
import android.media.Image;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;

public class ScanActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA};

    private PreviewView viewFinder;
    private ImageCapture imageCapture;
    private Button btnCapture;
    private Button btnGallery;
    private EditText etStoreName; // Untuk Nama Toko
    private EditText etAmount;   // Untuk Harga
    private EditText etDate;     // Untuk Tanggal


    @Override
    @androidx.camera.core.ExperimentalGetImage
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        viewFinder = findViewById(R.id.viewFinder);

        btnCapture = findViewById(R.id.btn_capture);


        btnGallery = findViewById(R.id.btn_gallery);

        btnCapture.setOnClickListener(v -> {
            captureAndAnalyze();
        });


        btnGallery.setOnClickListener(v -> {
            Toast.makeText(this, "Fitur Galeri belum dibuat", Toast.LENGTH_SHORT).show();
        });

        etStoreName = findViewById(R.id.et_store_name);
        etAmount = findViewById(R.id.et_amount);
        etDate = findViewById(R.id.et_date);
        // Cek Izin Kamera saat Activity dibuka
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                // 1. Preview (Layar)
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                // 2. ImageCapture (Alat Foto) - INI YANG BARU
                imageCapture = new ImageCapture.Builder().build();

                cameraProvider.unbindAll();

                // Gabungkan Preview + ImageCapture
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

            } catch (ExecutionException | InterruptedException e) {
                Log.e("ScanActivity", "Gagal start kamera", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    // Fungsi helper untuk cek izin
    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    // Menangani jawaban User (Boleh/Tolak izin)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Izin kamera ditolak, fitur scan tidak bisa digunakan.", Toast.LENGTH_SHORT).show();
                finish(); // Tutup activity jika ditolak
            }
        }
    }
    @androidx.camera.core.ExperimentalGetImage
    private void captureAndAnalyze() {
        if (imageCapture == null) return;

        // Ambil Gambar
        imageCapture.takePicture(ContextCompat.getMainExecutor(this), new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy imageProxy) {
                // Gambar berhasil ditangkap, sekarang kita baca!
                analyzeImage(imageProxy);
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Toast.makeText(ScanActivity.this, "Gagal ambil foto", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Fungsi Analisa Teks ML Kit
    @androidx.camera.core.ExperimentalGetImage
    private void analyzeImage(ImageProxy imageProxy) {
        Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());

            // Mulai ML Kit
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    .process(image)
                    .addOnSuccessListener(visionText -> {
                        // Sukses baca teks!
                        processTextResult(visionText);
                        imageProxy.close(); // Wajib tutup biar gak memory leak
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Scan", "Gagal baca teks", e);
                        imageProxy.close();
                    });
        }
    }

    // LOGIKA PINTAR (REGEX) UNTUK TANGGAL & HARGA

    private void processTextResult(Text text) {
        String fullText = text.getText();

        // --- 1. DETEKSI MERCHANT (TOKO) ---
        // Strategi: Ambil baris paling atas yang bukan kata umum nota.
        String merchantName = "";
        outerLoop:
        for (Text.TextBlock block : text.getTextBlocks()) {
            for (Text.Line line : block.getLines()) {
                String txt = line.getText();
                String lowerTxt = txt.toLowerCase();

                // Filter kata-kata teknis di header nota
                if (!lowerTxt.contains("tanggal") &&
                        !lowerTxt.contains("kepada") &&
                        !lowerTxt.contains("nota no") &&
                        txt.length() > 3) {

                    // Cek apakah ini nomor HP/Rekening di awal (misal 0813...)
                    String digitsOnly = lowerTxt.replaceAll("[^0-9]", "");
                    if (digitsOnly.length() > 9) continue;

                    merchantName = txt;
                    break outerLoop; // Ambil yang pertama valid, lalu stop.
                }
            }
        }
        etStoreName.setText(merchantName);


        // --- 2. DETEKSI TANGGAL (SMART DATE - HANDWRITING SUPPORT) ---
        // Strategi: Cari pola "Angka(1-2) Pemandu(spasi/strip/titik) Bulan(Huruf) Pemandu Tahun(2-4)"
        // Regex dibuat lebih fleksibel [\\s.-]* artinya "nol atau lebih spasi/titik/strip"
        String detectedDate = "";
        // Pola untuk "13 Des 2025" atau "13-Des-25"
        String datePatternIndo = "(\\d{1,2}[\\s.-]+[a-zA-Z]{3,}[\\s.-]+\\d{2,4})";
        // Pola cadangan untuk "13/12/2025"
        String datePatternDigit = "(\\d{1,2}[-./]\\d{1,2}[-./]\\d{2,4})";

        Pattern p1 = Pattern.compile(datePatternIndo);
        Matcher m1 = p1.matcher(fullText);

        Pattern p2 = Pattern.compile(datePatternDigit);
        Matcher m2 = p2.matcher(fullText);

        if (m1.find()) {
            detectedDate = m1.group(1); // Prioritas format nama bulan (Des)
        } else if (m2.find()) {
            detectedDate = m2.group(1); // Cadangan format angka (12)
        }

        // Bersihkan hasil (kadang ada spasi berlebih di awal/akhir)
        if (detectedDate != null) {
            etDate.setText(detectedDate.trim());
        } else {
            etDate.setText("");
        }


        // --- 3. DETEKSI HARGA (TOTAL RP) - ANTI REKENING ---
        // Masalah Utama: Nomor rekening BSI (10 digit) dianggap harga terbesar.
        // Solusi: Filter angka > 9 digit, dan prioritaskan baris yang ada kata "TOTAL".

        double priceFromKeyword = 0;
        double maxFallbackPrice = 0;
        boolean keywordFound = false;

        for (Text.TextBlock block : text.getTextBlocks()) {
            for (Text.Line line : block.getLines()) {
                String lineText = line.getText().toLowerCase();
                // Ambil hanya angka murni. Contoh "Rp. 160.000" -> "160000"
                String cleanNumberStr = lineText.replaceAll("[^0-9]", "");

                if (!cleanNumberStr.isEmpty()) {
                    // FILTER PENTING: Abaikan Nomor Rekening/HP (panjang >= 10 digit)
                    if (cleanNumberStr.length() >= 10) continue;

                    try {
                        double currentVal = Double.parseDouble(cleanNumberStr);

                        // Abaikan jika angkanya terlihat seperti tahun (misal 2025)
                        if (currentVal == 2024 || currentVal == 2025) continue;

                        // Strategi A: Prioritas Utama - Cari di baris yang ada kata "Total"/"Jumlah"
                        if (lineText.contains("total") || lineText.contains("jumlah")) {
                            // Jika di baris "Total" ada beberapa angka, ambil yang terbesar di baris itu
                            if (currentVal > priceFromKeyword) {
                                priceFromKeyword = currentVal;
                            }
                            keywordFound = true;
                        }

                        // Strategi B: Cadangan - Simpan angka terbesar yang masuk akal (bukan rekening)
                        if (currentVal > maxFallbackPrice) {
                            maxFallbackPrice = currentVal;
                        }

                    } catch (NumberFormatException e) {
                        // ignore
                    }
                }
            }
        }

        // Keputusan Akhir
        if (keywordFound && priceFromKeyword > 0) {
            // Jika ketemu kata "Total", PASTI pakai angka itu.
            etAmount.setText(String.format("%.0f", priceFromKeyword));
        } else if (maxFallbackPrice > 0) {
            // Jika tidak, pakai angka terbesar yang lolos filter.
            etAmount.setText(String.format("%.0f", maxFallbackPrice));
        } else {
            etAmount.setText("0");
        }

        Toast.makeText(this, "Scan Selesai. Cek data.", Toast.LENGTH_SHORT).show();
    }
}