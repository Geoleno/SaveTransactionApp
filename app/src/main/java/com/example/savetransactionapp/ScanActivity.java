package com.example.savetransactionapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@androidx.camera.core.ExperimentalGetImage
public class ScanActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA};

    // UI Components
    private ViewGroup rootLayout;
    private PreviewView viewFinder;
    private ImageView imgPreview;
    private com.google.android.material.floatingactionbutton.FloatingActionButton btnCapture;
    private com.google.android.material.floatingactionbutton.FloatingActionButton btnGallery;
    private ProgressBar loadingProgress;
    private CardView containerForm;
    private Button btnRetake;
    private Button btnSave;

    // Animation Components
    private ViewGroup overlaySaveAnim;
    private View animCircleExpand;
    private ProgressBar animLoadingSave;
    private TextView animTextSuccess;

    // Inputs
    private EditText etNotes;
    private EditText etAmount;
    private EditText etDate;

    // Logic Variables
    private java.io.File photoFile;
    private ImageCapture imageCapture;
    private ActivityResultLauncher<String> galleryLauncher;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        // Initialize UI Components
        rootLayout = findViewById(R.id.root_layout);
        viewFinder = findViewById(R.id.viewFinder);
        imgPreview = findViewById(R.id.img_preview);
        btnCapture = findViewById(R.id.btn_capture);
        btnGallery = findViewById(R.id.btn_gallery);
        loadingProgress = findViewById(R.id.loading_progress);
        containerForm = findViewById(R.id.container_form);

        etNotes = findViewById(R.id.et_notes);
        etAmount = findViewById(R.id.et_amount);
        etDate = findViewById(R.id.et_date);

        btnRetake = findViewById(R.id.btn_retake);
        btnSave = findViewById(R.id.btn_save);

        // Initialize Animation Components
        overlaySaveAnim = findViewById(R.id.overlay_save_anim);
        animCircleExpand = findViewById(R.id.anim_circle_expand);
        animLoadingSave = findViewById(R.id.anim_loading_save);
        animTextSuccess = findViewById(R.id.anim_text_success);

        // Setup Gallery Launcher
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        processGalleryImage(uri);
                    }
                }
        );

        // Set Click Listeners
        btnCapture.setOnClickListener(v -> captureAndAnalyze());
        btnGallery.setOnClickListener(v -> galleryLauncher.launch("image/*"));
        btnRetake.setOnClickListener(v -> resetCamera());
        btnSave.setOnClickListener(v -> saveTransactionToCloud());

        imgPreview.setOnClickListener(v -> {
            TransitionManager.beginDelayedTransition(rootLayout);
            if (containerForm.getVisibility() == View.VISIBLE) {
                containerForm.setVisibility(View.GONE);
                Toast.makeText(this, "Ketuk lagi untuk edit", Toast.LENGTH_SHORT).show();
            } else {
                containerForm.setVisibility(View.VISIBLE);
            }
        });

        setupDatePicker();

        // Check Camera Permissions
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    private void captureAndAnalyze() {
        if (imageCapture == null) return;

        // Update UI for Loading State
        loadingProgress.setVisibility(View.VISIBLE);
        btnCapture.setVisibility(View.INVISIBLE);
        btnGallery.setVisibility(View.INVISIBLE);

        // Prepare File Output
        java.io.File photoDir = new java.io.File(getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES), "StrukImages");
        if (!photoDir.exists()) photoDir.mkdirs();

        String fileName = "STRUK_" + System.currentTimeMillis() + ".jpg";
        photoFile = new java.io.File(photoDir, fileName);

        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        // Capture Image
        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                android.net.Uri savedUri = android.net.Uri.fromFile(photoFile);
                showResultUI(savedUri);
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                loadingProgress.setVisibility(View.GONE);
                btnCapture.setVisibility(View.VISIBLE);
                btnGallery.setVisibility(View.VISIBLE);
                Toast.makeText(ScanActivity.this, "Gagal mengambil foto: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void processGalleryImage(android.net.Uri imageUri) {
        try {
            java.io.InputStream inputStream = getContentResolver().openInputStream(imageUri);
            java.io.File photoDir = new java.io.File(getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES), "StrukImages");
            if (!photoDir.exists()) photoDir.mkdirs();

            java.io.File destFile = new java.io.File(photoDir, "GALERI_" + System.currentTimeMillis() + ".jpg");

            java.io.FileOutputStream out = new java.io.FileOutputStream(destFile);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
            out.close();
            inputStream.close();

            this.photoFile = destFile;
            showResultUI(android.net.Uri.fromFile(destFile));

        } catch (Exception e) {
            Toast.makeText(this, "Gagal memproses gambar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showResultUI(android.net.Uri uri) {
        TransitionManager.beginDelayedTransition(rootLayout);

        loadingProgress.setVisibility(View.GONE);
        btnCapture.setVisibility(View.GONE);
        btnGallery.setVisibility(View.GONE);

        imgPreview.setVisibility(View.VISIBLE);
        imgPreview.setImageURI(uri);
        viewFinder.setVisibility(View.GONE);

        containerForm.setVisibility(View.VISIBLE);

        analyzeSavedImage(uri);
    }

    private void resetCamera() {
        TransitionManager.beginDelayedTransition(rootLayout);

        imgPreview.setVisibility(View.GONE);
        containerForm.setVisibility(View.GONE);
        viewFinder.setVisibility(View.VISIBLE);

        btnCapture.setVisibility(View.VISIBLE);
        btnGallery.setVisibility(View.VISIBLE);
        loadingProgress.setVisibility(View.GONE);

        etAmount.setText("");
        etDate.setText("");
        etNotes.setText("");
    }

    private void playSuccessAnimation() {
        animLoadingSave.setVisibility(View.GONE);

        animCircleExpand.setVisibility(View.VISIBLE);
        animCircleExpand.setScaleX(1f);
        animCircleExpand.setScaleY(1f);

        animCircleExpand.animate()
                .scaleX(100f)
                .scaleY(100f)
                .setDuration(500)
                .setInterpolator(new android.view.animation.AccelerateInterpolator())
                .withEndAction(() -> {
                    animTextSuccess.animate()
                            .alpha(1f)
                            .setDuration(300)
                            .withEndAction(() -> {
                                new android.os.Handler().postDelayed(() -> {
                                    finish();
                                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                                }, 1500);
                            })
                            .start();
                })
                .start();
    }

    private void saveTransactionToCloud() {
        String notes = etNotes.getText().toString().trim();
        String amountStr = etAmount.getText().toString().trim();
        String date = etDate.getText().toString().trim();

        if (amountStr.isEmpty() || date.isEmpty()) {
            Toast.makeText(this, "Mohon lengkapi Harga dan Tanggal!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (notes.isEmpty()) notes = "Tanpa Catatan";

        if (photoFile == null || !photoFile.exists()) {
            Toast.makeText(this, "Foto belum ada!", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = 0;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) { return; }

        // Start Loading Animation
        overlaySaveAnim.setVisibility(View.VISIBLE);
        animLoadingSave.setVisibility(View.VISIBLE);
        animCircleExpand.setVisibility(View.INVISIBLE);
        animTextSuccess.setAlpha(0f);

        String imagePath = photoFile.getAbsolutePath();

        // FIXED: Parameter order matched with TransactionModel constructor
        TransactionModel newTransaction = new TransactionModel(amount, date, imagePath, notes);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("transactions")
                .add(newTransaction)
                .addOnSuccessListener(documentReference -> {
                    playSuccessAnimation();
                })
                .addOnFailureListener(e -> {
                    overlaySaveAnim.setVisibility(View.GONE);
                    Toast.makeText(this, "Gagal simpan: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setupDatePicker() {
        final Calendar myCalendar = Calendar.getInstance();
        DatePickerDialog.OnDateSetListener dateSetListener = (view, year, monthOfYear, dayOfMonth) -> {
            myCalendar.set(Calendar.YEAR, year);
            myCalendar.set(Calendar.MONTH, monthOfYear);
            myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            String myFormat = "yyyy-MM-dd";
            SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.US);
            etDate.setText(sdf.format(myCalendar.getTime()));
        };

        etDate.setOnClickListener(v -> {
            new DatePickerDialog(ScanActivity.this, dateSetListener,
                    myCalendar.get(Calendar.YEAR),
                    myCalendar.get(Calendar.MONTH),
                    myCalendar.get(Calendar.DAY_OF_MONTH)).show();
        });
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder()
                        .setTargetAspectRatio(androidx.camera.core.AspectRatio.RATIO_4_3)
                        .build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setTargetAspectRatio(androidx.camera.core.AspectRatio.RATIO_4_3)
                        .build();

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
            } catch (ExecutionException | InterruptedException e) {
                Log.e("ScanActivity", "Gagal start kamera", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void analyzeSavedImage(android.net.Uri photoUri) {
        try {
            InputImage image = InputImage.fromFilePath(this, photoUri);
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    .process(image)
                    .addOnSuccessListener(this::processTextResult)
                    .addOnFailureListener(e -> Toast.makeText(this, "Gagal baca teks", Toast.LENGTH_SHORT).show());
        } catch (java.io.IOException e) { e.printStackTrace(); }
    }

    private void processTextResult(Text text) {
        String fullText = text.getText();

        // 1. Detect Notes
        String detectedNotes = "";
        outerLoop:
        for (Text.TextBlock block : text.getTextBlocks()) {
            for (Text.Line line : block.getLines()) {
                String txt = line.getText();
                String lowerTxt = txt.toLowerCase();
                if (!lowerTxt.contains("tanggal") && !lowerTxt.contains("kepada") && !lowerTxt.contains("nota") && txt.length() > 3) {
                    String digitsOnly = lowerTxt.replaceAll("[^0-9]", "");
                    if (digitsOnly.length() > 9) continue;
                    detectedNotes = txt;
                    break outerLoop;
                }
            }
        }
        etNotes.setText(detectedNotes);

        // 2. Detect Date
        String detectedDate = "";
        Pattern p1 = Pattern.compile("(\\d{1,2}[\\s.-]+[a-zA-Z]{3,}[\\s.-]+\\d{2,4})");
        Matcher m1 = p1.matcher(fullText);
        Pattern p2 = Pattern.compile("(\\d{1,2}[-./]\\d{1,2}[-./]\\d{2,4})");
        Matcher m2 = p2.matcher(fullText);

        if (m1.find()) detectedDate = m1.group(1);
        else if (m2.find()) detectedDate = m2.group(1);
        if (detectedDate != null) etDate.setText(detectedDate.trim());

        // 3. Detect Amount
        double priceFromKeyword = 0;
        double maxFallbackPrice = 0;
        boolean keywordFound = false;

        for (Text.TextBlock block : text.getTextBlocks()) {
            for (Text.Line line : block.getLines()) {
                String lineText = line.getText().toLowerCase();
                String cleanNumberStr = lineText.replaceAll("[^0-9]", "");
                if (!cleanNumberStr.isEmpty()) {
                    if (cleanNumberStr.length() >= 10) continue;
                    try {
                        double currentVal = Double.parseDouble(cleanNumberStr);
                        if (currentVal == 2024 || currentVal == 2025 || currentVal == 2026) continue;
                        if (lineText.contains("total") || lineText.contains("jumlah")) {
                            if (currentVal > priceFromKeyword) priceFromKeyword = currentVal;
                            keywordFound = true;
                        }
                        if (currentVal > maxFallbackPrice) maxFallbackPrice = currentVal;
                    } catch (NumberFormatException e) { }
                }
            }
        }
        if (keywordFound && priceFromKeyword > 0) etAmount.setText(String.format("%.0f", priceFromKeyword));
        else if (maxFallbackPrice > 0) etAmount.setText(String.format("%.0f", maxFallbackPrice));
        else etAmount.setText("0");
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) startCamera();
            else {
                Toast.makeText(this, "Izin kamera ditolak.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}