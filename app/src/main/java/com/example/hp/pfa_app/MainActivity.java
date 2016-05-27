package com.example.hp.pfa_app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfImportedPage;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfWriter;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Scalar;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;
import it.sephiroth.android.library.imagezoom.ImageViewTouchBase;
import it.sephiroth.android.library.imagezoom.graphics.FastBitmapDrawable;


public class MainActivity extends Activity {
    private final static String DEBUG_TAG = "MainActivity";
    static Rectangle pagesize;
    private boolean openCVLoaded = false;
    public static int i=0;
    private ImageViewTouch sourceImageView;
    //private ImageViewTouch destinationImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(DEBUG_TAG, "onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set up touch-enabled image views. They cannot be fully configured with XML.
        sourceImageView = (ImageViewTouch) findViewById(R.id.source_image_view);
        sourceImageView.setDisplayType(ImageViewTouchBase.DisplayType.FIT_IF_BIGGER);

        Intent intent = getIntent();
        if (intent.getBooleanExtra(CameraActivity.EXTRA_PHOTO, false)) {
            Log.d(DEBUG_TAG, "Received a photo from camera.");

            Bitmap bitmap = PhotoHolder.getInstance().get();
            PhotoHolder.getInstance().clean();

            bitmap = resizeImageToShow(bitmap);

            Log.d(DEBUG_TAG, "Showing the photo from camera.");
            sourceImageView.setImageBitmap(bitmap);

            // Clear destination image.
            //destinationImageView.setImageResource(android.R.color.transparent);
        }
        // Do not use ImageViewTouch#setImageResource. Otherwise its getDrawable returns
        // BitmapDrawable that is incompatible with FastBitmapDrawable.
        //Bitmap sampleBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.unbeaten_tracks);
        //sourceImageView.setImageBitmap(sampleBitmap);

