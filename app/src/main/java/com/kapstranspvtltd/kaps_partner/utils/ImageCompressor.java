package com.kapstranspvtltd.kaps_partner.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ImageCompressor {
    public static class CompressedImage {
        public final byte[] bytes;
        public final int width;
        public final int height;
        public final int quality;

        public CompressedImage(byte[] bytes, int width, int height, int quality) {
            this.bytes = bytes;
            this.width = width;
            this.height = height;
            this.quality = quality;
        }
    }

    public static CompressedImage compress(Context context, Uri imageUri, int maxSizeInBytes) throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        
        InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
        BitmapFactory.decodeStream(inputStream, null, options);
        inputStream.close();

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, 1024, 1024);
        options.inJustDecodeBounds = false;

        // Read bitmap with inSampleSize set
        inputStream = context.getContentResolver().openInputStream(imageUri);
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
        inputStream.close();

        // Compress bitmap
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int quality = 100;
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);

        // Reduce quality until size is under maxSizeInBytes
        while (outputStream.size() > maxSizeInBytes && quality > 20) {
            outputStream.reset();
            quality -= 10;
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
        }

        byte[] bytes = outputStream.toByteArray();
        outputStream.close();
        
        CompressedImage result = new CompressedImage(
            bytes,
            bitmap.getWidth(),
            bitmap.getHeight(),
            quality
        );

        bitmap.recycle();
        return result;
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
}