package com.creator.RetailFlow.product.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.EAN13Writer;
import com.google.zxing.client.j2se.MatrixToImageWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

public class BarcodeImageGenerator {

    public static byte[] generatePng(String ean13Value) throws IOException {
        EAN13Writer writer = new EAN13Writer();
        BitMatrix matrix = writer.encode(
                ean13Value,
                BarcodeFormat.EAN_13,
                300,
                150,
                Map.of(EncodeHintType.MARGIN, 10)
        );

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", outputStream);
        return outputStream.toByteArray();
    }
}