        /*destinationImageView = (ImageViewTouch) findViewById(R.id.destination_image_view);
        destinationImageView.setDisplayType(ImageViewTouchBase.DisplayType.FIT_IF_BIGGER);*/
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Initialize OpenCV.
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, openCVLoaderCallback);
    }

   /* ,,,,,,,,,,,,,,, @Override
   protected void onNewIntent(Intent intent) {
        Log.d(DEBUG_TAG, "New intent has come.");

        super.onNewIntent(intent);

        if (intent.getBooleanExtra(CameraActivity.EXTRA_PHOTO, false)) {
            Log.d(DEBUG_TAG, "Received a photo from camera.");

            Bitmap bitmap = PhotoHolder.getInstance().get();
            PhotoHolder.getInstance().clean();

            bitmap = resizeImageToShow(bitmap);

            Log.d(DEBUG_TAG, "Showing the photo from camera.");
            sourceImageView.setImageBitmap(bitmap);

            // Clear destination image.
            //destinationImageView.setImageResource(android.R.color.transparent);
        }
    }*/

    // ImageView cannot show too large image.
    private Bitmap resizeImageToShow(Bitmap bitmap) {
        final float LIMIT = 2048f;

        if (bitmap.getWidth() <= LIMIT && bitmap.getHeight() <= LIMIT) {
            return bitmap;
        }

        double widthRatio = bitmap.getWidth() / LIMIT;
        double heightRatio = bitmap.getHeight() / LIMIT;

        double ratio = Math.max(widthRatio, heightRatio);

        int resizedWidth = (int)(bitmap.getWidth() / ratio);
        int resizedHeight = (int)(bitmap.getHeight() / ratio);

        Log.d(DEBUG_TAG, String.format("ResizTouching image to %d %d.", resizedWidth, resizedHeight));

        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
    }

    public void onPhotoButtonClick(View view) {
        Intent intent = new Intent(this, CameraActivity.class);
        startActivity(intent);
    }
    public void onSaveClick(View view) {

        i++;
        FastBitmapDrawable drawable = (FastBitmapDrawable) sourceImageView.getDrawable();
        Bitmap bmp = drawable.getBitmap();

        String file_path = Environment.getExternalStorageDirectory().getAbsolutePath() +
                "/Scan Rectifier";
        File dir = new File(file_path);
        if(!dir.exists())
            dir.mkdirs();
        File file = new File(dir, "image"+Integer.toString(i) + ".jpg");
        try{
        FileOutputStream fOut = new FileOutputStream(file);
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, fOut);

        fOut.flush();
        fOut.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        try {
            Image image = Image.getInstance(file_path+"/image"+Integer.toString(i)+".jpg");
            Rectangle pagesize = new Rectangle(image.getScaledWidth(),image.getScaledHeight());
            Document document = new Document(pagesize);
            File file1 = new File(dir, "doc"+Integer.toString(i) + ".pdf");
            PdfWriter.getInstance(document, new FileOutputStream(file1));
            document.open();



            image.scaleToFit(image.getScaledWidth(),image.getScaledHeight());
            image.setAbsolutePosition(0, 0);
            document.add(image);

            document.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }



        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(R.layout.save_layout, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                this);

        // set prompts.xml to alertdialog builder
        alertDialogBuilder.setView(promptsView);

        final EditText userInput = (EditText) promptsView
                .findViewById(R.id.docname);


        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // get user input and set it to result
                                // edit text
                                String file_path = Environment.getExternalStorageDirectory().getAbsolutePath() +
                                        "/Scan Rectifier";
                                File dir = new File(file_path);
                                //************************************
                                List<InputStream> list = new ArrayList<InputStream>();
                                try {
                                    // Source pdfs
                                    for (int j = 1; j <= i; j++) {
                                        File file = new File(dir, "doc" + j + ".pdf");
                                        // list.add(new FileInputStream(new File(dir, "doc1.pdf")));
                                        list.add(new FileInputStream(file));
                                    }
                                    // Resulting pdf
                                    OutputStream out = new FileOutputStream(new File(dir, userInput.getText() + ".pdf"));
                                    doMerge(list, out);
                                    for (int j = 1; j <= i; j++) {
                                        File file = new File(dir, "doc" + j + ".pdf");
                                        // list.add(new FileInputStream(new File(dir, "doc1.pdf")));
                                       // list.add(new FileInputStream(file));
                                        file.delete();
                                    }
                                    for (int j = 1; j <= i; j++) {
                                        File file = new File(dir, "image" + j + ".jpg");
                                        File file1 = new File(dir, userInput.getText().toString()+ j + ".jpg");
                                        // list.add(new FileInputStream(new File(dir, "doc1.pdf")));
                                        // list.add(new FileInputStream(file));
                                        file.renameTo(file1);
                                    }
                                    i=0;
                                } catch (FileNotFoundException e) {
                                    e.printStackTrace();
                                } catch (DocumentException e) {
                                    e.printStackTrace();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }





                                /*for (int j = 1; j < i+1; j++) {


                                    File file = new File(dir, "image"+Integer.toString(j)+".png");
                                    File file1 = new File(dir, userInput.getText() + Integer.toString(j) + ".png");

                                    file.renameTo(file1);
                                }*/
                                Context context = getApplicationContext();
                                CharSequence text = "Document Saved!";
                                int duration = Toast.LENGTH_SHORT;

                                Toast toast = Toast.makeText(context, text, duration);
                                toast.show();
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();


    }

    public static void doMerge(List<InputStream> list, OutputStream outputStream)
            throws DocumentException, IOException {
        Document document = new Document(pagesize);
        PdfWriter writer = PdfWriter.getInstance(document, outputStream);
        document.open();
        PdfContentByte cb = writer.getDirectContent();

        for (InputStream in : list) {
            PdfReader reader = new PdfReader(in);
            for (int i = 1; i <= reader.getNumberOfPages(); i++) {
                document.newPage();
                //import the page from source pdf
                PdfImportedPage page = writer.getImportedPage(reader, i);
                //add the page to the destination pdf
                cb.addTemplate(page, 0, 0);
            }
        }

        outputStream.flush();
        document.close();
        outputStream.close();
    }




    public void onNextButtonClick(View view){
        i++;
        FastBitmapDrawable drawable = (FastBitmapDrawable) sourceImageView.getDrawable();
        Bitmap bmp = drawable.getBitmap();

        String file_path = Environment.getExternalStorageDirectory().getAbsolutePath() +
                "/Scan Rectifier";
        File dir = new File(file_path);
        if(!dir.exists())
            dir.mkdirs();
        File file = new File(dir, "image"+Integer.toString(i) + ".jpg");
        try{
            FileOutputStream fOut = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
            fOut.flush();
            fOut.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        try {
            Image image = Image.getInstance(file_path+"/image"+Integer.toString(i)+".jpg");
            pagesize = new Rectangle(image.getScaledWidth(),image.getScaledHeight());
            Document document = new Document(pagesize);
            File file1 = new File(dir, "doc"+Integer.toString(i) + ".pdf");
            PdfWriter.getInstance(document, new FileOutputStream(file1));
            document.open();



            image.scaleToFit(image.getScaledWidth(),image.getScaledHeight());
            image.setAbsolutePosition(0,0);
            document.add(image);

            document.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }

        // open Camera
        Intent intent = new Intent(this, CameraActivity.class);
        startActivity(intent);


    }

    public void onRectifyButtonClick(View view) {
        if (!openCVLoaded) {
            Toast.makeText(this, "OpenCV is not yet loaded.", Toast.LENGTH_LONG).show();
            return;
        }

        FloatingActionButton rectifyButton = (FloatingActionButton) findViewById(R.id.rectify_button);
        rectifyButton.setEnabled(false);

        // Get the bitmap from the image view.
        // Notice that ImageViewTouch uses FastBitmapDrawable that does not inherit BitmapDrawable.
        FastBitmapDrawable drawable = (FastBitmapDrawable) sourceImageView.getDrawable();
        Bitmap bitmap = drawable.getBitmap();

        // Create an OpenCV mat from the bitmap.
        Mat srcMat = ImageUtils.bitmapToMat(bitmap);

        // Find the largest rectangle.
        // Find image views.
        RectFinder rectFinder = new RectFinder(0.2, 0.98);
        MatOfPoint2f rectangle = rectFinder.findRectangle(srcMat);

        if (rectangle == null) {
            Toast.makeText(this, "No rectangles were found.", Toast.LENGTH_LONG).show();
            rectifyButton.setEnabled(true);
            return;
        }

        // Transform the rectangle.
        PerspectiveTransformation perspective = new PerspectiveTransformation();
        Mat dstMat = perspective.transform(srcMat, rectangle);

        // Create a bitmap from the result mat.
        Bitmap resultBitmap = ImageUtils.matToBitmap(dstMat);
        Log.d(DEBUG_TAG, String.format("Result bitmap: %d %d", resultBitmap.getWidth(), resultBitmap.getHeight()));

        // Show the result bitmap on the destination image view.
        sourceImageView.setImageBitmap(resultBitmap);

        rectifyButton.setEnabled(true);
    }
/*
    public void onMaskButtonClick(View view) {
        Log.d(DEBUG_TAG, "Masking image.");

        FastBitmapDrawable drawable = (FastBitmapDrawable) destinationImageView.getDrawable();
        Bitmap bitmap = drawable.getBitmap();

        maskBitmap(bitmap, 0.03f, 0.02f, 0.45f, 0.32f);

        destinationImageView.setImageBitmap(bitmap);
    }*/

    /*private void maskBitmap(Bitmap bitmap, float xRatio, float yRatio, float widthRatio, float heightRatio) {
        Canvas canvas = new Canvas(bitmap);

        Paint blackFill = new Paint();
        blackFill.setColor(Color.BLACK);
        blackFill.setStyle(Paint.Style.FILL);

        float left = bitmap.getWidth() * xRatio;
        float top = bitmap.getHeight() * yRatio;
        float right = bitmap.getWidth() * (xRatio + widthRatio);
        float bottom = bitmap.getHeight() * (yRatio + heightRatio);

        canvas.drawRect(left, top, right, bottom, blackFill);
    }*/

    private BaseLoaderCallback openCVLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            if (status != LoaderCallbackInterface.SUCCESS) {
                Log.e(DEBUG_TAG, "Failed to load OpenCV.");
                super.onManagerConnected(status);
                return;
            }

            openCVLoaded = true;
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(DEBUG_TAG, "onCreateOptionsMenu");

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(DEBUG_TAG, "onOptionsItemSelected");

        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
