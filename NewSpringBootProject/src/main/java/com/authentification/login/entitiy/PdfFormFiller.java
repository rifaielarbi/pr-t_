package com.authentification.login.entitiy;

import com.itextpdf.forms.PdfAcroForm;
import com.itextpdf.forms.fields.PdfFormField;
import com.itextpdf.io.IOException;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;

import java.util.Map;

public class PdfFormFiller {

    public void fillForm(String src, String dest, Map<String, String> fieldValues) throws IOException, java.io.IOException {
        try (PdfReader reader = new PdfReader(src);
             PdfWriter writer = new PdfWriter(dest);
             PdfDocument pdfDoc = new PdfDocument(reader, writer)) {

            PdfAcroForm form = PdfAcroForm.getAcroForm(pdfDoc, true);
            if (form != null) {
                for (Map.Entry<String, String> entry : fieldValues.entrySet()) {
                    String fieldName = entry.getKey();
                    String fieldValue = entry.getValue();
                    PdfFormField field = form.getField(fieldName);
                    if (field != null) {
                        field.setValue(fieldValue);
                    }
                }
            }
        }
    }
}
